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

    public Map<String, Object> getCryptoWithCurrentPrice(String symbol) {
        Map<String, Object> result = new HashMap<>();

        CryptoWatchlist crypto = cryptoWatchlistRepository.findBySymbol(symbol);
        Map<String, Object> priceData = externalApiService.getCryptoData(symbol);

        result.put("watchlist", crypto);
        result.put("price", priceData);

        return result;
    }

    public Map<String, Object> getCryptoPriceHistory(String symbol) {
        return externalApiService.getCryptoHistory(symbol);
    }
}

