package com.example.Finanzmanager.repository;

import com.example.Finanzmanager.model.Buchung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface BuchungRepository extends JpaRepository<Buchung, Long> {
    List<Buchung> findByCategoryId(Long categoryId);

    List<Buchung> findByDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT t FROM Buchung t WHERE YEAR(t.date) = ?1 AND MONTH(t.date) = ?2")
    List<Buchung> findByYearAndMonth(int year, int month);

    List<Buchung> findByCurrency(String currency);
}