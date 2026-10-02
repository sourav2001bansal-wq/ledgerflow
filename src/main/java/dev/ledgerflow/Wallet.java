package dev.ledgerflow;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id private UUID id;
    @Column(nullable = false, length = 80) private String owner;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal balance;
    @Column(nullable = false) private Instant createdAt;
    protected Wallet() { }
    Wallet(String owner, BigDecimal initialBalance) {
        this.id = UUID.randomUUID();
        this.owner = owner.strip();
        this.balance = initialBalance;
        this.createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public UUID getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
    void debit(BigDecimal amount) { balance = balance.subtract(amount); }
    void credit(BigDecimal amount) { balance = balance.add(amount); }
}
