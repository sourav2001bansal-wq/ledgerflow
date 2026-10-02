package dev.ledgerflow;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class Api {
    private Api() { }

    public record ReconciliationResponse(
            UUID walletId,
            BigDecimal storedBalance,
            BigDecimal ledgerBalance,
            BigDecimal difference,
            boolean matches
    ) { }

    public record CreateWalletRequest(
        @NotBlank @Size(max = 80) String owner,
        @NotNull @DecimalMin("0.00") @DecimalMax("1000000000.00") @Digits(integer = 10, fraction = 2) BigDecimal initialBalance) { }
    public record TransferRequest(
        @NotNull UUID requestId, @NotNull UUID fromWalletId, @NotNull UUID toWalletId,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000000.00") @Digits(integer = 10, fraction = 2) BigDecimal amount) { }
    public record WalletResponse(UUID id, String owner, String currency, BigDecimal balance, Instant createdAt) {
        static WalletResponse from(Wallet w) { return new WalletResponse(w.getId(), w.getOwner(), "INR", w.getBalance(), w.getCreatedAt()); }
    }
    public record TransferResponse(UUID id, UUID fromWalletId, UUID toWalletId, String currency, BigDecimal amount, String status, Instant createdAt) {
        static TransferResponse from(Transfer t) { return new TransferResponse(t.getId(), t.getFromWalletId(), t.getToWalletId(), "INR", t.getAmount(), "COMPLETED", t.getCreatedAt()); }
    }
    public record LedgerResponse(UUID id, UUID journalId, UUID walletId, String account, BigDecimal delta, String kind, Instant createdAt) {
        static LedgerResponse from(LedgerEntry e) { return new LedgerResponse(e.getId(), e.getJournalId(), e.getWalletId(), e.getAccount(), e.getDelta(), e.getKind(), e.getCreatedAt()); }
    }
    public record LedgerPage(List<LedgerResponse> entries, int page, int size, long totalElements) { }
    public record ErrorResponse(String code, String message, Instant timestamp) { }
}
