# TheTycoon ⛏️

<div align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8-62B47A?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.21.8">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Platform-Paper-5865F2?style=for-the-badge" alt="Paper">
  <img src="https://img.shields.io/badge/Developer-TheMurft-F1C40F?style=for-the-badge" alt="Developer: TheMurft">
</div>

---

TheTycoon is a vanilla-friendly mining tycoon for Paper 1.21.8. Players claim plots, build production around a personal miner, sell resources, and progress through upgrades, missions, and prestige—without a resource pack.

## ✨ Features

- ⛏️ **Personal plots and miners:** Claim a plot and place a miner that uses the player's own head.
- 📈 **Progression:** Upgrades, missions, prestige, and lifetime earnings rankings.
- 🧰 **Configurable minions:** A catalog of minion types, including level-15 minions.
- 💤 **Offline production:** Continue earning while away.
- 💰 **Economy integration:** Persistent economy exposed through Vault.
- 🪟 **In-game interface:** Tycoon dashboard and Hub-style scoreboard.
- 🌐 **Languages:** Personal language settings with a global fallback; English, Spanish, French, and Portuguese files are included.
- 🛡️ **Plot protection:** Protect player plots and miner areas, with permission-based bypass.

## 🚀 Quick start

1. Build the plugin with `mvn clean package`.
2. Place `target/TheTycoon-1.1.0.jar` in the server's `plugins/` folder.
3. Install the dependencies below, then start the server.
4. Configure the plugin and generate plots with `/tycoon generate`.

### Requirements

- Paper 1.21.8
- Java 21
- WorldEdit 7.3.19 (Java 21 compatible)
- Vault
- Maven to build from source

## 🎮 Main commands

| Command | Description |
| :--- | :--- |
| `/tycoon` | Open the main dashboard. |
| `/tycoon generate [amount]` | Generate claimable plots at the administrator's location. |
| `/tycoon plots`, `/tycoon tp`, `/tycoon claim`, `/tycoon unclaim` | Find, claim, or release plots. |
| `/tycoon miner` | Place or move the personal miner. |
| `/tycoon sell`, `/tycoon info` | Sell stored resources or view plot and miner details. |
| `/tycoon missions [claim <id>]` | View missions or claim a completed one. |
| `/tycoon prestige [confirm]`, `/tycoon top` | Prestige or view the lifetime earnings leaderboard. |
| `/tycoon money add|set|take <player> <amount>` | Manage a player's balance. |
| `/tycoonlang <language>` | Set your language. Use `reset` to follow the global language. |
| `/tycoonlang global <language>` | Set the global fallback language. |
| `/tycoon reload` | Reload configuration and language files. |

## ⚙️ Configuration

The main settings are in `plugins/TheTycoon/config.yml`. Language files are in `plugins/TheTycoon/lang/`. Player plots, miners, and progression data are stored by the plugin.

Language selection follows this order: the player's choice, the global language, then English.

## 🧰 Build from source

```bash
mvn clean package
```

The plugin JAR is created in `target/`.

## 💬 Support

For questions or help, join the [Discord support server](https://discord.gg/6C5t995jC6).

---

Made with ❤️ by [TheMurft](https://github.com/TheMurft).
