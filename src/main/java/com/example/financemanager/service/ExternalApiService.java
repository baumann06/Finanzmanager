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

    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;

    @Value("${exchangerate.api.key:}")
    private String exchangeRateApiKey;

    private static final Map<String, String> CRYPTO_SYMBOL_TO_ID = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "ADA", "cardano",
            "DOT", "polkadot",
            "XRP", "ripple",
            "LTC", "litecoin",
            "USDT", "tether",
            "SOL", "solana",
            "USDC", "usd-coin",
            "BNB", "binancecoin"
    );

    public boolean isCryptoSymbol(String symbol) {
        return CRYPTO_SYMBOL_TO_ID.containsKey(symbol.toUpperCase());
    }

    public String getAssetType(String symbol) {
        return isCryptoSymbol(symbol) ? "crypto" : "stock";
    }

    private String getCoinGeckoId(String symbol) {
        String upperSymbol = symbol.toUpperCase();
        return CRYPTO_SYMBOL_TO_ID.getOrDefault(upperSymbol, symbol.toLowerCase());
    }

    /**
     * Hauptmethode für Preisabfrage - berücksichtigt explizit den gewählten Asset-Typ
     */
    public Map<String, Object> getAssetPrice(String symbol, String assetType, String vsCurrency) {
        // Explizite Typ-Überprüfung basierend auf Frontend-Auswahl
        if ("crypto".equalsIgnoreCase(assetType)) {
            return getCryptoCurrentPrice(symbol, vsCurrency != null ? vsCurrency : "usd");
        } else if ("stock".equalsIgnoreCase(assetType)) {
            return getStockCurrentPrice(symbol);
        } else {
            // Fallback: Auto-Detection nur wenn kein Typ spezifiziert
            String detectedType = getAssetType(symbol);
            if ("crypto".equals(detectedType)) {
                return getCryptoCurrentPrice(symbol, vsCurrency != null ? vsCurrency : "usd");
            } else {
                return getStockCurrentPrice(symbol);
            }
        }
    }

    public Map<String, Object> getCryptoCurrentPrice(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId +
                "&vs_currencies=" + vsCurrency.toLowerCase() +
                "&include_24hr_change=true";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from CoinGecko for symbol: " + symbol);
            }

            // Handle case where API returns different key than expected
            if (!response.containsKey(coinId) && response.size() == 1) {
                String actualKey = response.keySet().iterator().next();
                Map<String, Object> altResponse = new HashMap<>();
                altResponse.put(coinId, response.get(actualKey));
                return altResponse;
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("CoinGecko API error for " + symbol + ": " + e.getMessage());
        }
    }

    public Map<String, Object> getStockCurrentPrice(String symbol) {
        // Priorität: TwelveData API wenn verfügbar, sonst Fallback-Alternativen
        if (twelveDataApiKey != null && !twelveDataApiKey.trim().isEmpty()) {
            try {
                return getTwelveDataStockPrice(symbol);
            } catch (Exception e) {
                System.err.println("TwelveData failed for " + symbol + ": " + e.getMessage());
                // Fall through to alternative
            }
        }

        // Fallback-Kette
        return getAlternativeStockPrice(symbol);
    }

    private Map<String, Object> getTwelveDataStockPrice(String symbol) {
        String url = "https://api.twelvedata.com/quote?symbol=" + symbol.toUpperCase() +
                "&apikey=" + twelveDataApiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new RuntimeException("Empty response from Twelve Data for symbol: " + symbol);
            }

            // Check for API error response
            if (response.containsKey("code")) {
                String code = response.get("code").toString();
                String message = response.getOrDefault("message", "Unknown error").toString();
                throw new RuntimeException("Twelve Data API Error: " + message);
            }

            // Check for status field (sometimes used instead of code)
            if ("error".equals(response.get("status"))) {
                String message = response.getOrDefault("message", "Unknown error").toString();
                throw new RuntimeException("Twelve Data API Error: " + message);
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Twelve Data network error for " + symbol + ": " + e.getMessage());
        }
    }

    private Map<String, Object> getAlternativeStockPrice(String symbol) {
        try {
            return getYahooFinancePrice(symbol);
        } catch (Exception e) {
            System.err.println("Yahoo Finance failed for " + symbol + ": " + e.getMessage());
            return getMockStockPrice(symbol);
        }
    }

    private Map<String, Object> getYahooFinancePrice(String symbol) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol.toUpperCase();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("chart")) {
                Map<String, Object> chart = (Map<String, Object>) response.get("chart");
                List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");

                if (result != null && !result.isEmpty()) {
                    Map<String, Object> stockData = result.get(0);
                    Map<String, Object> meta = (Map<String, Object>) stockData.get("meta");

                    if (meta != null) {
                        Map<String, Object> formattedResponse = new HashMap<>();
                        Object regularMarketPrice = meta.get("regularMarketPrice");
                        Object previousClose = meta.get("previousClose");

                        if (regularMarketPrice != null) {
                            double currentPrice = ((Number) regularMarketPrice).doubleValue();
                            double prevClose = previousClose != null ?
                                    ((Number) previousClose).doubleValue() : currentPrice;

                            formattedResponse.put("close", currentPrice);
                            formattedResponse.put("previous_close", prevClose);
                            formattedResponse.put("change", currentPrice - prevClose);
                            formattedResponse.put("change_percent",
                                    prevClose != 0 ? ((currentPrice - prevClose) / prevClose) * 100 : 0);

                            return formattedResponse;
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid Yahoo Finance response structure");
        } catch (Exception e) {
            throw new RuntimeException("Yahoo Finance API error: " + e.getMessage());
        }
    }

    private Map<String, Object> getMockStockPrice(String symbol) {
        Map<String, Object> mockResponse = new HashMap<>();
        double basePrice = 100.00 + (symbol.hashCode() % 1000); // Deterministic base price
        double change = (Math.random() - 0.5) * 10;
        double currentPrice = basePrice + change;

        mockResponse.put("close", currentPrice);
        mockResponse.put("previous_close", basePrice);
        mockResponse.put("change", change);
        mockResponse.put("change_percent", (change / basePrice) * 100);

        System.out.println("Using mock data for stock: " + symbol + " (Price: " + currentPrice + ")");
        return mockResponse;
    }

    public Map<String, Object> extractCurrentPrice(Map<String, Object> apiResponse, String type, String symbol) {
        if ("crypto".equalsIgnoreCase(type)) {
            return extractCryptoPrice(apiResponse, symbol);
        } else if ("stock".equalsIgnoreCase(type)) {
            return extractStockPrice(apiResponse, symbol);
        } else {
            throw new RuntimeException("Unknown asset type: " + type);
        }
    }

    private Map<String, Object> extractCryptoPrice(Map<String, Object> apiResponse, String symbol) {
        Map<String, Object> priceData = new HashMap<>();
        String coinId = getCoinGeckoId(symbol);

        Map<String, Object> coinData = (Map<String, Object>) apiResponse.get(coinId);
        if (coinData == null && apiResponse.size() == 1) {
            coinData = (Map<String, Object>) apiResponse.get(apiResponse.keySet().iterator().next());
        }

        if (coinData == null) {
            throw new RuntimeException("Coin data not found for " + symbol);
        }

        Double price = convertToDouble(coinData.get("usd"));
        if (price == null) {
            throw new RuntimeException("No valid price found for " + symbol);
        }

        priceData.put("price", price);

        // Add 24h change if available
        if (coinData.containsKey("usd_24h_change")) {
            Double change24h = convertToDouble(coinData.get("usd_24h_change"));
            if (change24h != null) {
                priceData.put("change_percent", change24h);
            }
        }

        return priceData;
    }

    private Map<String, Object> extractStockPrice(Map<String, Object> apiResponse, String symbol) {
        Map<String, Object> priceData = new HashMap<>();

        // Handle different response formats from different APIs
        Double price = null;
        Double change = null;
        Double changePercent = null;

        // TwelveData format
        if (apiResponse.containsKey("close")) {
            price = convertToDouble(apiResponse.get("close"));
            change = convertToDouble(apiResponse.get("change"));
            changePercent = convertToDouble(apiResponse.get("percent_change"));
        }
        // Alternative formats
        else if (apiResponse.containsKey("price")) {
            price = convertToDouble(apiResponse.get("price"));
        }

        if (price == null) {
            throw new RuntimeException("No valid price found for stock " + symbol);
        }

        priceData.put("price", price);

        if (change != null) {
            priceData.put("change", change);
        }

        if (changePercent != null) {
            priceData.put("change_percent", changePercent);
        } else if (change != null && apiResponse.containsKey("previous_close")) {
            // Calculate percentage change if not provided
            Double previousClose = convertToDouble(apiResponse.get("previous_close"));
            if (previousClose != null && previousClose != 0) {
                priceData.put("change_percent", (change / previousClose) * 100);
            }
        }

        return priceData;
    }

    private Double convertToDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // ========== HISTORICAL DATA METHODS ==========

    public Map<String, Object> getCryptoDailyData(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId +
                "/market_chart?vs_currency=" + vsCurrency.toLowerCase() +
                "&days=7&interval=daily";

        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            throw new RuntimeException("CoinGecko history API error for " + symbol + ": " + e.getMessage());
        }
    }

    public Map<String, Object> getCryptoIntradayData(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId +
                "/market_chart?vs_currency=" + vsCurrency.toLowerCase() +
                "&days=1&interval=hourly";

        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            throw new RuntimeException("CoinGecko intraday API error for " + symbol + ": " + e.getMessage());
        }
    }

    public Map<String, Object> getStockDailyData(String symbol) {
        if (twelveDataApiKey != null && !twelveDataApiKey.trim().isEmpty()) {
            try {
                return getTwelveDataStockHistory(symbol, "1day", "7");
            } catch (Exception e) {
                System.err.println("TwelveData history failed for " + symbol + ": " + e.getMessage());
            }
        }

        // Fallback to mock data for demonstration
        return getMockStockHistory(symbol);
    }

    public Map<String, Object> getStockIntradayData(String symbol, String interval) {
        if (twelveDataApiKey != null && !twelveDataApiKey.trim().isEmpty()) {
            try {
                return getTwelveDataStockHistory(symbol, interval, "1");
            } catch (Exception e) {
                System.err.println("TwelveData intraday failed for " + symbol + ": " + e.getMessage());
            }
        }

        return getMockStockHistory(symbol);
    }

    private Map<String, Object> getTwelveDataStockHistory(String symbol, String interval, String outputsize) {
        String url = "https://api.twelvedata.com/time_series?symbol=" + symbol.toUpperCase() +
                "&interval=" + interval +
                "&outputsize=" + outputsize +
                "&apikey=" + twelveDataApiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new RuntimeException("Empty response from Twelve Data history");
            }

            if (response.containsKey("code")) {
                String message = response.getOrDefault("message", "Unknown error").toString();
                throw new RuntimeException("Twelve Data History API Error: " + message);
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Twelve Data history network error: " + e.getMessage());
        }
    }

    private Map<String, Object> getMockStockHistory(String symbol) {
        Map<String, Object> mockHistory = new HashMap<>();
        List<Map<String, Object>> values = new java.util.ArrayList<>();

        double basePrice = 100.00 + (symbol.hashCode() % 1000);

        for (int i = 6; i >= 0; i--) {
            Map<String, Object> dayData = new HashMap<>();
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            double price = basePrice + (Math.random() - 0.5) * 20;

            dayData.put("datetime", date.toString().substring(0, 10));
            dayData.put("close", String.format("%.2f", price));
            dayData.put("open", String.format("%.2f", price + (Math.random() - 0.5) * 5));
            dayData.put("high", String.format("%.2f", price + Math.random() * 10));
            dayData.put("low", String.format("%.2f", price - Math.random() * 10));

            values.add(dayData);
        }

        mockHistory.put("values", values);
        mockHistory.put("status", "ok");

        System.out.println("⚠️ Using mock history data for stock: " + symbol);
        return mockHistory;
    }

    // ========== EXCHANGE RATE METHODS (unchanged) ==========

    public CurrencyRate getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            return getExchangeRateFromApi(fromCurrency, toCurrency);
        } catch (Exception e) {
            try {
                return getExchangeRateFromHost(fromCurrency, toCurrency);
            } catch (Exception e2) {
                throw new RuntimeException("All exchange rate APIs failed: " + e2.getMessage());
            }
        }
    }

    private CurrencyRate getExchangeRateFromApi(String fromCurrency, String toCurrency) {
        if (exchangeRateApiKey == null || exchangeRateApiKey.trim().isEmpty()) {
            throw new RuntimeException("ExchangeRate API Key not configured");
        }

        String url = "https://api.exchangerate-api.com/v4/latest/" + fromCurrency.toUpperCase() +
                "?apikey=" + exchangeRateApiKey;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            throw new RuntimeException("No response from exchangerate-api.com");
        }

        Map<String, Double> rates = (Map<String, Double>) response.get("rates");
        if (rates == null || !rates.containsKey(toCurrency.toUpperCase())) {
            throw new RuntimeException("Exchange rate for " + toCurrency + " not found");
        }

        return createCurrencyRate(fromCurrency, toCurrency, rates.get(toCurrency.toUpperCase()));
    }

    private CurrencyRate getExchangeRateFromHost(String fromCurrency, String toCurrency) {
        String url = "https://api.exchangerate.host/latest?base=" + fromCurrency.toUpperCase() +
                "&symbols=" + toCurrency.toUpperCase();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !(Boolean)response.get("success")) {
            throw new RuntimeException("exchangerate.host API error");
        }

        Map<String, Double> rates = (Map<String, Double>) response.get("rates");
        if (rates == null || !rates.containsKey(toCurrency.toUpperCase())) {
            throw new RuntimeException("Exchange rate for " + toCurrency + " not found");
        }

        return createCurrencyRate(fromCurrency, toCurrency, rates.get(toCurrency.toUpperCase()));
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