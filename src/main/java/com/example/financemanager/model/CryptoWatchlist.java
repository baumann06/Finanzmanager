package com.example.financemanager.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

// Diese Klasse repräsentiert eine Krypto-Watchlist, also eine Sammlung von beobachteten oder gehandelten Kryptowährungen
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Ignoriere technische Hibernate-Felder bei der JSON-Ausgabe
public class CryptoWatchlist {

    // Eindeutige ID (automatisch generiert)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kürzel der Kryptowährung, z. B. BTC
    private String symbol;

    // Vollständiger Name, z. B. Bitcoin
    private String name;

    // Notizen zur Kryptowährung
    private String notes;

    // Typ der Währung (z. B. Coin oder Token)
    private String type;

    // Zeitpunkt der Erstellung
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Liste aller Transaktionen, die zu dieser Watchlist gehören
    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // Ermöglicht JSON-Ausgabe (Gegenstück zu @JsonBackReference)
    private List<CryptoTransaction> transactions;

    // === KONSTRUKTOREN ===

    // Leerer Konstruktor, setzt Erstellungszeitpunkt auf jetzt
    public CryptoWatchlist() {
        this.createdAt = LocalDateTime.now();
    }

    // Konstruktor zum Anlegen einer Watchlist mit Symbol, Name und Typ
    public CryptoWatchlist(String symbol, String name, String type) {
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    // === BERECHNETE FELDER ===

    /**
     * Gibt den insgesamt investierten Betrag aus allen BUY-Transaktionen zurück
     */
    @JsonProperty("investedAmount")
    public BigDecimal getInvestedAmount() {
        if (transactions == null || transactions.isEmpty()) return BigDecimal.ZERO;

        return transactions.stream()
                .filter(t -> t.getTransactionType() == CryptoTransaction.TransactionType.BUY)
                .map(t -> t.getAmount().multiply(t.getPricePerUnit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gibt die gesamte gehaltene Menge (Käufe - Verkäufe) zurück
     */
    @JsonProperty("totalHoldings")
    public BigDecimal getTotalHoldings() {
        if (transactions == null || transactions.isEmpty()) return BigDecimal.ZERO;

        BigDecimal buyAmount = transactions.stream()
                .filter(t -> t.getTransactionType() == CryptoTransaction.TransactionType.BUY)
                .map(CryptoTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sellAmount = transactions.stream()
                .filter(t -> t.getTransactionType() == CryptoTransaction.TransactionType.SELL)
                .map(CryptoTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return buyAmount.subtract(sellAmount).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Durchschnittlicher Kaufpreis über alle BUY-Transaktionen
     */
    @JsonProperty("averageBuyPrice")
    public BigDecimal getAverageBuyPrice() {
        if (transactions == null || transactions.isEmpty()) return BigDecimal.ZERO;

        List<CryptoTransaction> buyTransactions = transactions.stream()
                .filter(t -> t.getTransactionType() == CryptoTransaction.TransactionType.BUY)
                .toList();

        if (buyTransactions.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalValue = buyTransactions.stream()
                .map(t -> t.getAmount().multiply(t.getPricePerUnit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = buyTransactions.stream()
                .map(CryptoTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return totalValue.divide(totalAmount, 8, RoundingMode.HALF_UP);
    }

    /**
     * Gibt an, ob eine Investition (Kauf) getätigt wurde
     */
    @JsonProperty("hasInvestment")
    public boolean getHasInvestment() {
        return getInvestedAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    // === STANDARD GETTER UND SETTER ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<CryptoTransaction> getTransactions() { return transactions; }

    // === LIFECYCLE-METHODE ===

    // Wird aufgerufen, bevor das Objekt gespeichert wird
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // === HILFSMETHODEN ===

    @Override
    public String toString() {
        return "CryptoWatchlist{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", notes='" + notes + '\'' +
                ", createdAt=" + createdAt +
                ", investedAmount=" + getInvestedAmount() +
                ", totalHoldings=" + getTotalHoldings() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CryptoWatchlist that = (CryptoWatchlist) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
