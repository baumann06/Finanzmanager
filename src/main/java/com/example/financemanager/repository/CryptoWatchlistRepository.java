package com.example.financemanager.repository;

import com.example.financemanager.model.CryptoWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CryptoWatchlistRepository extends JpaRepository<CryptoWatchlist, Long> {
    CryptoWatchlist findBySymbol(String symbol);
    boolean existsBySymbol(String symbol);

    Optional<CryptoWatchlist> findBySymbolIgnoreCase(String symbol);

    @Query("SELECT c FROM CryptoWatchlist c WHERE UPPER(c.symbol) = UPPER(:symbol)")
    Optional<CryptoWatchlist> findBySymbolIgnoreCaseCustom(@Param("symbol") String symbol);
}
