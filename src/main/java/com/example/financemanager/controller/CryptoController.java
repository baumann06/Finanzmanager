package com.example.financemanager.controller;

import com.example.financemanager.model.CryptoWatchlist;
import com.example.financemanager.service.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "http://localhost:8080")
public class CryptoController {

    @Autowired
    private CryptoService cryptoService;

    @PostMapping("/watchlist")
    public ResponseEntity<CryptoWatchlist> addToWatchlist(@RequestBody CryptoWatchlist crypto) {
        return ResponseEntity.ok(cryptoService.addToWatchlist(crypto));
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

    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Object>> getCryptoPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(cryptoService.getCryptoWithCurrentPrice(symbol));
    }

    @GetMapping("/history/{symbol}")
    public ResponseEntity<Map<String, Object>> getCryptoHistory(@PathVariable String symbol) {
        return ResponseEntity.ok(cryptoService.getCryptoPriceHistory(symbol));
    }
}