package com.example.Finanzmanager.service;

import com.example.Finanzmanager.model.Kategorie;
import com.example.Finanzmanager.model.Buchung;
import com.example.Finanzmanager.repository.KategorieRepository;
import com.example.Finanzmanager.repository.BuchungRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final BuchungRepository buchungRepository;
    private final KategorieRepository kategorieRepository;
    private final ExchangeRateService exchangeRateService;

    public TransactionService(
            BuchungRepository buchungRepository,
            KategorieRepository kategorieRepository,
            ExchangeRateService exchangeRateService) {
        this.buchungRepository = buchungRepository;
        this.kategorieRepository = kategorieRepository;
        this.exchangeRateService = exchangeRateService;
    }

    // Alle Buchungen laden
    public List<Buchung> getAllTransactions() {
        return buchungRepository.findAll();
    }

    // Buchung speichern
    public Buchung saveTransaction(Buchung buchung) {
        // Wenn keine Kategorie angegeben, nutze "Sonstiges"
        if (buchung.getCategory() == null) {
            Optional<Kategorie> defaultKategorie = kategorieRepository.findByName("Sonstiges");
            if (defaultKategorie.isPresent()) {
                buchung.setCategory(defaultKategorie.get());
            } else {
                Kategorie neueKategorie = new Kategorie();
                neueKategorie.setName("Sonstiges");
                neueKategorie.setColor("#CCCCCC");
                kategorieRepository.save(neueKategorie);
                buchung.setCategory(neueKategorie);
            }
        }

        // Wenn kein Datum gesetzt, nutze heute
        if (buchung.getDate() == null) {
            buchung.setDate(LocalDate.now());
        }

        return buchungRepository.save(buchung);
    }

    // Buchung nach ID laden
    public Optional<Buchung> getTransactionById(Long id) {
        return buchungRepository.findById(id);
    }

    // Buchung löschen
    public void deleteTransaction(Long id) {
        buchungRepository.deleteById(id);
    }

    // Buchungen nach Kategorie filtern
    public List<Buchung> getTransactionsByCategory(Long kategorieId) {
        return buchungRepository.findByCategoryId(kategorieId);
    }

    // Buchungen nach Monat filtern
    public List<Buchung> getTransactionsByMonth(int jahr, int monat) {
        return buchungRepository.findByYearAndMonth(jahr, monat);
    }

    // Buchungen nach Währung filtern
    public List<Buchung> getTransactionsByCurrency(String waehrung) {
        return buchungRepository.findByCurrency(waehrung);
    }

    // Summe aller Buchungen im angegebenen Zeitraum berechnen
    public double getSumForPeriod(LocalDate start, LocalDate end, String zielwaehrung) {
        List<Buchung> buchungen = buchungRepository.findByDateBetween(start, end);
        return buchungen.stream()
                .mapToDouble(b -> exchangeRateService.convertCurrency(
                        b.getAmount(), b.getCurrency(), zielwaehrung))
                .sum();
    }

    // Monatliche Summen für ein Jahr berechnen
    public Map<String, Double> getMonthlySummary(int jahr, String zielwaehrung) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (int monat = 1; monat <= 12; monat++) {
            YearMonth jahrMonat = YearMonth.of(jahr, monat);
            LocalDate start = jahrMonat.atDay(1);
            LocalDate end = jahrMonat.atEndOfMonth();

            List<Buchung> buchungen = buchungRepository.findByDateBetween(start, end);
            double summe = buchungen.stream()
                    .mapToDouble(b -> exchangeRateService.convertCurrency(
                            b.getAmount(), b.getCurrency(), zielwaehrung))
                    .sum();

            result.put(jahrMonat.toString(), summe);
        }

        return result;
    }

    // Kategorie-Zusammenfassung für ein Zeitfenster berechnen
    public Map<String, Double> getCategorySummary(LocalDate start, LocalDate end, String zielwaehrung) {
        List<Buchung> buchungen = buchungRepository.findByDateBetween(start, end);

        Map<Kategorie, Double> kategorieSummen = new HashMap<>();
        for (Buchung b : buchungen) {
            Kategorie kategorie = b.getCategory();
            double umgerechneterBetrag = exchangeRateService.convertCurrency(
                    b.getAmount(), b.getCurrency(), zielwaehrung);

            kategorieSummen.put(kategorie,
                    kategorieSummen.getOrDefault(kategorie, 0.0) + umgerechneterBetrag);
        }

        return kategorieSummen.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getName(),
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    // Alle Kategorien laden oder Standard-Kategorien erstellen
    public List<Kategorie> getAllCategories() {
        List<Kategorie> kategorien = kategorieRepository.findAll();

        if (kategorien.isEmpty()) {
            createDefaultCategories();
            kategorien = kategorieRepository.findAll();
        }

        return kategorien;
    }

    // Standard-Kategorien erstellen
    private void createDefaultCategories() {
        List<Kategorie> defaultKategorien = new ArrayList<>();

        Kategorie lebensmittel = new Kategorie();
        lebensmittel.setName("Lebensmittel");
        lebensmittel.setColor("#4CAF50");
        defaultKategorien.add(lebensmittel);

        Kategorie miete = new Kategorie();
        miete.setName("Miete");
        miete.setColor("#2196F3");
        defaultKategorien.add(miete);

        Kategorie freizeit = new Kategorie();
        freizeit.setName("Freizeit");
        freizeit.setColor("#FFC107");
        defaultKategorien.add(freizeit);

        Kategorie transport = new Kategorie();
        transport.setName("Transport");
        transport.setColor("#FF5722");
        defaultKategorien.add(transport);

        Kategorie sonstiges = new Kategorie();
        sonstiges.setName("Sonstiges");
        sonstiges.setColor("#9E9E9E");
        defaultKategorien.add(sonstiges);

        kategorieRepository.saveAll(defaultKategorien);
    }
}
