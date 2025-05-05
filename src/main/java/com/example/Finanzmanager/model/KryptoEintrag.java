package com.example.Finanzmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class KryptoEintrag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String symbol;
    private Double currentPrice;
    private LocalDateTime timestamp;
    private String priceHistory; // JSON-String mit Preisdaten

    @OneToMany(mappedBy = "cryptoEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserNotiz> notes = new ArrayList<>();

    // Getter und Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPriceHistory() {
        return priceHistory;
    }

    public void setPriceHistory(String priceHistory) {
        this.priceHistory = priceHistory;
    }

    public List<UserNotiz> getNotes() {
        return notes;
    }

    public void setNotes(List<UserNotiz> notes) {
        this.notes = notes;
    }

    // Hilfsmethode zum Hinzufügen von Notizen
    public void addNote(UserNotiz note) {
        notes.add(note);
        note.setCryptoEntry(this);
    }

    public void removeNote(UserNotiz note) {
        notes.remove(note);
        note.setCryptoEntry(null);
    }
}
