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

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }

    public Income getIncome() { return income; }
    public void setIncome(Income income) { this.income = income; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum TransactionType {
        BUY, SELL
    }
}

 /*

@Entity
public class CryptoTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String type; // BUY, SELL
    private Double amount;
    private Double price;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "watchlist_id")
    @JsonBackReference
    private CryptoWatchlist watchlist;

    // Konstruktoren
    public CryptoTransaction() {
        this.createdAt = LocalDateTime.now();
    }

    // Alle Getter und Setter für price, createdAt etc.
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

*/
