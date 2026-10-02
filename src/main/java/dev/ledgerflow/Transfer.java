package dev.ledgerflow;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {
    // The client-supplied request ID is also the transfer's immutable primary key.
    @Id private UUID id;
    @Column(nullable = false) private UUID fromWalletId;
    @Column(nullable = false) private UUID toWalletId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private Instant createdAt;
    protected Transfer() { }
    Transfer(Api.TransferRequest request) {
        this.id = request.requestId();
        this.fromWalletId = request.fromWalletId();
        this.toWalletId = request.toWalletId();
        this.amount = request.amount().setScale(2);
        this.createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public UUID getId() { return id; }
    public UUID getFromWalletId() { return fromWalletId; }
    public UUID getToWalletId() { return toWalletId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
    boolean matches(Api.TransferRequest request) {
        return fromWalletId.equals(request.fromWalletId()) && toWalletId.equals(request.toWalletId())
            && amount.compareTo(request.amount()) == 0;
    }
}
