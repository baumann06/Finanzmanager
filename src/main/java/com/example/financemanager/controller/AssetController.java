package com.example.financemanager.controller;

import com.example.financemanager.model.CryptoWatchlist;
import com.example.financemanager.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:8080")
public class AssetController {

    @Autowired
    private AssetService assetService;

    // ========== WATCHLIST MANAGEMENT ==========

    @PostMapping("/watchlist")
    public ResponseEntity<CryptoWatchlist> addToWatchlist(@RequestBody CryptoWatchlist asset) {
        return ResponseEntity.ok(assetService.addToWatchlist(asset));
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<CryptoWatchlist>> getWatchlist() {
        return ResponseEntity.ok(assetService.getWatchlist());
    }

    @DeleteMapping("/watchlist/{id}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable Long id) {
        assetService.removeFromWatchlist(id);
        return ResponseEntity.ok().build();
    }

    // ========== CURRENT PRICE DATA ==========

    /**
     * Aktuellen Preis für Krypto oder Aktie holen
     * GET /api/assets/price/BTC?type=crypto&market=USD
     * GET /api/assets/price/AAPL?type=stock
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Object>> getAssetPrice(
            @PathVariable String symbol,
            @RequestParam String type,
            @RequestParam(required = false) String market) {

        Map<String, Object> result = assetService.getAssetWithCurrentPrice(symbol, type, market);
        return ResponseEntity.ok(result);
    }

    /**
     * Mehrere Assets gleichzeitig abfragen
     * POST /api/assets/prices
     * Body: {"symbols": ["BTC", "ETH"], "type": "crypto", "market": "USD"}
     */
    @PostMapping("/prices")
    public ResponseEntity<Map<String, Object>> getMultipleAssetPrices(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> symbols = (List<String>) request.get("symbols");
        String type = (String) request.get("type");
        String market = (String) request.get("market");

        Map<String, Object> result = assetService.getMultipleAssetsWithPrices(symbols, type, market);
        return ResponseEntity.ok(result);
    }

    // ========== HISTORICAL DATA ==========

    /**
     * Historische Preisdaten für Krypto oder Aktie holen (Daily)
     * GET /api/assets/history/BTC?type=crypto&market=USD
     * GET /api/assets/history/AAPL?type=stock
     */
    @GetMapping("/history/{symbol}")
    public ResponseEntity<Map<String, Object>> getAssetHistory(
            @PathVariable String symbol,
            @RequestParam String type,
            @RequestParam(required = false) String market) {

        Map<String, Object> result = assetService.getAssetPriceHistory(symbol, type, market);
        return ResponseEntity.ok(result);
    }

    /**
     * Intraday-Daten für detailliertere Analysen
     * GET /api/assets/intraday/AAPL?type=stock&interval=5min
     * GET /api/assets/intraday/BTC?type=crypto&market=USD&interval=15min
     */
    @GetMapping("/intraday/{symbol}")
    public ResponseEntity<Map<String, Object>> getAssetIntradayData(
            @PathVariable String symbol,
            @RequestParam String type,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "5min") String interval) {

        Map<String, Object> result = assetService.getAssetPriceHistory(symbol, type, market, "intraday", interval);
        return ResponseEntity.ok(result);
    }

    // ========== CHART DATA ==========

    /**
     * Formatierte Chart-Daten für Frontend
     * GET /api/assets/chart/AAPL?type=stock&period=daily
     */
    @GetMapping("/chart/{symbol}")
    public ResponseEntity<Map<String, Object>> getChartData(
            @PathVariable String symbol,
            @RequestParam String type,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) String interval) {

        // Hole historische Daten
        Map<String, Object> historyResult = assetService.getAssetPriceHistory(symbol, type, market, period, interval);

        if (historyResult.containsKey("error")) {
            return ResponseEntity.ok(historyResult);
        }

        // Formatiere für Chart
        @SuppressWarnings("unchecked")
        Map<String, Object> historyData = (Map<String, Object>) historyResult.get("historyData");
        Map<String, Object> chartData = assetService.formatChartData(historyData, type);

        return ResponseEntity.ok(chartData);
    }

    // ========== ERROR HANDLING ==========

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "success", false
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(500).body(Map.of(
                "error", "Interner Server-Fehler: " + e.getMessage(),
                "success", false
        ));
    }
}