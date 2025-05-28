package com.example.financemanager.repository;

import com.example.financemanager.model.CryptoWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CryptoWatchlistRepository extends JpaRepository<CryptoWatchlist, Long> {
    CryptoWatchlist findBySymbol(String symbol);
    boolean existsBySymbol(String symbol);
}