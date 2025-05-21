package com.example.financemanager.service;

import com.example.financemanager.model.CryptoWatchlist;
import com.example.financemanager.repository.CryptoWatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CryptoService {

    @Autowired
    private CryptoWatchlistRepository cryptoWatchlistRepository;

    @Autowired
    private ExternalApiService externalApiService;

    public CryptoWatchlist addToWatchlist(CryptoWatchlist crypto) {
        return cryptoWatchlistRepository.save(crypto);
    }

    public List<CryptoWatchlist> getWatchlist() {
        return cryptoWatchlistRepository.findAll();
    }

    public void removeFromWatchlist(Long id) {
        cryptoWatchlistRepository.deleteById(id);
    }

    /**
     * Holt aktuelle Preisdaten für das Asset
     *
     * @param symbol z.B. "BTC" oder "AAPL"
     * @param type  "crypto" oder "stock"
     * @param market nur für Krypto, z.B. "USD"
     * @return Map mit Watchlist-Daten und Preis-Daten
     */
    public Map<String, Object> getAssetWithCurrentPrice(String symbol, String type, String market) {
        Map<String, Object> result = new HashMap<>();

        CryptoWatchlist asset = cryptoWatchlistRepository.findBySymbol(symbol);
        Map<String, Object> priceData;

        if ("crypto".equalsIgnoreCase(type)) {
            priceData = externalApiService.getCryptoData(symbol, market);
        } else if ("stock".equalsIgnoreCase(type)) {
            priceData = externalApiService.getStockData(symbol);
        } else {
            throw new IllegalArgumentException("Unbekannter Asset-Typ: " + type);
        }

        result.put("watchlist", asset);
        result.put("price", priceData);

        return result;
    }

    /**
     * Holt historische Preisdaten
     *
     * @param symbol z.B. "BTC" oder "AAPL"
     * @param type "crypto" oder "stock"
     * @param market nur für Krypto (z.B. USD), für Aktien nicht nötig
     * @return Map mit historischen Kursdaten
     */
    public Map<String, Object> getAssetPriceHistory(String symbol, String type, String market) {
        if ("crypto".equalsIgnoreCase(type)) {
            // Alpha Vantage hat keine direkte History-API, sondern liefert alles in DIGITAL_CURRENCY_DAILY
            return externalApiService.getCryptoData(symbol, market);
        } else if ("stock".equalsIgnoreCase(type)) {
            return externalApiService.getStockData(symbol);
        } else {
            throw new IllegalArgumentException("Unbekannter Asset-Typ: " + type);
        }
    }
}