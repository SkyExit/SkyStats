# Prüfung von 0.7.0-BETA

Stand: 15. Oktober 2026. JDK: Oracle Java 25.0.3, Windows 11.

## Automatisierte Regressionstests

```powershell
.\mvnw.cmd -B clean verify
```

39 Tests erfolgreich, keine Fehler und keine übersprungenen Tests:

| Bereich | Tests |
| --- | ---: |
| Befehle, Rechte und Spielerauflösung | 6 |
| Menüschutz bei Klicks und Drag-Aktionen | 2 |
| Levelgrenzen, getrennte Instanzen und große Werte | 10 |
| Kopierte Spielerstatistiken und Profile | 1 |
| EXP-Gewichtungen und Überlaufschutz | 3 |
| Menüabfragen über getrennte Scheduler, Reihenfolge und Disconnects | 4 |
| Zeitformatierung | 8 |
| Bukkit-/Entity-Scheduler, Disconnects und Abschaltung | 5 |

Die Scheduler-Tests verwenden kontrollierte Warteschlangen und eine Nachbildung
der Entity-Scheduler-Schnittstelle. Sie prüfen, dass Live-Spielerzugriffe erst im
geplanten Task stattfinden und auf Folia kein Bukkit-Fallback benutzt wird.
Die reale API-Erkennung wurde zusätzlich mit den folgenden Serverstarts geprüft.

## Server-Ladetests

Dieselbe gebaute JAR wurde in isolierten Verzeichnissen unter
`target/test-servers/` mit Bindung an `127.0.0.1` gestartet.
Die Server-JARs stammen aus der offiziellen PaperMC-Download-API; ihre
SHA-256-Prüfsummen wurden vor dem Start geprüft.

| Server | Version | Ergebnis |
| --- | --- | --- |
| Paper | 26.3, Build 190, Stable | Plugin aktiviert, Version abgefragt, Konsolenbefehle abgefangen, sauber deaktiviert |
| Folia | 26.3, Build 3, Alpha | Plugin aktiviert, Entity-Scheduler-API erkannt, Konsolenbefehle abgefangen, sauber deaktiviert |
| Spigot | API 26.3-R0.1-SNAPSHOT | Kompilierung und Bukkit-Scheduler-Regressionstest erfolgreich; kein echter Serverstart durchgeführt |

Auf Paper und Folia liefern `/stats`, `/level` und `/compare` in der Konsole
jeweils die vorgesehene Spieler-only-Fehlermeldung. Es gab keine Aktivierungs-
oder Deaktivierungsfehler von SkyStats. Die Testumgebung meldete unabhängig
vom Plugin Warnungen zu Windows-Leistungszählern und zum lokalen Offline-Testmodus.

Ein Mehrspielertest mit echten Clients wurde nicht durchgeführt. Teleports
zwischen Folia-Regionen, Client-Darstellung und sämtliche Inventar-Interaktionen
sollten vor einem produktiven Einsatz zusätzlich mit verbundenen Spielern
geprüft werden; die Schritte stehen in der README.
