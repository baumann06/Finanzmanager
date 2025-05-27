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

    // ========== CURRENT PRICE DATA ==========

    /**
     * Holt aktuelle Preisdaten mit Change-Informationen
     */
    public Map<String, Object> getAssetWithCurrentPrice(String symbol, String type, String market) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (!isValidSymbol(symbol, type)) {
                throw new IllegalArgumentException("Invalid symbol for type " + type);
            }

            Map<String, Object> priceData = new HashMap<>();
            if ("crypto".equalsIgnoreCase(type)) {
                Map<String, Object> apiResponse = externalApiService.getCryptoCurrentPrice(symbol, market);
                priceData = externalApiService.extractCurrentPrice(apiResponse, "crypto", symbol);
                // Ensure we always have a price field
                if (priceData.containsKey("close") && !priceData.containsKey("price")) {
                    priceData.put("price", priceData.get("close"));
                }
            } else {
                // Stock handling...
            }

            result.put("success", true);
            result.put("priceData", priceData);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to fetch price: " + e.getMessage());
            // Detailed error for debugging
            result.put("errorDetails", getErrorDetails(e));
        }

        return result;
    }

    /**
     * Extrahiert detaillierte Informationen aus einer Exception.
     */
    private String getErrorDetails(Exception e) {
        StringBuilder details = new StringBuilder();
        details.append("Exception: ").append(e.getClass().getName()).append("\n");
        details.append("Message: ").append(e.getMessage()).append("\n");

        // Stacktrace hinzufügen
        for (StackTraceElement element : e.getStackTrace()) {
            details.append("at ").append(element.toString()).append("\n");
        }

        return details.toString();
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

    /**
     * Überladene Methode für Rückwärtskompatibilität
     */
    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market) {
        return getAssetPriceHistory(symbol, type, market, "daily", null);
    }

    // ========== MULTIPLE ASSETS ==========

    /**
     * Holt mehrere Assets mit aktuellen Preisen
     */
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

    /**
     * Formatiert Chart-Daten für Frontend
     */
    public Map<String, Object> formatChartData(Map<String, Object> apiResponse, String type) {
        Map<String, Object> chartData = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                // CoinGecko: prices als Liste von [timestamp, price]
                @SuppressWarnings("unchecked")
                List<List<Object>> prices = (List<List<Object>>) apiResponse.get("prices");

                if (prices != null && !prices.isEmpty()) {
                    // Umwandlung in Chart-freundliches Format
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
                // Twelve Data: values als Liste von Maps
                @SuppressWarnings("unchecked")
                List<Map<String, String>> values = (List<Map<String, String>>) apiResponse.get("values");

                if (values != null && !values.isEmpty()) {
                    // Umwandlung in Chart-freundliches Format
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
     * Validiert Asset-Symbol vor API-Aufruf
     */
    public boolean isValidSymbol(String symbol, String type) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }

        // Basis-Validierung
        if ("crypto".equalsIgnoreCase(type)) {
            // Für Krypto: prüfen ob Symbol bekannt ist oder als gültiger CoinGecko ID verwendet werden kann
            return symbol.length() >= 2 && symbol.length() <= 20;
        } else if ("stock".equalsIgnoreCase(type)) {
            // Für Aktien: typische Ticker-Symbol Länge
            return symbol.length() >= 1 && symbol.length() <= 10;
        }

        return false;
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