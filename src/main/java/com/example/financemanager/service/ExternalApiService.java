package com.example.financemanager.service;

import com.example.financemanager.model.CurrencyRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalApiService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${alphavantage.api.key}")
    private String alphaVantageKey;

    @Value("${coincap.api.key}")
    private String coincapKey;

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

    public Map<String, Object> getCryptoData(String symbol) {
        String url = "https://api.coincap.io/v2/assets/" + symbol.toLowerCase();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + coincapKey);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return (Map<String, Object>) response.get("data");
    }

    public Map<String, Object> getCryptoHistory(String symbol) {
        String url = "https://api.coincap.io/v2/assets/" + symbol.toLowerCase() + "/history?interval=d1";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + coincapKey);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response;
    }
}
