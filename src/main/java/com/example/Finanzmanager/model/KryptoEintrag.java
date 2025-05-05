package com.example.Finanzmanager.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import java.util.ArrayList;

@Entity
public class KryptoEintrag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String fachgebiet;

    @OneToMany(mappedBy = "lehrer")
    private List<Notiz> schueler = new ArrayList<>();

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

    public String getFachgebiet() {
        return fachgebiet;
    }

    public void setFachgebiet(String fachgebiet) {
        this.fachgebiet = fachgebiet;
    }

    public List<Notiz> getSchueler() {
        return schueler;
    }

    public void setSchueler(List<Notiz> schueler) {
        this.schueler = schueler;
    }
}