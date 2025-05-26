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
     * @param symbol z.B. "bitcoin" oder "AAPL"
     * @param type "crypto" oder "stock"
     * @param market nur für Krypto, z.B. "usd"
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
                // CoinGecko: aktuelle Krypto-Daten (intraday oder Tagesdaten)
                apiResponse = externalApiService.getCryptoIntradayData(symbol, market != null ? market : "usd");
                // Annahme: extractLatestPrice ist in ExternalApiService entsprechend implementiert
                priceData = externalApiService.extractLatestPrice(apiResponse, "crypto");
            } else if ("stock".equalsIgnoreCase(type)) {
                // Twelve Data: aktueller Aktienkurs via time_series endpoint (interval 1min oder 1day)
                apiResponse = externalApiService.getStockIntradayData(symbol, "1min");
                priceData = externalApiService.extractLatestPrice(apiResponse, "stock");
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
     * @param symbol z.B. "bitcoin" oder "AAPL"
     * @param type "crypto" oder "stock"
     * @param market nur für Krypto (z.B. usd)
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
                    // CoinGecko Intraday-Daten (stündlich)
                    apiResponse = externalApiService.getCryptoIntradayData(symbol, market != null ? market : "usd");
                } else {
                    // CoinGecko Tagesdaten (30 Tage)
                    apiResponse = externalApiService.getCryptoDailyData(symbol, market != null ? market : "usd");
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                if ("intraday".equalsIgnoreCase(period)) {
                    // Twelve Data Intraday (1min bis 60min Intervall)
                    apiResponse = externalApiService.getStockIntradayData(symbol, interval != null ? interval : "5min");
                } else {
                    // Twelve Data Tagesdaten
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
                // CoinGecko liefert meist Preise als Liste von Timestamp/Price-Paaren in "prices"
                List<List<Object>> prices = (List<List<Object>>) apiResponse.get("prices");
                // Beispiel: Umwandlung in Map<Timestamp, Price>
                Map<Long, Double> formattedPrices = new HashMap<>();
                for (List<Object> entry : prices) {
                    Long timestamp = ((Number) entry.get(0)).longValue();
                    Double price = ((Number) entry.get(1)).doubleValue();
                    formattedPrices.put(timestamp, price);
                }
                chartData.put("prices", formattedPrices);

            } else if ("stock".equalsIgnoreCase(type)) {
                // Twelve Data liefert "values" als Liste von Tages- oder Intraday-Daten
                List<Map<String, String>> values = (List<Map<String, String>>) apiResponse.get("values");
                // Hier könnte man Daten in ein passendes Format bringen
                chartData.put("values", values);
            }
        } catch (Exception e) {
            chartData.put("error", "Fehler beim Formatieren der Chart-Daten: " + e.getMessage());
        }

        return chartData;
    }
}
