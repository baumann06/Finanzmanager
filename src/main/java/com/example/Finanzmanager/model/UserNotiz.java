package com.example.Finanzmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class UserNotiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String text;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private KryptoEintrag cryptoEntry;

    // Getter und Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public KryptoEintrag getCryptoEntry() {
        return cryptoEntry;
    }

    public void setCryptoEntry(KryptoEintrag cryptoEntry) {
        this.cryptoEntry = cryptoEntry;
    }

    // Vor dem Speichern den Zeitstempel setzen
    @PrePersist
    public void beforeSave() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
