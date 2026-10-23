# Minecraft-Statistik-Plugin – Anforderungsspezifikation

> **Projektstatus:** Konzept / funktionale Spezifikation  
> **Zielplattform:** Minecraft Java Edition, primär **26.3**, rückwärtskompatibel bis **mindestens 1.18**  
> **Serversoftware:** Paper und **Folia**  
> **Kernprinzip:** Vanilla-Statistiken anzeigen und auswerten, ohne Spielereignisse selbst mitzuzählen.
> **Code**: Der gesamte Code inklusive aller Kommentare muss auf englisch sein

## 1. Projektziel

Entwickelt werden soll ein übersichtliches, vollständig inventarbasiertes Statistik-Plugin für einen Minecraft-Java-Server. Spieler sollen ihre eigenen Statistiken, die Statistiken anderer Spieler – einschließlich **Offline-Spielern** – sowie serverweite Bestenlisten abrufen können.

Der Schwerpunkt liegt auf:

- umfassender Darstellung aller von der jeweiligen Minecraft-Version nativ erfassten Statistiken;
- Kategorien mit sortierten Detailansichten und komfortabler Seitennavigation;
- serverweiten Bestenlisten für einzelne Blöcke, Items, Kreaturen und allgemeine Kennzahlen;
- einem **Gold-/Silber-/Bronze-Medaillensystem** mit übergreifendem Medaillenranking;
- einer durchgehend **schreibgeschützten und gegen Item-Exploits abgesicherten GUI**;
- stabiler Nutzung auf Paper und Folia sowie einer gepflegten Versionskompatibilität.

**Wichtige technische Präzisierung:** Minecraft-Statistiken gehören zum Vanilla-Statistiksystem, **nicht** zum Advancement-System. Sie werden serverseitig unter anderem in `world/stats/<uuid>.json` gespeichert. Das Plugin soll diese vorhandenen Daten auswerten. Für Online-Spieler sind nach Möglichkeit die aktuellen Statistikwerte über die Server-API zu lesen; für Offline-Spieler können gespeicherte Statistikdateien verwendet werden. Die GUI, Sortierung und zwischengespeicherte Berechnungen sind erlaubt – es soll lediglich **kein eigenes Gameplay-Tracking** erfolgen.

## 2. Versionen und Plattformkompatibilität

### Verbindliche Anforderungen

| Bereich | Anforderung |
|---|---|
| Primäre Zielversion | Minecraft Java **26.3** |
| Älteste Zielversion | **1.18** oder älter, soweit technisch umsetzbar |
| Server | Paper und Folia, nutze aber Spigot als API version, um maximale Kompatibilität zu gewährleisten|
| Sprache des Plugins | Deutsch als Standard; interne Namen und technische IDs dürfen Englisch bleiben |
| Vanilla-Daten | Versionabhängige Statistik-IDs korrekt erkennen und auslesen |
| Fehlende neue Inhalte | Auf älteren Serverversionen nicht vorhandene Blöcke, Items und Statistiken ausblenden oder sinnvoll kennzeichnen |

### Technische Umsetzung

- Die Entwicklung muss **Folia-konform** sein: Spieler- und Inventaroperationen auf dem zuständigen Entity-Scheduler ausführen; globale, regionale und asynchrone Aufgaben korrekt trennen. Niemals voraussetzen, dass es einen einzigen globalen Hauptthread für sämtliche Welten und Spieler gibt.
- Die Plugin-Metadaten dürfen `folia-supported: true` **erst dann** enthalten, wenn die erforderlichen Folia-Sicherheitsregeln tatsächlich umgesetzt und getestet wurden.
- Kompatibilität zur 1.18-Reihe darf nicht bloß behauptet werden: Neue API-Aufrufe, Item-Metadaten, Kopfdaten und Statistiknamen benötigen gegebenenfalls **Versionsadapter oder getrennte Builds**.
- Unterschiedliche Java- und Server-API-Anforderungen der Zielversionen sind im Build- und Testkonzept zu berücksichtigen.
- **Versionsgrenze:** Die Unterstützung für Minecraft **1.18** bezieht sich auf dafür verfügbare Paper-/Spigot-kompatible Server; Folia ist nur für Minecraft-Versionen zu unterstützen, für die Folia selbst tatsächlich verfügbar ist.
- Keine unnötige Abhängigkeit von NMS oder versionsabhängigen internen Minecraft-Klassen.

**Abnahmekriterium:** Dieselben grundlegenden Funktionen laufen nachweislich auf den vorgesehenen Paper- und Folia-Versionen; auf älteren Versionen entfallen nur Features, die die jeweilige Vanilla-Version nicht bietet.

## 3. Befehle und Berechtigungen

| Befehl | Funktion |
|---|---|
| `/stats` | Öffnet die eigene Statistikübersicht. |
| `/stats <Spielername>` | Öffnet die Statistikübersicht des angegebenen Spielers, **auch wenn dieser offline ist**. |

**Gewünschtes Verhalten:**

- Bei `/stats` wird immer der ausführende Spieler als Ziel verwendet.
- Bei `/stats <Spielername>` wird der Spieler über seine **UUID** identifiziert; der Name dient nur zur Suche und Anzeige. Namensänderungen dürfen Statistiken nicht falsch zuordnen.
- Es dürfen auch Spieler angezeigt werden, die aktuell nicht online sind, aber bereits Statistikdaten auf dem Server besitzen.
- Unbekannte Spieler oder Spieler ohne gespeicherte Daten erhalten eine klare Fehlermeldung bzw. eine verständliche „Noch keine Statistiken vorhanden“-Anzeige.
- Optional: Tab-Vervollständigung für bekannte Spielernamen.
- Vorgeschlagene Permissions: `stats.use` und `stats.others` (beide standardmäßig für normale Spieler freigegeben). Administrationsrechte sind für reines Lesen nicht erforderlich.

## 4. Menüstruktur und Navigation

### 4.1 Übersicht der Menüebenen

```text
/stats [Spieler]
└── Statistik-Hauptmenü: [Spielername]
    ├── Allgemeine Statistiken
    │   └── Einzelwerte, z. B. Sprünge, Tode, Schaden, Spielzeit
    ├── Abgebaute Blöcke (Blocks Mined)
    ├── Hergestellte Items (Items Crafted)
    ├── Benutzte Items / platzierte Blöcke (Items Used)
    ├── Verbrauchte / zerbrochene Items (Items Broken)
    ├── Aufgehobene Items (Items Picked Up)
    ├── Weggeworfene Items (Items Dropped)
    ├── Getötete Kreaturen (Mobs Killed)
    ├── Von Kreaturen getötet (Killed By)
    ├── Fortbewegung / zurückgelegte Strecken
    ├── Spielzeit
    └── Server-Leaderboards
        ├── Dieselben Statistik-Kategorien
        │   └── Einzelstatistiken, nach Server-Gesamtsumme sortiert
        │       └── Optional: vollständige Spieler-Rangliste für den Wert
        └── Medaillen-Ranking
            └── Optional: Medaillenprofil eines Spielers
```

Das Menüsystem soll visuell einheitlich und selbsterklärend sein. Ein Wechsel zur Bestenliste muss **direkt aus dem Statistik-Hauptmenü** möglich sein. Von jeder Unterseite führt ein Zurück-Button zur jeweils vorherigen Menüebene. Wenn möglich sollte Tab-Autocomplete auch offline Spieler anzeigen.

### 4.2 Grundregeln für jede Inventaransicht

- Inventargröße: **6 × 9 = 54 Slots**.
- **Slots 0–44:** maximal **45 Inhaltsplätze**, also die oberen fünf Reihen.
- **Slots 45–53:** ausschließlich Navigation, Kontextaktionen und Status in der untersten Reihe.
- Ist die Ergebnisliste länger als 45 Einträge, wird sie auf mehrere Seiten aufgeteilt.
- Die aktuelle Seite und die Gesamtzahl der Seiten sollen sichtbar sein, z. B. `Seite 2 / 7`.
- Ein nicht verfügbarer Vorwärts-/Zurück-Pfeil wird ausgeblendet oder eindeutig deaktiviert.
- Die aktuelle Kategorie, der betrachtete Spieler und gegebenenfalls der Leaderboard-Modus sollen jederzeit klar erkennbar sein, vorzugsweise im Inventartitel und ergänzend in der Item-Lore.

### 4.3 Vorgeschlagene Belegung der Navigationszeile

| Slot (0-basiert) | Inhalt | Funktion |
|---|---|---|
| **45** | Spieler-Kopf mit Pfeil nach links | Vorherige Seite |
| 46–47 | Dekoration / frei | Keine Aktion |
| **48** | Zurück-Button | Vorheriges Menü |
| **49** | Info-Item | `Seite X / Y`, ggf. Anzahl Einträge |
| **50** | Aktualisieren-Button | Daten erneut laden, sofern sinnvoll und nicht im Cooldown |
| 51–52 | Dekoration / frei | Keine Aktion |
| **53** | Spieler-Kopf mit Pfeil nach rechts | Nächste Seite |

**Konkrete Kopftexturen:**

- **Links – „Oak Wood Arrow Left“, Head ID 2762 (ab v2.1.0-BETA):** https://minecraft-heads.com/custom-heads/head/2762-oak-wood-arrow-left
- **Rechts – „Oak Wood Arrow Right“, Head ID 2763 (ab v2.1.0-BETA):** https://minecraft-heads.com/custom-heads/head/2763-oak-wood-arrow-right

Diese beiden Köpfe bilden ein optisch zusammenpassendes Pfeilpaar. Es sollen **echte texturierte `PLAYER_HEAD`-Items** mit sichtbar aufgebrachten Pfeilen sein, keine normalen Pfeil-Items. Die Kopftexturen bzw. ihre Texture-Properties sollten im Plugin hinterlegt werden, damit zur Laufzeit weder eine Webanfrage noch ein zusätzliches Head-Database-Plugin zwingend notwendig ist. Eine kompatible Umsetzung für die jeweiligen Minecraft-Versionen ist erforderlich.

## 5. Statistik-Kategorien und Datenabbildung

Das Plugin soll **sämtliche auf der laufenden Minecraft-Version verfügbaren Vanilla-Statistiken** erfassen und übersichtlich gruppieren. Keine feste, zu kurze Liste von Blöcken oder Items: Die verfügbaren Statistiktypen und Material-/Entity-IDs sind versionsabhängig zu berücksichtigen.

| Angezeigte Kategorie | Vanilla-Statistiktyp / Datenquelle | Darstellung |
|---|---|---|
| Abgebaute Blöcke | `minecraft:mined` | Ein Item pro Blocktyp, Anzahl abgebauter Blöcke |
| Hergestellte Items | `minecraft:crafted` | Ein Item pro Material, gefertigte Anzahl |
| Benutzte Items | `minecraft:used` | Ein Item pro Material, Anzahl der Benutzungen; darunter fallen u. a. bestimmte platzierte Blöcke |
| Zerbrochene Items | `minecraft:broken` | Ein Item pro Gegenstand, Anzahl vollständig verbrauchter Werkzeuge usw. |
| Aufgehobene Items | `minecraft:picked_up` | Ein Item pro Material, aufgehobene Menge |
| Weggeworfene Items | `minecraft:dropped` | Ein Item pro Material, weggeworfene Menge |
| Getötete Kreaturen | `minecraft:killed` | Ein Eintrag pro Entity-Typ |
| Tode durch Kreaturen | `minecraft:killed_by` | Ein Eintrag pro Entity-Typ |
| Allgemeine Statistiken | `minecraft:custom` | Einzelwerte wie Sprünge, Tode, Schaden, Interaktionen, Angeln, Raids usw. |
| Fortbewegung | Passende Werte aus `minecraft:custom` | Zurückgelegte Strecken getrennt nach Bewegungsart |
| Spielzeit | `minecraft:custom` → `minecraft:play_time` | Gut lesbare Stunden-/Tage-Angabe |

**Begriffsklärung:** „Blocks broken“ soll im Sinne von **abgebauten Blöcken** interpretiert werden (`minecraft:mined`); `minecraft:broken` bezeichnet dagegen **zerbrochene bzw. aufgebrauchte Items**. Entsprechend gibt es keine allgemeine Vanilla-Kategorie „Blocks traveled“: Gemeint sind die **Bewegungsstatistiken** (z. B. gelaufen, gesprintet, geschwommen, geflogen und gefahren), die als Distanzen angezeigt werden.

Die allgemeinen Statistiken sollen vollständig erreichbar bleiben, auch wenn häufig verwendete Werte zusätzlich in eigene, besser auffindbare Kategorien wie „Spielzeit“ und „Fortbewegung“ ausgelagert werden. Neue oder unbekannte Vanilla-Statistik-IDs sollen nach Möglichkeit mit einer neutralen Darstellung und ihrer technischen Kennung angezeigt werden, statt stillschweigend zu verschwinden.

## 6. Persönliche Statistikansichten

### Sortierung

- Nach dem Öffnen einer Kategorie werden alle zugehörigen Einträge **absteigend nach dem Wert des betrachteten Spielers** sortiert.
- Einträge mit **Wert 0** dürfen standardmäßig ausgeblendet werden. Diese Entscheidung soll möglichst konfigurierbar bleiben.
- Bei gleichen Werten wird nach einem stabilen sekundären Schlüssel sortiert (z. B. angezeigter Name oder Minecraft-ID), damit sich Einträge nicht zufällig verschieben.
- Lange Kategorien werden über die Navigationsköpfe seitenweise dargestellt.

### Item-Darstellung

Nach Möglichkeit wird jeweils das passende Minecraft-Item bzw. der passende Block als Icon verwendet. Nicht als Inventaritem verfügbare Blöcke, Entity-Typen oder allgemeine Statistiken benötigen ein verständliches Ersatzicon.

**Beispiel für eine Item-Lore in „Abgebaute Blöcke“:**

```text
Stein
Abgebaut: 18.492
Anteil an deinen abgebauten Blöcken: 24,6 %   (optional)
```

Wichtig ist die **korrekte Statistikbezeichnung** und die klare Darstellung des Wertes. Ein Wert, der tatsächlich Benutzungen oder verbrauchte Werkzeuge misst, darf nicht irreführend als Blockabbau ausgegeben werden.

### Andere Spieler

Wird das Menü für eine andere Person aufgerufen, ändert sich ausschließlich die Datenquelle: Das Aussehen, die Kategorien, die Sortierung und die Navigation bleiben identisch. Im Titel muss erkennbar sein, **wessen Statistiken** angezeigt werden. Ab v2.1.1-BETA steht der Spielername in persönlichen Ansichten und Medaillenprofilen ausschließlich im Inventartitel; Kategorie-Items, Statistik-Items und Seiteninformationen wiederholen ihn nicht. Das gilt auch beim eigenen `/stats`. Für Offline-Spieler darf kein Beitritt zum Server nötig sein.

## 7. Serverweite Leaderboards

### Zugang und Aufbau

Vom `/stats`-Hauptmenü gelangt man zu **Server-Leaderboards**. Dort stehen grundsätzlich dieselben Kategorien wie bei den persönlichen Statistiken zur Auswahl.

### Server-Sortierung

Anders als in einer persönlichen Kategorie werden die Einträge **absteigend nach ihrer Gesamtsumme über alle bekannten Spieler des Servers** sortiert, einschließlich Offline-Spielern.

Für eine Statistik `s` gilt:

`Server-Gesamtsumme(s) = Summe des Vanilla-Werts von s über alle berücksichtigten Spieler-UUIDs`

Beispiel: In „Abgebaute Blöcke“ steht der insgesamt am häufigsten abgebaute Block ganz oben – unabhängig davon, welcher einzelne Spieler ihn am häufigsten abgebaut hat.

### Lore pro Eintrag

In der Item-Lore jedes Eintrags sollen die **Server-Gesamtsumme** und die besten Spieler für **genau diese Einzelstatistik** erscheinen.

**Beispiel:**

```text
Stein
Serverweit abgebaut: 1.284.651

🥇 SpielerA  –  410.200
🥈 SpielerB  –  302.100
🥉 SpielerC  –  227.450

"Kommentar: Wenn der EIGENE Name nicht unter den Top 3 ist, dann soll ... POSITION - DEIN NAME - BLÖCKE stehen"

Klicken: komplette Rangliste anzeigen  (optional)
```

- Die Top-Platzierungen sollen auch **Offline-Spieler** berücksichtigen.
- Platz 1 erhält eine **Goldmedaille**, Platz 2 eine **Silbermedaille**, Platz 3 eine **Bronzemedaille**.
- Die Item-Lore muss die Spieler und ihre Werte ausweisen; die Medaillen können als Symbole oder farbige Textdarstellung umgesetzt werden, damit sie unabhängig von Emoji-/Font-Unterstützung eindeutig bleiben.
- Optional kann ein Klick auf einen Eintrag eine eigene **Spieler-Bestenliste für diese Einzelstatistik** öffnen, wiederum mit 45 Einträgen pro Seite.
- Allgemeine Statistiken wie „Sprünge“ oder „Spielzeit“ werden als **eigene messbare Einzelwerte** behandelt. Für einen zusammengefassten Kategorienamen ohne eindeutig definierte Summe werden keine künstlichen Rankings erfunden.

### Gleichstände und fehlende Daten

- Empfohlene Regel: **Gleichstände teilen sich den Platz und die entsprechende Medaille.** Beispiel: Zwei Spieler mit identischem Höchstwert bekommen beide Gold; die nächste Platzierung ist dann Bronze (Wettkampfrangfolge `1, 1, 3`).
- **Nullwerte vergeben keine Medaille.** Ein Eintrag muss mindestens einen positiven erfassten Wert haben, damit Plätze ausgezeichnet werden.
- Fehlende Statistikdateien bedeuten „keine Daten vorhanden“; sie dürfen nicht als belegte Rekorde oder als negative Werte interpretiert werden.
- Ein stabiler alphabetischer Zweitschlüssel sorgt bei Gleichständen für eine nachvollziehbare Anzeige-Reihenfolge, ohne den eigentlichen Rang zu verändern.

## 8. Globales Medaillen-Ranking

Zusätzlich zum Leaderboard einzelner Statistiken gibt es ein **serverweites Medaillen-Ranking der Spieler**.

### Vergabe

- **Jede eindeutig definierte Einzelstatistik** mit mindestens einem positiven Wert auf dem Server ist ein potenzieller Medaillenwettbewerb, beispielsweise `minecraft:mined` + `minecraft:stone`, `minecraft:killed` + `minecraft:zombie` oder `minecraft:custom` + `minecraft:jump`.
- Für diese Einzelstatistik werden die besten drei Plätze aus allen bekannten Spielern (online und offline) ermittelt.
- Die Gold-, Silber- und Bronzemedaillen aus diesen Einzelstatistiken werden **pro Spieler zusammengezählt**.
- Ein Spieler kann somit viele Medaillen über ganz unterschiedliche Statistiken hinweg besitzen.
- Kategorien, die Werte lediglich zusätzlich gruppieren (z. B. „Fortbewegung“), dürfen **nicht zu einer doppelten Medaillenvergabe** für dieselbe zugrunde liegende Statistik führen.

### Hauptsortierung

Standardmäßig bestimmt die **Gesamtzahl aller Medaillen** den Rang, wie gewünscht:

`Medaillen gesamt = Gold + Silber + Bronze`

Bei gleicher Gesamtzahl erfolgt die Sortierung nach **Gold > Silber > Bronze**, danach nach Spielername. Die Sortierung und die Gleichstandsbehandlung sollen nachvollziehbar sein.

**Beispiel für das Ranking:**

| Rang | Spieler | Gold | Silber | Bronze | Gesamt |
|---|---|---:|---:|---:|---:|
| 1 | SpielerA | 41 | 16 | 9 | **66** |
| 2 | SpielerB | 30 | 18 | 14 | **62** |
| 3 | SpielerC | 21 | 24 | 11 | **56** |

Die Beispielzahlen sind fiktiv. Im GUI bietet sich pro Spieler sein jeweiliges Kopf-Item mit Name, Gesamtzahl und Aufschlüsselung der Medaillen in der Lore an. Auch dieses Ranking muss **blätterbar** sein. Optional kann ein Spielerprofil anzeigen, **für welche konkreten Statistiken** die Medaillen gewonnen wurden.

## 9. Spielzeit und Fortbewegung

### Spielzeit

Die Spielzeit gehört ausdrücklich zum Pflichtumfang:

- Primäre Quelle: Vanilla-Statistik **`minecraft:play_time`** (über die kompatible API bzw. gespeicherte Vanilla-Daten).
- Anzeige nicht nur als Rohwert, sondern beispielsweise als `12 Tage, 6 Stunden, 14 Minuten` sowie optional als Gesamtstunden.
- Intern liefert Minecraft die Zeitstatistik in **Ticks**; sie muss korrekt in eine verständliche Einheit umgerechnet werden. Die Anzeige entspricht der Vanilla-Spielzeitmessung und ist keine eigens gemessene Echtzeit-Sessiondauer.
- Spielzeit muss sowohl im individuellen Menü als auch als **serverweite Bestenliste mit Medaillen** erscheinen.

### Fortbewegung

Vorhandene Vanilla-Distanzstatistiken sollen je nach Version getrennt dargestellt werden, z. B.:

- gelaufen, gesprintet, geschlichen;
- geschwommen, unter Wasser gegangen, geklettert;
- geflogen / mit Elytra unterwegs;
- per Boot, Minecart, Pferd oder anderen Fahrzeugen/Reittieren gereist.

Minecraft erfasst viele Strecken in **Zentimetern**. Die Anzeige soll automatisch sinnvoll in **Blöcke/Meter bzw. Kilometer** umgerechnet werden (100 Minecraft-Zentimeter = 1 Block = 1 angezeigter Meter). Wo die Vanilla-Werte sich fachlich überschneiden könnten, werden die Einzelwerte nicht ungeprüft addiert. Serverweite Rankings erfolgen zunächst **pro Fortbewegungsart**.

## 10. Datenquellen, Spieleridentität und Aktualität

### Keine eigene Spielereignis-Erfassung

Für alle Pflichtfunktionen sollen ausschließlich existierende Vanilla-Zähler genutzt werden. Das Plugin benötigt daher **keine Listener**, die beispielsweise Blockabbau, Kills, Sprünge oder zurückgelegte Meter selbst mitzählen.

Zulässig und für die Funktion erforderlich sind:

- Einlesen der Vanilla-Statistiken und ihrer jeweiligen Rohwerte;
- Pflege einer Zuordnung **Spieler-UUID ↔ bekannter letzter Spielername** zur Auflösung von Offline-Spielern;
- kurzlebiger Cache für aggregierte Werte, Sortierung und Medaillenzahlen;
- UI-Zustand (aktuelle Seite, Kategorie, betrachteter Spieler, Navigation);
- optionale serverseitige Einstellungen.

Dies ist **Datenverarbeitung**, aber kein separates Gameplay-Tracking.

### Online und offline

- **Online:** nach Möglichkeit aktuelle Werte über die dafür vorgesehenen Bukkit-/Paper-APIs abfragen. Gerade unter Folia muss die Abfrage der jeweils zuständigen Thread-/Scheduler-Regel entsprechen.
- **Offline:** gespeicherte Werte aus der betreffenden Vanilla-Statistikdatei lesen, ohne den Spieler simuliert online zu setzen.
- **Keine Doppelzählung:** Eine UUID fließt genau einmal in die Server-Aggregation ein. Während ein Spieler online ist, darf dessen möglicherweise veraltete JSON-Datei nicht zusätzlich zum aktuellen API-Wert gezählt werden.
- **Mehrere Dimensionen:** Die servereigenen Vanilla-Spielerstatistiken verwenden die zur Serverwelt gehörende Statistikablage; nicht pro Dimension dieselben Spielerstatistiken erneut aufsummieren.
- **Identität:** UUID ist der Primärschlüssel. Bei Namensänderungen oder `online-mode`-Umstellungen dürfen Datensätze nicht versehentlich zusammengeführt werden.
- **Nicht vorhandene Daten:** Spieler ohne bisherige Statistikeinträge werden informativ behandelt. Für ein serverweites Ranking gelten nur die tatsächlich bekannten und vorhandenen Spielerstatistiken.

### Aktualisierung und Konsistenz

- Statistikansichten sollen **möglichst aktuelle** Werte zeigen.
- Leaderboards und Medaillen dürfen zur Performance zeitweilig gecacht werden; die GUI sollte bei Bedarf einen **Aktualisieren-Button** bereitstellen.
- Bei gecachten Daten ist eine Kennzeichnung wie `Stand: 14:32:10` sinnvoll.
- Eine frei konfigurierbare Cache-Dauer und Schutz vor häufigen aufwendigen Neuberechnungen sind empfehlenswert.
- Wenn Statistiken während der Berechnung verändert werden, muss das Plugin eine nachvollziehbare Momentaufnahme darstellen und darf keine beschädigten oder doppelt gezählten Ergebnisse erzeugen.

## 11. Sicherheit: Inventare strikt schreibgeschützt

**Nicht verhandelbare Anforderung:** Aus keinem `/stats`-Menü dürfen Gegenstände entnommen, eingeschleust, dupliziert, bewegt oder sonst wie in das Spielerinventar übertragen werden. Die Inventare sind reine **Read-only-Oberflächen**.

Die Implementierung muss besonders absichern:

- normale Links- und Rechtsklicks sowie Shift-Klicks;
- Hotbar-Zifferntasten und Austausch mit der Offhand;
- Doppelklicks zum Einsammeln ähnlicher Items;
- Ziehen/Verteilen von Items über mehrere Slots (`InventoryDragEvent`);
- Creative-Mode-spezifische Inventaraktionen;
- Drop-, Swap-, Collect-to-Cursor- und sonstige Inventaraktionen, die UI-Items bewegen könnten;
- Interaktionen aus dem unteren Spielerinventar, falls sie Items in das obere Statistik-Inventar verschieben würden;
- schnelle Seitenwechsel, Menüschließungen, Disconnects und verzögert eintreffende asynchrone Daten.

**Umsetzungshinweise:**

- Eigene GUI-Inventare über eindeutige Holder/Ansichtstypen identifizieren, nicht ausschließlich am Fenstertitel.
- Inventarereignisse gezielt prüfen und **alle den GUI-Inhalt verändernden Aktionen vor der Vanilla-Verarbeitung abbrechen**.
- Die Anzeige-Items sind ausschließlich UI-Elemente; aus Klicks werden nur definierte Navigationsaktionen.
- Vor einem asynchron verzögerten GUI-Update prüfen, ob der Spieler noch online ist und wirklich dieselbe Ansicht geöffnet hat.
- Keine Fremdthreads dürfen direkt Spielerinventare verändern.

**Abnahmekriterium:** Auch bei bewusstem Versuch mit bekannten Inventar-Klickkombinationen entstehen weder Duplikate noch entnehmbare GUI-Items oder unerwünschte Inventaränderungen.

## 12. Performance und Stabilität

Da große Server viele Spieler und sehr viele verschiedene Statistik-IDs enthalten können, muss die Umsetzung ressourcenschonend arbeiten.

- JSON-Dateien und große aggregierte Berechnungen **asynchron** lesen bzw. verarbeiten, soweit dies sicher möglich ist.
- Auf Folia keine blockierenden Dateizugriffe oder vollständigen Ranglistenberechnungen im Entity-/Region-Tick ausführen.
- Große Datenmengen nur einmal pro Aktualisierungszyklus aggregieren und anschließend für Menüs wiederverwenden.
- Inventarinhalte nur für die benötigte **45-Slot-Seite** erstellen, statt sämtliche möglichen ItemStacks vorab zu generieren.
- Cache-Größen, Invalidierung, Ladezustände und Fehlerfälle begrenzen.
- Fehlerhaft formatierte oder teilweise fehlende JSON-Dateien robust abfangen; keine ganze Bestenliste wegen eines einzelnen defekten Datensatzes abbrechen.
- Datentypen und Summen so wählen, dass Server-Aggregate nicht durch Integer-Überläufe verfälscht werden; Anzeigewerte sauber formatieren.
- Temporäre Ladeanzeige statt Lag beim Öffnen einer großen Bestenliste.
- Versionsbedingte Änderungen der Statistik- oder Material-IDs zentral behandeln.

## 13. Ergänzende Funktionen, die **ohne** eigenes Gameplay-Tracking möglich wären

Diese Ideen sind nicht Teil der Pflichtfunktionen, passen aber zum datenbasierten Konzept:

1. **Suchfunktion und Filter** für sehr große Kategorien (z. B. nur Steinarten, nur Waffen oder Namenssuche).
2. **Sortierumschalter** zwischen höchstem Wert, alphabetisch und – wo sinnvoll – eigener Position im Serverranking.
3. **Spielervergleich**: Zwei bereits gespeicherte Vanilla-Statistikprofile direkt gegenüberstellen.
4. **Lieblingsstatistiken** zum schnelleren Wiederfinden wichtiger Kennzahlen.
5. **Detailranking per Klick** auf einen Block / eine Kreatur / eine allgemeine Statistik.
6. **Medaillenprofil** mit Auflistung aller Gold-, Silber- und Bronzewertungen eines Spielers.
7. **Konfigurierbare Kategorienamen, Item-Icons, Farben und Lore-Texte** sowie optional englische Übersetzungen.


## 14. Abnahmekriterien / Testfälle

Die Implementierung gilt erst als fertig, wenn mindestens diese Fälle erfolgreich getestet sind:

- [ ] `/stats` öffnet ein 54-Slot-Menü mit den eigenen verfügbaren Vanilla-Statistiken.
- [ ] `/stats <Name>` funktioniert mit online und **offline** gespeicherten Spielern.
- [ ] Die verfügbaren Statistiktypen der Zielversion sind vollständig zugänglich.
- [ ] „Abgebaute Blöcke“ ist korrekt als **`minecraft:mined`** abgebildet; Item-Verschleiß ist separat.
- [ ] Kategorieeinträge sind nach dem eigenen Wert absteigend sortiert.
- [ ] Die unteren neun Slots sind für Navigation reserviert; mehr als 45 Einträge lassen sich durchblättern.
- [ ] Die Navigation verwendet passende **Pfeil-Spielerköpfe** für links und rechts.
- [ ] Zurück und Vorwärts navigieren zuverlässig; aktuelle Seite und Zielspieler sind erkennbar.
- [ ] Die Leaderboards beziehen alle bekannten Spieler mit Statistikdaten ein, auch Offline-Spieler.
- [ ] Im Leaderboard sind Einzelstatistiken nach ihrer **Server-Gesamtsumme** sortiert.
- [ ] Die Lore zeigt je Einzelstatistik mindestens die drei führenden Spieler mit Werten und Medaillen.
- [ ] Gold, Silber und Bronze werden aus eindeutigen Einzelstatistiken konsistent vergeben; Gleichstände und Nullwerte sind geregelt.
- [ ] Das Medaillenranking ist vorhanden, nach der **Gesamtanzahl** sortiert und blätterbar.
- [ ] Spielzeit wird korrekt aus Vanilla-Ticks umgerechnet; Distanzwerte sind verständlich formatiert.
- [ ] Kein eigener Gameplay-Zähler ist für Pflichtfunktionen nötig.
- [ ] Keine Doppelzählung zwischen Live-Werten und Offline-Dateien; keine doppelte Aggregation der Dimensionen.
- [ ] GUI-Items sind selbst durch Shift-Klick, Zifferntasten, Doppelklick, Drag, Offhand und Creative-Aktionen nicht entnehmbar oder duplizierbar.
- [ ] Größere Leaderboards verursachen keine spürbaren Tick-Spikes.
- [ ] Folia-Scheduler, asynchrone Aktualisierungen und Menüs funktionieren ohne Thread-Sicherheitsfehler.
- [ ] Kompatibilität auf **26.3** und **mindestens 1.18** ist durch reale Tests belegt.

## 15. Fachliche Referenzen

Diese Quellen dienen zur technischen Einordnung und für die konkreten Pfeilkopf-Texturen:

- [Minecraft Java Edition 26.3 – offizielles Changelog](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3)
- [Minecraft Wiki – Statistics: Kategorien, Einheiten, Speicherung und Vanilla-IDs](https://minecraft.wiki/w/Statistic)
- [PaperMC – Supporting Paper and Folia: Scheduler und Threading](https://docs.papermc.io/paper/dev/folia-support/)
- [Spigot API – `org.bukkit.Statistic` (aktuelle API-Referenz)](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html)
- [Minecraft-Heads – Oak Wood Arrow Left (2762)](https://minecraft-heads.com/custom-heads/head/2762-oak-wood-arrow-left)
- [Minecraft-Heads – Oak Wood Arrow Right (2763)](https://minecraft-heads.com/custom-heads/head/2763-oak-wood-arrow-right)

---

**Kurzfassung:** Ein Paper-/Folia-kompatibles, deutschsprachiges `/stats`-Plugin für Minecraft 26.3 bis mindestens 1.18, das sämtliche verfügbaren **Vanilla-Statistiken** von Online- und Offline-Spielern in sicheren, blätterbaren Inventar-GUIs visualisiert, serverweite Bestenlisten und ein konsistentes Medaillensystem bereitstellt und für seine Kernfunktionen **kein eigenes Gameplay-Tracking** benötigt.
