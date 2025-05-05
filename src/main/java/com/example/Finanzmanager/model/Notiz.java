package com.example.Finanzmanager.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Notiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String klasse;

    @ManyToOne
    @JoinColumn(name = "lehrer_id", nullable = false)
    @JsonIgnore
    private KryptoEintrag lehrer;

    // Getter und Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKlasse() {
        return klasse;
    }

    public void setKlasse(String klasse) {
        this.klasse = klasse;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KryptoEintrag getLehrer() {
        return lehrer;
    }

    public void setLehrer(KryptoEintrag lehrer) {
        this.lehrer = lehrer;
    }
}