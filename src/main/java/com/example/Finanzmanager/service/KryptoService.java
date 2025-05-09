package com.example.Finanzmanager.service;

import com.example.Finanzmanager.model.KryptoEintrag;
import com.example.Finanzmanager.repository.KryptoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KryptoService {

    private final KryptoRepository kryptoRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.coincap.key}")
    private String coincapApiKey;

    public KryptoService(KryptoRepository kryptoRepository) {
        this.kryptoRepository = kryptoRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // Ruft alle gespeicherten Kryptowährungen ab
    public List<KryptoEintrag> getAllCryptos() {
        return kryptoRepository.findAll();
    }

    // Ruft eine bestimmte Kryptowährung nach Symbol ab
    public Optional<KryptoEintrag> getCryptoBySymbol(String symbol) {
        return kryptoRepository.findBySymbol(symbol);
    }

    // Ruft eine bestimmte Kryptowährung nach ID ab
    public Optional<KryptoEintrag> getCryptosById(Long id) {
        return kryptoRepository.findById(id);
    }

    // Fügt eine neue Kryptowährung zur Watchlist hinzu
    public KryptoEintrag addCryptoToWatchlist(String symbol) throws Exception {
        // Prüfen, ob bereits in der Datenbank
        Optional<KryptoEintrag> existingEntry = kryptoRepository.findBySymbol(symbol);
        if (existingEntry.isPresent()) {
            return existingEntry.get();
        }

        // Von API abrufen
        KryptoEintrag newEntry = fetchCryptoDataFromApi(symbol);
        return kryptoRepository.save(newEntry);
    }

    // Entfernt eine Kryptowährung von der Watchlist
    public void removeCryptoFromWatchlist(Long id) {
        kryptoRepository.deleteById(id);
    }

    // Aktualisiert die Preisdaten aller gespeicherten Kryptowährungen
    @Scheduled(fixedRate = 900000) // Alle 15 Minuten
    public void updateAllCryptoPrices() {
        List<KryptoEintrag> eintraege = kryptoRepository.findAll();
        for (KryptoEintrag eintrag : eintraege) {
            try {
                KryptoEintrag updatedEntry = fetchCryptoDataFromApi(eintrag.getSymbol());
                eintrag.setCurrentPrice(updatedEntry.getCurrentPrice());
                eintrag.setTimestamp(LocalDateTime.now());
                eintrag.setPriceHistory(updatedEntry.getPriceHistory());
                kryptoRepository.save(eintrag);
            } catch (Exception e) {
                // Fehlerbehandlung - nur loggen, nicht stoppen
                System.err.println("Fehler beim Aktualisieren von " + eintrag.getSymbol() + ": " + e.getMessage());
            }
        }
    }

    // Ruft Daten für eine Kryptowährung von der API ab
    private KryptoEintrag fetchCryptoDataFromApi(String symbol) throws Exception {
        // API-Anfrage für aktuelle Preisdaten
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + coincapApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String currentPriceUrl = "https://api.coincap.io/v2/assets/" + symbol.toLowerCase();
        ResponseEntity<String> response = restTemplate.exchange(
                currentPriceUrl, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("API-Anfrage fehlgeschlagen: " + response.getStatusCode());
        }

        // JSON verarbeiten
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.get("data");

        if (data == null) {
            throw new Exception("Keine Daten für Symbol " + symbol + " gefunden");
        }

        KryptoEintrag eintrag = new KryptoEintrag();
        eintrag.setName(data.get("name").asText());
        eintrag.setSymbol(data.get("symbol").asText());
        eintrag.setCurrentPrice(data.get("priceUsd").asDouble());
        eintrag.setTimestamp(LocalDateTime.now());

        // Historische Daten abrufen (letzten 7 Tage)
        String historyUrl = "https://api.coincap.io/v2/assets/" + symbol.toLowerCase() + "/history?interval=d1";
        ResponseEntity<String> historyResponse = restTemplate.exchange(
                historyUrl, HttpMethod.GET, entity, String.class);

        if (historyResponse.getStatusCode().is2xxSuccessful()) {
            eintrag.setPriceHistory(historyResponse.getBody());
        }

        return eintrag;
    }
}
