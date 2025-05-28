package com.example.financemanager.service;

import com.example.financemanager.model.CryptoWatchlist;
import com.example.financemanager.repository.CryptoWatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetService {

    @Autowired
    private CryptoWatchlistRepository cryptoWatchlistRepository;

    @Autowired
    private ExternalApiService externalApiService;

    // ========== CURRENT PRICE DATA ==========

    /**
     * 🔧 FIXED: Holt aktuelle Preisdaten mit verbesserter Fehlerbehandlung
     */
    public Map<String, Object> getAssetWithCurrentPrice(String symbol, String type, String market) {
        Map<String, Object> result = new HashMap<>();

        System.out.println("=== AssetService Debug ===");
        System.out.println("Symbol: " + symbol);
        System.out.println("Type: " + type);
        System.out.println("Market: " + market);

        try {
            if (!isValidSymbol(symbol, type)) {
                throw new IllegalArgumentException("Invalid symbol '" + symbol + "' for type '" + type + "'");
            }

            Map<String, Object> priceData = new HashMap<>();

            if ("crypto".equalsIgnoreCase(type)) {
                System.out.println("🪙 Processing as CRYPTO");
                Map<String, Object> apiResponse = externalApiService.getCryptoCurrentPrice(symbol, market != null ? market : "usd");
                System.out.println("🔍 Raw API Response: " + apiResponse);

                priceData = externalApiService.extractCurrentPrice(apiResponse, "crypto", symbol);
                System.out.println("💰 Extracted Price Data: " + priceData);

                // Ensure we always have a price field
                if (priceData.containsKey("close") && !priceData.containsKey("price")) {
                    priceData.put("price", priceData.get("close"));
                }

            } else if ("stock".equalsIgnoreCase(type)) {
                System.out.println("📈 Processing as STOCK");
                // TODO: Implement stock handling
                Map<String, Object> apiResponse = externalApiService.getStockCurrentPrice(symbol);
                priceData = externalApiService.extractCurrentPrice(apiResponse, "stock", symbol);

            } else {
                throw new IllegalArgumentException("Unbekannter Asset-Typ: '" + type + "'. Erlaubt sind: 'crypto' oder 'stock'");
            }

            // Prüfen ob priceData leer ist
            if (priceData == null || priceData.isEmpty()) {
                System.out.println("⚠️ WARNUNG: Leere Price Data für " + symbol + " (" + type + ")");
                throw new RuntimeException("Keine Preisdaten verfügbar für " + symbol);
            }

            result.put("success", true);
            result.put("priceData", priceData);
            System.out.println("✅ Success Result: " + result);

        } catch (Exception e) {
            System.out.println("❌ ERROR in getAssetWithCurrentPrice:");
            System.out.println("Exception Type: " + e.getClass().getName());
            System.out.println("Exception Message: " + e.getMessage());
            e.printStackTrace();

            result.put("success", false);
            result.put("error", "Failed to fetch price for " + symbol + " (" + type + "): " + e.getMessage());
            result.put("errorDetails", getErrorDetails(e));
        }

        System.out.println("🔚 Final Result: " + result);
        return result;
    }

    // ========== WATCHLIST MANAGEMENT ==========

    public CryptoWatchlist addToWatchlist(CryptoWatchlist asset) {
        return cryptoWatchlistRepository.save(asset);
    }

    public List<CryptoWatchlist> getWatchlist() {
        return cryptoWatchlistRepository.findAll();
    }

    public void removeFromWatchlist(Long id) {
        cryptoWatchlistRepository.deleteById(id);
    }

    // ========== HISTORICAL DATA ==========

    /**
     * Holt historische Preisdaten
     */
    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market, String period, String interval) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> apiResponse;

            if ("crypto".equalsIgnoreCase(type)) {
                if ("intraday".equalsIgnoreCase(period)) {
                    apiResponse = externalApiService.getCryptoIntradayData(symbol, market != null ? market : "usd");
                } else {
                    apiResponse = externalApiService.getCryptoDailyData(symbol, market != null ? market : "usd");
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                if ("intraday".equalsIgnoreCase(period)) {
                    apiResponse = externalApiService.getStockIntradayData(symbol, interval != null ? interval : "5min");
                } else {
                    apiResponse = externalApiService.getStockDailyData(symbol);
                }
            } else {
                throw new IllegalArgumentException("Unbekannter Asset-Typ: " + type + ". Erlaubt sind: 'crypto' oder 'stock'");
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

    // ========== MULTIPLE ASSETS ==========

    public Map<String, Object> getMultipleAssetsWithPrices(List<String> symbols, String type, String market) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> assetsData = new HashMap<>();

        for (String symbol : symbols) {
            try {
                Map<String, Object> assetData = getAssetWithCurrentPrice(symbol, type, market);
                assetsData.put(symbol, assetData);
            } catch (Exception e) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", e.getMessage());
                errorData.put("success", false);
                assetsData.put(symbol, errorData);
            }
        }

        result.put("assets", assetsData);
        result.put("count", symbols.size());
        result.put("success", true);

        return result;
    }

    // ========== CHART DATA FORMATTING ==========

    public Map<String, Object> formatChartData(Map<String, Object> apiResponse, String type) {
        Map<String, Object> chartData = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                @SuppressWarnings("unchecked")
                List<List<Object>> prices = (List<List<Object>>) apiResponse.get("prices");

                if (prices != null && !prices.isEmpty()) {
                    List<Map<String, Object>> chartPoints = prices.stream()
                            .map(pricePoint -> {
                                Map<String, Object> point = new HashMap<>();
                                point.put("timestamp", ((Number) pricePoint.get(0)).longValue());
                                point.put("price", ((Number) pricePoint.get(1)).doubleValue());
                                point.put("date", new java.util.Date(((Number) pricePoint.get(0)).longValue()).toString());
                                return point;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    chartData.put("data", chartPoints);
                    chartData.put("type", "crypto");
                }

            } else if ("stock".equalsIgnoreCase(type)) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> values = (List<Map<String, String>>) apiResponse.get("values");

                if (values != null && !values.isEmpty()) {
                    List<Map<String, Object>> chartPoints = values.stream()
                            .map(valuePoint -> {
                                Map<String, Object> point = new HashMap<>();
                                point.put("date", valuePoint.get("datetime"));
                                point.put("price", Double.parseDouble(valuePoint.get("close")));
                                point.put("open", Double.parseDouble(valuePoint.get("open")));
                                point.put("high", Double.parseDouble(valuePoint.get("high")));
                                point.put("low", Double.parseDouble(valuePoint.get("low")));
                                point.put("volume", valuePoint.get("volume"));
                                return point;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    chartData.put("data", chartPoints);
                    chartData.put("type", "stock");
                }
            }

            chartData.put("success", true);

        } catch (Exception e) {
            chartData.put("error", "Fehler beim Formatieren der Chart-Daten: " + e.getMessage());
            chartData.put("success", false);
        }

        return chartData;
    }

    // ========== UTILITY METHODS ==========

    /**
     * 🔧 FIXED: Verbesserte Symbol-Validierung mit Asset-Type Detection
     */
    public boolean isValidSymbol(String symbol, String type) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }

        String trimmed = symbol.trim().toUpperCase();

        // Bekannte Krypto-Symbole
        String[] cryptoSymbols = {"BTC", "ETH", "ADA", "DOT", "SOL", "MATIC", "LINK", "UNI", "AVAX", "ATOM"};
        boolean isCryptoSymbol = java.util.Arrays.asList(cryptoSymbols).contains(trimmed);

        if ("crypto".equalsIgnoreCase(type)) {
            // Für Krypto: Länge prüfen oder bekannte Symbole
            return symbol.length() >= 2 && symbol.length() <= 20;
        } else if ("stock".equalsIgnoreCase(type)) {
            // Warnung wenn Krypto-Symbol als Stock verwendet wird
            if (isCryptoSymbol) {
                System.out.println("⚠️ WARNING: " + symbol + " appears to be a CRYPTO symbol but type=stock was specified!");
            }
            return symbol.length() >= 1 && symbol.length() <= 10;
        }

        return false;
    }

    /**
     * 🔧 NEW: Auto-Detection des Asset-Typs
     */
    public String detectAssetType(String symbol) {
        String[] cryptoSymbols = {"BTC", "ETH", "ADA", "DOT", "SOL", "MATIC", "LINK", "UNI", "AVAX", "ATOM"};
        String trimmed = symbol.trim().toUpperCase();

        if (java.util.Arrays.asList(cryptoSymbols).contains(trimmed)) {
            return "crypto";
        }

        // Default zu stock für unbekannte Symbole
        return "stock";
    }

    /**
     * Extrahiert detaillierte Informationen aus einer Exception.
     */
    private String getErrorDetails(Exception e) {
        StringBuilder details = new StringBuilder();
        details.append("Exception: ").append(e.getClass().getName()).append("\n");
        details.append("Message: ").append(e.getMessage()).append("\n");

        // Top 5 Stack Trace Elements
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            details.append("at ").append(stackTrace[i].toString()).append("\n");
        }

        return details.toString();
    }

    /**
     * Erstellt einheitliche Fehler-Response
     */
    public Map<String, Object> createErrorResponse(String message, Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        if (e != null) {
            error.put("details", e.getMessage());
        }
        return error;
    }
}