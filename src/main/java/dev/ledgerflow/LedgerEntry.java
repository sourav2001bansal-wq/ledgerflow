package dev.ledgerflow;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id private UUID id;
    @Column(nullable = false) private UUID journalId;
    private UUID walletId;
    @Column(nullable = false, length = 64) private String account;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal delta;
    @Column(nullable = false, length = 20) private String kind;
    @Column(nullable = false) private Instant createdAt;
    protected LedgerEntry() { }
    LedgerEntry(UUID journalId, UUID walletId, BigDecimal delta, String kind) {
        this.id = UUID.randomUUID(); this.journalId = journalId; this.walletId = walletId;
        this.account = walletId == null ? "SIMULATED_FUNDING" : "WALLET:" + walletId;
        this.delta = delta; this.kind = kind; this.createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public UUID getId() { return id; }
    public UUID getJournalId() { return journalId; }
    public UUID getWalletId() { return walletId; }
    public String getAccount() { return account; }
    public BigDecimal getDelta() { return delta; }
    public String getKind() { return kind; }
    public Instant getCreatedAt() { return createdAt; }
}
