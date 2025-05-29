package com.example.financemanager.controller;

import com.example.financemanager.dto.WatchlistDto;
import com.example.financemanager.model.CryptoWatchlist;
import com.example.financemanager.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "http://localhost:8080")
public class AssetController {

    @Autowired
    private AssetService assetService;

    // ========== WATCHLIST MANAGEMENT ==========

    /**
     * Asset zur Watchlist hinzufügen
     * POST /api/assets/watchlist
     * Body: {"symbol": "BTC", "name": "Bitcoin", "type": "crypto", "notes": "Long term hold"}
     */
    @PostMapping("/watchlist")
    public ResponseEntity<WatchlistDto> addToWatchlist(@RequestBody CryptoWatchlist asset) {
        try {
            CryptoWatchlist savedAsset = assetService.addToWatchlist(asset);
            WatchlistDto dto = assetService.convertToDto(savedAsset);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Komplette Watchlist abrufen (mit DTOs - löst Zirkularreferenz-Problem)
     * GET /api/assets/watchlist
     */
    @GetMapping("/watchlist")
    public ResponseEntity<List<WatchlistDto>> getWatchlist() {
        try {
            List<WatchlistDto> watchlistDtos = assetService.getWatchlistDtos();
            return ResponseEntity.ok(watchlistDtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Einzelnes Watchlist-Item abrufen
     * GET /api/assets/watchlist/{id}
     */
    @GetMapping("/watchlist/{id}")
    public ResponseEntity<WatchlistDto> getWatchlistItem(@PathVariable Long id) {
        try {
            WatchlistDto dto = assetService.getWatchlistItemDto(id);
            if (dto != null) {
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Watchlist-Item aktualisieren
     * PUT /api/assets/watchlist/{id}
     * Body: {"symbol": "BTC", "name": "Bitcoin", "type": "crypto", "notes": "Updated notes"}
     */
    @PutMapping("/watchlist/{id}")
    public ResponseEntity<WatchlistDto> updateWatchlistItem(
            @PathVariable Long id,
            @RequestBody CryptoWatchlist updatedAsset) {
        try {
            CryptoWatchlist updated = assetService.updateWatchlistItem(id, updatedAsset);
            if (updated != null) {
                WatchlistDto dto = assetService.convertToDto(updated);
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Asset aus Watchlist entfernen
     * DELETE /api/assets/watchlist/{id}
     */
    @DeleteMapping("/watchlist/{id}")
    public ResponseEntity<Map<String, Object>> removeFromWatchlist(@PathVariable Long id) {
        try {
            boolean removed = assetService.removeFromWatchlist(id);
            if (removed) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Asset erfolgreich aus Watchlist entfernt"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Fehler beim Entfernen des Assets: " + e.getMessage()
            ));
        }
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

        try {
            Map<String, Object> result = assetService.getAssetWithCurrentPrice(symbol, type, market);

            if (result.containsKey("error")) {
                return ResponseEntity.status(404).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ungültige Parameter: " + e.getMessage(),
                    "success", false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler beim Abrufen des Preises: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Mehrere Assets gleichzeitig abfragen
     * POST /api/assets/prices
     * Body: {"symbols": ["BTC", "ETH"], "type": "crypto", "market": "USD"}
     */
    @PostMapping("/prices")
    public ResponseEntity<Map<String, Object>> getMultipleAssetPrices(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> symbols = (List<String>) request.get("symbols");
            String type = (String) request.get("type");
            String market = (String) request.get("market");

            if (symbols == null || symbols.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Symbols-Array ist erforderlich und darf nicht leer sein",
                        "success", false
                ));
            }

            Map<String, Object> result = assetService.getMultipleAssetsWithPrices(symbols, type, market);
            return ResponseEntity.ok(result);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ungültiges Request-Format",
                    "success", false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler beim Abrufen der Preise: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Watchlist mit aktuellen Preisen anreichern
     * GET /api/assets/watchlist/with-prices
     */
    @GetMapping("/watchlist/with-prices")
    public ResponseEntity<Map<String, Object>> getWatchlistWithPrices() {
        try {
            Map<String, Object> result = assetService.getWatchlistWithCurrentPrices();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler beim Abrufen der Watchlist mit Preisen: " + e.getMessage(),
                    "success", false
            ));
        }
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

        try {
            Map<String, Object> result = assetService.getAssetPriceHistory(symbol, type, market);

            if (result.containsKey("error")) {
                return ResponseEntity.status(404).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ungültige Parameter: " + e.getMessage(),
                    "success", false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler beim Abrufen der historischen Daten: " + e.getMessage(),
                    "success", false
            ));
        }
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

        try {
            Map<String, Object> result = assetService.getAssetPriceHistory(symbol, type, market, "intraday", interval);

            if (result.containsKey("error")) {
                return ResponseEntity.status(404).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ungültige Parameter: " + e.getMessage(),
                    "success", false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler beim Abrufen der Intraday-Daten: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    // ========== INVESTMENT MANAGEMENT ==========

    /**
     * Neue Investition erstellen
     * POST /api/assets/investment
     * Body: {"symbol": "BTC", "name": "Bitcoin", "type": "crypto", "investmentAmount": 1000}
     */
    @PostMapping("/investment")
    public ResponseEntity<Map<String, Object>> addInvestment(@RequestBody Map<String, Object> request) {
        try {
            String symbol = (String) request.get("symbol");
            String name = (String) request.get("name");
            String type = (String) request.get("type");

            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Symbol ist erforderlich"
                ));
            }

            if (request.get("investmentAmount") == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Investment-Betrag ist erforderlich"
                ));
            }

            BigDecimal investmentAmount = new BigDecimal(request.get("investmentAmount").toString());

            if (investmentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Investment-Betrag muss größer als 0 sein"
                ));
            }

            Map<String, Object> result = assetService.addInvestment(symbol, name, type, investmentAmount);
            return ResponseEntity.ok(result);

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Ungültiger Investment-Betrag"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Fehler beim Erstellen der Investition: " + e.getMessage()
            ));
        }
    }

    /**
     * Portfolio-Prozentsatz abrufen
     * GET /api/assets/portfolio/{watchlistId}
     */
    @GetMapping("/portfolio/{watchlistId}")
    public ResponseEntity<Map<String, Object>> getPortfolioPercentage(@PathVariable Long watchlistId) {
        try {
            Map<String, Object> result = assetService.getPortfolioPercentage(watchlistId);

            if (result.containsKey("error")) {
                return ResponseEntity.status(404).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler beim Abrufen des Portfolio-Prozentsatzes: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Gesamtes Portfolio mit Performance-Daten abrufen
     * GET /api/assets/portfolio/overview
     */
    @GetMapping("/portfolio/overview")
    public ResponseEntity<Map<String, Object>> getPortfolioOverview() {
        try {
            Map<String, Object> result = assetService.getPortfolioOverview();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler beim Abrufen der Portfolio-Übersicht: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    // ========== SEARCH & DISCOVERY ==========

    /**
     * Asset-Suche für Watchlist-Hinzufügung
     * GET /api/assets/search?query=Bitcoin&type=crypto
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAssets(
            @RequestParam String query,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Suchbegriff ist erforderlich",
                        "success", false
                ));
            }

            Map<String, Object> result = assetService.searchAssets(query.trim(), type, limit);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fehler bei der Asset-Suche: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    // ========== HEALTH CHECK ==========

    /**
     * API Health Check
     * GET /api/assets/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "timestamp", System.currentTimeMillis(),
                "service", "AssetController",
                "version", "1.0.0"
        ));
    }

    // ========== ERROR HANDLING ==========

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "success", false,
                "timestamp", System.currentTimeMillis()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(500).body(Map.of(
                "error", "Interner Server-Fehler: " + e.getMessage(),
                "success", false,
                "timestamp", System.currentTimeMillis()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return ResponseEntity.status(500).body(Map.of(
                "error", "Unerwarteter Fehler: " + e.getMessage(),
                "success", false,
                "timestamp", System.currentTimeMillis()
        ));
    }
}