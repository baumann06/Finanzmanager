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

@Service
public class AssetService {

    @Autowired
    private CryptoWatchlistRepository cryptoWatchlistRepository;

    @Autowired
    private ExternalApiService externalApiService;

    @Autowired
    private CryptoTransactionRepository cryptoTransactionRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    // ========== PRICE DATA METHODS ==========

    public Map<String, Object> getAssetWithCurrentPrice(String symbol, String type, String market) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (!isValidSymbol(symbol, type)) {
                throw new IllegalArgumentException("Invalid symbol '" + symbol + "' for type '" + type + "'");
            }

            Map<String, Object> priceData;
            String targetMarket = market != null ? market : "usd";

            if ("crypto".equalsIgnoreCase(type)) {
                Map<String, Object> apiResponse = externalApiService.getCryptoCurrentPrice(symbol, targetMarket);
                priceData = externalApiService.extractCurrentPrice(apiResponse, "crypto", symbol);
                if (priceData.containsKey("close") && !priceData.containsKey("price")) {
                    priceData.put("price", priceData.get("close"));
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                Map<String, Object> apiResponse = externalApiService.getStockCurrentPrice(symbol);
                priceData = externalApiService.extractCurrentPrice(apiResponse, "stock", symbol);
            } else {
                throw new IllegalArgumentException("Unbekannter Asset-Typ: '" + type + "'");
            }

            if (priceData == null || priceData.isEmpty()) {
                throw new RuntimeException("Keine Preisdaten verfügbar für " + symbol);
            }

            result.put("success", true);
            result.put("priceData", priceData);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to fetch price for " + symbol + " (" + type + "): " + e.getMessage());
            result.put("errorDetails", getErrorDetails(e));
        }

        return result;
    }

    public Map<String, Object> getMultipleAssetsWithPrices(List<String> symbols, String type, String market) {
        Map<String, Object> assetsData = new HashMap<>();
        int successCount = 0;

        for (String symbol : symbols) {
            try {
                Map<String, Object> assetData = getAssetWithCurrentPrice(symbol, type, market);
                assetsData.put(symbol, assetData);
                if ((Boolean) assetData.get("success")) {
                    successCount++;
                }
            } catch (Exception e) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", e.getMessage());
                errorData.put("success", false);
                assetsData.put(symbol, errorData);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("assets", assetsData);
        result.put("totalCount", symbols.size());
        result.put("successCount", successCount);
        result.put("failedCount", symbols.size() - successCount);
        result.put("success", true);
        return result;
    }

    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market, String period, String interval) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> apiResponse;
            String targetMarket = market != null ? market : "usd";

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

    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market) {
        return getAssetPriceHistory(symbol, type, market, "daily", null);
    }

    // ========== WATCHLIST MANAGEMENT ==========

    public CryptoWatchlist addToWatchlist(CryptoWatchlist asset) {
        // Validierung vor dem Speichern
        if (asset.getSymbol() == null || asset.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol ist erforderlich");
        }

        // Prüfe auf Duplikate
        Optional<CryptoWatchlist> existing = cryptoWatchlistRepository.findBySymbolIgnoreCase(asset.getSymbol().trim());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.getSymbol() + " ist bereits in der Watchlist");
        }

        asset.setSymbol(asset.getSymbol().trim().toUpperCase());
        if (asset.getType() == null || asset.getType().trim().isEmpty()) {
            asset.setType(detectAssetType(asset.getSymbol()));
        }

        return cryptoWatchlistRepository.save(asset);
    }

    public List<CryptoWatchlist> getWatchlist() {
        return cryptoWatchlistRepository.findAll();
    }

    /**
     * Gibt Watchlist als DTOs zurück (verhindert Zirkularreferenzen)
     */
    public List<WatchlistDto> getWatchlistDtos() {
        List<CryptoWatchlist> entities = cryptoWatchlistRepository.findAll();
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Watchlist Entities: ");
        System.out.println(entities);
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Einzelnes Watchlist-Item als DTO abrufen
     */
    public WatchlistDto getWatchlistItemDto(Long id) {
        Optional<CryptoWatchlist> entity = cryptoWatchlistRepository.findById(id);
        return entity.map(this::convertToDto).orElse(null);
    }

    /**
     * Watchlist-Item aktualisieren
     */
    public CryptoWatchlist updateWatchlistItem(Long id, CryptoWatchlist updatedAsset) {
        Optional<CryptoWatchlist> existingOpt = cryptoWatchlistRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return null;
        }

        CryptoWatchlist existing = existingOpt.get();

        // Aktualisiere nur erlaubte Felder
        if (updatedAsset.getName() != null) {
            existing.setName(updatedAsset.getName().trim());
        }
        if (updatedAsset.getNotes() != null) {
            existing.setNotes(updatedAsset.getNotes());
        }
        // Symbol und Type sollten normalerweise nicht geändert werden

        return cryptoWatchlistRepository.save(existing);
    }

    /**
     * Öffentliche Methode für DTO-Konvertierung (für Controller)
     */
    public WatchlistDto convertToDto(CryptoWatchlist entity) {
        WatchlistDto dto = new WatchlistDto();
        dto.setId(entity.getId());
        dto.setSymbol(entity.getSymbol());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());

        // Berechne investierte Beträge aus Transaktionen
        if (entity.getTransactions() != null && !entity.getTransactions().isEmpty()) {
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;
            int transactionCount = 0;

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

            // Berechne Durchschnittspreis wenn Menge > 0
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal avgPrice = totalInvested.divide(totalAmount, 8, RoundingMode.HALF_UP);
                dto.setAveragePrice(avgPrice);
            }
        } else {
            dto.setInvestedAmount(BigDecimal.ZERO);
            dto.setTotalAmount(BigDecimal.ZERO);
            dto.setTransactionCount(0);
        }

        return dto;
    }

    public boolean removeFromWatchlist(Long id) {
        if (cryptoWatchlistRepository.existsById(id)) {
            /* Prüfe ob Transaktionen existieren
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
     * Watchlist mit aktuellen Preisen anreichern
     */
    public Map<String, Object> getWatchlistWithCurrentPrices() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<WatchlistDto> watchlistDtos = getWatchlistDtos();
            List<Map<String, Object>> enrichedWatchlist = new ArrayList<>();

            for (WatchlistDto dto : watchlistDtos) {
                Map<String, Object> enrichedItem = new HashMap<>();
                enrichedItem.put("watchlistItem", dto);

                try {
                    // Hole aktuellen Preis
                    Map<String, Object> priceResponse = getAssetWithCurrentPrice(
                            dto.getSymbol(), dto.getType(), "usd");

                    if ((Boolean) priceResponse.get("success")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> priceData = (Map<String, Object>) priceResponse.get("priceData");
                        enrichedItem.put("currentPrice", priceData);

                        // Berechne aktuelle Performance
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

    public Map<String, Object> getPortfolioPercentage(Long watchlistId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<CryptoWatchlist> watchlistOpt = cryptoWatchlistRepository.findById(watchlistId);
            if (watchlistOpt.isEmpty()) {
                result.put("error", "Watchlist-Item nicht gefunden");
                result.put("success", false);
                return result;
            }

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

            // Berechne Portfolio-Prozentsatz im Vergleich zu Gesamtportfolio
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
     * Gesamte Portfolio-Übersicht
     */
    public Map<String, Object> getPortfolioOverview() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<WatchlistDto> watchlistDtos = getWatchlistDtos();
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            List<Map<String, Object>> portfolioItems = new ArrayList<>();

            for (WatchlistDto dto : watchlistDtos) {
                if (dto.getInvestedAmount() != null && dto.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> portfolioItem = new HashMap<>();
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

                            BigDecimal profit = currentValue.subtract(dto.getInvestedAmount());
                            BigDecimal profitPercentage = profit.divide(dto.getInvestedAmount(), 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"));

                            portfolioItem.put("profit", profit);
                            portfolioItem.put("profitPercentage", profitPercentage);

                            totalCurrentValue = totalCurrentValue.add(currentValue);
                        }
                    } catch (Exception e) {
                        portfolioItem.put("priceError", e.getMessage());
                    }

                    portfolioItems.add(portfolioItem);
                }
            }

            // Berechne Portfolio-Prozentsätze
            for (Map<String, Object> item : portfolioItems) {
                BigDecimal invested = (BigDecimal) item.get("investedAmount");
                if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal percentage = invested.divide(totalInvested, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    item.put("portfolioPercentage", percentage);
                }
            }

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

    private BigDecimal getTotalPortfolioValue() {
        List<CryptoTransaction> allTransactions = cryptoTransactionRepository.findAll();
        return allTransactions.stream()
                .filter(tx -> tx.getTransactionType() == CryptoTransaction.TransactionType.BUY)
                .map(CryptoTransaction::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== INVESTMENT MANAGEMENT ==========

    public Map<String, Object> addInvestment(String symbol, String name, String type, BigDecimal investmentAmount) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validierung
            if (investmentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Investment-Betrag muss größer als 0 sein");
            }

            // Hole aktuellen Preis
            Map<String, Object> priceResponse = getAssetWithCurrentPrice(symbol, type, "usd");
            if (!(Boolean) priceResponse.get("success")) {
                return createErrorResponse("Konnte aktuellen Preis nicht abrufen", null);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> priceData = (Map<String, Object>) priceResponse.get("priceData");
            BigDecimal currentPrice = new BigDecimal(priceData.get("price").toString());

            // Erstelle oder finde Watchlist-Item
            CryptoWatchlist watchlistItem;
            Optional<CryptoWatchlist> existingOpt = cryptoWatchlistRepository.findBySymbolIgnoreCase(symbol);

            if (existingOpt.isPresent()) {
                watchlistItem = existingOpt.get();
            } else {
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