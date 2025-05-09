package com.example.Finanzmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private Map<String, Double> exchangeRates = new HashMap<>();

    @Value("${api.alphavantage.key}")
    private String alphavantageApiKey;

    public ExchangeRateService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        // Standard-Wechselkurse setzen, falls API-Anfrage fehlschlägt
        exchangeRates.put("EUR", 1.0);
        exchangeRates.put("USD", 1.08);
        exchangeRates.put("GBP", 0.86);
    }

    // Aktualisiert die Wechselkurse
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 86400000) // Einmal pro Tag
    public void updateExchangeRates() {
        try {
            String url = "https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE&from_currency=EUR&to_currency=USD&apikey=" + alphavantageApiKey;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.get("Realtime Currency Exchange Rate");

            if (result != null) {
                double rate = result.get("5. Exchange Rate").asDouble();
                exchangeRates.put("USD", rate);
            }

            // Auch GBP abrufen
            url = "https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE&from_currency=EUR&to_currency=GBP&apikey=" + alphavantageApiKey;
            response = restTemplate.getForObject(url, String.class);

            root = objectMapper.readTree(response);
            result = root.get("Realtime Currency Exchange Rate");

            if (result != null) {
                double rate = result.get("5. Exchange Rate").asDouble();
                exchangeRates.put("GBP", rate);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Aktualisieren der Wechselkurse: " + e.getMessage());
        }
    }

    // Konvertiert einen Betrag zwischen Währungen
    public double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // Da wir alles auf EUR-Basis haben, rechnen wir entsprechend um
        double amountInEur;
        if (fromCurrency.equals("EUR")) {
            amountInEur = amount;
        } else {
            amountInEur = amount / exchangeRates.getOrDefault(fromCurrency, 1.0);
        }

        if (toCurrency.equals("EUR")) {
            return amountInEur;
        } else {
            return amountInEur * exchangeRates.getOrDefault(toCurrency, 1.0);
        }
    }

    // Gibt alle aktuellen Wechselkurse zurück
    public Map<String, Double> getAllExchangeRates() {
        return exchangeRates;
    }
}
