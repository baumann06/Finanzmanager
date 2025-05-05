package com.example.Finanzmanager.repository;

import com.example.Finanzmanager.model.KryptoEintrag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KryptoRepository extends JpaRepository<KryptoEintrag, Long> {
}