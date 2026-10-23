# SkyStats 0.7.0-BETA

SkyStats zeigt Minecraft-Statistiken und daraus berechnete Level an. Dieselbe JAR
unterstützt **Spigot, Paper und Folia für Minecraft Java 26.3**. Java 25 ist erforderlich.

## Bauen und installieren

```powershell
.\mvnw.cmd clean verify
```

Unter Linux/macOS: `sh mvnw clean verify`. Der Wrapper lädt Maven 3.10.0 und prüft
die SHA-256-Prüfsumme des Downloads. Das Ergebnis ist
`target/skystats-0.7.0-BETA.jar`. Diese Datei in den `plugins`-Ordner kopieren und
den Server neu starten. Es werden keine zusätzlichen Plugins benötigt.

## Befehle und Berechtigungen

| Befehl | Funktion | Berechtigung |
| --- | --- | --- |
| `/stats` | Eigene Statistiken öffnen | `skystats.stats` |
| `/stats <player>` | Statistiken eines anderen Spielers öffnen | Zusätzlich `skystats.stats.others` |
| `/level`, `/lvl` | Eigenes Level und Fortschritt anzeigen | `skystats.level` |
| `/compare <player1> <player2>` | Zwei Spieler vergleichen | `skystats.compare` |

Alle vier Berechtigungen sind standardmäßig aktiviert. Sie können über einen
Bukkit-kompatiblen Berechtigungsmanager vergeben oder entzogen werden.
`skystats.compare` erlaubt unabhängig von `skystats.stats.others` den Vergleich.
Die Befehle sind nur für Spieler verfügbar; Ziele müssen online sein und mit
ihrem vollständigen Namen angegeben werden.

## Änderungen gegenüber 0.6.0

- Leveldaten sind pro Abfrage getrennt. Level steigen an festen Grenzen auf;
  Fortschrittsbalken können keine negativen Längen mehr bekommen.
- EXP-Summen und Multiplikationen verwenden `long`, damit hohe Statistiken
  nicht in negative Werte überlaufen. Die bisherigen EXP-Gewichtungen bleiben erhalten.
- Eine Stunde Spielzeit wird korrekt angezeigt. Schadenswerte werden aus
  Minecraft-Zehntelpunkten in Lebenspunkte (HP) umgerechnet.
- Unbekannte Spielernamen und falsche Argumentzahlen erzeugen verständliche
  Fehlermeldungen. Sie zeigen nicht mehr versehentlich die eigenen Statistiken.
- Eigene InventoryHolder identifizieren die Statistikmenüs. Klicks, Shift-Klicks,
  Hotbar-Tausch und Drag-Aktionen können keine Menügegenstände verschieben.
  Andere Inventare mit „Sky“ im Titel bleiben benutzbar.
- Spielerstatistiken und Profile werden auf dem jeweiligen Spielerthread
  erfasst. Das Menü wird auf dem Thread seines Betrachters aufgebaut und geöffnet.
  Neuere Menüanfragen verdrängen ältere; Disconnects und Plugin-Abschaltung
  beenden ausstehende Abfragen.
- Spigot-API und Maven-Buildplugins wurden aktualisiert. Die Server-API wird
  weiterhin nicht mitgeliefert; der unnötige Shade-Schritt entfällt.

Die bisherige `permission.yml` und ihr namensbasierter Admin-Abgleich werden
nicht mehr verwendet. Vorhandene Dateien werden nicht gelöscht. Rechte werden
über die oben genannten Bukkit-Nodes verwaltet. Die leere `data.yml` und die
unfertigen, unbenutzten Daten-/Vergleichsmanager entfallen. Minecraft selbst
speichert die Spielerstatistiken weiterhin; SkyStats benötigt keinen eigenen Datenspeicher.

## Levelberechnung

Level 1 startet bei 0 EXP. Für den ersten Aufstieg werden 220 EXP benötigt;
jeder weitere Aufstieg kostet 20 EXP mehr: Level 2 bei 220, Level 3 bei 460,
Level 4 bei 720. Die Startgrenze für Level `L` ist
`10 * (L - 1)^2 + 210 * (L - 1)`.

Da die alte Berechnung fehlerhaft war, können angezeigte Level nach dem Update
abweichen. Die zugrunde liegenden Minecraft-Statistiken werden nicht verändert.
Reise-EXP behalten die bisherigen Gewichtungen für Klettern, Schleichen, Fallen,
Sprinten, Fliegen, Schwimmen, Gehen, Boot, Elytra und Pferd; weitere angezeigte
Reisearten dienen weiterhin zur Information.

## Kompatibilität und Tests

Die Implementierung kompiliert gegen die Spigot-API `26.3-R0.1-SNAPSHOT`.
Auf Paper und Folia wird die Entity-Scheduler-API zur Laufzeit erkannt; Spigot
verwendet den Bukkit-Scheduler. Zwischen Spielerthreads werden nur kopierte
Statistikwerte und Profile übergeben. Es gibt keine blockierenden Warteoperationen
auf einem Serverthread. Dieses Vorgehen folgt den
[Folia-Scheduler-Vorgaben](https://docs.papermc.io/paper/dev/folia-support/).

`clean verify` führt die Regressionstests für Levelgrenzen, hohe EXP-Werte,
Formatierung, Befehle, Rechte, Menüschutz, Snapshots und Scheduler-Abbrüche aus.
Ein Ladetest ersetzt keinen Spieltest mit mehreren verbundenen Spielern.
Für die Abnahme auf jeder Plattform sind insbesondere folgende Fälle sinnvoll:

1. `/stats`, `/stats <player>`, `/level`, `/lvl` und `/compare <player1> <player2>` ausführen.
2. Auf Folia die Zielspieler weit voneinander entfernt platzieren, während
   Vergleichen teleportieren und einen Zielspieler ausloggen lassen.
3. Mehrere Menüanfragen schnell nacheinander stellen; das jüngste Ergebnis muss sichtbar sein.
4. Klicks, Shift-Klicks, Zahlentasten, Doppelklicks und Drag-Aktionen im Menü prüfen.
5. Befehlsrechte entziehen und unbekannte Namen beziehungsweise falsche Argumentzahlen testen.
