package com.example.Finanzmanager.controller;

import com.example.Finanzmanager.model.Kategorie;
import com.example.Finanzmanager.model.Buchung;
import com.example.Finanzmanager.service.ExchangeRateService;
import com.example.Finanzmanager.service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*") // Für Entwicklung, im Produktivsystem einschränken
public class FinanzController {

    private final TransactionService buchungService;
    private final ExchangeRateService exchangeRateService;

    public FinanzController(TransactionService buchungService, ExchangeRateService exchangeRateService) {
        this.buchungService = buchungService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/transactions")
    public List<Buchung> getAllBuchungen() {
        return buchungService.getAllTransactions();
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Buchung> getBuchungById(@PathVariable Long id) {
        Optional<Buchung> buchung = buchungService.getTransactionById(id);
        return buchung.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/transactions")
    public ResponseEntity<?> saveBuchung(@RequestBody Buchung buchung) {
        try {
            Buchung saved = buchungService.saveTransaction(buchung);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Speichern der Buchung: " + e.getMessage());
        }
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<?> updateBuchung(
            @PathVariable Long id,
            @RequestBody Buchung buchung) {
        try {
            Optional<Buchung> existing = buchungService.getTransactionById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            buchung.setId(id);
            Buchung updated = buchungService.saveTransaction(buchung);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Aktualisieren der Buchung: " + e.getMessage());
        }
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteBuchung(@PathVariable Long id) {
        try {
            if (buchungService.getTransactionById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            buchungService.deleteTransaction(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Löschen der Buchung: " + e.getMessage());
        }
    }

    @GetMapping("/transactions/category/{categoryId}")
    public List<Buchung> getBuchungenByKategorie(@PathVariable Long categoryId) {
        return buchungService.getTransactionsByCategory(categoryId);
    }

    @GetMapping("/transactions/month")
    public List<Buchung> getBuchungenByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return buchungService.getTransactionsByMonth(year, month);
    }

    @GetMapping("/transactions/currency/{currency}")
    public List<Buchung> getBuchungenByCurrency(@PathVariable String currency) {
        return buchungService.getTransactionsByCurrency(currency);
    }

    @GetMapping("/summary/period")
    public ResponseEntity<?> getSumForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "EUR") String currency) {
        try {
            double sum = buchungService.getSumForPeriod(start, end, currency);
            return ResponseEntity.ok(Map.of("sum", sum, "currency", currency));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler bei der Berechnung: " + e.getMessage());
        }
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<?> getMonthlySummary(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "EUR") String currency) {
        try {
            if (year == 0) {
                year = Year.now().getValue();
            }

            Map<String, Double> summary = buchungService.getMonthlySummary(year, currency);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler bei der Berechnung: " + e.getMessage());
        }
    }

    @GetMapping("/summary/category")
    public ResponseEntity<?> getCategorySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "EUR") String currency) {
        try {
            Map<String, Double> summary = buchungService.getCategorySummary(start, end, currency);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler bei der Berechnung: " + e.getMessage());
        }
    }

    @GetMapping("/categories")
    public List<Kategorie> getAllKategorien() {
        return buchungService.getAllCategories();
    }

    @GetMapping("/exchange-rates")
    public Map<String, Double> getExchangeRates() {
        return exchangeRateService.getAllExchangeRates();
    }

    @GetMapping("/convert")
    public ResponseEntity<?> convertCurrency(
            @RequestParam double amount,
            @RequestParam String from,
            @RequestParam String to) {
        try {
            double converted = exchangeRateService.convertCurrency(amount, from, to);
            return ResponseEntity.ok(Map.of(
                    "amount", amount,
                    "fromCurrency", from,
                    "toCurrency", to,
                    "convertedAmount", converted
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler bei der Umrechnung: " + e.getMessage());
        }
    }
}
