# Finanzmanager - Persönliches Finanz- & Krypto-Dashboard

Dieses Projekt kombiniert einen Finanzmanager mit einem Krypto-Dashboard, um sowohl persönliche Finanzen zu verwalten als auch Kryptowährungen zu beobachten.

## Projektstruktur

Das Projekt besteht aus einem Spring Boot Backend und einem Vue.js Frontend. Die Struktur ist wie folgt:

- `src/frontend/vue_finance`: Vue.js Frontend
- `src/main/java`: Spring Boot Backend
- `src/main/resources`: Konfigurationsdateien

## Voraussetzungen

1. Java 11 oder höher
2. Node.js und npm
3. Maven oder Gradle

## Backend starten

1. Navigiere in das Hauptverzeichnis des Projekts
2. Führe den folgenden Befehl aus:

```
./mvnw spring-boot:run
```

Das Backend sollte jetzt unter `http://localhost:8081` erreichbar sein.

## Frontend starten

1. Navigiere zum Vue-Frontend-Verzeichnis:

```
cd src/frontend/vue_finance
```

2. Installiere die Abhängigkeiten:

```
npm install
```

3. Starte den Entwicklungsserver:

```
npm run serve
```

Das Frontend sollte jetzt unter `http://localhost:8080` erreichbar sein.

## Funktionen

### Finanzmodul

- Erfassen von Einnahmen und Ausgaben
- Kategorisierung von Ausgaben
- Berechnung des aktuellen Kontostands
- Währungsumrechner mit aktuellen Wechselkursen

### Kryptomodul

- Verwaltung einer persönlichen Watchlist von Kryptowährungen
- Anzeige aktueller Preise und 24-Stunden-Änderungen
- Darstellung des Preisverlaufs der letzten 7 Tage
- Hinterlegen von persönlichen Notizen zu jeder Kryptowährung

## API-Keys

Die Anwendung verwendet zwei externe APIs:

1. Alpha Vantage für Währungskurse: `J23ZY20YSGXA91CU`
2. CoinCap für Kryptowährungsdaten: `59c5d2c3458d016bab4cf8580bae251d2420bd0de7a0eccdf591ab6ecdfa5e8a`

Diese sind bereits in der `application.properties` Datei konfiguriert.

## Entwicklungshinweise

Die aktuelle Implementation ist bewusst einfach gehalten. Für eine Produktivumgebung sollten folgende Punkte beachtet werden:

1. Implementierung einer Benutzerverwaltung
2. Verbesserte Fehlerbehandlung
3. Validierung von Benutzereingaben
4. Erweiterte Darstellung von Charts mit Chart.js
5. Responsive Design für mobile Endgeräte
6. Cache-Implementierung für API-Anfragen
7. Rate Limiting für externe API-Aufrufe