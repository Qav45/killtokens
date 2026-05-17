# 🗡️ KillTokens

> **Spigot plugin · Minecraft 1.20+**  
> Award players a token for every player-kill, then let them cash out for real in-game money — or carry tokens in their inventory as physical gold nugget items.

---

## ✨ Features

| Feature | Detail |
|---|---|
| 🏆 Kill rewards | Every player-kill automatically awards configurable tokens |
| 💰 Cash out | Exchange tokens for economy currency (Vault) with one command |
| 🪙 Physical tokens | Withdraw tokens as gold nugget items that can be held & traded |
| 📊 Balance UI | Styled in-chat balance card shows tokens & exchange rate |
| 🔧 Admin tools | Set, add, or remove any player's token balance |
| ⌨️ Tab-complete | All subcommands and player names tab-complete |
| 💾 YAML storage | Per-player token data persisted in a YAML file |

---

## 📋 Requirements

- **Spigot / Paper** 1.20.1 or later  
- **Java 17+**  
- **[Vault](https://www.spigotmc.org/resources/vault.34315/)** *(optional — required only for the `/tokens cashout` command)*  
- Any Vault-compatible economy plugin (e.g., EssentialsX Economy, CMI)

---

## 🚀 Installation

1. Download or build the `killtokens-1.0.0.jar` (see [Building from Source](#-building-from-source)).
2. Drop the JAR into your server's `plugins/` folder.
3. *(Optional)* Install **Vault** and an economy plugin.
4. Restart your server.
5. Edit `plugins/KillTokens/config.yml` to your liking (see [Configuration](#️-configuration)).
6. Reload with `/reload confirm` or restart again.

---

## 🎮 How to Use

### Player Flow

```
1. Kill another player in PvP
      ↓
2. Receive a chat notification: "+1 Kill Token awarded!"
      ↓
3. Check your balance:     /tokens balance
      ↓
4a. Cash out for money:    /tokens cashout     (requires Vault)
4b. Or take physical item: /tokens withdraw <amount>
```

### Admin Flow

```
/tokens set <player> <amount>     — Set a player's tokens to an exact value
/tokens add <player> <amount>     — Add tokens to a player
/tokens remove <player> <amount>  — Remove tokens from a player
```

---

## 📟 Commands

All commands are under `/tokens`.

| Command | Permission | Description |
|---|---|---|
| `/tokens balance` | `killtokens.use` | View your current token balance and the cashout rate |
| `/tokens withdraw <amount>` | `killtokens.use` | Take up to 64 tokens as physical gold nugget items |
| `/tokens cashout` | `killtokens.use` | Exchange tokens for economy money (requires Vault) |
| `/tokens set <player> <amount>` | `killtokens.admin` | Set a player's token balance |
| `/tokens add <player> <amount>` | `killtokens.admin` | Add tokens to a player |
| `/tokens remove <player> <amount>` | `killtokens.admin` | Remove tokens from a player |

> **Note:** `/tokens withdraw` is limited to **64 tokens per transaction** (one inventory stack).

---

## 🖥️ GUI / Chat UI Preview

![KillTokens UI Preview](docs/gui_preview.png)

All output is in-chat — there are no chest/inventory GUIs.

### Kill Notification *(appears immediately after a kill)*
```
+1 Kill Token awarded!
Balance: 5 tokens | Use /tokens balance for more info.
```

### `/tokens balance` Card
```
====================
  KillTokens Balance
====================
  Kill Tokens: 5
  Cash Out: 10 tokens → $100.00
====================
```

### `/tokens withdraw` Item
When withdrawn, tokens become a **Gold Nugget** in your inventory with a custom tooltip:
- **Name:** `Kill Token` (gold color)
- **Lore 1:** `Earned through honorable combat`
- **Lore 2:** `Use /tokens cashout to exchange for money`

> If your inventory is full, excess tokens are dropped at your feet.

### `/tokens cashout` Confirmation
```
Cashed out 10 tokens for $100.00!
```

---

## ⚙️ Configuration

**`plugins/KillTokens/config.yml`**

```yaml
# Tokens awarded per player kill
kill-reward: 1

# Tokens required to cash out (one transaction)
cash-tokens: 10

# Money deposited per cash-out transaction
cash-amount: 100.0

# Display name of the physical token item (supports & color codes)
token-item-name: "&6Kill Token"
```

| Key | Default | Description |
|---|---|---|
| `kill-reward` | `1` | Tokens given per PvP kill |
| `cash-tokens` | `10` | Tokens consumed per `/tokens cashout` |
| `cash-amount` | `100.0` | Money deposited per cashout |
| `token-item-name` | `&6Kill Token` | Display name of withdrawn gold nugget |

---

## 🔑 Permissions

| Node | Default | Description |
|---|---|---|
| `killtokens.use` | **true** (all players) | Access to `balance`, `withdraw`, `cashout` |
| `killtokens.admin` | **op** | Access to `set`, `add`, `remove` |

---

## 🏗️ Building from Source

Requires **Maven** and **Java 17**.

```bash
git clone https://github.com/Qav45/killtokens.git
cd killtokens
mvn clean package
```

The compiled JAR will be at `target/killtokens-1.0.0.jar`.

### Dependencies (resolved automatically by Maven)

| Dependency | Scope |
|---|---|
| `spigot-api 1.20.1` | provided |
| `VaultAPI 1.7` | provided |
| `junit 4.13.2` | test |

---

## 📁 Project Structure

```
killtokens/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/killtokens/
    │   │   ├── KillTokensPlugin.java          # Plugin entry point, Vault hook
    │   │   ├── commands/
    │   │   │   └── TokensCommand.java          # /tokens handler + tab-complete
    │   │   ├── listeners/
    │   │   │   └── PlayerDeathListener.java    # Awards tokens on PvP kill
    │   │   ├── storage/
    │   │   │   ├── TokenStorage.java           # Interface
    │   │   │   └── YamlTokenStorage.java       # YAML-backed implementation
    │   │   └── util/
    │   │       └── MessageUtil.java            # & → § color code translator
    │   └── resources/
    │       ├── plugin.yml
    │       └── config.yml
    └── test/
        └── java/com/example/killtokens/storage/
            └── YamlTokenStorageTest.java
```

---

## 📜 License

No license file is included. All rights reserved by [Qav45](https://github.com/Qav45).  
Contact the author before redistributing or modifying.
