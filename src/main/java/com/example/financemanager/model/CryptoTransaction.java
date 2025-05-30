package com.example.financemanager.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CryptoTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "watchlist_id")
    @JsonBackReference // Verhindert Zirkularreferenz
    private CryptoWatchlist watchlist;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType; // BUY, SELL

    @Column(precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(precision = 38, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(precision = 38, scale = 2)
    private BigDecimal totalValue;

    private LocalDateTime transactionDate;

    @OneToOne
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @OneToOne
    @JoinColumn(name = "income_id")
    private Income income;
    private LocalDateTime createdAt;

    // Konstruktoren
    public CryptoTransaction() {
        this.transactionDate = LocalDateTime.now();
    }

    public CryptoTransaction(CryptoWatchlist watchlist, TransactionType transactionType,
                             BigDecimal amount, BigDecimal pricePerUnit) {
        this();
        this.watchlist = watchlist;
        this.transactionType = transactionType;
        this.amount = amount;
        this.pricePerUnit = pricePerUnit;
        this.totalValue = amount.multiply(pricePerUnit);
    }

    // Getters und Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CryptoWatchlist getWatchlist() { return watchlist; }
    public void setWatchlist(CryptoWatchlist watchlist) { this.watchlist = watchlist; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public enum TransactionType {
        BUY, SELL
    }
}