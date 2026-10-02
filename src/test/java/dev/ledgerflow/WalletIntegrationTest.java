package dev.ledgerflow;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletIntegrationTest {
    @Autowired WalletService service;
    @Autowired WalletRepository wallets;
    @Autowired TransferRepository transfers;
    @MockitoSpyBean LedgerRepository ledger;
    @Autowired TestRestTemplate http;
    @BeforeEach void clean() {
        reset(ledger);
        ledger.deleteAllInBatch(); transfers.deleteAllInBatch(); wallets.deleteAllInBatch();
    }
    Api.WalletResponse wallet(String name, String amount) {
        return service.create(new Api.CreateWalletRequest(name, new BigDecimal(amount)));
    }
    Api.TransferRequest request(UUID from, UUID to, String amount) {
        return new Api.TransferRequest(UUID.randomUUID(), from, to, new BigDecimal(amount));
    }
    void balance(UUID id, String expected) {
        assertThat(service.get(id).balance()).isEqualByComparingTo(expected);
    }
    @Test void transferMovesFiveHundredAndEveryJournalBalances() {
        var a = wallet("Sourav", "1000.00"); var b = wallet("Friend", "200.00");
        var result = service.transfer(request(a.id(), b.id(), "500.00"));
        balance(a.id(), "500.00"); balance(b.id(), "700.00");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(ledger.count()).isEqualTo(6);
        Map<UUID, BigDecimal> totals = new HashMap<>();
        ledger.findAll().forEach(e -> totals.merge(e.getJournalId(), e.getDelta(), BigDecimal::add));
        assertThat(totals).hasSize(3);
        totals.values().forEach(total -> assertThat(total).isEqualByComparingTo("0"));
        var statement = service.ledger(a.id(), 0, 20);
        assertThat(statement.entries()).hasSize(2);
        assertThat(statement.entries().stream().map(Api.LedgerResponse::delta).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("500");
    }
    @Test void insufficientFundsChangesNothing() {
        var a = wallet("A", "100.00"); var b = wallet("B", "0.00");
        assertThatThrownBy(() -> service.transfer(request(a.id(), b.id(), "500.00"))).isInstanceOf(DomainException.class).hasMessageContaining("insufficient");
        balance(a.id(), "100.00"); balance(b.id(), "0.00");
        assertThat(transfers.count()).isZero(); assertThat(ledger.count()).isEqualTo(2);
    }
    @Test void storageFailureAfterBalanceChangesRollsBackEverything() {
        var a = wallet("A", "1000.00"); var b = wallet("B", "0.00");
        doThrow(new IllegalStateException("Injected ledger write failure")).when(ledger).saveAll(any());
        assertThatThrownBy(() -> service.transfer(request(a.id(), b.id(), "500.00"))).isInstanceOf(IllegalStateException.class);
        balance(a.id(), "1000.00"); balance(b.id(), "0.00");
        assertThat(transfers.count()).isZero(); assertThat(ledger.count()).isEqualTo(2);
    }
    @Test void sameRequestIdReturnsOriginalTransferWithoutAnotherDebit() {
        var a = wallet("A", "1000.00"); var b = wallet("B", "0.00");
        var r = request(a.id(), b.id(), "500.00");
        assertThat(service.transfer(r)).isEqualTo(service.transfer(r));
        balance(a.id(), "500.00"); balance(b.id(), "500.00");
        assertThat(transfers.count()).isEqualTo(1); assertThat(ledger.count()).isEqualTo(4);
    }
    @Test void reusedRequestIdWithDifferentAmountIsRejected() {
        var a = wallet("A", "1000.00"); var b = wallet("B", "0.00");
        var r = request(a.id(), b.id(), "500.00"); service.transfer(r);
        var changed = new Api.TransferRequest(r.requestId(), a.id(), b.id(), new BigDecimal("400.00"));
        assertThatThrownBy(() -> service.transfer(changed)).isInstanceOf(DomainException.class).hasMessageContaining("different");
        balance(a.id(), "500.00"); assertThat(transfers.count()).isEqualTo(1);
    }
    @Test void concurrentRequestsCannotOverdrawOneWallet() throws Exception {
        var a = wallet("A", "1000.00"); var b = wallet("B", "0.00"); var c = wallet("C", "0.00");
        var results = concurrently(request(a.id(), b.id(), "800.00"), request(a.id(), c.id(), "800.00"));
        assertThat(results).containsExactlyInAnyOrder("OK", "INSUFFICIENT_FUNDS");
        balance(a.id(), "200.00");
        assertThat(service.get(b.id()).balance().add(service.get(c.id()).balance())).isEqualByComparingTo("800.00");
        assertThat(transfers.count()).isEqualTo(1);
    }
    @Test void concurrentDuplicateRequestsOnlyTransferOnce() throws Exception {
        var a = wallet("A", "1000.00"); var b = wallet("B", "0.00");
        var r = request(a.id(), b.id(), "500.00");
        assertThat(concurrently(r, r)).containsExactly("OK", "OK");
        balance(a.id(), "500.00"); balance(b.id(), "500.00");
        assertThat(transfers.count()).isEqualTo(1); assertThat(ledger.count()).isEqualTo(4);
    }
    @Test void oppositeDirectionTransfersCompleteWithoutDeadlock() throws Exception {
        var a = wallet("A", "1000.00"); var b = wallet("B", "1000.00");
        assertThat(concurrently(request(a.id(), b.id(), "700.00"), request(b.id(), a.id(), "600.00"))).containsExactly("OK", "OK");
        balance(a.id(), "900.00"); balance(b.id(), "1100.00");
    }
    List<String> concurrently(Api.TransferRequest one, Api.TransferRequest two) throws Exception {
        var pool = Executors.newFixedThreadPool(2); var gate = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (var r : List.of(one, two)) futures.add(pool.submit(() -> {
                if (!gate.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start gate timed out");
                try { service.transfer(r); return "OK"; } catch (DomainException e) { return e.code; }
            }));
            gate.countDown(); return List.of(futures.get(0).get(15, TimeUnit.SECONDS), futures.get(1).get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
    ResponseEntity<String> json(String path, String body) {
        var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        return http.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }
    @Test void httpRejectsInvalidMoneyAndMissingFields() {
        assertThat(json("/api/wallets", "{\"owner\":\"  \",\"initialBalance\":0}").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var a = wallet("A", "1000.00"); var b = wallet("B", "0.00");
        for (String amount : List.of("0", "-1", "0.001", "1000000001", "null")) {
            String body = "{\"requestId\":\""+UUID.randomUUID()+"\",\"fromWalletId\":\""+a.id()+"\",\"toWalletId\":\""+b.id()+"\",\"amount\":"+amount+"}";
            assertThat(json("/api/transfers", body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(json("/api/transfers", "{}").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        balance(a.id(), "1000"); assertThat(transfers.count()).isZero();
    }
    @Test void httpExposesStableErrorsAndLimits() {
        var a = wallet("A", "1000.00");
        assertThat(http.getForEntity("/api/wallets/"+UUID.randomUUID(), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForEntity("/api/wallets/"+a.id()+"/ledger?size=101", String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body="{\"requestId\":\""+UUID.randomUUID()+"\",\"fromWalletId\":\""+a.id()+"\",\"toWalletId\":\""+a.id()+"\",\"amount\":1}";
        var result=json("/api/transfers",body); assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); assertThat(result.getBody()).contains("SAME_WALLET");
    }

    @Test
    void reconciliationMatchesAfterTransfer() {
        var a = wallet("A", "1000.00");
        var b = wallet("B", "200.00");

        service.transfer(request(a.id(), b.id(), "500.00"));

        var result = service.reconcile(a.id());

        assertThat(result.walletId()).isEqualTo(a.id());
        assertThat(result.storedBalance())
                .isEqualByComparingTo("500.00");
        assertThat(result.ledgerBalance())
                .isEqualByComparingTo("500.00");
        assertThat(result.difference())
                .isEqualByComparingTo("0.00");
        assertThat(result.matches()).isTrue();
    }
    @Test
    void reconciliationReportsMismatchWithoutChangingBalance() {
        var wallet = wallet("Mismatch Test", "500.00");

        doReturn(new BigDecimal("450.00"))
                .when(ledger).sumDeltaByWalletId(wallet.id());

        var result = service.reconcile(wallet.id());

        assertThat(result.storedBalance()).isEqualByComparingTo("500");
        assertThat(result.ledgerBalance()).isEqualByComparingTo("450");
        assertThat(result.difference()).isEqualByComparingTo("50");
        assertThat(result.matches()).isFalse();

        balance(wallet.id(), "500.00");
    }
}
