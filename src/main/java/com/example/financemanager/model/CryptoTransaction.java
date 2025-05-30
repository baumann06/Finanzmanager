package com.example.financemanager.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Diese Klasse stellt eine Krypto-Transaktion dar, z. B. einen Kauf oder Verkauf
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Ignoriert technische Details bei der JSON-Ausgabe
public class CryptoTransaction {

    // Eindeutige ID für die Transaktion (wird automatisch generiert)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Verknüpfung zur zugehörigen Watchlist
    @ManyToOne
    @JoinColumn(name = "watchlist_id")
    @JsonBackReference // Verhindert Endlosschleife bei JSON-Ausgabe
    private CryptoWatchlist watchlist;

    // Gibt an, ob es sich um einen Kauf (BUY) oder Verkauf (SELL) handelt
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    // Anzahl der gekauften oder verkauften Einheiten
    @Column(precision = 38, scale = 2)
    private BigDecimal amount;

    // Preis pro Einheit bei der Transaktion
    @Column(precision = 38, scale = 2)
    private BigDecimal pricePerUnit;

    // Gesamtwert der Transaktion (amount * pricePerUnit)
    @Column(precision = 38, scale = 2)
    private BigDecimal totalValue;

    // Datum und Uhrzeit der Transaktion
    private LocalDateTime transactionDate;

    // Verknüpfung zur Ausgabe (z. B. bei Kauf)
    @OneToOne
    @JoinColumn(name = "expense_id")
    private Expense expense;

    // Verknüpfung zur Einnahme (z. B. bei Verkauf)
    @OneToOne
    @JoinColumn(name = "income_id")
    private Income income;

    // Wann die Transaktion erstellt wurde
    private LocalDateTime createdAt;

    // Leerer Konstruktor, setzt automatisch das Transaktionsdatum auf jetzt
    public CryptoTransaction() {
        this.transactionDate = LocalDateTime.now();
    }

    // Konstruktor zum Erstellen einer neuen Transaktion mit Berechnung des Gesamtwerts
    public CryptoTransaction(CryptoWatchlist watchlist, TransactionType transactionType,
                             BigDecimal amount, BigDecimal pricePerUnit) {
        this(); // Ruft den leeren Konstruktor auf
        this.watchlist = watchlist;
        this.transactionType = transactionType;
        this.amount = amount;
        this.pricePerUnit = pricePerUnit;
        this.totalValue = amount.multiply(pricePerUnit);
    }

    // Getter und Setter für alle Felder

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CryptoWatchlist getWatchlist() { return watchlist; }

    public TransactionType getTransactionType() { return transactionType; }

    public BigDecimal getAmount() { return amount; }

    public BigDecimal getPricePerUnit() { return pricePerUnit; }

    public BigDecimal getTotalValue() { return totalValue; }

    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    // Enum für die Transaktionstypen: Kauf oder Verkauf
    public enum TransactionType {
        BUY, SELL
    }
}