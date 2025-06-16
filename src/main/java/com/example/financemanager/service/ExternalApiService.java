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

/**
 * Service-Klasse für die Kommunikation mit externen Finanz-APIs.
 * Bietet Funktionalitäten für:
 * - Aktuelle Preise von Kryptowährungen und Aktien
 * - Historische Kursdaten
 * - Wechselkurse zwischen verschiedenen Währungen
 */
@Service
public class ExternalApiService {

    // RestTemplate für HTTP-Anfragen an externe APIs
    private final RestTemplate restTemplate = new RestTemplate();

    // API-Schlüssel für TwelveData (Aktien-API) - aus application.properties
    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;

    // API-Schlüssel für ExchangeRate-API (Wechselkurse) - aus application.properties
    @Value("${exchangerate.api.key:}")
    private String exchangeRateApiKey;

    /**
     * Mapping von Kryptowährungs-Symbolen zu CoinGecko-IDs.
     * CoinGecko verwendet spezifische IDs anstelle der üblichen Ticker-Symbole.
     */
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

    /**
     * Prüft, ob ein Symbol eine bekannte Kryptowährung repräsentiert.
     *
     * @param symbol Das zu prüfende Symbol (z.B. "BTC", "ETH")
     * @return true wenn es sich um eine Kryptowährung handelt, false sonst
     */
    public boolean isCryptoSymbol(String symbol) {
        return CRYPTO_SYMBOL_TO_ID.containsKey(symbol.toUpperCase());
    }

    /**
     * Bestimmt automatisch den Asset-Typ basierend auf dem Symbol.
     *
     * @param symbol Das Symbol des Assets
     * @return "crypto" für Kryptowährungen, "stock" für Aktien
     */
    public String getAssetType(String symbol) {
        return isCryptoSymbol(symbol) ? "crypto" : "stock";
    }

    /**
     * Konvertiert ein Krypto-Symbol zu seiner entsprechenden CoinGecko-ID.
     *
     * @param symbol Das Krypto-Symbol (z.B. "BTC")
     * @return Die entsprechende CoinGecko-ID (z.B. "bitcoin")
     */
    private String getCoinGeckoId(String symbol) {
        String upperSymbol = symbol.toUpperCase();
        return CRYPTO_SYMBOL_TO_ID.getOrDefault(upperSymbol, symbol.toLowerCase());
    }

    /**
     * Hauptmethode für Preisabfrage - berücksichtigt explizit den gewählten Asset-Typ.
     * Diese Methode entscheidet basierend auf dem Asset-Typ, welche API verwendet wird.
     *
     * @param symbol Das Symbol des Assets (z.B. "BTC", "AAPL")
     * @param assetType Der explizit gewählte Asset-Typ ("crypto" oder "stock")
     * @param vsCurrency Die Zielwährung für Kryptowährungen (Standard: "usd")
     * @return Map mit Preisdaten der entsprechenden API
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

    /**
     * Ruft aktuelle Kryptowährungsdaten von der CoinGecko-API ab.
     *
     * @param symbol Das Kryptowährungs-Symbol
     * @param vsCurrency Die Zielwährung (z.B. "usd", "eur")
     * @return Map mit aktuellen Preisdaten inklusive 24h-Änderung
     * @throws RuntimeException bei API-Fehlern oder leeren Antworten
     */
    public Map<String, Object> getCryptoCurrentPrice(String symbol, String vsCurrency) {
        String coinId = getCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId +
                "&vs_currencies=" + vsCurrency.toLowerCase() +
                "&include_24hr_change=true";

        try {
            // Sendet die HTTP-GET-Anfrage und erwartet eine Antwort als Map
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from CoinGecko for symbol: " + symbol);
            }

            // Behandlung von Fällen, wo die API einen anderen Schlüssel als erwartet zurückgibt
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

    /**
     * Ruft aktuelle Aktiendaten ab mit Fallback-Mechanismus.
     * Versucht zuerst TwelveData, dann alternative APIs.
     *
     * @param symbol Das Aktien-Symbol (z.B. "AAPL", "TSLA")
     * @return Map mit aktuellen Aktiendaten
     */
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

        // wenn TwelveData nicht verfügbar oder fehlschlägt
        return getAlternativeStockPrice(symbol);
    }

    /**
     * Ruft Aktiendaten von der TwelveData-API ab.
     *
     * @param symbol Das Aktien-Symbol
     * @return Map mit Aktiendaten von TwelveData
     * @throws RuntimeException bei API-Fehlern
     */
    private Map<String, Object> getTwelveDataStockPrice(String symbol) {
        String url = "https://api.twelvedata.com/quote?symbol=" + symbol.toUpperCase() +
                "&apikey=" + twelveDataApiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new RuntimeException("Empty response from Twelve Data for symbol: " + symbol);
            }

            // Prüfung auf API-Fehler-Antworten (code-Feld)
            if (response.containsKey("code")) {
                String code = response.get("code").toString();
                String message = response.getOrDefault("message", "Unknown error").toString();
                throw new RuntimeException("Twelve Data API Error: " + message);
            }

            // Prüfung auf Status-Feld (alternative Fehlerbehandlung)
            if ("error".equals(response.get("status"))) {
                String message = response.getOrDefault("message", "Unknown error").toString();
                throw new RuntimeException("Twelve Data API Error: " + message);
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Twelve Data network error for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Fallback-Methode für Aktiendaten wenn TwelveData nicht verfügbar ist.
     * Versucht zuerst Yahoo Finance, dann Mock-Daten.
     *
     * @param symbol Das Aktien-Symbol
     * @return Map mit Aktiendaten aus alternativen Quellen
     */
    private Map<String, Object> getAlternativeStockPrice(String symbol) {
        try {
            return getYahooFinancePrice(symbol);
        } catch (Exception e) {
            System.err.println("Yahoo Finance failed for " + symbol + ": " + e.getMessage());
            // Letzter Fallback: Mock-Daten für Demonstrationszwecke
            return getMockStockPrice(symbol);
        }
    }

    /**
     * Ruft Aktiendaten von Yahoo Finance ab (kostenlose API).
     *
     * @param symbol Das Aktien-Symbol
     * @return Map mit formatierten Aktiendaten
     * @throws RuntimeException bei API-Fehlern oder ungültiger Antwortstruktur
     */
    private Map<String, Object> getYahooFinancePrice(String symbol) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol.toUpperCase();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // Navigation durch die komplexe Yahoo Finance API-Struktur
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

                            // Formatierung der Daten in einheitliches Format
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

    /**
     * Generiert Mock-Aktiendaten für Demonstrationszwecke.
     * Wird als letzter Fallback verwendet wenn alle APIs fehlschlagen.
     *
     * @param symbol Das Aktien-Symbol
     * @return Map mit simulierten Aktiendaten
     */
    private Map<String, Object> getMockStockPrice(String symbol) {
        Map<String, Object> mockResponse = new HashMap<>();
        // Deterministischer Basispreis basierend auf Symbol-Hash
        double basePrice = 100.00 + (symbol.hashCode() % 1000);
        double change = (Math.random() - 0.5) * 10; // Zufällige Änderung ±5
        double currentPrice = basePrice + change;

        mockResponse.put("close", currentPrice);
        mockResponse.put("previous_close", basePrice);
        mockResponse.put("change", change);
        mockResponse.put("change_percent", (change / basePrice) * 100);

        System.out.println("Using mock data for stock: " + symbol + " (Price: " + currentPrice + ")");
        return mockResponse;
    }

    /**
     * Extrahiert und normalisiert Preisdaten aus verschiedenen API-Antworten.
     * Zentrale Methode zur einheitlichen Datenverarbeitung.
     *
     * @param apiResponse Die rohe API-Antwort
     * @param type Der Asset-Typ ("crypto" oder "stock")
     * @param symbol Das Asset-Symbol
     * @return Map mit normalisierten Preisdaten
     */
    public Map<String, Object> extractCurrentPrice(Map<String, Object> apiResponse, String type, String symbol) {
        if ("crypto".equalsIgnoreCase(type)) {
            return extractCryptoPrice(apiResponse, symbol);
        } else if ("stock".equalsIgnoreCase(type)) {
            return extractStockPrice(apiResponse, symbol);
        } else {
            throw new RuntimeException("Unknown asset type: " + type);
        }
    }

    /**
     * Extrahiert Kryptowährungs-Preisdaten aus CoinGecko-API-Antworten.
     *
     * @param apiResponse Die CoinGecko-API-Antwort
     * @param symbol Das Krypto-Symbol
     * @return Map mit normalisierten Krypto-Preisdaten
     */
    private Map<String, Object> extractCryptoPrice(Map<String, Object> apiResponse, String symbol) {
        Map<String, Object> priceData = new HashMap<>();
        String coinId = getCoinGeckoId(symbol);

        // Suche nach den Coin-Daten in der API-Antwort
        Map<String, Object> coinData = (Map<String, Object>) apiResponse.get(coinId);
        if (coinData == null && apiResponse.size() == 1) {
            // Fallback: Verwende den ersten verfügbaren Schlüssel
            coinData = (Map<String, Object>) apiResponse.get(apiResponse.keySet().iterator().next());
        }

        if (coinData == null) {
            throw new RuntimeException("Coin data not found for " + symbol);
        }

        // Extraktion des USD-Preises
        Double price = convertToDouble(coinData.get("usd"));
        if (price == null) {
            throw new RuntimeException("No valid price found for " + symbol);
        }

        priceData.put("price", price);

        // Hinzufügen der 24h-Änderung falls verfügbar
        if (coinData.containsKey("usd_24h_change")) {
            Double change24h = convertToDouble(coinData.get("usd_24h_change"));
            if (change24h != null) {
                priceData.put("change_percent", change24h);
            }
        }

        return priceData;
    }

    /**
     * Extrahiert Aktien-Preisdaten aus verschiedenen API-Antwortformaten.
     * Unterstützt TwelveData, Yahoo Finance und andere Formate.
     *
     * @param apiResponse Die API-Antwort von einer Aktien-API
     * @param symbol Das Aktien-Symbol
     * @return Map mit normalisierten Aktien-Preisdaten
     */
    private Map<String, Object> extractStockPrice(Map<String, Object> apiResponse, String symbol) {
        Map<String, Object> priceData = new HashMap<>();

        // Behandlung verschiedener Antwortformate von verschiedenen APIs
        Double price = null;
        Double change = null;
        Double changePercent = null;

        // TwelveData-Format
        if (apiResponse.containsKey("close")) {
            price = convertToDouble(apiResponse.get("close"));
            change = convertToDouble(apiResponse.get("change"));
            changePercent = convertToDouble(apiResponse.get("percent_change"));
        }
        // Alternative Formate (z.B. Yahoo Finance normalisiert)
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
            // Berechnung der prozentualen Änderung falls nicht direkt verfügbar
            Double previousClose = convertToDouble(apiResponse.get("previous_close"));
            if (previousClose != null && previousClose != 0) {
                priceData.put("change_percent", (change / previousClose) * 100);
            }
        }

        return priceData;
    }

    /**
     * Hilfsmethode zur sicheren Konvertierung von Objekten zu Double-Werten.
     * Behandelt sowohl Number- als auch String-Eingaben.
     *
     * @param obj Das zu konvertierende Objekt
     * @return Double-Wert oder null bei Konvertierungsfehlern
     */
    private Double convertToDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null; // Sichere Behandlung ungültiger String-Werte
            }
        }
        return null;
    }

    // ========== HISTORICAL DATA METHODS ==========

    /**
     * Ruft tägliche historische Daten für Kryptowährungen ab (7 Tage).
     *
     * @param symbol Das Kryptowährungs-Symbol
     * @param vsCurrency Die Zielwährung
     * @return Map mit historischen Preisdaten
     */
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

    /**
     * Ruft stündliche Intraday-Daten für Kryptowährungen ab (1 Tag).
     *
     * @param symbol Das Kryptowährungs-Symbol
     * @param vsCurrency Die Zielwährung
     * @return Map mit stündlichen Preisdaten
     */
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

    /**
     * Ruft tägliche historische Daten für Aktien ab.
     * Verwendet TwelveData wenn verfügbar, sonst Mock-Daten.
     *
     * @param symbol Das Aktien-Symbol
     * @return Map mit historischen Aktiendaten
     */
    public Map<String, Object> getStockDailyData(String symbol) {
        if (twelveDataApiKey != null && !twelveDataApiKey.trim().isEmpty()) {
            try {
                return getTwelveDataStockHistory(symbol, "1day", "7");
            } catch (Exception e) {
                System.err.println("TwelveData history failed for " + symbol + ": " + e.getMessage());
            }
        }

        // Fallback zu Mock-Daten für Demonstrationszwecke
        return getMockStockHistory(symbol);
    }

    /**
     * Ruft Intraday-Daten für Aktien ab.
     *
     * @param symbol Das Aktien-Symbol
     * @param interval Das Zeitintervall (z.B. "5min", "15min", "1h")
     * @return Map mit Intraday-Aktiendaten
     */
    public Map<String, Object> getStockIntradayData(String symbol, String interval) {
        if (twelveDataApiKey != null && !twelveDataApiKey.trim().isEmpty()) {
            try {
                return getTwelveDataStockHistory(symbol, interval, "1");
            } catch (Exception e) {
                System.err.println("TwelveData intraday failed for " + symbol + ": " + e.getMessage());
            }
        }

        // Fallback zu Mock-Daten
        return getMockStockHistory(symbol);
    }

    /**
     * Ruft historische Daten von der TwelveData-API ab.
     *
     * @param symbol Das Aktien-Symbol
     * @param interval Das Zeitintervall
     * @param outputsize Die Anzahl der gewünschten Datenpunkte
     * @return Map mit historischen Daten von TwelveData
     */
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

            // Prüfung auf API-Fehler
            if (response.containsKey("code")) {
                String message = response.getOrDefault("message", "Unknown error").toString();
                throw new RuntimeException("Twelve Data History API Error: " + message);
            }

            return response;
        } catch (RestClientException e) {
            throw new RuntimeException("Twelve Data history network error: " + e.getMessage());
        }
    }

    /**
     * Generiert Mock-Daten für historische Aktienkurse.
     * Erstellt 7 Tage Beispieldaten für Demonstrationszwecke.
     *
     * @param symbol Das Aktien-Symbol
     * @return Map mit simulierten historischen Daten
     */
    private Map<String, Object> getMockStockHistory(String symbol) {
        Map<String, Object> mockHistory = new HashMap<>();
        List<Map<String, Object>> values = new java.util.ArrayList<>();

        // Deterministischer Basispreis basierend auf Symbol
        double basePrice = 100.00 + (symbol.hashCode() % 1000);

        // Generierung von 7 Tagen historischer Daten
        for (int i = 6; i >= 0; i--) {
            Map<String, Object> dayData = new HashMap<>();
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            double price = basePrice + (Math.random() - 0.5) * 20; // ±10 Variation

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

    // ========== EXCHANGE RATE METHODS ==========

    /**
     * Hauptmethode für Wechselkurs-Abfragen mit Fallback-Mechanismus.
     * Versucht zuerst die primäre API, dann alternative Quellen.
     *
     * @param fromCurrency Die Ausgangswährung (z.B. "USD")
     * @param toCurrency Die Zielwährung (z.B. "EUR")
     * @return CurrencyRate-Objekt mit aktuellem Wechselkurs
     */
    public CurrencyRate getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            return getExchangeRateFromApi(fromCurrency, toCurrency);
        } catch (Exception e) {
            try {
                // Fallback zur alternativen API
                return getExchangeRateFromHost(fromCurrency, toCurrency);
            } catch (Exception e2) {
                throw new RuntimeException("All exchange rate APIs failed: " + e2.getMessage());
            }
        }
    }

    /**
     * Ruft Wechselkurse von der ExchangeRate-API ab (primäre Quelle).
     *
     * @param fromCurrency Die Ausgangswährung
     * @param toCurrency Die Zielwährung
     * @return CurrencyRate mit aktuellem Wechselkurs
     */
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

        // Extraktion der Wechselkurse aus der API-Antwort
        Map<String, Double> rates = (Map<String, Double>) response.get("rates");
        if (rates == null || !rates.containsKey(toCurrency.toUpperCase())) {
            throw new RuntimeException("Exchange rate for " + toCurrency + " not found");
        }

        return createCurrencyRate(fromCurrency, toCurrency, rates.get(toCurrency.toUpperCase()));
    }

    /**
     * Ruft Wechselkurse von der exchangerate.host-API ab (Fallback-Quelle).
     *
     * @param fromCurrency Die Ausgangswährung
     * @param toCurrency Die Zielwährung
     * @return CurrencyRate mit aktuellem Wechselkurs
     */
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

    /**
     * Erstellt ein CurrencyRate-Objekt aus den gegebenen Parametern.
     *
     * @param fromCurrency Die Ausgangswährung
     * @param toCurrency Die Zielwährung
     * @param rate Der Wechselkurs als Double-Wert
     * @return Vollständig konfiguriertes CurrencyRate-Objekt
     */
    private CurrencyRate createCurrencyRate(String fromCurrency, String toCurrency, Double rate) {
        CurrencyRate currencyRate = new CurrencyRate();
        currencyRate.setFromCurrency(fromCurrency.toUpperCase());
        currencyRate.setToCurrency(toCurrency.toUpperCase());
        currencyRate.setRate(BigDecimal.valueOf(rate));
        currencyRate.setTimestamp(LocalDateTime.now());
        return currencyRate;
    }
}