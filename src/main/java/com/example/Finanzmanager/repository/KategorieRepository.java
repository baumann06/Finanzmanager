package com.example.Finanzmanager.repository;

import com.example.Finanzmanager.model.Kategorie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KategorieRepository extends JpaRepository<Kategorie, Long> {
    Optional<Kategorie> findByName(String name);
}