package com.example.financemanager.repository;

import com.example.financemanager.model.CryptoTransaction;
import com.example.financemanager.model.CryptoWatchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CryptoTransactionRepository extends JpaRepository<CryptoTransaction, Long> {
    List<CryptoTransaction> findByWatchlist(CryptoWatchlist watchlist);
    List<CryptoTransaction> findByWatchlistId(Long watchlistId);
}