package com.example.financemanager.service;

import com.example.financemanager.model.CurrencyRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExternalApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;

    @Value("${exchangerate.api.key:}")
    private String exchangeRateApiKey;

    // ========== SYMBOL MAPPING ==========
    private static final Map<String, String> CRYPTO_SYMBOL_TO_ID = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "ADA", "cardano",
            "DOT", "polkadot",
            "XRP", "ripple",
            "LTC", "litecoin",
            "USDT", "tether",
            "SOL", "solana",
            "MATIC", "matic-network",
            "AVAX", "avalanche-2"
    );

    private String getCoinGeckoId(String symbol) {
        String upperSymbol = symbol.toUpperCase();
        String coinId = CRYPTO_SYMBOL_TO_ID.get(upperSymbol);
        if (coinId != null) {
            System.out.println("Mapped " + symbol + " to CoinGecko ID: " + coinId);
            return coinId;
        }

        // Fallback to lowercase symbol
        String fallbackId = symbol.toLowerCase();
        System.out.println("No mapping found for " + symbol + ", using fallback: " + fallbackId);
        return fallbackId;
    }

    // ========== CURRENT PRICES WITH CHANGE DATA ==========
    public Map<String, Object> getCryptoCurrentPrice(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId +
                "&vs_currencies=" + vsCurrency.toLowerCase() +
                "&include_24hr_change=true&include_24hr_vol=true&include_last_updated_at=true";

        System.out.println("=== CoinGecko API Call ===");
        System.out.println("URL: " + url);
        System.out.println("Symbol: " + symbol + " -> CoinGecko ID: " + coinId);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("Raw CoinGecko Response: " + response);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Leere Antwort von CoinGecko für Symbol: " + symbol);
            }

            if (!response.containsKey(coinId)) {
                System.err.println("CoinGecko ID '" + coinId + "' nicht in Antwort gefunden.");
                System.err.println("Verfügbare Schlüssel: " + response.keySet());

                // Try alternative: if response has only one key, use it
                if (response.size() == 1) {
                    String actualKey = response.keySet().iterator().next();
                    System.out.println("Verwende alternativen Schlüssel: " + actualKey);
                    Map<String, Object> altResponse = new HashMap<>();
                    altResponse.put(coinId, response.get(actualKey));
                    return altResponse;
                }

                throw new RuntimeException("Krypto-Symbol nicht gefunden: " + symbol + " (ID: " + coinId + ")");
            }

            return response;
        } catch (RestClientException e) {
            System.err.println("CoinGecko API Fehler: " + e.getMessage());
            throw new RuntimeException("Fehler bei CoinGecko Current Price für " + symbol + ": " + e.getMessage());
        }
    }

    public Map<String, Object> getStockCurrentPrice(String symbol) {
        if (twelveDataApiKey == null || twelveDataApiKey.trim().isEmpty()) {
            throw new RuntimeException("Twelve Data API Key ist nicht konfiguriert");
        }

        String url = "https://api.twelvedata.com/quote?symbol=" + symbol.toUpperCase() +
                "&apikey=" + twelveDataApiKey;

        System.out.println("=== Twelve Data API Call ===");
        System.out.println("URL: " + url.replace(twelveDataApiKey, "***"));

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("Raw Twelve Data Response: " + response);

            if (response == null) {
                throw new RuntimeException("Leere Antwort von Twelve Data für Symbol: " + symbol);
            }

            if (response.containsKey("code") || response.containsKey("status")) {
                String errorMsg = "Twelve Data API Fehler: " + response;
                System.err.println(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Fehler bei Twelve Data Quote für " + symbol + ": " + e.getMessage());
        }
    }

    // ========== HISTORICAL DATA ==========
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

    public Map<String, Object> getStockDailyData(String symbol) {
        if (twelveDataApiKey == null || twelveDataApiKey.trim().isEmpty()) {
            throw new RuntimeException("Twelve Data API Key ist nicht konfiguriert");
        }

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

    public Map<String, Object> getStockIntradayData(String symbol, String interval) {
        if (twelveDataApiKey == null || twelveDataApiKey.trim().isEmpty()) {
            throw new RuntimeException("Twelve Data API Key ist nicht konfiguriert");
        }

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

    // ========== IMPROVED DATA EXTRACTION ==========
    public Map<String, Object> extractCurrentPrice(Map<String, Object> apiResponse, String type, String symbol) {
        Map<String, Object> priceData = new HashMap<>();

        System.out.println("=== DEBUG: Extracting price for " + symbol + " (" + type + ") ===");
        System.out.println("API Response keys: " + apiResponse.keySet());
        System.out.println("API Response: " + apiResponse);

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                return extractCryptoPrice(apiResponse, symbol);
            } else if ("stock".equalsIgnoreCase(type)) {
                return extractStockPrice(apiResponse, symbol);
            } else {
                throw new RuntimeException("Unbekannter Asset-Typ: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error extracting price data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Fehler beim Extrahieren der Preisdaten für " + symbol + ": " + e.getMessage(), e);
        }
    }

    private Map<String, Object> extractCryptoPrice(Map<String, Object> apiResponse, String symbol) {
        Map<String, Object> priceData = new HashMap<>();
        String coinId = getCoinGeckoId(symbol);

        System.out.println("Suche nach CoinGecko ID: " + coinId);
        System.out.println("Verfügbare Schlüssel in Antwort: " + apiResponse.keySet());

        // Try to find the coin data
        Map<String, Object> coinData = null;

        // First try exact match
        if (apiResponse.containsKey(coinId)) {
            coinData = (Map<String, Object>) apiResponse.get(coinId);
        }
        // If exact match fails, try all keys (in case of case sensitivity issues)
        else {
            for (String key : apiResponse.keySet()) {
                if (key.toLowerCase().equals(coinId.toLowerCase())) {
                    coinData = (Map<String, Object>) apiResponse.get(key);
                    System.out.println("Gefunden mit case-insensitive match: " + key);
                    break;
                }
            }
        }
        // Last resort: if only one entry, use it
        if (coinData == null && apiResponse.size() == 1) {
            String onlyKey = apiResponse.keySet().iterator().next();
            coinData = (Map<String, Object>) apiResponse.get(onlyKey);
            System.out.println("Verwende einzigen verfügbaren Schlüssel: " + onlyKey);
        }

        if (coinData == null) {
            throw new RuntimeException("Coin-Daten für " + symbol + " (ID: " + coinId + ") nicht gefunden in API-Antwort");
        }

        System.out.println("Coin-Daten gefunden: " + coinData);

        // Extract price (try multiple currency formats)
        String[] currencyKeys = {"usd", "USD", "eur", "EUR"};
        Double price = null;
        String usedCurrency = null;

        for (String currKey : currencyKeys) {
            Object priceObj = coinData.get(currKey);
            if (priceObj != null) {
                price = convertToDouble(priceObj);
                if (price != null) {
                    usedCurrency = currKey;
                    break;
                }
            }
        }

        if (price == null) {
            System.err.println("Kein Preis in Coin-Daten gefunden. Verfügbare Felder: " + coinData.keySet());
            throw new RuntimeException("Kein gültiger Preis für " + symbol + " gefunden");
        }

        priceData.put("price", price);
        System.out.println("Extrahierter Krypto-Preis: " + price + " " + usedCurrency.toUpperCase());

        // Extract 24h change
        String[] changeKeys = {
                usedCurrency + "_24h_change",
                usedCurrency.toUpperCase() + "_24h_change",
                "usd_24h_change",
                "USD_24h_change"
        };

        for (String changeKey : changeKeys) {
            Object changeObj = coinData.get(changeKey);
            if (changeObj != null) {
                Double change24h = convertToDouble(changeObj);
                if (change24h != null) {
                    priceData.put("change_percent", change24h);
                    // Calculate absolute change from percentage
                    Double absoluteChange = (price * change24h) / (100 + change24h);
                    priceData.put("change", absoluteChange);
                    System.out.println("Extrahierte Krypto-Änderung: " + change24h + "%");
                    break;
                }
            }
        }

        return priceData;
    }

    private Map<String, Object> extractStockPrice(Map<String, Object> apiResponse, String symbol) {
        Map<String, Object> priceData = new HashMap<>();

        System.out.println("Verarbeite Aktien-Daten für: " + symbol);
        System.out.println("Verfügbare Felder: " + apiResponse.keySet());

        // Try different field names for price
        String[] priceFields = {"close", "price", "last", "current_price", "open"};
        Double price = null;

        for (String field : priceFields) {
            Object priceObj = apiResponse.get(field);
            if (priceObj != null) {
                price = convertToDouble(priceObj);
                if (price != null) {
                    System.out.println("Preis gefunden in Feld '" + field + "': " + price);
                    break;
                }
            }
        }

        if (price == null) {
            System.err.println("Kein Preis in Aktien-Daten gefunden. Verfügbare Felder: " + apiResponse.keySet());
            throw new RuntimeException("Kein gültiger Preis für Aktie " + symbol + " gefunden");
        }

        priceData.put("price", price);

        // Extract change data
        Object changeObj = apiResponse.get("change");
        if (changeObj != null) {
            Double change = convertToDouble(changeObj);
            if (change != null) {
                priceData.put("change", change);
                System.out.println("Extrahierte Aktien-Änderung: " + change);
            }
        }

        Object percentChangeObj = apiResponse.get("percent_change");
        if (percentChangeObj != null) {
            Double changePercent = convertToDouble(percentChangeObj);
            if (changePercent != null) {
                priceData.put("change_percent", changePercent);
                System.out.println("Extrahierte Aktien-Änderung %: " + changePercent);
            }
        }

        return priceData;
    }

    // Improved helper method to safely convert objects to Double
    private Double convertToDouble(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            } else if (obj instanceof String) {
                String str = obj.toString().trim();
                if (str.isEmpty()) {
                    return null;
                }
                // Remove any percentage signs and commas
                str = str.replace("%", "").replace(",", "");
                return Double.parseDouble(str);
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert to double: '" + obj + "' (" + obj.getClass().getSimpleName() + ")");
        }

        return null;
    }

    public Map<String, Object> extractLatestPrice(Map<String, Object> apiResponse, String type) {
        Map<String, Object> priceData = new HashMap<>();

        try {
            if ("crypto".equalsIgnoreCase(type)) {
                List<List<Object>> prices = (List<List<Object>>) apiResponse.get("prices");
                if (prices != null && prices.size() >= 2) {
                    List<Object> currentEntry = prices.get(prices.size() - 1);
                    List<Object> previousEntry = prices.get(prices.size() - 2);

                    Long timestamp = ((Number) currentEntry.get(0)).longValue();
                    Double currentPrice = ((Number) currentEntry.get(1)).doubleValue();
                    Double previousPrice = ((Number) previousEntry.get(1)).doubleValue();

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
    public CurrencyRate getExchangeRate(String fromCurrency, String toCurrency) {
        // Zuerst exchangerate-api.com mit API-Key versuchen
        try {
            return getExchangeRateFromApi(fromCurrency, toCurrency);
        } catch (Exception e) {
            System.err.println("exchangerate-api.com fehlgeschlagen: " + e.getMessage());

            // Fallback zu exchangerate.host
            try {
                return getExchangeRateFromHost(fromCurrency, toCurrency);
            } catch (Exception e2) {
                System.err.println("exchangerate.host fehlgeschlagen: " + e2.getMessage());
                throw new RuntimeException("Alle Wechselkurs-APIs sind fehlgeschlagen. Letzte Fehlermeldung: " + e2.getMessage());
            }
        }
    }

    private CurrencyRate getExchangeRateFromApi(String fromCurrency, String toCurrency) {
        if (exchangeRateApiKey == null || exchangeRateApiKey.trim().isEmpty()) {
            throw new RuntimeException("ExchangeRate API Key ist nicht konfiguriert");
        }

        String url = "https://api.exchangerate-api.com/v4/latest/" + fromCurrency.toUpperCase() +
                "?apikey=" + exchangeRateApiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new RuntimeException("Keine Antwort von exchangerate-api.com");
            }

            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
            if (rates == null || !rates.containsKey(toCurrency.toUpperCase())) {
                throw new RuntimeException("Wechselkurs für " + toCurrency + " nicht gefunden");
            }

            Double rate = rates.get(toCurrency.toUpperCase());
            return createCurrencyRate(fromCurrency, toCurrency, rate);

        } catch (RestClientException e) {
            throw new RuntimeException("Netzwerkfehler bei exchangerate-api.com: " + e.getMessage(), e);
        }
    }

    private CurrencyRate getExchangeRateFromHost(String fromCurrency, String toCurrency) {
        String url = "https://api.exchangerate.host/latest?base=" + fromCurrency.toUpperCase() +
                "&symbols=" + toCurrency.toUpperCase();

        try {
            ResponseEntity<Map> responseEntity = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> response = responseEntity.getBody();

            if (response == null || !(Boolean)response.get("success")) {
                throw new RuntimeException("exchangerate.host API Fehler: " + response);
            }

            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
            if (rates == null || !rates.containsKey(toCurrency.toUpperCase())) {
                throw new RuntimeException("Wechselkurs für " + toCurrency + " nicht gefunden");
            }

            Double rate = rates.get(toCurrency.toUpperCase());
            return createCurrencyRate(fromCurrency, toCurrency, rate);

        } catch (RestClientException e) {
            throw new RuntimeException("Netzwerkfehler bei exchangerate.host: " + e.getMessage(), e);
        }
    }

    private CurrencyRate createCurrencyRate(String fromCurrency, String toCurrency, Double rate) {
        CurrencyRate currencyRate = new CurrencyRate();
        currencyRate.setFromCurrency(fromCurrency.toUpperCase());
        currencyRate.setToCurrency(toCurrency.toUpperCase());
        currencyRate.setRate(BigDecimal.valueOf(rate));
        currencyRate.setTimestamp(LocalDateTime.now());
        return currencyRate;
    }
}