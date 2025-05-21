package com.example.financemanager.controller;

import com.example.financemanager.model.CryptoWatchlist;
import com.example.financemanager.service.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:8080")

public class CryptoController {

    @Autowired
    private CryptoService cryptoService;

    @PostMapping("/watchlist")
    public ResponseEntity<CryptoWatchlist> addToWatchlist(@RequestBody CryptoWatchlist asset) {
        return ResponseEntity.ok(cryptoService.addToWatchlist(asset));
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<CryptoWatchlist>> getWatchlist() {
        return ResponseEntity.ok(cryptoService.getWatchlist());
    }

    @DeleteMapping("/watchlist/{id}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable Long id) {
        cryptoService.removeFromWatchlist(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Aktuellen Preis für Krypto oder Aktie holen
     * Beispiel: /api/assets/price/BTC?type=crypto&market=USD
     * Beispiel: /api/assets/price/AAPL?type=stock
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Object>> getAssetPrice(
            @PathVariable String symbol,
            @RequestParam String type,
            @RequestParam(required = false) String market) {

        return ResponseEntity.ok(cryptoService.getAssetWithCurrentPrice(symbol, type, market));
    }

    /**
     * Historische Preisdaten für Krypto oder Aktie holen
     * Beispiel: /api/assets/history/BTC?type=crypto&market=USD
     * Beispiel: /api/assets/history/AAPL?type=stock
     */
    @GetMapping("/history/{symbol}")
    public ResponseEntity<Map<String, Object>> getAssetHistory(
            @PathVariable String symbol,
            @RequestParam String type,
            @RequestParam(required = false) String market) {

        return ResponseEntity.ok(cryptoService.getAssetPriceHistory(symbol, type, market));
    }
}