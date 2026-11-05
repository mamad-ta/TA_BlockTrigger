# TA_BlockTrigger — Configuration reference

Every option of `config.yml` and `messages.yml`, what it does, and its default.
All values are validated on load: an invalid value is logged as a warning and replaced by the
default below, so a typo can never break the plugin.

After editing a file run **`/bt reload`** (or restart the server).

---

## 1. `config.yml`

### 1.1 `settings`

| Option | Type | Default | Description |
|---|---|---|---|
| `settings.enabled` | boolean | `true` | Master switch. `false` disables block interactions **and** block configuration; `/bt reload` and `/bt help` keep working so you can re-enable it. |
| `settings.max-selection-distance` | integer `1–100` | `10` | How far (in blocks) an administrator may stand from the block being configured or removed. Values outside the range are clamped with a warning. |
| `settings.default-execution-type` | `console` \| `player` | `console` | Execution type used when `/bt` is typed without a `console`/`player` first argument. |
| `settings.default-click-type` | `right` \| `left` \| `both` | `right` | Click type used when the last argument is not `right`/`left`/`both`. |
| `settings.debug` | boolean | `false` | Extra console output (interactions, dispatches, storage, sounds). Use while troubleshooting. |

### 1.2 `permissions`

| Option | Type | Default | Description |
|---|---|---|---|
| `permissions.op-only` | boolean | `true` | Only operators may use `/bt`. Recommended default. |
| `permissions.use-permission-node` | boolean | `false` | Additionally/alternatively require `permissions.permission-node`. |
| `permissions.permission-node` | string | `"tablocktrigger.admin"` | The node required when `use-permission-node` is `true`. Works with LuckPerms. |
| `permissions.ops-bypass` | boolean | `true` | Operators always pass, even without the node. |

If **both** switches are `false`, the sender still needs `permissions.permission-node`, so
`/bt` can never become public by accident (a warning is logged).

Console and command blocks are always allowed to manage block commands.

### 1.3 `worlds`

| Option | Type | Default | Description |
|---|---|---|---|
| `worlds.mode` | `all` \| `whitelist` \| `blacklist` | `all` | Where block commands are active. |
| `worlds.list` | list of strings | `[]` | World names (case-insensitive) for whitelist/blacklist mode. |
| `worlds.run-in-filtered-worlds` | boolean | `false` | `true` → configured blocks still fire in worlds excluded by the filter (the filter then only affects configuration); `false` → filtered worlds cannot execute commands and players get `execution.world-disabled`. |

### 1.4 `interactions`

| Option | Type | Default | Description |
|---|---|---|---|
| `interactions.right-click` | boolean | `true` | Allow right-click activated commands (click modes `right` and `both`). |
| `interactions.left-click` | boolean | `true` | Allow left-click activated commands (click modes `left` and `both`). |
| `interactions.both` | boolean | `true` | Allow blocks that use the `both` click mode at all. |
| `interactions.prevent-block-breaking` | boolean | `true` | Configured blocks cannot be broken by players (interaction cancel + `BlockDamageEvent`/`BlockBreakEvent` fallback). |
| `interactions.prevent-offhand-duplicates` | boolean | `true` | Ignore the off-hand duplicate of a right click (prevents double execution). |
| `interactions.prevent-duplicate-interactions` | boolean | `true` | Ignore a second interaction with the same block in the same server tick. |
| `interactions.allow-normal-interaction` | boolean | `true` | `true` → players can still open chests/press buttons on configured blocks; `false` → the normal use is cancelled, only the command runs. |
| `interactions.run-when-event-cancelled` | boolean | `false` | `true` → execute commands even if another plugin cancelled the interaction. |

### 1.5 `execution`

| Option | Type | Default | Description |
|---|---|---|---|
| `execution.console` | boolean | `true` | Allow the `console` execution type (configuring and running). |
| `execution.player` | boolean | `true` | Allow the `player` execution type. |
| `execution.accept-leading-slash` | boolean | `true` | Accept `/bt player /spawn right`. When `false`, a leading slash is rejected with `set.leading-slash`. The slash is always stripped before dispatching. |
| `execution.use-placeholders` | boolean | `true` | Process placeholders in commands. Placeholders are applied only when **this and** `placeholders.enabled` are `true`. |
| `execution.log-commands` | boolean | `false` | Log every executed block command to the server console. |
| `execution.show-failed-messages` | boolean | `true` | Tell the player when a command could not be executed (`execution.failed`). |
| `execution.require-permission` | boolean | `false` | Only players with `execution.permission-node` may trigger block commands. |
| `execution.permission-node` | string | `"tablocktrigger.use"` | Node used when the option above is enabled. |

### 1.6 `cooldown`

| Option | Type | Default | Description |
|---|---|---|---|
| `cooldown.enabled` | boolean | `false` | Enable click cooldowns. |
| `cooldown.seconds` | decimal `0–86400` | `1.0` | Cooldown duration in seconds (decimals allowed, e.g. `0.5`). |
| `cooldown.type` | `player` \| `block` | `player` | `player` → one cooldown per player; `block` → one cooldown per block, shared by everyone. |
| `cooldown.separate-click-types` | boolean | `true` | `true` → left and right clicks have independent cooldowns; `false` → one shared cooldown. |

The cooldown starts only after a successful execution. Cooldown message (`cooldown.active`,
supports `{seconds}`, `{player}`) and cooldown sound are configurable.

### 1.7 `sounds`

| Option | Type | Default | Description |
|---|---|---|---|
| `sounds.enabled` | boolean | `true` | Master switch for every sound. |
| `sounds.<slot>.enabled` | boolean | see below | Enable this sound. |
| `sounds.<slot>.sound` | string | see below | Sound name (three notations accepted, see below). |
| `sounds.<slot>.volume` | decimal `0–10` | `1.0` | Playback volume. |
| `sounds.<slot>.pitch` | decimal `0.1–2` | `1.0` | Playback pitch. |

Slots and their defaults:

| Slot | Default state | Default sound | Played when |
|---|---|---|---|
| `set-command` | enabled | `ENTITY_EXPERIENCE_ORB_PICKUP` | a command is assigned to a block |
| `remove-command` | enabled | `BLOCK_NOTE_BLOCK_BELL` | a command is removed |
| `invalid-command` | enabled | `ENTITY_VILLAGER_NO` | invalid syntax, no targeted block, nothing to remove |
| `no-permission` | enabled | `BLOCK_ANVIL_LAND` (0.6 / 0.8) | a player without permission tries to manage commands, or may not trigger a block |
| `click` | **disabled** | `UI_BUTTON_CLICK` | a configured block successfully executed its command |
| `reload` | enabled | `BLOCK_NOTE_BLOCK_PLING` (pitch 1.5) | `/bt reload` succeeded |
| `cooldown` | **disabled** | `BLOCK_NOTE_BLOCK_BASS` | a click was blocked by the cooldown |

Accepted sound notations:

```yaml
sound: "ENTITY_EXPERIENCE_ORB_PICKUP"        # Bukkit constant
sound: "entity.experience_orb.pickup"        # Minecraft key (underscores preserved)
sound: "minecraft:block.note_block.bell"     # namespaced key
```

Invalid names are reported in the console (`Unknown sound '…' for '<slot>'`) and that single
sound is disabled - the plugin keeps running.

### 1.8 `placeholders`

| Option | Type | Default | Description |
|---|---|---|---|
| `placeholders.enabled` | boolean | `true` | Master switch for all placeholders. |
| `placeholders.player` | boolean | `true` | `{player}` → player name. |
| `placeholders.uuid` | boolean | `true` | `{uuid}` → player UUID. |
| `placeholders.world` | boolean | `true` | `{world}` → world the player is in. |
| `placeholders.coordinates` | boolean | `true` | `{x}`, `{y}`, `{z}` → the player's block coordinates. |

Custom placeholders from other plugins are **not** resolved; this plugin only replaces its own
built-in placeholders (that keeps commands deterministic and avoids dependency issues).

### 1.9 `storage`

| Option | Type | Default | Description |
|---|---|---|---|
| `storage.file` | string ending in `.yml` | `"blocks.yml"` | Name of the data file inside `plugins/TA_BlockTrigger/`. |
| `storage.auto-save.enabled` | boolean | `true` | Periodically rewrite the data file as a safety net (changes are saved immediately anyway). |
| `storage.auto-save.interval-minutes` | integer `1–1440` | `5` | Interval of the safety-net save. |
| `storage.reload-on-plugin-reload` | boolean | `true` | `true` → `/bt reload` re-reads the data file; `false` → the in-memory commands are kept and written back. |
| `storage.validate-on-load` | boolean | `true` | Validate entries while loading and log every invalid entry. |
| `storage.backup-on-save` | boolean | `false` | Keep a `blocks.yml.bak` copy of the previous file on every save. |
| `storage.debug-logging` | boolean | `false` | Log storage read/write operations. |

Data integrity is never traded for an option: writes are always atomic (temp file + atomic move)
and every change is persisted immediately.

---

## 2. `messages.yml`

Formatting rules:

* `&0`–`&f`, `&k`–`&o`, `&r` — classic colour/format codes.
* `&#RRGGBB` — hex colours (translated internally, works on every Paper version).
* `{prefix}` — replaced with the message prefix.
* Set a message to `''`, `-` or `disabled` to hide it entirely.
* Missing keys fall back to the copy inside the jar, so a partial `messages.yml` is safe.

| Key | Placeholders | Sent when |
|---|---|---|
| `prefix` | — | prepended wherever `{prefix}` is used |
| `general.no-permission` | — | a sender without permission runs `/bt` |
| `general.plugin-disabled` | — | `settings.enabled: false` |
| `general.player-only` | — | a non-player sender uses a block-targeting subcommand |
| `general.usage` | `{label}` | `/bt` without arguments |
| `general.block-protected` | — | a player tried to break a configured block |
| `general.error` | `{error}` | unexpected internal error (also logged) |
| `select.no-block` | `{distance}` | no block targeted within the selection distance |
| `set.success` | `{world}`, `{x}`, `{y}`, `{z}`, `{block}`, `{execution}`, `{click}`, `{command}`, `{replaced}` | command assigned |
| `set.invalid-command` | — | command part missing/empty |
| `set.leading-slash` | — | leading slash used while `accept-leading-slash: false` |
| `set.execution-disabled` | `{type}` | execution type disabled in config |
| `set.click-disabled` | `{click}` | click type disabled in config |
| `remove.success` | `{world}`, `{x}`, `{y}`, `{z}`, `{block}`, `{execution}`, `{click}`, `{command}` | command removed |
| `remove.no-command` | `{world}`, `{x}`, `{y}`, `{z}` | targeted block has no command |
| `reload.success` | `{time}`, `{blocks}` | reload finished |
| `reload.failed` | `{error}` | reload failed |
| `cooldown.active` | `{seconds}`, `{player}` | click blocked by cooldown |
| `execution.no-permission` | — | player may not trigger block commands |
| `execution.failed` | `{command}` | command could not be executed |
| `execution.type-disabled` | `{type}` | execution type disabled at runtime |
| `execution.world-disabled` | `{world}` | clicking a configured block in a filtered world |
| `help.lines` | `{distance}`, `{default-execution}`, `{default-click}`, `{placeholders}` | `/bt help` (list, one line per entry) |

---

## 3. `blocks.yml`

Managed by the plugin; edit it only while the server is stopped.

```yaml
blocks:
  - world: world                        # world name (informational)
    world-uuid: aafa890c-...            # primary identifier
    x: 12
    y: -60
    z: 7
    execution: CONSOLE                  # CONSOLE or PLAYER
    click: BOTH                         # RIGHT, LEFT or BOTH
    command: say Hello World            # always without a leading slash
    created-by: MamaD_TA                # optional metadata
    created-at: 1793892693573           # optional metadata (epoch millis)
```

Notes:

* A block is uniquely identified by world UUID + coordinates.
* Entries of worlds that are not loaded are preserved and rebound automatically when the world
  loads (so Multiverse-style delayed worlds keep working).
* Invalid entries are skipped with a warning instead of breaking the whole file.
