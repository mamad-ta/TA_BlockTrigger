# TA_BlockTrigger — Administrator & user guide

A practical guide for everyone who runs this plugin on a server: what it does, how the pieces
fit together, how to set it up safely, and what to expect from each interaction.
For the option-by-option reference see **[CONFIGURATION.md](CONFIGURATION.md)**; for a quick
overview see the **[README](../README.md)**.

---

## Table of contents

1. [What the plugin does](#1-what-the-plugin-does)
2. [Installation](#2-installation)
3. [First run walkthrough](#3-first-run-walkthrough)
4. [The `/bt` command](#4-the-bt-command)
5. [Click modes and interaction rules](#5-click-modes-and-interaction-rules)
6. [Execution types](#6-execution-types)
7. [Placeholders](#7-placeholders)
8. [Permissions and access model](#8-permissions-and-access-model)
9. [Cooldowns](#9-cooldowns)
10. [World filter](#10-world-filter)
11. [Block protection](#11-block-protection)
12. [Messages and sounds](#12-messages-and-sounds)
13. [Storage, reloads and shutdown](#13-storage-reloads-and-shutdown)
14. [Multi-version compatibility](#14-multi-version-compatibility)
15. [Project structure](#15-project-structure)
16. [Verification checklist](#16-verification-checklist)
17. [Troubleshooting](#17-troubleshooting)

---

## 1. What the plugin does

TA_BlockTrigger binds a **command** to one **exact block**. When a player interacts with that
block, the command runs — either as the console or as the clicking player.

- A block is identified by **world UUID + X/Y/Z**, so two identical stone blocks in the same
  world can hold completely different commands.
- Blocks without a binding are never touched: normal gameplay (opening chests, pressing
  buttons, breaking blocks) is unaffected.
- Bindings survive restarts, world reloads and plugin reloads.

Everything is managed with one command, `/bt`, and configured in three YAML files inside
`plugins/TA_BlockTrigger/`: `config.yml`, `messages.yml` and `blocks.yml`.

---

## 2. Installation

1. Put `TA_BlockTrigger-1.1.0.jar` into your server's `plugins/` folder.
2. Start (or restart) the server. The plugin creates:
   ```
   plugins/TA_BlockTrigger/
   ├── config.yml     behaviour, permissions, cooldowns, sounds, storage
   ├── messages.yml   every message the plugin sends
   └── blocks.yml     your block bindings (written by the plugin)
   ```
3. Check the console for `TA_BlockTrigger v1.1.0 enabled` (and for any warnings about invalid
   config values — the plugin replaces them with the defaults instead of failing).

**Requirements:** Paper, Spigot or CraftBukkit, Minecraft 1.20.1 or newer, Java 17 or newer
(Java 21 on 1.20.5+ / 1.21.x servers). No other plugins or libraries.

---

## 3. First run walkthrough

```
1. Stand in front of the block you want to use and look at it (max. 10 blocks away by default).
2. Type:  /bt console say &eWelcome to the shop!
3. Right-click the block.
```

The console prints `Welcome to the shop!` — and keeps doing so every time anyone right-clicks
that block. The binding is saved immediately, so it also works after a restart.

To remove it, look at the same block and run `/bt remove`.

A few more examples:

| What you type | What happens when the block is clicked |
|---|---|
| `/bt console eco give {player} 100 both` | left **or** right click → the clicker receives 100 |
| `/bt player spawn right` | right click → the clicker runs `/spawn` |
| `/bt player warp pvp left` | left click → the clicker runs `/warp pvp` |
| `/bt say Hello World` | uses the default execution type (`console`) and the default click (`right`) |
| `/bt remove` | deletes the binding of the targeted block |
| `/bt reload` | reloads `config.yml`, `messages.yml` and the block data |

---

## 4. The `/bt` command

```
/bt <console|player> <command ...> [right|left|both]
/bt <command ...> [right|left|both]        (uses settings.default-execution-type)
/bt remove
/bt reload
/bt help
```

**Aliases:** `ctb`, `tbt`, `blocktrigger`, `tablocktrigger`, `cmdtoblock`

### How the arguments are parsed

The parser only looks at the **two edges** of the input; everything in the middle is kept
verbatim, spaces included.

| Position | Checked against | Fallback when it does not match |
|---|---|---|
| first argument | exactly `console` / `player` (case-insensitive) | `settings.default-execution-type` |
| last argument | exactly `right` / `left` / `both` (case-insensitive) | `settings.default-click-type` |
| everything between | the command itself | — |

Because of that, `say right` is a valid command *and* a valid click keyword depending on
position:

```
/bt console say right          → command "say right", click = default
/bt console say right left     → command "say right", click = left
```

A **leading `/`** is accepted while `execution.accept-leading-slash: true` and always stripped
before the command runs. Block commands are always stored without the slash.

**Tab completion:** the first argument completes to `console`, `player`, `remove`, `reload`,
`help`; while defining a command, the last argument completes to `right`, `left`, `both`.

### Validation

Before a binding is created the plugin checks, in this order:

1. the sender may manage block commands ([permissions](#8-permissions-and-access-model));
2. `settings.enabled` is `true`;
3. the sender is a player, and is looking at a block within
   `settings.max-selection-distance`;
4. the command part is not empty;
5. the chosen execution type is enabled (`execution.console` / `execution.player`);
6. the chosen click type is enabled (`interactions.right-click` / `left-click` / `both`).

A failure reports the matching message from `messages.yml` and plays the `invalid-command`
sound; nothing is written to `blocks.yml`.

---

## 5. Click modes and interaction rules

Each binding stores one click mode:

| Mode | Triggers on |
|---|---|
| `right` | right click |
| `left` | left click (players get no block damage — see [protection](#11-block-protection)) |
| `both` | left **or** right click |

Two layers decide whether a click actually fires:

1. **The binding's mode** — a `right` binding ignores left clicks.
2. **The global switches** in `interactions` — `right-click`, `left-click`, `both` can disable
   whole categories server-wide without editing every binding.

Additional guards, all configurable:

| Setting | Effect when `true` |
|---|---|
| `prevent-offhand-duplicates` | the off-hand copy of a right click is ignored (no double execution) |
| `prevent-duplicate-interactions` | a second interaction with the same block in the same server tick is ignored |
| `allow-normal-interaction` | `false` cancels the vanilla use of the block (chest, button, anvil…) so only the command runs |
| `run-when-event-cancelled` | `false` (default) skips commands on interactions another plugin already cancelled |

Commands are dispatched **on the main thread**, after the event, so they behave exactly like
commands typed in chat or the console. A `player`-type command runs with the player's own
permissions and luck; a `console`-type command bypasses player permissions entirely.

---

## 6. Execution types

| Type | Runs as | Typical use |
|---|---|---|
| `console` | the server console | giving items/money, `eco give`, `lp` commands, anything a player must not be able to fake |
| `player` | the clicking player | `/spawn`, `/warp`, `/kit`, menu commands that need the player's own context |

Each type can be disabled server-wide (`execution.console`, `execution.player`). A binding of a
disabled type stays stored but refuses to run with the `execution.type-disabled` message — useful
for temporarily freezing a category without losing bindings.

> **Security note:** a `console` binding runs *any* command as the server. Only bind commands you
> would be comfortable letting every player trigger, and keep `/bt` OP-only
> (`permissions.op-only: true`). Placeholders are attacker-influenced only through the player's
> own name/world/coordinates, never through free text.

---

## 7. Placeholders

Placeholders are replaced **inside the stored command** at execution time, using the clicking
player's context:

| Placeholder | Value |
|---|---|
| `{player}` | player name |
| `{uuid}` | player UUID |
| `{world}` | name of the world the player is in |
| `{x}` `{y}` `{z}` | the player's block coordinates |

They are applied only when **both** `placeholders.enabled` and `execution.use-placeholders` are
`true`, and each placeholder also has its own switch (`placeholders.player`, `uuid`, `world`,
`coordinates`). A disabled placeholder is left in the command as literal text.

Example:

```
/bt console eco give {player} 100 both
/bt console say &e{player} &7used the shop at &b{world} {x} {y} {z}
```

---

## 8. Permissions and access model

| Node | Default | Purpose |
|---|---|---|
| `tablocktrigger.admin` | `op` | use `/bt` when the permission node is required |
| `tablocktrigger.use` | `true` | trigger block commands when `execution.require-permission: true` |

Two independent layers:

**Management (`/bt`)** — `permissions.op-only` and `permissions.use-permission-node` decide who
may create/remove/reload bindings. `ops-bypass: true` lets operators through even without the
node. If both switches are `false`, the permission node is still required (and a warning is
logged), so `/bt` can never become public by accident. Console and command blocks are always
allowed to manage bindings.

**Triggering** — clicking configured blocks works for everyone by default. Set
`execution.require-permission: true` to require `execution.permission-node`
(`tablocktrigger.use` by default) before a player may trigger any block command.

Recommended LuckPerms setup for a staff-only management node:

```yaml
permissions:
  op-only: false
  use-permission-node: true
  permission-node: "tablocktrigger.admin"
  ops-bypass: true
```

---

## 9. Cooldowns

Disabled by default. When enabled (`cooldown.enabled: true`):

| Option | Meaning |
|---|---|
| `cooldown.seconds` | duration in seconds, decimals allowed (`0.5`) |
| `cooldown.type` | `player` → one timer per player; `block` → one timer per block, shared by everyone |
| `cooldown.separate-click-types` | `true` → left and right clicks use independent timers |

A blocked click sends `cooldown.active` with the remaining seconds (and the `cooldown` sound, if
enabled). Cooldown state is in memory only and is cleared on shutdown/reload.

---

## 10. World filter

`worlds.mode` is `all`, `whitelist` (only the listed worlds) or `blacklist` (every world except
the listed ones); `worlds.list` holds case-insensitive world names.

- `worlds.run-in-filtered-worlds: false` → blocked worlds cannot **execute** bindings; players
  get `execution.world-disabled`.
- `worlds.run-in-filtered-worlds: true` → execution is allowed everywhere; the filter only
  affects where administrators may configure blocks.

The filter is checked on every click, so a world rename or a late-loading world cannot silently
enable commands in the wrong place.

---

## 11. Block protection

With `interactions.prevent-block-breaking: true` (default) a configured block cannot be
destroyed by players:

- left-click activation cancels the interaction (the client never starts breaking);
- `BlockDamageEvent` and `BlockBreakEvent` are additionally cancelled as a fallback,
  covering creative/instant-break and plugins that bypass the interaction event.

The player sees `general.block-protected` when a break attempt is stopped. Operators can still
remove the binding with `/bt remove` and then break the block normally. Removing a world or
deleting `blocks.yml` also frees the blocks.

---

## 12. Messages and sounds

`messages.yml` holds every text the plugin sends:

- colour codes `&a`, `&l`, `&o` … and hex colours as `&#RRGGBB`;
- `{prefix}` inserts the configured `prefix` at that position;
- set a message to `''`, `-` or `disabled` to hide it completely;
- a missing key falls back to the bundled default, so editing one message can never break the
  others.

Message placeholders (documented above each entry) include `{world}`, `{x}`, `{y}`, `{z}`,
`{block}`, `{command}`, `{execution}`, `{click}`, `{seconds}`, `{player}`, `{time}`, `{blocks}`,
`{error}`, `{type}`, `{distance}`.

`sounds` has seven independent slots — `set-command`, `remove-command`, `invalid-command`,
`no-permission`, `click`, `reload`, `cooldown` — each with `enabled`, `sound`, `volume`, `pitch`.
Sound names accept three notations:

```yaml
sound: "ENTITY_EXPERIENCE_ORB_PICKUP"       # Bukkit constant
sound: "entity.experience_orb.pickup"       # Minecraft key
sound: "minecraft:block.note_block.bell"    # namespaced key
```

Invalid names are reported in the console once per reload and skipped — they never throw at
runtime. Sounds are resolved through key-based Adventure sounds, so they keep working on Paper
versions where `org.bukkit.Sound` is no longer an enum.

---

## 13. Storage, reloads and shutdown

`blocks.yml` is the only stateful file:

```yaml
blocks:
  - world: world
    world-uuid: aafa890c-647e-4342-9ebd-8bb1441fa97d
    x: 12
    y: -60
    z: 7
    execution: CONSOLE
    click: BOTH
    command: say Hello World
    created-by: MamaD_TA
    created-at: 1793892693573
```

- **Immediate, atomic writes** — every change schedules a save; the file is written to a temp
  file and atomically moved into place, so a crash cannot leave a half-written file.
- **Coalesced** — all I/O happens on one background thread; changes made while a write is in
  progress are merged into the next write instead of queueing hundreds of them.
- **`auto-save`** (`storage.auto-save`) periodically rewrites the file as a safety net; it is
  never the thing that makes a change durable.
- **`backup-on-save: true`** keeps `blocks.yml.bak` from the previous save.
- **Validation on load** — invalid entries (bad world UUID, non-numeric coordinates, unknown
  execution/click type) are skipped with a warning instead of corrupting the rest.
- **World-UUID based** — bindings of worlds that are not loaded at startup are kept and
  re-bound when the world loads (`WorldLoadEvent`), which makes the plugin Multiverse-friendly.
- **Shutdown flush** — pending changes are written synchronously in `onDisable()`.

`/bt reload` re-reads `config.yml`, `messages.yml` and (with `storage.reload-on-plugin-reload:
true`) `blocks.yml`, re-applies every cached value and keeps all bindings. The success message
reports the elapsed time and the number of active bindings.

---

## 14. Multi-version compatibility

- Compiled against **`spigot-api:1.20.1-R0.1-SNAPSHOT`**, target bytecode Java 17,
  `api-version: '1.20'`. Building against the Spigot API (the common denominator of the
  Bukkit family) is what allows a single jar to run on Paper, Spigot and CraftBukkit.
- **`api-version` is the minimum server version.** `'1.20'` loads on every server from
  1.20.1 upwards, including the 1.21.x line.
- **No Paper-only API is used.** `getPluginMeta()` (Paper 1.20.4+) is replaced by
  `getDescription()`, `Bukkit.getCurrentTick()` by a plugin-side tick counter, and Adventure
  by `ChatColor` plus `Player#playSound(Location, String, float, float)`.
- **No NMS**, no `CraftBukkit`, no version-specific classes — only the stable Bukkit/Paper API.
- Only long-stable events are used: `PlayerInteractEvent`, `BlockDamageEvent`,
  `BlockBreakEvent`, `WorldLoadEvent`.
- No enum constants are compiled in: sound names are resolved at runtime via reflection on
  `org.bukkit.Sound`, so the plugin survives the `Sound`-enum → `Sound`-interface change.
- Built with `-Xlint:deprecation`; the code deliberately avoids deprecated calls.
- To build for a different server version, change the `spigot.version` property in `pom.xml` and
  rebuild — no source changes are required.

### Supported server versions

| Server | Status |
|---|---|
| Paper 1.20.1 | ✅ reference version |
| Spigot 1.20.1 | ✅ supported — same jar |
| CraftBukkit 1.20.1 | ✅ supported — same jar |
| Paper/Spigot 1.20.2 – 1.20.4 | ✅ supported |
| Paper/Spigot 1.20.5 – 1.20.6 | ✅ supported (Java 21 on the server) |
| Paper/Spigot 1.21.x | ✅ supported (Java 21 on the server) |
| older than 1.20.1 | ❌ refused at load (`api-version: '1.20'`) |
| Folia | ❌ not supported (commands dispatch on the main thread) |

---

## 15. Project structure

```
TA_BlockTrigger/
├── pom.xml                     Maven build (spigot-api provided, Java 17 release)
├── docs/
│   ├── GUIDE.md                this guide
│   └── CONFIGURATION.md        every config option
└── src/main/
    ├── java/com/mamadta/tablocktrigger/
    │   ├── TABlockTrigger.java              plugin lifecycle, wiring, reload, tasks
    │   ├── command/    BtCommand            /bt parser and validation
    │   │               BtTabCompleter       tab completion
    │   ├── config/     ConfigManager        typed, cached access to config.yml
    │   │               MessageManager       messages.yml, prefix, colour/hex handling
    │   │               SoundManager         sound slots, name resolution, playback
    │   ├── execution/  CommandDispatcher    permissions, cooldowns, placeholders, dispatch
    │   ├── listener/   BlockInteractionListener   clicks, duplicate guards, protection
    │   │               WorldListener              rebinds late-loading worlds
    │   ├── manager/    BlockCommandManager  O(1) index of bindings, world repair
    │   │               CooldownManager      per-player/per-block timers, duplicate guard
    │   │               PlaceholderManager   {player} {uuid} {world} {x} {y} {z}
    │   ├── storage/    BlockStorage         atomic, coalesced blocks.yml I/O
    │   ├── model/      BlockCommand, BlockKey, ExecutionType, ClickMode
    │   └── util/       TextUtil             colour codes and hex colours
    └── resources/
        ├── plugin.yml
        ├── config.yml
        ├── messages.yml
        └── blocks.yml
```

---

## 16. Verification checklist

Verified by hand on **Paper 1.20.1 (build 196)** with a real client:

- [x] assign a binding with an explicit execution type and click mode
- [x] assign with defaults (no `console`/`player`, no click keyword)
- [x] command containing spaces, and a command whose last word is `right`/`left`
- [x] `right`, `left` and `both` activation, `interactions.*` switches respected
- [x] `console` and `player` execution
- [x] all six placeholders
- [x] `/bt remove` on a configured and on an unconfigured block
- [x] cooldowns per player and per block, separate click types
- [x] world whitelist/blacklist, both `run-in-filtered-worlds` values
- [x] block protection (survival left click, creative instant break)
- [x] non-OP player clicking a configured block (allowed) vs. running `/bt` (denied)
- [x] `/bt reload` with edited `config.yml`, `messages.yml`, `blocks.yml`
- [x] restart: bindings persist; a binding of a missing world rebinds when the world loads

The Maven build also runs in CI (`.github/workflows/build.yml`, Java 17) on every push and PR.

---

## 17. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `/bt` replies “no permission” | You are not OP while `permissions.op-only: true`. Use `/op <name>`, or enable the permission node setup from [§8](#8-permissions-and-access-model). |
| “You are not looking at a block within N blocks” | Look directly at the block face; increase `settings.max-selection-distance` (max 100). |
| Clicking does nothing | Check the binding's click mode, the `interactions.*` switches, the world filter, `settings.enabled` and `cooldown`. Set `settings.debug: true` to see each decision in the console. |
| “The execution type … is disabled” | `execution.console` / `execution.player` is `false`. |
| Player commands fail though the binding looks fine | A `player`-type command runs with the player's permissions — that player may not be allowed to run it. `execution.show-failed-messages` tells them. |
| No sounds | Check `sounds.enabled` and the slot's `enabled`; look for “Unknown sound” warnings after `/bt reload`. |
| A configured block can still be broken | `interactions.prevent-block-breaking` must be `true`; other plugins may also be cancelling your protection events. |
| Config edits are ignored | Run `/bt reload` or restart. If a value was invalid, the console logged the replacement with the default. |
| Bindings disappeared after moving a world | The world UUID changed. Bindings key on world UUID; moving/regenerating a world changes it. Re-create the bindings or restore the old world folder. |
