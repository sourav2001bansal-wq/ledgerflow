package dev.ledgerflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {
    Page<LedgerEntry> findByWalletId(UUID walletId, Pageable pageable);

    @Query("""
    select sum(e.delta)
    from LedgerEntry e
    where e.walletId = :walletId
    """)
    BigDecimal sumDeltaByWalletId(@Param("walletId") UUID walletId);
}
