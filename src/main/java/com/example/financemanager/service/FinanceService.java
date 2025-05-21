package com.example.financemanager.service;

import com.example.financemanager.model.Expense;
import com.example.financemanager.model.Income;
import com.example.financemanager.repository.ExpenseRepository;
import com.example.financemanager.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private ExternalApiService externalApiService;

    public Expense addExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public Income addIncome(Income income) {
        return incomeRepository.save(income);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public List<Income> getAllIncomes() {
        return incomeRepository.findAll();
    }

    public BigDecimal calculateBalance() {
        BigDecimal totalIncome = incomeRepository.findAll().stream()
                .map(Income::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = expenseRepository.findAll().stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalIncome.subtract(totalExpense);
    }

    public Map<String, BigDecimal> getExpensesByCategory() {
        List<Expense> expenses = expenseRepository.findAll();
        Map<String, BigDecimal> categoryTotals = new HashMap<>();

        for (Expense expense : expenses) {
            String category = expense.getCategory();
            BigDecimal currentTotal = categoryTotals.getOrDefault(category, BigDecimal.ZERO);
            categoryTotals.put(category, currentTotal.add(expense.getAmount()));
        }

        return categoryTotals;
    }
}
