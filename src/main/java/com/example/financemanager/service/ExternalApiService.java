package com.example.financemanager.service;

import com.example.financemanager.model.CurrencyRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Service
public class ExternalApiService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${alphavantage.api.key:J23ZY20YSGXA91CU}")
    private String alphaVantageKey;

    /**
     * Holt Wechselkurs zwischen zwei Fiat-Währungen
     */
    public CurrencyRate getExchangeRate(String fromCurrency, String toCurrency) {
        String url = "https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE" +
                "&from_currency=" + fromCurrency +
                "&to_currency=" + toCurrency +
                "&apikey=" + alphaVantageKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, String> exchangeRate = (Map<String, String>) response.get("Realtime Currency Exchange Rate");

            if (exchangeRate == null) {
                throw new RuntimeException("Keine Wechselkursdaten gefunden");
            }

            CurrencyRate rate = new CurrencyRate();
            rate.setFromCurrency(fromCurrency);
            rate.setToCurrency(toCurrency);
            rate.setRate(new BigDecimal(exchangeRate.get("5. Exchange Rate")));
            rate.setTimestamp(LocalDateTime.now());

            return rate;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler beim Abrufen der Wechselkursdaten: " + e.getMessage());
        }
    }

    /**
     * Holt aktuelle Kryptowährungs-Daten (Tageswerte)
     * @param symbol Krypto-Symbol (z.B. "BTC")
     * @param market Markt-Währung (z.B. "USD")
     */
    public Map<String, Object> getCryptoData(String symbol, String market) {
        String url = "https://www.alphavantage.co/query?function=DIGITAL_CURRENCY_DAILY" +
                "&symbol=" + symbol.toUpperCase() +
                "&market=" + market.toUpperCase() +
                "&apikey=" + alphaVantageKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response.containsKey("Error Message")) {
                throw new RuntimeException("API Fehler: " + response.get("Error Message"));
            }

            if (response.containsKey("Note")) {
                throw new RuntimeException("API Limit erreicht: " + response.get("Note"));
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler beim Abrufen der Krypto-Daten: " + e.getMessage());
        }
    }

    /**
     * Holt Krypto-Intraday-Daten (für aktuellere Preise)
     * @param symbol Krypto-Symbol (z.B. "BTC")
     * @param market Markt-Währung (z.B. "USD")
     * @param interval Zeitintervall (1min, 5min, 15min, 30min, 60min)
     */
    public Map<String, Object> getCryptoIntradayData(String symbol, String market, String interval) {
        String url = "https://www.alphavantage.co/query?function=CRYPTO_INTRADAY" +
                "&symbol=" + symbol.toUpperCase() +
                "&market=" + market.toUpperCase() +
                "&interval=" + interval +
                "&apikey=" + alphaVantageKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response.containsKey("Error Message")) {
                throw new RuntimeException("API Fehler: " + response.get("Error Message"));
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler beim Abrufen der Krypto-Intraday-Daten: " + e.getMessage());
        }
    }

    /**
     * Holt tägliche Aktienkurse
     * @param symbol Aktien-Symbol (z.B. "AAPL", "IBM")
     */
    public Map<String, Object> getStockData(String symbol) {
        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY" +
                "&symbol=" + symbol.toUpperCase() +
                "&apikey=" + alphaVantageKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response.containsKey("Error Message")) {
                throw new RuntimeException("API Fehler: " + response.get("Error Message"));
            }

            if (response.containsKey("Note")) {
                throw new RuntimeException("API Limit erreicht: " + response.get("Note"));
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler beim Abrufen der Aktien-Daten: " + e.getMessage());
        }
    }

    /**
     * Holt Intraday-Aktienkurse (für aktuellere Preise)
     * @param symbol Aktien-Symbol (z.B. "AAPL", "IBM")
     * @param interval Zeitintervall (1min, 5min, 15min, 30min, 60min)
     * @param outputsize compact oder full
     */
    public Map<String, Object> getStockIntradayData(String symbol, String interval, String outputsize) {
        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY" +
                "&symbol=" + symbol.toUpperCase() +
                "&interval=" + interval +
                "&outputsize=" + outputsize +
                "&apikey=" + alphaVantageKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response.containsKey("Error Message")) {
                throw new RuntimeException("API Fehler: " + response.get("Error Message"));
            }

            if (response.containsKey("Note")) {
                throw new RuntimeException("API Limit erreicht: " + response.get("Note"));
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler beim Abrufen der Intraday-Aktien-Daten: " + e.getMessage());
        }
    }

    /**
     * Holt aktuelle Quote/Preis für eine Aktie
     * @param symbol Aktien-Symbol
     */
    public Map<String, Object> getStockQuote(String symbol) {
        String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE" +
                "&symbol=" + symbol.toUpperCase() +
                "&apikey=" + alphaVantageKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response.containsKey("Error Message")) {
                throw new RuntimeException("API Fehler: " + response.get("Error Message"));
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler beim Abrufen der Aktien-Quote: " + e.getMessage());
        }
    }

    /**
     * Hilfsmethode um den aktuellsten Preis aus den Daten zu extrahieren
     */
    public Map<String, Object> extractLatestPrice(Map<String, Object> apiResponse, String assetType) {
        Map<String, Object> result = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(assetType)) {
                // Für Krypto-Daten
                Map<String, Object> timeSeries = (Map<String, Object>) apiResponse.get("Time Series (Digital Currency Daily)");
                if (timeSeries != null && !timeSeries.isEmpty()) {
                    String latestDate = timeSeries.keySet().iterator().next();
                    Map<String, String> latestData = (Map<String, String>) timeSeries.get(latestDate);

                    result.put("date", latestDate);
                    result.put("open", latestData.get("1a. open (USD)"));
                    result.put("high", latestData.get("2a. high (USD)"));
                    result.put("low", latestData.get("3a. low (USD)"));
                    result.put("close", latestData.get("4a. close (USD)"));
                    result.put("volume", latestData.get("5. volume"));
                }
            } else if ("stock".equalsIgnoreCase(assetType)) {
                // Für Aktien-Daten
                Map<String, Object> timeSeries = (Map<String, Object>) apiResponse.get("Time Series (Daily)");
                if (timeSeries != null && !timeSeries.isEmpty()) {
                    String latestDate = timeSeries.keySet().iterator().next();
                    Map<String, String> latestData = (Map<String, String>) timeSeries.get(latestDate);

                    result.put("date", latestDate);
                    result.put("open", latestData.get("1. open"));
                    result.put("high", latestData.get("2. high"));
                    result.put("low", latestData.get("3. low"));
                    result.put("close", latestData.get("4. close"));
                    result.put("volume", latestData.get("5. volume"));
                }
            } else if ("quote".equalsIgnoreCase(assetType)) {
                // Für Global Quote Daten
                Map<String, String> quote = (Map<String, String>) apiResponse.get("Global Quote");
                if (quote != null) {
                    result.put("symbol", quote.get("01. symbol"));
                    result.put("open", quote.get("02. open"));
                    result.put("high", quote.get("03. high"));
                    result.put("low", quote.get("04. low"));
                    result.put("price", quote.get("05. price"));
                    result.put("volume", quote.get("06. volume"));
                    result.put("latest_trading_day", quote.get("07. latest trading day"));
                    result.put("previous_close", quote.get("08. previous close"));
                    result.put("change", quote.get("09. change"));
                    result.put("change_percent", quote.get("10. change percent"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Extrahieren der Preisdaten: " + e.getMessage());
        }

        return result;
    }
}