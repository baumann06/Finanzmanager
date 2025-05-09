package com.example.Finanzmanager.controller;

import com.example.Finanzmanager.model.KryptoEintrag;
import com.example.Finanzmanager.model.UserNotiz;
import com.example.Finanzmanager.repository.NotizRepository;
import com.example.Finanzmanager.service.KryptoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/krypto")
@CrossOrigin(origins = "*") // Für Entwicklung, im Produktivsystem einschränken
public class KryptoController {

    private final KryptoService kryptoService;
    private final NotizRepository benutzerNotizRepository;

    public KryptoController(KryptoService kryptoService, NotizRepository benutzerNotizRepository) {
        this.kryptoService = kryptoService;
        this.benutzerNotizRepository = benutzerNotizRepository;
    }

    @GetMapping
    public List<KryptoEintrag> getAlleKryptos() {
        return kryptoService.getAllCryptos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<KryptoEintrag> getKryptoNachId(@PathVariable Long id) {
        Optional<KryptoEintrag> krypto = kryptoService.getCryptosById(id);
        return krypto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<KryptoEintrag> getKryptoNachSymbol(@PathVariable String symbol) {
        Optional<KryptoEintrag> krypto = kryptoService.getCryptoBySymbol(symbol);
        return krypto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> fuegeKryptoZurWatchlistHinzu(@RequestBody Map<String, String> daten) {
        try {
            String symbol = daten.get("symbol");
            if (symbol == null || symbol.isEmpty()) {
                return ResponseEntity.badRequest().body("Symbol ist erforderlich");
            }

            KryptoEintrag neuerEintrag = kryptoService.addCryptoToWatchlist(symbol);
            return ResponseEntity.status(HttpStatus.CREATED).body(neuerEintrag);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Hinzufügen: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> entferneKryptoVonWatchlist(@PathVariable Long id) {
        try {
            kryptoService.removeCryptoFromWatchlist(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Entfernen: " + e.getMessage());
        }
    }

    @PostMapping("/{kryptoId}/notizen")
    public ResponseEntity<?> fuegeNotizHinzu(
            @PathVariable Long kryptoId,
            @RequestBody Map<String, String> daten) {
        try {
            String text = daten.get("text");
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body("Notiztext ist erforderlich");
            }

            Optional<KryptoEintrag> kryptoOpt = kryptoService.getCryptosById(kryptoId);
            if (kryptoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            KryptoEintrag krypto = kryptoOpt.get();

            UserNotiz notiz = new UserNotiz();
            notiz.setText(text);
            notiz.setCreatedAt(LocalDateTime.now());
            notiz.setCryptoEntry(krypto);

            UserNotiz gespeicherteNotiz = benutzerNotizRepository.save(notiz);
            return ResponseEntity.status(HttpStatus.CREATED).body(gespeicherteNotiz);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Speichern der Notiz: " + e.getMessage());
        }
    }

    @GetMapping("/{kryptoId}/notizen")
    public ResponseEntity<List<UserNotiz>> getNotizenFuerEintrag(@PathVariable Long kryptoId) {
        try {
            if (kryptoService.getCryptosById(kryptoId).isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<UserNotiz> notizen = benutzerNotizRepository.findByCryptoEntryId(kryptoId);
            return ResponseEntity.ok(notizen);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/notizen/{notizId}")
    public ResponseEntity<?> aktualisiereNotiz(
            @PathVariable Long notizId,
            @RequestBody Map<String, String> daten) {
        try {
            String text = daten.get("text");
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body("Notiztext ist erforderlich");
            }

            Optional<UserNotiz> notizOpt = benutzerNotizRepository.findById(notizId);
            if (notizOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            UserNotiz notiz = notizOpt.get();
            notiz.setText(text);
            benutzerNotizRepository.save(notiz);

            return ResponseEntity.ok(notiz);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Aktualisieren der Notiz: " + e.getMessage());
        }
    }

    @DeleteMapping("/notizen/{notizId}")
    public ResponseEntity<?> loescheNotiz(@PathVariable Long notizId) {
        try {
            if (!benutzerNotizRepository.existsById(notizId)) {
                return ResponseEntity.notFound().build();
            }

            benutzerNotizRepository.deleteById(notizId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Löschen der Notiz: " + e.getMessage());
        }
    }
}
