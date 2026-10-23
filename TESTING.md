# Verification

v2.2.0-BETA, 23 October 2026: **83 automated tests passed** and the complete
integration suite passed on both current local server instances:

| Server | Runtime | Result | Largest observed entity-tick interval |
| --- | --- | --- | --- |
| Paper 26.3 build 190 | Java 25.0.3 | PASS | 130 ms |
| Folia 26.3 build 3 | Java 25.0.3 | PASS | 153 ms |

These intervals include world and profile processing across the complete local
test server. They are observations, not an isolated plugin benchmark.
A production load test with many simultaneous players was not performed.
Older Paper 1.18/1.18.2 integration runs belong to earlier releases and were not
repeated for this version.

## Build and unit tests

`./mvnw verify` builds the installable shaded JAR and runs the tests. Coverage includes:

- Competition ranks, ties, zero values, stable ordering, medal totals and overflow.
- Aggregation of 2,000 players with 80 positive statistics each.
- Classic and modern statistics storage, dimension paths, aliases, malformed JSON,
  read limits and unsupported IDs.
- Online data replacing disk values once, request coalescing, cache behavior,
  fresh offline reads and shutdown cancellation.
- Personal reads completing while another player's server read remains pending;
  repeated online/offline personal reads returning updated values without a snapshot.
- UUID/name resolution, ambiguous names, offline profiles, persisted names and placeholders.
- Scheduler-safe asynchronous heads, skin caching and embedded navigation textures.
- Every Spigot 1.18.2 statistic assigned to exactly one category; every supported
  custom statistic has an English label and a dedicated icon.
- Representative inventory items for every known mined block, including blocks
  without their own inventory form.
- Item uses and placements are disjoint, including seeds, redstone, tools, boats,
  buckets and edible planting items.
- All time counters belong to Play time. Player entities are excluded from both
  mob categories and cannot create hidden medal competitions.
- English number/time/distance/damage formatting, 35-entry pagination, reserved
  side borders, navigation slots and scope selection.
- Every Bukkit click type and inventory action, drag events and creative events.

The local Windows build used Maven 3.10.0 bundled with IntelliJ and the existing
offline dependency cache:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir=C:/Users/laurin/Desktop/CodingStuff/Java/SkyStats/target/test-temp'
mvn -o -B "-Daether.enhancedLocalRepository.trackingFilename=skystats-local.repositories" "-Dmaven.repo.local=.tools/m2" verify
```

The tracking-filename override accommodates dependencies originally cached with a
different repository identity. It does not modify source dependencies.
The final build ran outside the sandbox after Windows denied an atomic file
replacement in a temporary name-cache test.

## Real servers and network transactions

The existing Windows test instances are under `target/test-servers/paper` and
`target/test-servers/folia`, bound to localhost ports 25576 and 25577.
Both already have EULA acceptance recorded. They use offline mode and deliberately
generated test identities, statistics and worlds.

```powershell
python scripts/run-integration-tests.py --server-root target/test-servers --java "C:/Program Files/Java/jdk-25.0.3/bin/java.exe" paper-26.3 folia-26.3
```

The integration runner creates a separate probe plugin, starts each server, connects
a minimal protocol client, runs assertions on the player's entity scheduler, and
stops the server. The probe plugin is never included in SkyStats.
The client supports protocols 757, 758 and 777 for 1.18, 1.18.2 and 26.3 respectively.
It waits for the final inventory's Open Screen packet before sending attack packets,
so fast profile loading cannot leave it targeting the previous loading window.

The current suite verifies:

1. Personal menus have 54 slots, fourteen symmetric category positions, emerald
   borders and a central server button. Personal contexts do not require a server snapshot.
2. Every current runtime statistic has a category, every custom statistic has an
   English label and dedicated icon, and every known mined block has an inventory icon.
3. Inventory safety across 121 click/action combinations, drag and creative events,
   plus ten actual network attack packets. Player inventory, cursor and GUI remain unchanged.
4. Strider/elytra icons, live values and descending personal sorting.
5. Separate item-use and placement menus; Minecraft item names remain unset for
   client translation.
6. Time since last death/rest, including absent offline counters displayed at zero.
7. Offline lookup through saved Bukkit player data, diamond borders, fresh file values,
   both pages of navigation and textured wooden arrow heads.
8. Both mob categories exclude players; server snapshots contain no hidden player-entity
   medal competitions.
9. Gold-ingot server borders, titles independent of the previously viewed player,
   offline totals and online values replacing old disk data exactly once.
10. Competition ranks `1, 1, 3`, top-three lore, the viewer's own position, complete
    rankings, retained login skins, paginated medals and individual medal profiles.
11. Names appear only in the title of personal/profile views, with ranking entries
    intentionally identifying their competitors.
12. Closing a loading menu prevents delayed reopening; unknown-player messages,
    offline tab completion and invalid-file warnings remain functional.

The sandbox blocks even Java's internal loopback connections. Server integration
therefore ran outside it. Warnings about deliberately corrupt fixture files are expected.
Synthetic offline identities can also produce cosmetic profile-service warnings.

## Evidence and artifacts

- `target/verify.log`: final Maven output.
- `target/surefire-reports/`: individual unit-test results.
- `target/integration-results.txt`: both successful server reports.
- Per server: `probe-result.txt`, `integration-console.log`, `integration-client.log`.
- `target/SkyStats-2.2.0-BETA.jar`: installable plugin.

All runtime downloads, test worlds and reports are ignored by Git.
Avoid `clean` when retaining the existing `target/test-servers/` instances;
the normal setup script uses the separate `.test-servers/` folder.
