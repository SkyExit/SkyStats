# Testnachweise

Die Implementierung wird durch Unit-Tests und eine separate Prüfung auf tatsächlichen Minecraft-Servern abgesichert. Die ausgeführten Server-Builds sind in der README dokumentiert. Die Dateien unter `.test-servers/` und `target/` bleiben lokale, ignorierte Testartefakte.

Ergebnis für **v2.1.0-BETA vom 23.10.2026: 66 automatische Tests bestanden**, dazu die vollständige Integrationssuite auf **Paper 26.3 Build 190 und Folia 26.3 Build 3**, jeweils unter Java 25.0.3. Auf beiden Instanzen blieben Spielerinventar, Cursor und GUI auch nach den zehn tatsächlichen Netzwerk-Klickpaketen unverändert. Die größte beobachtete Pause zwischen Entity-Ticks betrug 64 ms auf Paper und 84 ms auf Folia. Die früher dokumentierten Läufe auf 1.18 und 1.18.2 wurden für diese Version nicht wiederholt.

Die vorhandenen Windows-Testinstanzen lagen bei diesem Lauf unter `target/test-servers/`. Verwendete Befehle (Maven 3.10.0 aus der lokalen Installation):

```powershell
mvn -o -B "-Dmaven.repo.local=.tools/m2" verify
python scripts/run-integration-tests.py --server-root target/test-servers --java "C:/Program Files/Java/jdk-25.0.3/bin/java.exe" paper-26.3 folia-26.3
```

Ergebnisse: `target/verify.log`, `target/integration-results.txt` und pro Server `probe-result.txt`, `integration-console.log`, `integration-client.log`. Die Testinstanzen werden nach der Prüfung geordnet gestoppt. `clean` entfernt auch die alten Testinstanzen unter `target/`; die reguläre Einrichtung verwendet deshalb `.test-servers/`.

## Automatische Tests

`./mvnw clean verify` prüft unter anderem:

- Wettkampfränge bei Gleichständen, Nullwerte, stabile Namenssortierung und die Medaillen-Gesamtsortierung.
- Einmalige Medaillenvergabe trotz mehrerer Kategorien derselben Statistik.
- Summen über der Integer-Grenze und Überlaufschutz bei Long-Summen.
- Eine Datenmenge mit 2.000 Spielern und 80 positiven Einzelstatistiken pro Spieler.
- Vanilla-Kategorien, unbekannte IDs, Spielzeit-Aliase, unvollständige Dateien, Größenlimits und ungültige Werte.
- Ersetzen veralteter Online-Dateiwerte, gemeinsame Nutzung laufender Abfragen, Cache-Wiederverwendung und Abbruch beim Herunterfahren.
- Erneutes Einlesen einzelner Offline-Profile und das globale Mengenlimit vor dem Ranglistenaufbau.
- Namenänderungen, mehrdeutige Namen verschiedener UUIDs und direkte UUID-Suche.
- Klassische Ablage `world/stats` und moderne Ablage `world/players/stats`, auch bei Dimension-Pfaden und zurückgebliebenem alten Statistikordner. Offline-Namenssuche, Tabvervollständigung, persönliche Werte und Gesamtsummen werden für das moderne Layout gemeinsam geprüft.
- Hinweise bei fehlenden Statistikordnern.
- Eingebettete Holztexturen inklusive expliziter Texturübergabe, asynchrone und gemeinsam genutzte Skin-Abfragen, Login-Skin-Cache, Offline-UUIDs, fehlgeschlagene Abfragen und Herunterfahren bei ausstehenden Abfragen.
- Tick-, Distanz- und Schadenseinheiten, deutsche Texte und Seitengrenzen.
- Sämtliche Bukkit-Klicktypen und Inventaraktionen sowie Drag- und Creative-Ereignisse. Fremde Inventare werden nicht blockiert.

## Echte Server und Netzwerktransaktionen

`scripts/run-integration-tests.py` baut `SkyStatsProbe.jar` getrennt vom eigentlichen Plugin. Der verbundene Client verwendet Protokoll 757 für 1.18, 758 für 1.18.2 und 777 für 26.3. Für 26.3 wurden die benötigten Paketdefinitionen anhand des heruntergeladenen Servercodes geprüft. Der Client verarbeitet Verbindung, Konfiguration, Teleportbestätigungen und Keepalives; die GUI-Assertions laufen auf dem zuständigen Spielerthread im Prüfplugin.

Die Suite prüft:

1. Eigene Statistikübersicht mit 54 Slots und Live-Werten aus der Bukkit-API; alle zwölf Kategoriepositionen und deren horizontale/vertikale Symmetrie um den zentralen Bestenlisten-Button.
2. 121 Kombinationen aus Klicktyp, Slot und Inventaraktion sowie Drag und Creative gegen die geöffnete GUI auf 26.3.
3. Zusätzlich echte Container-Klickpakete über die Netzwerkverbindung: normale Klicks, Shift-Klicks oben und unten, Hotbar, Offhand, Drop und Einsammeln. Danach müssen Spielerinventar, Cursor und GUI exakt unverändert sein.
4. Offline-Namenssuche, Dateiwerte, persönliche Sortierung und beide Seiten der 45-Slot-Navigation mit texturierten Holzpfeilen. Auf 26.3 werden Fixtures ausschließlich unter `world/players/stats` angelegt, damit die Prüfung keine veraltete Pfaderkennung verdeckt.
5. Server-Gesamtsummen inklusive Offline-Spielern; eine absichtlich veraltete Online-Datei wird genau einmal durch Live-Werte ersetzt.
6. Gleichstände mit `1, 1, 3`, Top-3-Lore und die eigene Position außerhalb der Top 3.
7. Vollständige Einzelrangliste, Spieler-Köpfe, blätterbares Medaillenranking und Medaillenprofil. Eine vorgegebene Login-Skin-Textur muss im Kopf des Online-Spielers erhalten bleiben; die Prüfung wartet auf das asynchrone Einsetzen.
8. Schließen eines Ladefensters vor Abschluss der Live-Abfrage; ein verspätetes Ergebnis darf es nicht wieder öffnen.
9. Unbekannte Namen, Offline-Tabvervollständigung und Hinweise auf fehlerhafte Statistikdateien.

Die größte gemessene Pause zwischen den Entity-Tick-Aufrufen wird im Ergebnis mitgeschrieben. Sie ist eine Beobachtung des gesamten Testservers einschließlich Welt- und Profilverarbeitung und kein isolierter Plugin-Benchmark. Die größere synthetische Aggregation wird separat außerhalb des Ticks getestet. Eine Lastprüfung mit vielen gleichzeitig spielenden Menschen ist damit nicht ersetzt.

Die rein synthetischen Offline-UUIDs können bei der standardmäßigen Skin-Auflösung der Serversoftware Meldungen des Mojang-Profildienstes auslösen. Das betrifft die kosmetische Skin-Auflösung; die UUIDs, Statistikwerte, Kopftexturen der Navigation und GUI-Sicherheit werden davon unabhängig geprüft.
