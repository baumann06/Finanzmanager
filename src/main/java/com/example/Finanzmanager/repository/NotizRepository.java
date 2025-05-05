package com.example.Finanzmanager.repository;

import com.example.Finanzmanager.model.UserNotiz;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotizRepository extends JpaRepository<UserNotiz, Long> {
    List<UserNotiz> findByLehrerId(Long lehrerId);
}