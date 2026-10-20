# TheTycoon 1.0

Paper 1.21.8 mining tycoon plugin without a resource pack. Each player can create a logical plot and place an animated miner wearing their own head.

Includes a configurable minion catalog, level-15 minions, personal/global languages, Vault economy, offline production, missions, prestige, lifetime rankings, a dashboard GUI and a Hub-style scoreboard.

## Requirements

- Paper 1.21.8 and Java 21
- WorldEdit 7.3.19 (Java 21 compatible)
- Vault (TheTycoon provides and persists its own economy)

## Commands

- `/tycoon generate [amount]`: generates 8x8 claimable plots at the administrator's location.
- `/tycoon plots`: teleports to the owner's plot or the next available plot.
- `/tycoon tp`: teleports to the owner's plot or the next available plot.
- `/tycoon claim`: claims the plot the player is standing in.
- `/tycoon miner`: places or moves the miner.
- `/tycoon sell`: sells stored resources through Vault.
- `/tycoon`: opens the main Tycoon dashboard.
- `/tycoon missions`: shows progression missions.
- `/tycoon missions claim <id>`: claims a completed mission.
- `/tycoon prestige [confirm]`: previews or performs a prestige reset.
- `/tycoon top`: shows the lifetime earnings leaderboard.
- `/tycoon info`: shows storage, miner status, and balance.
- `/tycoon unclaim`: releases and resets the plot.
- `/tycoon reload`: reloads configuration and languages.
- `/tycoon money add <player> <amount>`: adds money to a player.
- `/tycoon money set <player> <amount>`: sets a player's balance.
- `/tycoon money take <player> <amount>`: removes money from a player.
- `/tycoonlang <language>`: sets a personal language.
- `/tycoonlang reset`: follows the global language again.
- `/tycoonlang global <language>`: sets the global fallback language.

## Language priority

Personal player language, then global language, then English fallback. Translation files are located in `plugins/TheTycoon/lang/`.

## Build

```text
mvn clean package
```

The final file is generated as `target/TheTycoon-1.0.0.jar`.
