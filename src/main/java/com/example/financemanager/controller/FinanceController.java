package com.example.financemanager.controller;

import com.example.financemanager.model.Expense;
import com.example.financemanager.model.Income;
import com.example.financemanager.service.ExternalApiService;
import com.example.financemanager.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "http://localhost:8080")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @Autowired
    private ExternalApiService externalApiService;

    @PostMapping("/expenses")
    public ResponseEntity<Expense> addExpense(@RequestBody Expense expense) {
        return ResponseEntity.ok(financeService.addExpense(expense));
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(financeService.getAllExpenses());
    }

    @PostMapping("/incomes")
    public ResponseEntity<Income> addIncome(@RequestBody Income income) {
        return ResponseEntity.ok(financeService.addIncome(income));
    }

    @GetMapping("/incomes")
    public ResponseEntity<List<Income>> getAllIncomes() {
        return ResponseEntity.ok(financeService.getAllIncomes());
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance() {
        return ResponseEntity.ok(financeService.calculateBalance());
    }

    @GetMapping("/expenses/by-category")
    public ResponseEntity<Map<String, BigDecimal>> getExpensesByCategory() {
        return ResponseEntity.ok(financeService.getExpensesByCategory());
    }

    @GetMapping("/exchange-rate")
    public ResponseEntity<?> getExchangeRate(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency) {
        return ResponseEntity.ok(externalApiService.getExchangeRate(fromCurrency, toCurrency));
    }
}
