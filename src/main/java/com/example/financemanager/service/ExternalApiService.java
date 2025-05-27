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

    @Value("${twelvedata.api.key}")
    private String twelveDataApiKey;

    // ========== SYMBOL MAPPING ==========

    /**
     * Mapping von Ticker-Symbolen zu CoinGecko IDs
     */
    private static final Map<String, String> CRYPTO_SYMBOL_TO_ID = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "ADA", "cardano",
            "DOT", "polkadot",
            "XRP", "ripple",
            "LTC", "litecoin",
            "BCH", "bitcoin-cash",
            "BNB", "binancecoin"
    );

    /**
     * Konvertiert Ticker-Symbol zu CoinGecko ID
     */
    private String getCoinGeckoId(String symbol) {
        return CRYPTO_SYMBOL_TO_ID.getOrDefault(symbol.toUpperCase(), symbol.toLowerCase());
    }

    // ========== CURRENT PRICES WITH CHANGE DATA ==========

    /**
     * Aktuelle Krypto-Preise mit 24h Änderung von CoinGecko
     */
    public Map<String, Object> getCryptoCurrentPrice(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId +
                "&vs_currencies=" + vsCurrency.toLowerCase() +
                "&include_24hr_change=true&include_24hr_vol=true";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey(coinId)) {
                throw new RuntimeException("Krypto-Symbol nicht gefunden: " + symbol);
            }
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei CoinGecko Current Price: " + e.getMessage());
        }
    }

    /**
     * Aktueller Aktienpreis mit Change-Daten von Twelve Data Quote-Endpoint
     */
    public Map<String, Object> getStockCurrentPrice(String symbol) {
        String url = "https://api.twelvedata.com/quote?symbol=" + symbol.toUpperCase() +
                "&apikey=" + twelveDataApiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.containsKey("code")) {
                throw new RuntimeException("Twelve Data Quote API Fehler: " + response);
            }
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei Twelve Data Quote: " + e.getMessage());
        }
    }

    // ========== HISTORICAL DATA ==========

    /**
     * Krypto-Tagesdaten von CoinGecko
     */
    public Map<String, Object> getCryptoDailyData(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/market_chart" +
                "?vs_currency=" + vsCurrency.toLowerCase() + "&days=30&interval=daily";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new RuntimeException("Keine Daten von CoinGecko erhalten");
            }
            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei CoinGecko Historical Data: " + e.getMessage());
        }
    }

    /**
     * Krypto-Intraday-Daten von CoinGecko
     */
    public Map<String, Object> getCryptoIntradayData(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/market_chart" +
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
     */
    public Map<String, Object> getStockDailyData(String symbol) {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol.toUpperCase() +
                "&interval=1day&apikey=" + twelveDataApiKey;

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

    // ========== DATA EXTRACTION WITH CHANGE CALCULATION ==========

    /**
     * Extrahiert aktuelle Preisdaten mit Change-Informationen
     */
    public Map<String, Object> extractCurrentPrice(Map<String, Object> apiResponse, String type, String symbol) {
        Map<String, Object> priceData = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                String coinId = getCoinGeckoId(symbol);
                Map<String, Object> coinData = (Map<String, Object>) apiResponse.get(coinId);

                if (coinData != null) {
                    Object priceObj = coinData.get("usd");
                    Object changeObj = coinData.get("usd_24h_change");

                    if (priceObj != null) {
                        Double price = ((Number) priceObj).doubleValue();
                        priceData.put("price", price);

                        if (changeObj != null) {
                            Double change24h = ((Number) changeObj).doubleValue();
                            priceData.put("change_percent", change24h);
                            // Absolute Änderung berechnen
                            Double absoluteChange = (price * change24h) / (100 + change24h);
                            priceData.put("change", absoluteChange);
                        }
                    }
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                // Twelve Data Quote Response
                Object closeObj = apiResponse.get("close");
                Object changeObj = apiResponse.get("change");
                Object percentChangeObj = apiResponse.get("percent_change");

                if (closeObj != null) {
                    Double price = Double.parseDouble(closeObj.toString());
                    priceData.put("price", price);

                    if (changeObj != null) {
                        Double change = Double.parseDouble(changeObj.toString());
                        priceData.put("change", change);
                    }

                    if (percentChangeObj != null) {
                        Double changePercent = Double.parseDouble(percentChangeObj.toString());
                        priceData.put("change_percent", changePercent);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Extrahieren der Preisdaten: " + e.getMessage(), e);
        }

        return priceData;
    }

    /**
     * Extrahiert den letzten verfügbaren Preis aus historischen Daten
     */
    public Map<String, Object> extractLatestPrice(Map<String, Object> apiResponse, String type) {
        Map<String, Object> priceData = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                List<List<Object>> prices = (List<List<Object>>) apiResponse.get("prices");
                if (prices != null && prices.size() >= 2) {
                    // Aktueller und vorheriger Preis für Change-Berechnung
                    List<Object> currentEntry = prices.get(prices.size() - 1);
                    List<Object> previousEntry = prices.get(prices.size() - 2);

                    Long timestamp = ((Number) currentEntry.get(0)).longValue();
                    Double currentPrice = ((Number) currentEntry.get(1)).doubleValue();
                    Double previousPrice = ((Number) previousEntry.get(1)).doubleValue();

                    // Change berechnen
                    Double change = currentPrice - previousPrice;
                    Double changePercent = (change / previousPrice) * 100;

                    priceData.put("timestamp", timestamp);
                    priceData.put("price", currentPrice);
                    priceData.put("change", change);
                    priceData.put("change_percent", changePercent);
                } else {
                    throw new RuntimeException("Nicht genügend Preisdaten für Change-Berechnung");
                }
            } else if ("stock".equalsIgnoreCase(type)) {
                List<Map<String, String>> values = (List<Map<String, String>>) apiResponse.get("values");
                if (values != null && values.size() >= 2) {
                    Map<String, String> current = values.get(0);
                    Map<String, String> previous = values.get(1);

                    String datetime = current.get("datetime");
                    Double currentPrice = Double.parseDouble(current.get("close"));
                    Double previousPrice = Double.parseDouble(previous.get("close"));

                    // Change berechnen
                    Double change = currentPrice - previousPrice;
                    Double changePercent = (change / previousPrice) * 100;

                    priceData.put("timestamp", datetime);
                    priceData.put("price", currentPrice);
                    priceData.put("change", change);
                    priceData.put("change_percent", changePercent);
                } else {
                    throw new RuntimeException("Nicht genügend Preisdaten für Change-Berechnung");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Extrahieren des Preises: " + e.getMessage(), e);
        }

        return priceData;
    }

    // ========== CURRENCY EXCHANGE ==========

    /**
     * Wechselkurs zwischen zwei Fiat-Währungen
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
}