package com.example.Finanzmanager.model;

import com.example.Finanzmanager.model.Buchung;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Kategorie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String color; // Für die Visualisierung im Frontend

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Buchung> transactions = new ArrayList<>();

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<Buchung> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Buchung> transactions) {
        this.transactions = transactions;
    }
}
