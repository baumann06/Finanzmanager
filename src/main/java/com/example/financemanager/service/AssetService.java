package com.example.financemanager.service;

import com.example.financemanager.dto.WatchlistDto;
import com.example.financemanager.model.CryptoTransaction;
import com.example.financemanager.model.CryptoWatchlist;
import com.example.financemanager.model.Expense;
import com.example.financemanager.repository.CryptoTransactionRepository;
import com.example.financemanager.repository.CryptoWatchlistRepository;
import com.example.financemanager.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service-Klasse für Asset-Management (Kryptowährungen und Aktien).
 * Bietet Funktionalitäten für:
 * - Preisdaten-Abfrage (aktuell und historisch)
 * - Watchlist-Verwaltung
 * - Portfolio-Management und -Analyse
 * - Investment-Tracking
 * - Asset-Suche und -Discovery
 *
 * @author [Your Name]
 * @version 1.0
 * @since 2024
 */
@Service
public class AssetService {

    // ========== DEPENDENCIES ==========

    @Autowired
    private CryptoWatchlistRepository cryptoWatchlistRepository;

    @Autowired
    private ExternalApiService externalApiService;

    @Autowired
    private CryptoTransactionRepository cryptoTransactionRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    // ========== PRICE DATA METHODS ==========

    /**
     * Ruft den aktuellen Preis für ein bestimmtes Asset ab.
     * Unterstützt sowohl Kryptowährungen als auch Aktien.
     *
     * @param symbol Das Asset-Symbol (z.B. "BTC", "AAPL")
     * @param type Der Asset-Typ ("crypto" oder "stock")
     * @param market Der Zielmarkt für die Preisabfrage (Standard: "usd")
     * @return Map mit Preisdaten oder Fehlermeldung
     */
    public Map<String, Object> getAssetWithCurrentPrice(String symbol, String type, String market) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validiere das Asset-Symbol basierend auf dem Typ
            if (!isValidSymbol(symbol, type)) {
                throw new IllegalArgumentException("Invalid symbol '" + symbol + "' for type '" + type + "'");
            }

            Map<String, Object> priceData;
            String targetMarket = market != null ? market : "usd";

            // Behandle verschiedene Asset-Typen unterschiedlich
            if ("crypto".equalsIgnoreCase(type)) {
                Map<String, Object> apiResponse = externalApiService.getCryptoCurrentPrice(symbol, targetMarket);
                priceData = externalApiService.extractCurrentPrice(apiResponse, "crypto", symbol);

                // Normalisiere die Preisdaten (verschiedene APIs verwenden unterschiedliche Feldnamen)
                if (priceData.containsKey("close") && !priceData.containsKey("price")) {
                    priceData.put("price", priceData.get("close"));
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                Map<String, Object> apiResponse = externalApiService.getStockCurrentPrice(symbol);
                priceData = externalApiService.extractCurrentPrice(apiResponse, "stock", symbol);
            } else {
                throw new IllegalArgumentException("Unbekannter Asset-Typ: '" + type + "'");
            }

            // Prüfe ob Preisdaten verfügbar sind
            if (priceData == null || priceData.isEmpty()) {
                throw new RuntimeException("Keine Preisdaten verfügbar für " + symbol);
            }

            result.put("success", true);
            result.put("priceData", priceData);

        } catch (Exception e) {
            // Erstelle detaillierte Fehler-Response
            result.put("success", false);
            result.put("error", "Failed to fetch price for " + symbol + " (" + type + "): " + e.getMessage());
            result.put("errorDetails", getErrorDetails(e));
        }

        return result;
    }

    /**
     * Ruft Preisdaten für multiple Assets gleichzeitig ab.
     * Nützlich für Batch-Operationen und Dashboard-Updates.
     *
     * @param symbols Liste der Asset-Symbole
     * @param type Asset-Typ für alle Symbole
     * @param market Zielmarkt für Preisabfrage
     * @return Map mit Preisdaten für alle Assets und Statistiken
     */
    public Map<String, Object> getMultipleAssetsWithPrices(List<String> symbols, String type, String market) {
        Map<String, Object> assetsData = new HashMap<>();
        int successCount = 0;

        // Verarbeite jedes Symbol einzeln (parallelisierbar in Zukunft)
        for (String symbol : symbols) {
            try {
                Map<String, Object> assetData = getAssetWithCurrentPrice(symbol, type, market);
                assetsData.put(symbol, assetData);

                // Zähle erfolgreiche Abfragen für Statistiken
                if ((Boolean) assetData.get("success")) {
                    successCount++;
                }
            } catch (Exception e) {
                // Fehlerbehandlung pro Asset (ein Fehler stoppt nicht die anderen)
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", e.getMessage());
                errorData.put("success", false);
                assetsData.put(symbol, errorData);
            }
        }

        // Erstelle zusammenfassende Statistiken
        Map<String, Object> result = new HashMap<>();
        result.put("assets", assetsData);
        result.put("totalCount", symbols.size());
        result.put("successCount", successCount);
        result.put("failedCount", symbols.size() - successCount);
        result.put("success", true);
        return result;
    }

    /**
     * Ruft historische Preisdaten für ein Asset ab.
     * Unterstützt sowohl Intraday- als auch Daily-Daten.
     *
     * @param symbol Asset-Symbol
     * @param type Asset-Typ
     * @param market Zielmarkt
     * @param period Zeitraum ("intraday" oder "daily")
     * @param interval Intervall für Intraday-Daten (z.B. "5min")
     * @return Map mit historischen Preisdaten
     */
    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market, String period, String interval) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> apiResponse;
            String targetMarket = market != null ? market : "usd";

            // Wähle entsprechende API-Methode basierend auf Asset-Typ und Zeitraum
            if ("crypto".equalsIgnoreCase(type)) {
                apiResponse = "intraday".equalsIgnoreCase(period)
                        ? externalApiService.getCryptoIntradayData(symbol, targetMarket)
                        : externalApiService.getCryptoDailyData(symbol, targetMarket);
            } else if ("stock".equalsIgnoreCase(type)) {
                apiResponse = "intraday".equalsIgnoreCase(period)
                        ? externalApiService.getStockIntradayData(symbol, interval != null ? interval : "5min")
                        : externalApiService.getStockDailyData(symbol);
            } else {
                throw new IllegalArgumentException("Unbekannter Asset-Typ: " + type);
            }

            result.put("historyData", apiResponse);
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
        }

        return result;
    }

    /**
     * Überladene Methode für historische Preisdaten mit Standardparametern.
     * Verwendet "daily" als Standard-Zeitraum.
     */
    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market) {
        return getAssetPriceHistory(symbol, type, market, "daily", null);
    }

    // ========== WATCHLIST MANAGEMENT ==========

    /**
     * Fügt ein neues Asset zur Watchlist hinzu.
     * Führt Validierung und Duplikatsprüfung durch.
     *
     * @param asset Das CryptoWatchlist-Objekt mit Asset-Informationen
     * @return Gespeichertes CryptoWatchlist-Objekt
     * @throws IllegalArgumentException bei ungültigen Daten oder Duplikaten
     */
    public CryptoWatchlist addToWatchlist(CryptoWatchlist asset) {
        // Basis-Validierung
        if (asset.getSymbol() == null || asset.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol ist erforderlich");
        }

        // Duplikatsprüfung (case-insensitive)
        Optional<CryptoWatchlist> existing = cryptoWatchlistRepository.findBySymbolIgnoreCase(asset.getSymbol().trim());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.getSymbol() + " ist bereits in der Watchlist");
        }

        // Normalisiere Symbol (Großbuchstaben)
        asset.setSymbol(asset.getSymbol().trim().toUpperCase());

        // Auto-Erkennung des Asset-Typs falls nicht angegeben
        if (asset.getType() == null || asset.getType().trim().isEmpty()) {
            asset.setType(detectAssetType(asset.getSymbol()));
        }

        return cryptoWatchlistRepository.save(asset);
    }

    /**
     * Ruft die komplette Watchlist ab.
     *
     * @return Liste aller Watchlist-Einträge
     */
    public List<CryptoWatchlist> getWatchlist() {
        return cryptoWatchlistRepository.findAll();
    }

    /**
     * Ruft die Watchlist als DTOs ab.
     * DTOs verhindern zirkuläre Referenzen bei der JSON-Serialisierung
     * und enthalten berechnete Felder wie investierte Beträge.
     *
     * @return Liste von WatchlistDto-Objekten
     */
    public List<WatchlistDto> getWatchlistDtos() {
        List<CryptoWatchlist> entities = cryptoWatchlistRepository.findAll();

        // Debug-Ausgabe (sollte in Production entfernt werden)
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Watchlist Entities: ");
        System.out.println(entities);

        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Ruft ein einzelnes Watchlist-Item als DTO ab.
     *
     * @param id ID des Watchlist-Items
     * @return WatchlistDto oder null falls nicht gefunden
     */
    public WatchlistDto getWatchlistItemDto(Long id) {
        Optional<CryptoWatchlist> entity = cryptoWatchlistRepository.findById(id);
        return entity.map(this::convertToDto).orElse(null);
    }

    /**
     * Aktualisiert ein bestehendes Watchlist-Item.
     * Nur bestimmte Felder können aktualisiert werden (Name, Notizen).
     *
     * @param id ID des zu aktualisierenden Items
     * @param updatedAsset Objekt mit neuen Werten
     * @return Aktualisiertes CryptoWatchlist-Objekt oder null falls nicht gefunden
     */
    public CryptoWatchlist updateWatchlistItem(Long id, CryptoWatchlist updatedAsset) {
        Optional<CryptoWatchlist> existingOpt = cryptoWatchlistRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return null;
        }

        CryptoWatchlist existing = existingOpt.get();

        // Aktualisiere nur erlaubte Felder (Symbol und Type sind normalerweise unveränderlich)
        if (updatedAsset.getName() != null) {
            existing.setName(updatedAsset.getName().trim());
        }
        if (updatedAsset.getNotes() != null) {
            existing.setNotes(updatedAsset.getNotes());
        }

        return cryptoWatchlistRepository.save(existing);
    }

    /**
     * Konvertiert eine CryptoWatchlist-Entity zu einem DTO.
     * Berechnet dabei investierte Beträge, Durchschnittspreise und weitere Metriken
     * basierend auf zugehörigen Transaktionen.
     *
     * @param entity Die zu konvertierende Entity
     * @return WatchlistDto mit berechneten Feldern
     */
    public WatchlistDto convertToDto(CryptoWatchlist entity) {
        WatchlistDto dto = new WatchlistDto();

        // Basis-Felder kopieren
        dto.setId(entity.getId());
        dto.setSymbol(entity.getSymbol());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());

        // Berechne investierte Beträge und Mengen aus allen Transaktionen
        if (entity.getTransactions() != null && !entity.getTransactions().isEmpty()) {
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;
            int transactionCount = 0;

            // Verarbeite alle Transaktionen (BUY addiert, SELL subtrahiert)
            for (CryptoTransaction transaction : entity.getTransactions()) {
                if (transaction.getTransactionType() == CryptoTransaction.TransactionType.BUY) {
                    totalInvested = totalInvested.add(transaction.getTotalValue() != null ?
                            transaction.getTotalValue() : BigDecimal.ZERO);
                    totalAmount = totalAmount.add(transaction.getAmount() != null ?
                            transaction.getAmount() : BigDecimal.ZERO);
                } else if (transaction.getTransactionType() == CryptoTransaction.TransactionType.SELL) {
                    totalInvested = totalInvested.subtract(transaction.getTotalValue() != null ?
                            transaction.getTotalValue() : BigDecimal.ZERO);
                    totalAmount = totalAmount.subtract(transaction.getAmount() != null ?
                            transaction.getAmount() : BigDecimal.ZERO);
                }
                transactionCount++;
            }

            dto.setInvestedAmount(totalInvested);
            dto.setTotalAmount(totalAmount);
            dto.setTransactionCount(transactionCount);

            // Berechne Durchschnittspreis (nur wenn Menge > 0)
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgPrice = totalInvested.divide(totalAmount, 8, RoundingMode.HALF_UP);
                dto.setAveragePrice(avgPrice);
            }
        } else {
            // Standardwerte für Assets ohne Transaktionen
            dto.setInvestedAmount(BigDecimal.ZERO);
            dto.setTotalAmount(BigDecimal.ZERO);
            dto.setTransactionCount(0);
        }

        return dto;
    }

    /**
     * Entfernt ein Asset aus der Watchlist.
     *
     * TODO: Implementiere Transaktionsprüfung (auskommentiert)
     * um zu verhindern, dass Assets mit Transaktionen gelöscht werden.
     *
     * @param id ID des zu löschenden Assets
     * @return true wenn erfolgreich gelöscht, false wenn nicht gefunden
     */
    public boolean removeFromWatchlist(Long id) {
        if (cryptoWatchlistRepository.existsById(id)) {
            /*
            // Sicherheitsprüfung: Verhindere Löschung wenn Transaktionen existieren
            List<CryptoTransaction> transactions = cryptoTransactionRepository.findByWatchlistId(id);
            if (!transactions.isEmpty()) {
                throw new IllegalStateException("Watchlist-Item kann nicht gelöscht werden, da Transaktionen vorhanden sind");
            }
            */

            cryptoWatchlistRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Ruft die Watchlist mit aktuellen Preisen und berechneten Performance-Metriken ab.
     * Diese Methode kombiniert Watchlist-Daten mit Live-Preisen für eine vollständige Übersicht.
     *
     * @return Map mit angereicherter Watchlist und Statistiken
     */
    public Map<String, Object> getWatchlistWithCurrentPrices() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<WatchlistDto> watchlistDtos = getWatchlistDtos();
            List<Map<String, Object>> enrichedWatchlist = new ArrayList<>();

            // Bearbeite jedes Watchlist-Item
            for (WatchlistDto dto : watchlistDtos) {
                Map<String, Object> enrichedItem = new HashMap<>();
                enrichedItem.put("watchlistItem", dto);

                try {
                    // Hole aktuellen Preis für das Asset
                    Map<String, Object> priceResponse = getAssetWithCurrentPrice(
                            dto.getSymbol(), dto.getType(), "usd");

                    if ((Boolean) priceResponse.get("success")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> priceData = (Map<String, Object>) priceResponse.get("priceData");
                        enrichedItem.put("currentPrice", priceData);

                        // Berechne Performance-Metriken nur wenn Investitionen vorhanden
                        if (dto.getInvestedAmount() != null && dto.getTotalAmount() != null &&
                                dto.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {

                            BigDecimal currentPrice = new BigDecimal(priceData.get("price").toString());
                            BigDecimal currentValue = dto.getTotalAmount().multiply(currentPrice);
                            BigDecimal profit = currentValue.subtract(dto.getInvestedAmount());
                            BigDecimal profitPercentage = profit.divide(dto.getInvestedAmount(), 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"));

                            enrichedItem.put("currentValue", currentValue);
                            enrichedItem.put("profit", profit);
                            enrichedItem.put("profitPercentage", profitPercentage);
                        }
                    } else {
                        // Speichere Preisfehler für einzelne Assets
                        enrichedItem.put("priceError", priceResponse.get("error"));
                    }
                } catch (Exception e) {
                    enrichedItem.put("priceError", "Fehler beim Laden des Preises: " + e.getMessage());
                }

                enrichedWatchlist.add(enrichedItem);
            }

            result.put("watchlist", enrichedWatchlist);
            result.put("count", enrichedWatchlist.size());
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", "Fehler beim Laden der Watchlist: " + e.getMessage());
            result.put("success", false);
        }

        return result;
    }

    // ========== PORTFOLIO MANAGEMENT ==========

    /**
     * Berechnet den Portfolio-Prozentsatz für ein bestimmtes Watchlist-Item.
     * Zeigt an, welchen Anteil dieses Asset am Gesamtportfolio hat.
     *
     * @param watchlistId ID des Watchlist-Items
     * @return Map mit Portfolio-Statistiken
     */
    public Map<String, Object> getPortfolioPercentage(Long watchlistId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Prüfe ob Watchlist-Item existiert
            Optional<CryptoWatchlist> watchlistOpt = cryptoWatchlistRepository.findById(watchlistId);
            if (watchlistOpt.isEmpty()) {
                result.put("error", "Watchlist-Item nicht gefunden");
                result.put("success", false);
                return result;
            }

            // Berechne investierte Summen basierend auf Transaktionen
            List<CryptoTransaction> transactions = cryptoTransactionRepository.findByWatchlistId(watchlistId);
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CryptoTransaction tx : transactions) {
                if (tx.getTransactionType() == CryptoTransaction.TransactionType.BUY) {
                    totalInvested = totalInvested.add(tx.getTotalValue());
                    totalAmount = totalAmount.add(tx.getAmount());
                } else {
                    totalInvested = totalInvested.subtract(tx.getTotalValue());
                    totalAmount = totalAmount.subtract(tx.getAmount());
                }
            }

            // Berechne Portfolio-Prozentsatz im Verhältnis zum Gesamtportfolio
            BigDecimal totalPortfolioValue = getTotalPortfolioValue();
            BigDecimal portfolioPercentage = BigDecimal.ZERO;

            if (totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0) {
                portfolioPercentage = totalInvested.divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            result.put("success", true);
            result.put("totalInvested", totalInvested);
            result.put("totalAmount", totalAmount);
            result.put("transactionCount", transactions.size());
            result.put("portfolioPercentage", portfolioPercentage);
            result.put("totalPortfolioValue", totalPortfolioValue);

        } catch (Exception e) {
            result = createErrorResponse("Fehler beim Berechnen des Portfolios", e);
        }

        return result;
    }

    /**
     * Erstellt eine umfassende Portfolio-Übersicht mit allen Assets,
     * aktuellen Bewertungen und Performance-Metriken.
     * Diese Methode ist ideal für Dashboard-Ansichten.
     *
     * @return Map mit vollständiger Portfolio-Analyse
     */
    public Map<String, Object> getPortfolioOverview() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<WatchlistDto> watchlistDtos = getWatchlistDtos();
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            List<Map<String, Object>> portfolioItems = new ArrayList<>();

            // Verarbeite nur Assets mit tatsächlichen Investitionen
            for (WatchlistDto dto : watchlistDtos) {
                if (dto.getInvestedAmount() != null && dto.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> portfolioItem = new HashMap<>();

                    // Basis-Informationen
                    portfolioItem.put("symbol", dto.getSymbol());
                    portfolioItem.put("name", dto.getName());
                    portfolioItem.put("type", dto.getType());
                    portfolioItem.put("investedAmount", dto.getInvestedAmount());
                    portfolioItem.put("totalAmount", dto.getTotalAmount());

                    totalInvested = totalInvested.add(dto.getInvestedAmount());

                    try {
                        // Hole aktuellen Preis für Bewertung
                        Map<String, Object> priceResponse = getAssetWithCurrentPrice(
                                dto.getSymbol(), dto.getType(), "usd");

                        if ((Boolean) priceResponse.get("success")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> priceData = (Map<String, Object>) priceResponse.get("priceData");
                            BigDecimal currentPrice = new BigDecimal(priceData.get("price").toString());
                            BigDecimal currentValue = dto.getTotalAmount().multiply(currentPrice);

                            portfolioItem.put("currentPrice", currentPrice);
                            portfolioItem.put("currentValue", currentValue);

                            // Berechne Gewinn/Verlust
                            BigDecimal profit = currentValue.subtract(dto.getInvestedAmount());
                            BigDecimal profitPercentage = profit.divide(dto.getInvestedAmount(), 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"));

                            portfolioItem.put("profit", profit);
                            portfolioItem.put("profitPercentage", profitPercentage);

                            totalCurrentValue = totalCurrentValue.add(currentValue);
                        }
                    } catch (Exception e) {
                        // Einzelne Preisfehler sollen das Gesamtergebnis nicht beeinträchtigen
                        portfolioItem.put("priceError", e.getMessage());
                    }

                    portfolioItems.add(portfolioItem);
                }
            }

            // Berechne Portfolio-Verteilung (Prozentsätze)
            for (Map<String, Object> item : portfolioItems) {
                BigDecimal invested = (BigDecimal) item.get("investedAmount");
                if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentage = invested.divide(totalInvested, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    item.put("portfolioPercentage", percentage);
                }
            }

            // Berechne Gesamtperformance
            BigDecimal totalProfit = totalCurrentValue.subtract(totalInvested);
            BigDecimal totalProfitPercentage = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                totalProfitPercentage = totalProfit.divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            result.put("portfolioItems", portfolioItems);
            result.put("totalInvested", totalInvested);
            result.put("totalCurrentValue", totalCurrentValue);
            result.put("totalProfit", totalProfit);
            result.put("totalProfitPercentage", totalProfitPercentage);
            result.put("itemCount", portfolioItems.size());
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", "Fehler beim Laden der Portfolio-Übersicht: " + e.getMessage());
            result.put("success", false);
        }

        return result;
    }

    /**
     * Berechnet den Gesamtwert des Portfolios basierend auf allen Kauf-Transaktionen.
     *
     * @return Gesamter investierter Betrag
     */
    private BigDecimal getTotalPortfolioValue() {
        List<CryptoTransaction> allTransactions = cryptoTransactionRepository.findAll();
        return allTransactions.stream()
                .filter(tx -> tx.getTransactionType() == CryptoTransaction.TransactionType.BUY)
                .map(CryptoTransaction::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== INVESTMENT MANAGEMENT ==========

    /**
     * Erstellt eine neue Investition durch Kauf eines Assets zum aktuellen Marktpreis.
     * Diese Methode kombiniert mehrere Operationen:
     * 1. Preisabfrage
     * 2. Watchlist-Erstellung/-Update
     * 3. Expense-Eintrag
     * 4. Transaction-Eintrag
     *
     * @param symbol Asset-Symbol
     * @param name Asset-Name (optional)
     * @param type Asset-Typ
     * @param investmentAmount Zu investierender Betrag
     * @return Map mit Investitionsergebnissen
     */
    public Map<String, Object> addInvestment(String symbol, String name, String type, BigDecimal investmentAmount) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validierung des Investitionsbetrags
            if (investmentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Investment-Betrag muss größer als 0 sein");
            }

            // Hole aktuellen Preis für die Berechnung der gekauften Menge
            Map<String, Object> priceResponse = getAssetWithCurrentPrice(symbol, type, "usd");
            if (!(Boolean) priceResponse.get("success")) {
                return createErrorResponse("Konnte aktuellen Preis nicht abrufen", null);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> priceData = (Map<String, Object>) priceResponse.get("priceData");
            BigDecimal currentPrice = new BigDecimal(priceData.get("price").toString());

            // Erstelle oder finde bestehendes Watchlist-Item
            CryptoWatchlist watchlistItem;
            Optional<CryptoWatchlist> existingOpt = cryptoWatchlistRepository.findBySymbolIgnoreCase(symbol);

            if (existingOpt.isPresent()) {
                watchlistItem = existingOpt.get();
            } else {
                // Erstelle neues Watchlist-Item
                watchlistItem = new CryptoWatchlist();
                watchlistItem.setSymbol(symbol.toUpperCase());
                watchlistItem.setName(name != null ? name : symbol);
                watchlistItem.setType(type);
                watchlistItem = cryptoWatchlistRepository.save(watchlistItem);
            }

            // Berechne gekaufte Menge
            BigDecimal amount = investmentAmount.divide(currentPrice, 8, RoundingMode.HALF_UP);

            // Erstelle Expense-Eintrag
            Expense expense = new Expense();
            expense.setAmount(investmentAmount);
            expense.setCategory("Investment");
            expense.setDate(LocalDate.now());
            expense.setDescription("Kauf von " + amount + " " + symbol);
            expense = expenseRepository.save(expense);

            // Erstelle Transaction
            CryptoTransaction transaction = new CryptoTransaction(
                    watchlistItem,
                    CryptoTransaction.TransactionType.BUY,
                    amount,
                    currentPrice
            );
            transaction.setExpense(expense);
            transaction = cryptoTransactionRepository.save(transaction);

            result.put("success", true);
            result.put("transaction", transaction);
            result.put("expense", expense);
            result.put("watchlistItem", convertToDto(watchlistItem));
            result.put("currentPrice", currentPrice);
            result.put("amount", amount);
            result.put("message", "Investment erfolgreich erstellt");

        } catch (Exception e) {
            result = createErrorResponse("Fehler beim Erstellen der Investition: " + e.getMessage(), e);
        }

        return result;
    }

    // ========== SEARCH & DISCOVERY ==========

    /**
     * Asset-Suche für Watchlist-Hinzufügung
     */
    public Map<String, Object> searchAssets(String query, String type, int limit) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> searchResults = new ArrayList<>();

            // Einfache Symbol-basierte Suche (kann später erweitert werden)
            String queryUpper = query.toUpperCase();

            // Vordefinierte Asset-Listen für Demo
            List<Map<String, String>> cryptoAssets = Arrays.asList(
                    Map.of("symbol", "BTC", "name", "Bitcoin", "type", "crypto"),
                    Map.of("symbol", "ETH", "name", "Ethereum", "type", "crypto"),
                    Map.of("symbol", "ADA", "name", "Cardano", "type", "crypto"),
                    Map.of("symbol", "DOT", "name", "Polkadot", "type", "crypto"),
                    Map.of("symbol", "SOL", "name", "Solana", "type", "crypto"),
                    Map.of("symbol", "MATIC", "name", "Polygon", "type", "crypto"),
                    Map.of("symbol", "LINK", "name", "Chainlink", "type", "crypto"),
                    Map.of("symbol", "UNI", "name", "Uniswap", "type", "crypto"),
                    Map.of("symbol", "AVAX", "name", "Avalanche", "type", "crypto"),
                    Map.of("symbol", "ATOM", "name", "Cosmos", "type", "crypto")
            );

            List<Map<String, String>> stockAssets = Arrays.asList(
                    Map.of("symbol", "AAPL", "name", "Apple Inc.", "type", "stock"),
                    Map.of("symbol", "GOOGL", "name", "Alphabet Inc.", "type", "stock"),
                    Map.of("symbol", "MSFT", "name", "Microsoft Corporation", "type", "stock"),
                    Map.of("symbol", "AMZN", "name", "Amazon.com Inc.", "type", "stock"),
                    Map.of("symbol", "TSLA", "name", "Tesla Inc.", "type", "stock"),
                    Map.of("symbol", "META", "name", "Meta Platforms Inc.", "type", "stock"),
                    Map.of("symbol", "NVDA", "name", "NVIDIA Corporation", "type", "stock"),
                    Map.of("symbol", "NFLX", "name", "Netflix Inc.", "type", "stock"),
                    Map.of("symbol", "DIS", "name", "The Walt Disney Company", "type", "stock"),
                    Map.of("symbol", "V", "name", "Visa Inc.", "type", "stock")
            );

            List<Map<String, String>> assetsToSearch = new ArrayList<>();

            if (type == null || "crypto".equalsIgnoreCase(type)) {
                assetsToSearch.addAll(cryptoAssets);
            }
            if (type == null || "stock".equalsIgnoreCase(type)) {
                assetsToSearch.addAll(stockAssets);
            }

            // Filter basierend auf Query
            searchResults = assetsToSearch.stream()
                    .filter(asset ->
                            asset.get("symbol").contains(queryUpper) ||
                                    asset.get("name").toUpperCase().contains(queryUpper))
                    .limit(limit)
                    .map(asset -> {
                        Map<String, Object> searchResult = new HashMap<>(asset);

                        // Prüfe ob bereits in Watchlist
                        boolean inWatchlist = cryptoWatchlistRepository
                                .findBySymbolIgnoreCase(asset.get("symbol")).isPresent();
                        searchResult.put("inWatchlist", inWatchlist);

                        return searchResult;
                    })
                    .collect(Collectors.toList());

            result.put("results", searchResults);
            result.put("query", query);
            result.put("type", type);
            result.put("count", searchResults.size());
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", "Fehler bei der Suche: " + e.getMessage());
            result.put("success", false);
        }

        return result;
    }

    // ========== UTILITY METHODS ==========

    /**
     * Validiert Symbol basierend auf Typ
     */

    private boolean isValidSymbol(String symbol, String type) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }

        String cleanSymbol = symbol.trim().toUpperCase();

        if ("crypto".equalsIgnoreCase(type)) {
            // Basis-Validierung für Crypto-Symbole (3-10 Zeichen, nur Buchstaben/Zahlen)
            return cleanSymbol.matches("^[A-Z0-9]{2,10}$");
        } else if ("stock".equalsIgnoreCase(type)) {
            // Basis-Validierung für Stock-Symbole (1-5 Zeichen, meist Buchstaben)
            return cleanSymbol.matches("^[A-Z]{1,5}$");
        }

        return false;
    }

    /**
     * Automatische Asset-Typ-Erkennung basierend auf Symbol
     */
    private String detectAssetType(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return "crypto"; // Default
        }

        String cleanSymbol = symbol.trim().toUpperCase();

        // Bekannte Stock-Symbole (erweitere diese Liste nach Bedarf)
        List<String> knownStocks = Arrays.asList(
                "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA",
                "NFLX", "DIS", "V", "JPM", "JNJ", "WMT", "PG", "UNH", "HD"
        );

        // Bekannte Crypto-Symbole
        List<String> knownCryptos = Arrays.asList(
                "BTC", "ETH", "ADA", "DOT", "SOL", "MATIC", "LINK", "UNI",
                "AVAX", "ATOM", "XRP", "LTC", "BCH", "EOS", "TRX", "XLM"
        );

        if (knownStocks.contains(cleanSymbol)) {
            return "stock";
        } else if (knownCryptos.contains(cleanSymbol)) {
            return "crypto";
        } else {
            // Heuristik: Kurze Symbole (1-4 Zeichen) = wahrscheinlich Stock
            // Längere Symbole = wahrscheinlich Crypto
            return cleanSymbol.length() <= 4 ? "stock" : "crypto";
        }
    }

    /**
     * Erstellt standardisierte Fehler-Response
     */
    private Map<String, Object> createErrorResponse(String message, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());

        if (e != null) {
            errorResponse.put("errorDetails", getErrorDetails(e));
        }

        return errorResponse;
    }

    /**
     * Extrahiert detaillierte Fehlerinformationen
     */
    private Map<String, Object> getErrorDetails(Exception e) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", e.getClass().getSimpleName());
        details.put("message", e.getMessage());

        // Stack trace nur in Development-Umgebung
        if (isDevelopmentMode()) {
            details.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }

        return details;
    }

    /**
     * Prüft ob Development-Modus aktiv ist
     */
    private boolean isDevelopmentMode() {
        // Kann über Profile oder Environment-Variable gesteuert werden
        String profile = System.getProperty("spring.profiles.active", "dev");
        return "dev".equalsIgnoreCase(profile) || "development".equalsIgnoreCase(profile);
    }

    /**
     * Konvertiert Transaction zu Map für Export
     */
    private Map<String, Object> transactionToMap(CryptoTransaction transaction) {
        Map<String, Object> txMap = new HashMap<>();
        txMap.put("id", transaction.getId());
        txMap.put("watchlistId", transaction.getWatchlist().getId());
        txMap.put("symbol", transaction.getWatchlist().getSymbol());
        txMap.put("transactionType", transaction.getTransactionType().toString());
        txMap.put("amount", transaction.getAmount());
        txMap.put("price", transaction.getPricePerUnit());
        txMap.put("totalValue", transaction.getTotalValue());
        txMap.put("createdAt", transaction.getCreatedAt());

        if (transaction.getExpense() != null) {
            txMap.put("expenseId", transaction.getExpense().getId());
        }

        return txMap;
    }
}