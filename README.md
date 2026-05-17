# KillTokens

KillTokens is a Spigot 1.20 plugin that rewards players with virtual kill tokens, lets them cash tokens out through Vault, and adds a Refined Ore mining reward system with virtual storage.

## Requirements

- Java 17
- Spigot/Paper 1.20.x
- Vault and a Vault-compatible economy plugin
- PlaceholderAPI is optional, but required for placeholders

## Build

```powershell
C:\Users\banip\maven\apache-maven-3.9.6\bin\mvn.cmd package
```

The plugin jar is written to `target/`.

## Commands

| Command | Description |
| --- | --- |
| `/tokens` or `/tokens gui` | Opens the central storage GUI. |
| `/tokens balance` | Shows kill token balance and cashout rate. |
| `/tokens withdraw <amount>` | Withdraws virtual kill tokens into physical token items. |
| `/tokens cashout` | Converts the configured token amount into Vault money. |
| `/tokens set <player> <amount>` | Sets a player's kill token balance. |
| `/tokens add <player> <amount>` | Adds kill tokens to a player. |
| `/tokens remove <player> <amount>` | Removes kill tokens from a player. |
| `/refined` or `/refined balance` | Shows Refined Ore storage, totals, and auto-storage status. |
| `/refined storage` | Toggles automatic storage for mined Refined Ore drops. |
| `/refined store` | Moves physical Refined Ore items from inventory into virtual storage. |
| `/refined withdraw refined <amount>` | Withdraws stored Refined Ore. |
| `/refined withdraw compressed <amount>` | Withdraws stored Compressed Refined Ore. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `killtokens.use` | `true` | Allows basic `/tokens` commands and GUI access. |
| `killtokens.admin` | `op` | Allows admin token set/add/remove commands. |
| `killtokens.refined` | `true` | Allows `/refined` commands. |
| `killtokens.refined.admin` | `op` | Reserved for refined ore admin features. |
| `killtokens.alerts` | `op` | Receives dupe protection flag warnings. |

## Central GUI

`/tokens` opens a central GUI for kill tokens, Refined Ore, and Compressed Refined Ore. Players can view balances, cash out, withdraw items, toggle Refined Ore auto-storage, and store physical refined items.

All GUI mutation actions are guarded by the dupe protection service so only one storage operation can run for a player at a time.

## Refined Ore

Refined Ore rolls on natural ore block breaks. Silk Touch can be excluded with `refined.skip-silk-touch`. Fortune is intentionally ignored so each eligible ore break rolls once.

Default rewards:

- Refined Ore: `BLUE_DYE`, base `1/85`, pity after `100` misses.
- Compressed Refined Ore: `NAUTILUS_SHELL`, base `1/3500`, pity after `5000` misses.

Drop rates, pity thresholds, broadcast behavior, and item materials are configurable in `config.yml`.

## Dupe Protection And Staff Flags

KillTokens protects storage-changing paths with per-player operation locks. If a player triggers overlapping actions, a Vault cashout fails after deduction, or an item withdrawal overflows inventory, the plugin:

- blocks or safely completes the operation,
- logs a `[DupeFlag]` warning to the server log,
- notifies online staff with `killtokens.alerts`,
- exposes flag state through PlaceholderAPI.

Cashout paths deduct first, flush storage, then refund if the Vault deposit fails. Withdraw paths deduct virtual storage first and drop overflow items at the player's location instead of silently losing them.

## PlaceholderAPI

Identifier: `killtokens`

| Placeholder | Value |
| --- | --- |
| `%killtokens_balance%` | Current kill token balance. |
| `%killtokens_kill_reward%` | Tokens awarded per kill. |
| `%killtokens_cashout_cost%` | Tokens required for cashout. |
| `%killtokens_cashout_value%` | Money received per cashout. |
| `%killtokens_tokens_needed%` | Tokens still needed for cashout. |
| `%killtokens_can_cashout%` | `true` if the player can cash out. |
| `%killtokens_cashout_ready%` | Alias for cashout readiness. |
| `%killtokens_refined_balance%` | Stored Refined Ore balance. |
| `%killtokens_compressed_balance%` | Stored Compressed Refined Ore balance. |
| `%killtokens_refined_total%` | Lifetime Refined Ore earned. |
| `%killtokens_compressed_total%` | Lifetime Compressed Refined Ore earned. |
| `%killtokens_refined_pity%` | Current Refined Ore pity counter. |
| `%killtokens_compressed_pity%` | Current Compressed Refined Ore pity counter. |
| `%killtokens_auto_storage%` | `true` if Refined Ore auto-storage is enabled. |
| `%killtokens_total_virtual_items%` | Kill tokens plus stored refined balances. |
| `%killtokens_dupe_flags%` | Player dupe flag count. |
| `%killtokens_last_dupe_flag%` | Last recorded dupe flag summary. |

## Storage Files

Runtime data is stored in the plugin data folder:

- `tokens.yml` for kill token balances.
- `refined_data.yml` for refined balances, lifetime totals, pity counters, and auto-storage toggles.
