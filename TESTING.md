# Testnachweise

Die Implementierung wird durch Unit-Tests und eine separate Prüfung auf tatsächlichen Minecraft-Servern abgesichert. Die ausgeführten Server-Builds sind in der README dokumentiert. Die Dateien unter `.test-servers/` und `target/` bleiben lokale, ignorierte Testartefakte.

Ergebnis vom 23.10.2026: **56 automatische Tests bestanden**, dazu die vollständige Integrationssuite auf **Paper 1.18, Paper 1.18.2, Paper 26.3 und Folia 26.3**. Auf jeder Instanz blieben Spielerinventar, Cursor und GUI auch nach den zehn tatsächlichen Netzwerk-Klickpaketen unverändert.

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
- Klassische und moderne Dimension-Verzeichnisstrukturen.
- Tick-, Distanz- und Schadenseinheiten, deutsche Texte und Seitengrenzen.
- Sämtliche Bukkit-Klicktypen und Inventaraktionen sowie Drag- und Creative-Ereignisse. Fremde Inventare werden nicht blockiert.

## Echte Server und Netzwerktransaktionen

`scripts/run-integration-tests.py` baut `SkyStatsProbe.jar` getrennt vom eigentlichen Plugin. Der verbundene Client verwendet Protokoll 757 für 1.18, 758 für 1.18.2 und 777 für 26.3. Für 26.3 wurden die benötigten Paketdefinitionen anhand des heruntergeladenen Servercodes geprüft. Der Client verarbeitet Verbindung, Konfiguration, Teleportbestätigungen und Keepalives; die GUI-Assertions laufen auf dem zuständigen Spielerthread im Prüfplugin.

Die Suite prüft:

1. Eigene Statistikübersicht mit 54 Slots und Live-Werten aus der Bukkit-API.
2. 115 Kombinationen aus Klicktyp, Slot und Inventaraktion sowie Drag und Creative gegen die geöffnete GUI.
3. Zusätzlich echte Container-Klickpakete über die Netzwerkverbindung: normale Klicks, Shift-Klicks oben und unten, Hotbar, Offhand, Drop und Einsammeln. Danach müssen Spielerinventar, Cursor und GUI exakt unverändert sein.
4. Offline-Namenssuche, Dateiwerte, persönliche Sortierung und beide Seiten der 45-Slot-Navigation mit texturierten Köpfen.
5. Server-Gesamtsummen inklusive Offline-Spielern; eine absichtlich veraltete Online-Datei wird genau einmal durch Live-Werte ersetzt.
6. Gleichstände mit `1, 1, 3`, Top-3-Lore und die eigene Position außerhalb der Top 3.
7. Vollständige Einzelrangliste, Spieler-Köpfe, blätterbares Medaillenranking und Medaillenprofil.
8. Schließen eines Ladefensters vor Abschluss der Live-Abfrage; ein verspätetes Ergebnis darf es nicht wieder öffnen.
9. Unbekannte Namen, Offline-Tabvervollständigung und Hinweise auf fehlerhafte Statistikdateien.

Die größte gemessene Pause zwischen den Entity-Tick-Aufrufen wird im Ergebnis mitgeschrieben. Sie ist eine Beobachtung des gesamten Testservers einschließlich Welt- und Profilverarbeitung und kein isolierter Plugin-Benchmark. Die größere synthetische Aggregation wird separat außerhalb des Ticks getestet. Eine Lastprüfung mit vielen gleichzeitig spielenden Menschen ist damit nicht ersetzt.

Die rein synthetischen Offline-UUIDs können bei der standardmäßigen Skin-Auflösung der Serversoftware Meldungen des Mojang-Profildienstes auslösen. Das betrifft die kosmetische Skin-Auflösung; die UUIDs, Statistikwerte, Kopftexturen der Navigation und GUI-Sicherheit werden davon unabhängig geprüft.
