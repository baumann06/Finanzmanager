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

    public CryptoWatchlist addToWatchlist(CryptoWatchlist asset) {
        return cryptoWatchlistRepository.save(asset);
    }

    public List<CryptoWatchlist> getWatchlist() {
        return cryptoWatchlistRepository.findAll();
    }

    public void removeFromWatchlist(Long id) {
        cryptoWatchlistRepository.deleteById(id);
    }

    /**
     * Holt aktuelle Preisdaten für das Asset
     * @param symbol z.B. "BTC" oder "AAPL"
     * @param type "crypto" oder "stock"
     * @param market nur für Krypto, z.B. "USD"
     * @return Map mit Watchlist-Daten und aktuellen Preis-Daten
     */
    public Map<String, Object> getAssetWithCurrentPrice(String symbol, String type, String market) {
        Map<String, Object> result = new HashMap<>();

        // Watchlist-Eintrag suchen
        CryptoWatchlist asset = cryptoWatchlistRepository.findBySymbol(symbol);

        Map<String, Object> apiResponse;
        Map<String, Object> priceData;

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                // Für Krypto verwenden wir DIGITAL_CURRENCY_DAILY
                apiResponse = externalApiService.getCryptoData(symbol, market != null ? market : "USD");
                priceData = externalApiService.extractLatestPrice(apiResponse, "crypto");
            } else if ("stock".equalsIgnoreCase(type)) {
                // Für Aktien verwenden wir GLOBAL_QUOTE für aktuelle Preise
                apiResponse = externalApiService.getStockQuote(symbol);
                priceData = externalApiService.extractLatestPrice(apiResponse, "quote");
            } else {
                throw new IllegalArgumentException("Unbekannter Asset-Typ: " + type + ". Erlaubt sind: 'crypto' oder 'stock'");
            }

            result.put("watchlist", asset);
            result.put("priceData", priceData);
            result.put("fullApiResponse", apiResponse); // Für Debugging
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
            result.put("watchlist", asset);
        }

        return result;
    }

    /**
     * Holt historische Preisdaten
     * @param symbol z.B. "BTC" oder "AAPL"
     * @param type "crypto" oder "stock"
     * @param market nur für Krypto (z.B. USD)
     * @param period "daily" oder "intraday"
     * @param interval nur für intraday: "1min", "5min", "15min", "30min", "60min"
     * @return Map mit historischen Kursdaten
     */
    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market, String period, String interval) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> apiResponse;

            if ("crypto".equalsIgnoreCase(type)) {
                if ("intraday".equalsIgnoreCase(period)) {
                    apiResponse = externalApiService.getCryptoIntradayData(symbol, market != null ? market : "USD", interval != null ? interval : "5min");
                } else {
                    // Default: daily data
                    apiResponse = externalApiService.getCryptoData(symbol, market != null ? market : "USD");
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                if ("intraday".equalsIgnoreCase(period)) {
                    apiResponse = externalApiService.getStockIntradayData(symbol, interval != null ? interval : "5min", "compact");
                } else {
                    // Default: daily data
                    apiResponse = externalApiService.getStockData(symbol);
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

    /**
     * Holt mehrere Assets mit aktuellen Preisen
     * @param symbols Liste von Symbolen
     * @param type Asset-Typ
     * @param market Markt (für Krypto)
     * @return Map mit allen Asset-Daten
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

        return result;
    }

    /**
     * Hilfsmethode um Chart-Daten zu formatieren
     * @param apiResponse Rohe API-Antwort
     * @param type Asset-Typ
     * @return Formatierte Chart-Daten
     */
    public Map<String, Object> formatChartData(Map<String, Object> apiResponse, String type) {
        Map<String, Object> chartData = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                Map<String, Object> timeSeries = (Map<String, Object>) apiResponse.get("Time Series (Digital Currency Daily)");
                // Formatierung für Chart-Bibliothek (z.B. Chart.js)
                // Implementierung je nach Frontend-Anforderungen
            } else if ("stock".equalsIgnoreCase(type)) {
                Map<String, Object> timeSeries = (Map<String, Object>) apiResponse.get("Time Series (Daily)");
                // Formatierung für Chart-Bibliothek
            }
        } catch (Exception e) {
            chartData.put("error", "Fehler beim Formatieren der Chart-Daten: " + e.getMessage());
        }

        return chartData;
    }
}