# SkyStats

Inventarbasierte Vanilla-Statistiken für Paper und Folia, standardmäßig auf Deutsch. Das Plugin liest bestehende Zähler und zählt keine Spielereignisse mit.

## Verwendung

- `/stats` öffnet die eigenen Statistiken.
- `/stats <Name>` öffnet ein bekanntes Online- oder Offline-Profil.
- `/stats <UUID>` löst mehrdeutige Namen auf, etwa nach einem Wechsel von `online-mode`.
- `stats.use` und `stats.others` sind standardmäßig für alle Spieler freigegeben.

Das Hauptmenü enthält sämtliche Statistik-Kategorien und den direkten Zugang zu Server-Bestenlisten. Jede Ansicht hat 54 Slots; die untere Reihe bleibt für Zurück, Aktualisieren, Seitenanzeige und die beiden texturierten Pfeilköpfe reserviert. Einzelranglisten und Medaillenprofile sind ebenfalls enthalten.

Bei Einzelstatistiken teilen Gleichstände den Wettkampfplatz (`1, 1, 3`). Nur positive Werte erhalten Medaillen. Das Medaillenranking sortiert nach Gesamtzahl, anschließend Gold, Silber und Bronze. Identische Medaillenbilanzen teilen ihren Rang; Namen und UUIDs stabilisieren die Anzeigereihenfolge. Zusätzliche Ansichten für Fortbewegung und Spielzeit erzeugen keine doppelten Wettbewerbe.

## Bauen und installieren

Zum Bauen sind Java 17 oder Java 21 sowie `bash`, `curl`, `tar` und `sha512sum` erforderlich. Der erste Build lädt Maven und Abhängigkeiten in das ignorierte Verzeichnis `.tools/`:

```bash
./mvnw clean verify
```

Die installierbare Datei liegt unter `target/SkyStats-2.0.0-BETA.jar`. Diese Datei in das `plugins/`-Verzeichnis kopieren und den Server neu starten. Externe Plugins werden nicht benötigt; Gson wird isoliert in die JAR eingebettet. Der Code wird gegen Spigot 1.18.2 kompiliert und verwendet Java-17-Bytecode. Neuere Scheduler- und Profil-APIs werden über öffentliche Versionsadapter angesprochen.

| Tatsächlich getesteter Server | Java | Ergebnis |
| --- | --- | --- |
| Paper 1.18, Build 66 | 17 | Integrationssuite bestanden |
| Paper 1.18.2, Build 388 | 17 | Integrationssuite bestanden |
| Paper 26.3, Build 214 | 25 | Integrationssuite bestanden |
| Folia 26.3, Build 5 | 25 | Integrationssuite bestanden |

Zwischenversionen werden durch die dynamische API-Erkennung berücksichtigt; die Tabelle benennt die tatsächlich geprüften Builds. Folia existiert für 1.18 nicht. `folia-supported: true` wurde nach den erfolgreichen Tests auf dem echten Folia-Server aktiviert. Für Installation und Aktualisierung einen regulären Server-Neustart verwenden.

## Konfiguration und Daten

Die Datei `plugins/SkyStats/config.yml` enthält:

| Einstellung | Standard | Bedeutung |
| --- | --- | --- |
| `stats-directory` | leer | Automatisch die gemeinsame Statistikablage der ersten Serverwelt auswählen |
| `hide-zero-values` | `true` | Persönliche und serverweite Nullwerte ausblenden |
| `cache-seconds` | `30` | Gültigkeit einer Server-Momentaufnahme |
| `refresh-cooldown-seconds` | `5` | Mindestabstand aufwendiger Neuberechnungen und persönlicher Aktualisierungen |
| `live-reads-per-tick` | `400` | Maximale Statistikabfragen pro Spieler und Tick je laufender Abfrage |
| `live-read-timeout-seconds` | `30` | Zeitlimit für Live-Abfragen |
| `max-file-size-mib` | `16` | Größenlimit einer Statistikdatei |
| `max-player-files` | `100000` | Limit für Spielerdateien je Berechnung |
| `max-statistics-per-player` | `100000` | Limit für Einträge einer Statistikdatei |
| `max-total-values` | `2000000` | Grenze der gesamten positiven Datenmenge vor dem Aufbau der Ranglisten |

Bei abweichender Weltverwaltung kann `stats-directory` relativ zum Weltverzeichnis oder absolut angegeben werden. Es wird immer genau ein Statistikordner gelesen. Auch die neuere Ablage mit `world/dimensions/minecraft/overworld` wird auf den gemeinsamen Speicherort `world/stats` zurückgeführt.

Online-Daten werden auf dem zuständigen Entity-Scheduler gelesen und ersetzen den Dateistand derselben UUID. Offline-Profile werden asynchron aus Vanilla-JSON geladen. Die Server-Auswertung ist eine über mehrere Spielerticks entstandene Momentaufnahme; der Abschlusszeitpunkt steht im Info-Item. Persönliche Werte werden beim Öffnen erneut eingelesen. Dateien mit fehlerhaftem JSON werden ausgelassen und in der GUI als Hinweis ausgewiesen. Bei einem Verbindungsabbruch während einer Live-Abfrage schlägt dieser Ladevorgang nachvollziehbar fehl und kann erneut gestartet werden.

Das Plugin schreibt ausschließlich seine Konfiguration und die UUID-Namenszuordnung in `plugins/SkyStats/players.json`. Vanilla-Statistiken werden nicht verändert. Nicht bekannte IDs bleiben mit technischer Kennung und Ersatzicon erreichbar. Minecraft-Items behalten nach Möglichkeit ihren vom Client übersetzten Namen; allgemeine Kennzahlen und Menütexte sind deutsch. Spielerköpfe tragen UUID und Spielernamen; ihre Skin-Auflösung übernimmt die Serversoftware. Die beiden Pfeiltexturen sind fest eingebettet und benötigen keine Head-Database-Anbindung.

## Lokale Testserver

Python 3.12 oder neuer wird für die Einrichtung und die Integrationstests benötigt. Die folgenden Befehle laden offizielle Server-Builds sowie passende Temurin-Laufzeiten mit Prüfsummenprüfung. Alle Laufzeiten, Server, Welten und Logs liegen in den ignorierten Verzeichnissen `.tools/` und `.test-servers/`:

```bash
python3 scripts/setup-test-servers.py --include-paper-1.18
```

Ohne ausdrückliche Zustimmung bleibt `eula=false`. Nach Zustimmung zur [Minecraft-EULA](https://www.minecraft.net/eula) kann die Einrichtung mit `--accept-eula` aufgerufen werden. Im Rahmen dieser Implementierung wurde die Zustimmung bereits erteilt und lokal eingetragen.

```bash
.test-servers/paper-26.3/start.sh
```

| Instanz | Adresse |
| --- | --- |
| Paper 26.3 | `127.0.0.1:25565` |
| Folia 26.3 | `127.0.0.1:25566` |
| Paper 1.18.2 | `127.0.0.1:25568` |
| Paper 1.18 | `127.0.0.1:25569` |

Die Testinstanzen binden ausschließlich an die lokale Adresse und verwenden für automatisierte Testspieler `online-mode=false`. Die Integrationstests sind für diese lokalen Testwelten vorgesehen und erzeugen dort absichtlich synthetische Spieler und Statistiken.

```bash
./mvnw verify
python3 scripts/run-integration-tests.py paper-1.18 paper-1.18.2 paper-26.3 folia-26.3
```

Die Suite baut ein separates Prüfplugin, startet jeden Server, verbindet einen Python-Testclient und stoppt den Server anschließend geordnet. Das Prüfplugin gehört nicht zur ausgelieferten SkyStats-JAR. Ergebnisse und Logs liegen pro Instanz in `probe-result.txt`, `integration-console.log` und `integration-client.log`; zusammengefasste Ergebnisse stehen in `target/integration-results.txt`. Details der Abdeckung stehen in [TESTING.md](TESTING.md).

## Aufbau

- `model`: UUID-basierte Daten, Einheiten, Wettbewerbsränge und einmalige Medaillenvergabe.
- `data`: dynamischer Statistik-Katalog, JSON-Lesen, Namensauflösung und begrenzter Cache mit gemeinsam genutzten Ladeaufträgen.
- `platform`: öffentliche Scheduler-Adapter für klassische Server und Folia.
- `ui`: Holder-basierte Inventare, Seitennavigation, Profilköpfe und strikt abgebrochene Inventaraktionen.

Referenzen: [Spigot Statistic API](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html), [Paper/Folia Scheduler](https://docs.papermc.io/paper/dev/folia-support/), [Iron Arrow Left 2296](https://minecraft-heads.com/custom-heads/head/2296-arrow-left), [Iron Arrow Right 2298](https://minecraft-heads.com/custom-heads/head/2298-arrow-right).
