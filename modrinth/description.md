SkyStats brings Minecraft's built-in statistics into in-game inventory menus for Paper and Folia. Run `/stats` to see what you've mined, crafted, placed or fought, how far you've travelled, and how your activity compares with the rest of the server. Players' existing Vanilla statistics are available as soon as their data is loaded.

## Explore your stats

Browse 14 categories covering blocks mined and placed, item activity, combat, deaths, movement, play time, interactions and more. Entries are sorted by your values, with recognisable Minecraft items as icons. The menus distinguish your own profile, other players' profiles and server-wide statistics.

Open a category or an individual statistic to compare your values with server totals. Where applicable, you'll see your share of the server total and how your rate per hour of play compares with the server's. Open a known player's profile by name or UUID, including when they're offline.

## Leaderboards and medals

Server leaderboards include known players with saved statistics, including offline players. See the top three and your own position at a glance, then open the full leaderboard for any statistic. Select a ranked player to open their statistics.

The top three ranks with positive values earn gold, silver or bronze medals. Ties share a rank, so a tie for first produces ranks `1, 1, 3`. The server's medal ranking shows each player's total and their breakdown by tier. Select a player there to see the individual statistics behind their medals. Use Back to return to the same ranking page.

## Install and play

1. Put the SkyStats JAR in your Paper or Folia server's `plugins/` folder.
2. Restart the server.
3. Run `/stats` in-game.

SkyStats requires no additional plugins or client mods. The plugin requires Java 17 or later. Use the Java version required by your server build. Menu text is in English, and Minecraft item and spawn egg names follow the player's client language.

| Command | What it opens |
| --- | --- |
| `/stats` | Your statistics and the server leaderboards |
| `/stats <name>` | A known player's profile |
| `/stats <UUID>` | A specific profile when a name is ambiguous |

Both `stats.use` and `stats.others` are granted by default. Server owners can use `stats.others` to control who can open other players' profiles.

## How SkyStats gets its data

SkyStats reads live statistics for online players and saved Vanilla statistics for offline players. It leaves the game's statistics intact. Personal profiles are loaded when opened and can be refreshed from the menu. Server-wide statistics use a cached snapshot with configurable cache and refresh settings in `plugins/SkyStats/config.yml`.
