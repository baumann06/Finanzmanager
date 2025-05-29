package com.example.financemanager.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CryptoWatchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symbol;
    private String name;
    private String notes;
    private String type;

    // NEUES FELD hinzufügen:
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Konstruktoren
    public CryptoWatchlist() {
        this.createdAt = LocalDateTime.now();
    }

    public CryptoWatchlist(String symbol, String name, String type) {
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    // Bestehende Getter und Setter...
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

    // NEUER Getter/Setter für createdAt:
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CryptoTransaction> transactions;

    public List<CryptoTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<CryptoTransaction> transactions) { this.transactions = transactions; }

    // PrePersist für automatisches Setzen des Timestamps
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "CryptoWatchlist{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", notes='" + notes + '\'' +
                ", createdAt=" + createdAt +
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