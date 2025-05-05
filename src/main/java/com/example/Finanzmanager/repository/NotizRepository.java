package com.example.Finanzmanager.repository;

import com.example.Finanzmanager.model.Notiz;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotizRepository extends JpaRepository<Notiz, Long> {
    List<Notiz> findByLehrerId(Long lehrerId);
}