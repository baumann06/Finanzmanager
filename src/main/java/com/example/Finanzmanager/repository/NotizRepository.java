package com.example.Finanzmanager.repository;

import com.example.Finanzmanager.model.UserNotiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotizRepository extends JpaRepository<UserNotiz, Long> {
    List<UserNotiz> findByCryptoEntryId(Long cryptoEntryId);
}