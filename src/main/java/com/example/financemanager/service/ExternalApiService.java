package com.example.financemanager.service;

import com.example.financemanager.model.CurrencyRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ExternalApiService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${alphavantage.api.key}")
    private String alphaVantageKey;

    // Holen des Wechselkurses zwischen zwei Währungen (Fiat)
    public CurrencyRate getExchangeRate(String fromCurrency, String toCurrency) {
        String url = "https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE" +
                "&from_currency=" + fromCurrency +
                "&to_currency=" + toCurrency +
                "&apikey=" + alphaVantageKey;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        Map<String, String> exchangeRate = (Map<String, String>) response.get("Realtime Currency Exchange Rate");

        CurrencyRate rate = new CurrencyRate();
        rate.setFromCurrency(fromCurrency);
        rate.setToCurrency(toCurrency);
        rate.setRate(new BigDecimal(exchangeRate.get("5. Exchange Rate")));
        rate.setTimestamp(LocalDateTime.now());

        return rate;
    }

    // Tägliche Krypto-Daten abrufen
    // symbol z.B. "BTC", market z.B. "USD"
    public Map<String, Object> getCryptoData(String symbol, String market) {
        String url = "https://www.alphavantage.co/query?function=DIGITAL_CURRENCY_DAILY" +
                "&symbol=" + symbol.toUpperCase() +
                "&market=" + market.toUpperCase() +
                "&apikey=" + alphaVantageKey;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response;
    }

    // Tägliche Aktienkurse abrufen
    // symbol z.B. "AAPL"
    public Map<String, Object> getStockData(String symbol) {
        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY" +
                "&symbol=" + symbol.toUpperCase() +
                "&apikey=" + alphaVantageKey;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response;
    }
}