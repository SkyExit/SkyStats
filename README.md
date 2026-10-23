# SkyStats

Vanilla statistics, leaderboards and medals in inventory menus for Paper and Folia.
All plugin text is English. Minecraft item and spawn-egg names use the player's
client language. SkyStats reads existing statistics and never counts gameplay events.

## v2.2.0-BETA

- Reviewed icons for every supported custom statistic: strider spawn egg, elytra,
  boat, minecart, mounts, workstations, combat, activities and timers.
- Separate **Items used** and **Blocks placed** categories.
- Removed the general-statistics menu and the empty additional-statistics button.
  Custom values now belong to Movement, Play time, Combat, Interactions or Activities.
- Player entities are excluded from both mob categories and their medal competitions.
  The separate Vanilla Player kills statistic remains available under Combat.
- Play time includes time since last death, time since last rest, world-open time
  and time sneaking. Timers remain visible at zero and display seconds.
- Emerald borders mark your statistics, diamond borders another player's statistics,
  and gold-ingot borders server statistics. Titles and information items identify
  the scope as well.
- Personal menus load independently of server aggregation. Personal refresh only
  reloads that profile; server rankings are requested when entering the server view.

## Commands and menus

- `/stats` opens your statistics.
- `/stats <name>` opens a known online or offline player's statistics.
- `/stats <UUID>` resolves ambiguous names, including after online-mode changes.
- `stats.use` and `stats.others` are granted to everyone by default.

The main menu contains fourteen categories around the server-leaderboard button.
The server menu uses the same layout with medal rankings in the center.
Each inventory has 54 slots: 35 entries, ten side-border items, and nine navigation
slots. Textured wooden arrow heads switch pages; Back and Refresh retain their
positions. Player names appear in inventory titles, and in ranking entries where
they identify competitors.

Personal categories sort by that player's values. Server categories sort by totals
over all known UUIDs, including offline players, and show the top three plus your
own position. Click a server statistic to open its full ranking. Medal profiles
show the individual statistics behind a player's awards.

Ties use competition ranks (`1, 1, 3`). Only positive values earn medals.
Medal rankings sort by total awards, then gold, silver and bronze; equal medal
counts share ranks. Each underlying statistic awards medals exactly once.

## Item uses and placements

Vanilla stores both under `minecraft:used`. SkyStats splits those existing keys:
block items and dedicated planting items (such as seeds, cocoa beans, redstone
and string) appear under Blocks placed. Tools, buckets, boats, armor, food and
other items appear under Items used. The same key never appears in both categories.

Vanilla does not separate different actions using the same item. Edible planting
items such as carrots, potatoes and berries therefore remain under Items used;
their counters cannot reliably identify how many were planted. Buckets likewise
remain item uses. No historical placement counts are invented and no new gameplay
tracking is introduced.

## Build and install

Java 17 or newer is required. The wrapper also requires Bash, curl, tar and
sha512sum, and downloads Maven and dependencies into the ignored `.tools/` folder:

```bash
./mvnw verify
```

Install `target/SkyStats-2.2.0-BETA.jar` in the server's `plugins/` folder,
replacing the previous SkyStats JAR, and restart the server normally.
No other plugins are required. Gson is shaded into an isolated package.
The plugin compiles against Spigot 1.18.2 with Java 17 bytecode; newer public
scheduler and profile APIs are accessed through compatibility adapters.

| Server | Java | Verification |
| --- | --- | --- |
| Paper 26.3, build 190 | 25.0.3 | See TESTING.md |
| Folia 26.3, build 3 | 25.0.3 | See TESTING.md |

Earlier releases were also tested on Paper 1.18 build 66 and Paper 1.18.2 build 388.
Those older servers have not been rerun for v2.2.0-BETA. Folia has no 1.18 release.

## Configuration

`plugins/SkyStats/config.yml` contains:

| Setting | Default | Purpose |
| --- | --- | --- |
| `stats-directory` | empty | Detect the primary world's shared statistics directory |
| `hide-zero-values` | `true` | Hide zero values, except the Play time timers |
| `cache-seconds` | `30` | Server-snapshot lifetime |
| `refresh-cooldown-seconds` | `5` | Minimum interval between expensive rebuilds and personal refreshes |
| `live-reads-per-tick` | `400` | Maximum statistic API reads per player per tick in a pending request |
| `live-read-timeout-seconds` | `30` | Timeout for live reads |
| `max-file-size-mib` | `16` | Per-file size limit |
| `max-player-files` | `100000` | Player-file limit per rebuild |
| `max-statistics-per-player` | `100000` | Per-file entry limit |
| `max-total-values` | `2000000` | Sparse-dataset limit before ranking allocation |

Statistics paths may be absolute or relative to the world container. Automatic
detection supports both `world/stats` and the modern `world/players/stats` layout.
Dimension paths resolve to shared world storage. Modern storage takes precedence
over leftover legacy folders, and a single directory is read.
Clear an explicitly configured legacy path when migrating to modern storage.
The server-root `usercache.json` is read independently of the world container.

## Freshness and data handling

Each personal opening or refresh rereads offline JSON or scans the online player's
live statistics on their entity scheduler. Personal requests do not wait for other
players or server leaderboards. Only an unresolved player name during initial
indexing may need the first server scan to finish.

Server snapshots are still cached, timestamped and protected by the configured
refresh cooldown. Online values replace the same UUID's disk values exactly once.
Live reads are spread across ticks; snapshots represent a sampling interval.
An initial server snapshot is prepared in the background at startup.
Navigation inside an open profile uses that profile's loaded values until Refresh.

JSON reads, sorting and aggregation run on data workers. Invalid files are skipped
with English warnings in server views. Missing directories, limits, disconnected
players and timeouts produce a visible error or warning. Unsupported statistic
IDs retain a readable fallback name and their technical ID. Known blocks without
inventory forms use corresponding items; mobs use spawn eggs or suitable alternatives.

Names missing from both caches are resolved through saved Bukkit player profiles
on a data worker and persisted in `plugins/SkyStats/players.json`.
Unknown names display as `Unknown (<UUID prefix>)`; placeholders are never saved
as names or offered in tab completion. UUID remains the primary identity.

SkyStats writes only configuration and its name cache. Vanilla statistics are
never changed. Player heads reuse login skins and resolve missing profiles
asynchronously, with bounded successful/failed caches. Late skin or data results
only update the still-open inventory. Navigation-arrow textures are embedded.

## Tests

`./mvnw verify` runs unit and regression tests. The separate integration suite
uses disposable local Paper/Folia worlds and a real network client:

```bash
python scripts/setup-test-servers.py --include-paper-1.18
python scripts/run-integration-tests.py paper-1.18 paper-1.18.2 paper-26.3 folia-26.3
```

The setup script leaves `eula=false` until the Minecraft EULA is explicitly
accepted. Existing local instances already have acceptance recorded.
Test instances bind only to localhost, use offline mode for synthetic identities
and contain deliberately generated statistics. They stop after testing.
See [TESTING.md](TESTING.md) for results, coverage and local Windows commands.

References: [Spigot Statistic API](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Statistic.html),
[Paper/Folia scheduling](https://docs.papermc.io/paper/dev/folia-support/).
