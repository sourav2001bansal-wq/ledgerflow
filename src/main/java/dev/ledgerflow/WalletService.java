package dev.ledgerflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.*;

@Service
public class WalletService {
    private final WalletRepository wallets;
    private final TransferRepository transfers;
    private final LedgerRepository ledger;
    public WalletService(WalletRepository wallets, TransferRepository transfers, LedgerRepository ledger) {
        this.wallets = wallets; this.transfers = transfers; this.ledger = ledger;
    }
    @Transactional
    public Api.WalletResponse create(Api.CreateWalletRequest request) {
        Wallet wallet = wallets.save(new Wallet(request.owner(), request.initialBalance().setScale(2)));
        if (wallet.getBalance().signum() > 0) {
            UUID journal = UUID.randomUUID();
            ledger.saveAll(List.of(new LedgerEntry(journal, wallet.getId(), wallet.getBalance(), "OPENING"),
                new LedgerEntry(journal, null, wallet.getBalance().negate(), "OPENING")));
        }
        return Api.WalletResponse.from(wallet);
    }
    @Transactional(readOnly = true)
    public Api.WalletResponse get(UUID id) { return Api.WalletResponse.from(find(id)); }
    @Transactional(readOnly = true)
    public Api.LedgerPage ledger(UUID id, int page, int size) {
        find(id);
        var result = ledger.findByWalletId(id, PageRequest.of(page, size, Sort.by("createdAt").ascending().and(Sort.by("id"))));
        return new Api.LedgerPage(result.stream().map(Api.LedgerResponse::from).toList(), page, size, result.getTotalElements());
    }
    @Transactional
    public Api.TransferResponse transfer(Api.TransferRequest request) {
        if (request.fromWalletId().equals(request.toWalletId()))
            throw new DomainException("SAME_WALLET", "Source and destination must differ", HttpStatus.BAD_REQUEST);
        var existing = transfers.findById(request.requestId());
        if (existing.isPresent()) return replay(existing.get(), request);
        // Acquire both wallet locks in the same order for every request, including reverse transfers.
        List<UUID> ids = new ArrayList<>(List.of(request.fromWalletId(), request.toWalletId()));
        ids.sort(Comparator.naturalOrder());
        Map<UUID, Wallet> locked = new HashMap<>();
        for (UUID id : ids) locked.put(id, wallets.findLocked(id).orElseThrow(() -> missing(id)));
        // Another request may have committed while we waited for a wallet lock.
        existing = transfers.findById(request.requestId());
        if (existing.isPresent()) return replay(existing.get(), request);
        Wallet from = locked.get(request.fromWalletId()), to = locked.get(request.toWalletId());
        BigDecimal amount = request.amount().setScale(2);
        if (from.getBalance().compareTo(amount) < 0)
            throw new DomainException("INSUFFICIENT_FUNDS", "Source wallet has insufficient funds", HttpStatus.UNPROCESSABLE_ENTITY);
        from.debit(amount); to.credit(amount);
        Transfer transfer = transfers.save(new Transfer(request));
        ledger.saveAll(List.of(new LedgerEntry(transfer.getId(), from.getId(), amount.negate(), "TRANSFER"),
            new LedgerEntry(transfer.getId(), to.getId(), amount, "TRANSFER")));
        // Hibernate dirty checking persists both balances; all writes share this transaction.
        transfers.flush();
        return Api.TransferResponse.from(transfer);
    }
    private Api.TransferResponse replay(Transfer transfer, Api.TransferRequest request) {
        if (!transfer.matches(request))
            throw new DomainException("IDEMPOTENCY_CONFLICT", "Request ID was already used for different transfer details", HttpStatus.CONFLICT);
        return Api.TransferResponse.from(transfer);
    }
    @Transactional
    public Api.ReconciliationResponse reconcile(UUID walletId) {
        Wallet wallet = wallets.findLocked(walletId)
                .orElseThrow(() -> missing(walletId));

        BigDecimal ledgerBalance = ledger.sumDeltaByWalletId(walletId);

        if (ledgerBalance == null) {
            ledgerBalance = BigDecimal.ZERO;
        }

        BigDecimal storedBalance = wallet.getBalance();
        BigDecimal difference = storedBalance.subtract(ledgerBalance);
        boolean matches = storedBalance.compareTo(ledgerBalance) == 0;

        return new Api.ReconciliationResponse(
                walletId,
                storedBalance,
                ledgerBalance,
                difference,
                matches
        );
    }
    private Wallet find(UUID id) { return wallets.findById(id).orElseThrow(() -> missing(id)); }

    private DomainException missing(UUID id) { return new DomainException("WALLET_NOT_FOUND", "Wallet not found: " + id, HttpStatus.NOT_FOUND); }
}
