package com.example.financemanager.service;

import com.example.financemanager.model.CurrencyRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExternalApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Twelve Data API-Key (für Aktien & Intraday-Daten)
    @Value("${twelvedata.api.key}")
    private String twelveDataApiKey;

    /**
     * Wechselkurs zwischen zwei Fiat-Währungen mit ExchangeRate.host
     */
    public CurrencyRate getExchangeRate(String fromCurrency, String toCurrency) {
        String url = "https://api.exchangerate.host/latest?base=" + fromCurrency.toUpperCase() +
                "&symbols=" + toCurrency.toUpperCase();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !(Boolean)response.get("success")) {
                throw new RuntimeException("Fehler bei ExchangeRate.host API");
            }
            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
            Double rate = rates.get(toCurrency.toUpperCase());
            if (rate == null) {
                throw new RuntimeException("Wechselkurs nicht gefunden");
            }

            CurrencyRate currencyRate = new CurrencyRate();
            currencyRate.setFromCurrency(fromCurrency.toUpperCase());
            currencyRate.setToCurrency(toCurrency.toUpperCase());
            currencyRate.setRate(BigDecimal.valueOf(rate));
            currencyRate.setTimestamp(LocalDateTime.now());

            return currencyRate;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei Wechselkurs-Abfrage: " + e.getMessage());
        }
    }

    /**
     * Krypto-Tagesdaten von CoinGecko
     * @param coinId z.B. "bitcoin", "ethereum"
     * @param vsCurrency z.B. "usd"
     */
    public Map<String, Object> getCryptoDailyData(String coinId, String vsCurrency) {
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId.toLowerCase() + "/market_chart" +
                "?vs_currency=" + vsCurrency.toLowerCase() + "&days=30&interval=daily";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei CoinGecko Krypto-Daten: " + e.getMessage());
        }
    }

    /**
     * Krypto-Intraday-Daten von CoinGecko (für aktuellere Preise)
     * @param coinId z.B. "bitcoin"
     * @param vsCurrency z.B. "usd"
     */
    public Map<String, Object> getCryptoIntradayData(String coinId, String vsCurrency) {
        // CoinGecko gibt Intraday-Daten nur über market_chart mit kurzen Intervallen
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId.toLowerCase() + "/market_chart" +
                "?vs_currency=" + vsCurrency.toLowerCase() + "&days=1&interval=hourly";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei CoinGecko Intraday-Daten: " + e.getMessage());
        }
    }

    /**
     * Aktien-Tagesdaten von Twelve Data
     * @param symbol z.B. "AAPL"
     */
    public Map<String, Object> getStockDailyData(String symbol) {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol.toUpperCase() + "&interval=1day&apikey=" + twelveDataApiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.containsKey("code")) {
                throw new RuntimeException("Twelve Data API Fehler: " + response);
            }
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei Twelve Data Aktien-Daten: " + e.getMessage());
        }
    }

    /**
     * Aktien Intraday-Daten von Twelve Data
     * @param symbol z.B. "AAPL"
     * @param interval z.B. "1min", "5min", "15min", "30min", "60min"
     */
    public Map<String, Object> getStockIntradayData(String symbol, String interval) {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol.toUpperCase() +
                "&interval=" + interval +
                "&apikey=" + twelveDataApiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.containsKey("code")) {
                throw new RuntimeException("Twelve Data API Fehler: " + response);
            }
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei Twelve Data Intraday-Daten: " + e.getMessage());
        }
    }


    /**
     * Extrahiert den letzten verfügbaren Preis aus der API-Antwort
     * @param apiResponse Antwort der API
     * @param type "crypto" oder "stock"
     * @return Map mit "timestamp" und "price"
     */
    public Map<String, Object> extractLatestPrice(Map<String, Object> apiResponse, String type) {
        Map<String, Object> priceData = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                List<List<Object>> prices = (List<List<Object>>) apiResponse.get("prices");
                if (prices != null && !prices.isEmpty()) {
                    List<Object> lastEntry = prices.get(prices.size() - 1);
                    Long timestamp = ((Number) lastEntry.get(0)).longValue();
                    Double price = ((Number) lastEntry.get(1)).doubleValue();

                    priceData.put("timestamp", timestamp);
                    priceData.put("price", price);
                } else {
                    throw new RuntimeException("Keine Preisdaten gefunden (crypto)");
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                List<Map<String, String>> values = (List<Map<String, String>>) apiResponse.get("values");
                if (values != null && !values.isEmpty()) {
                    Map<String, String> latest = values.get(0); // Twelve Data: neueste Werte zuerst
                    String datetime = latest.get("datetime");
                    Double price = Double.parseDouble(latest.get("close"));

                    priceData.put("timestamp", datetime);
                    priceData.put("price", price);
                } else {
                    throw new RuntimeException("Keine Preisdaten gefunden (stock)");
                }
            } else {
                throw new IllegalArgumentException("Unbekannter Asset-Typ: " + type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Extrahieren des Preises: " + e.getMessage(), e);
        }

        return priceData;
    }
}


