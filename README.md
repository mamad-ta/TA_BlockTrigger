<div align="center">

# TA_BlockTrigger

**Bind any command to any block. One click, and it runs.**

A lightweight, fully configurable Paper plugin that lets administrators attach commands
to blocks — executed as the console or as the clicking player.

[![Paper](https://img.shields.io/badge/Paper-1.20.1%2B-3b82f6?style=for-the-badge)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%2B-f97316?style=for-the-badge)](https://adoptium.net)
[![Version](https://img.shields.io/badge/version-1.0.0-22c55e?style=for-the-badge)](#)
[![No Dependencies](https://img.shields.io/badge/dependencies-none-8b5cf6?style=for-the-badge)](#)

</div>

---

## 📖 Table of contents

- [Features](#-features)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Building from source](#-building-from-source)
- [Quick start](#-quick-start)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Placeholders](#-placeholders)
- [Configuration](#-configuration)
- [Data storage](#-data-storage)
- [How it works](#-how-it-works)
- [Compatibility](#-compatibility)
- [Project structure](#-project-structure)
- [Troubleshooting](#-troubleshooting)
- [Documentation](#-documentation)

---

## ✨ Features

- 🎯 **Exact block binding** — the block you are looking at (ray-traced, configurable distance)
  receives the command. Every block is identified by world UUID + `X`/`Y`/`Z`, so different
  blocks can hold completely different commands.
- 🖥️ **Two execution types** — `console` (run as the server) or `player` (run as the clicker).
- 🖱️ **Three click modes** — `right`, `left`, `both`.
- 🧩 **Smart argument parser** — first argument = execution type, last argument = click type,
  everything in between is kept verbatim (spaces included). Both edges are optional and fall
  back to configurable defaults.
- 🔤 **Built-in placeholders** — `{player}`, `{uuid}`, `{world}`, `{x}`, `{y}`, `{z}`,
  each individually switchable.
- 🗑️ **Easy removal** — `/bt remove` deletes only the targeted block's binding.
- 🔐 **Secure by default** — management is **OP-only**; the permission model
  (OP / permission node / both) is configurable. Clicking configured blocks works for everyone.
- ⏱️ **Cooldowns** — per player or per block, optionally separate for left/right clicks.
- 🔊 **Configurable sounds** — 7 slots, each with enable, name, volume and pitch.
  Invalid sound names are reported and safely ignored.
- 💬 **Full message control** — `messages.yml` with colour codes, `&#RRGGBB` hex colours,
  a prefix and a per-message kill switch.
- 💾 **Reliable storage** — immediate, atomic, coalesced background writes; world-UUID based
  entries survive renamed or late-loading worlds.
- ♻️ **Live reload** — `/bt reload` re-applies everything without a restart and without
  losing configured blocks.
- 🪶 **Lightweight** — O(1) lookups, cached config, no per-tick work, no NMS, no dependencies.

---

## 📋 Requirements

| | |
|---|---|
| **Server** | Paper 1.20.1 or newer |
| **Java** | 17+ (21 recommended for 1.20.4+) |
| **Dependencies** | None |

---

## 🚀 Installation

1. Download **`TA_BlockTrigger-1.0.0.jar`** from the [releases](../../releases) page
   (or build it yourself, see below).
2. Drop the jar into your server's `plugins/` folder.
3. Restart the server. The plugin generates:
   ```
   plugins/TA_BlockTrigger/
   ├── config.yml     ← behaviour, permissions, cooldowns, sounds, storage
   ├── messages.yml   ← every message, fully customisable
   └── blocks.yml     ← your block commands (managed by the plugin)
   ```
4. Configure it to your liking and run `/bt reload`.

---

## 🔨 Building from source

```bash
git clone <your-repo-url>
cd TA_BlockTrigger
mvn -B clean package
```

Output: **`target/TA_BlockTrigger-1.0.0.jar`**

Compiled against `paper-api:1.20.1-R0.1-SNAPSHOT` with `--release 17`.
To build for another Paper version, change the `paper.version` property in `pom.xml` —
the source uses no version-specific API.

---

## ⚡ Quick start

```
1. Look directly at the block you want to use.
2. /bt console say Hello World right
3. Right-click the block → the console says "Hello World".
```

| Command | Result |
|---|---|
| `/bt console say Hello World right` | right click → console runs `say Hello World` |
| `/bt player spawn right` | right click → the player runs `/spawn` |
| `/bt console eco give {player} 100 both` | left **or** right click → console gives 100 to the clicker |
| `/bt player warp pvp left` | left click → the player runs `/warp pvp` |
| `/bt remove` | removes the command from the targeted block |
| `/bt reload` | reloads all configuration and block data |

---

## 🕹️ Commands

```
/bt <console|player> <command ...> [right|left|both]
/bt <command ...> [right|left|both]      (uses the default execution type)
/bt remove
/bt reload
/bt help
```

**Aliases:** `ctb`, `tbt`, `blocktrigger`, `tablocktrigger`, `cmdtoblock`

**Parsing rules**

- **First argument** — if it is exactly `console` or `player` it is the execution type;
  otherwise the whole input is the command and `settings.default-execution-type` is used.
- **Last argument** — if it is exactly `right`, `left` or `both` it is the click type;
  otherwise `settings.default-click-type` is used.
- **Everything in between** is the command, preserved exactly (multiple arguments and spaces).
- A **leading `/`** is accepted when `execution.accept-leading-slash: true` and is always
  stripped before execution.

**Tab completion**

- first argument: `console`, `player`, `remove`, `reload`, `help`
- last argument while defining a command: `right`, `left`, `both`

---

## 🔑 Permissions

| Node | Default | Purpose |
|---|---|---|
| `tablocktrigger.admin` | `op` | use `/bt` in permission-node mode |
| `tablocktrigger.use` | `true` | trigger block commands when `execution.require-permission: true` |

Management is **OP-only out of the box** (`permissions.op-only: true`). The permission model is
fully configurable — OP only, permission node only, or both, with an `ops-bypass` switch:

```yaml
permissions:
  op-only: true                 # only operators may use /bt  (recommended)
  use-permission-node: false    # also/only require the node below (LuckPerms friendly)
  permission-node: "tablocktrigger.admin"
  ops-bypass: true              # OPs always pass, even without the node
```

> Non-OP players can never configure, modify, remove or reload anything — but they **can**
> click configured blocks.

---

## 🔤 Placeholders

| Placeholder | Replaced with | Toggle |
|---|---|---|
| `{player}` | name of the clicking player | `placeholders.player` |
| `{uuid}` | UUID of the clicking player | `placeholders.uuid` |
| `{world}` | world the player is in | `placeholders.world` |
| `{x}` `{y}` `{z}` | the player's block coordinates | `placeholders.coordinates` |

Placeholders are applied only when **both** `placeholders.enabled` and
`execution.use-placeholders` are `true`.

```yaml
/bt console eco give {player} 100 both
/bt console say &e{player} &7used the shop in &b{world}
```

---

## ⚙️ Configuration

Everything lives in `config.yml` and `messages.yml` — **no Java edits required.**
The full option-by-option reference is in **[docs/CONFIGURATION.md](docs/CONFIGURATION.md)**.

```yaml
settings:
  enabled: true
  max-selection-distance: 10
  default-execution-type: console
  default-click-type: right
  debug: false

worlds:
  mode: "all"              # all | whitelist | blacklist
  list: []
  run-in-filtered-worlds: false

interactions:
  right-click: true
  left-click: true
  both: true
  prevent-block-breaking: true
  prevent-offhand-duplicates: true
  prevent-duplicate-interactions: true
  allow-normal-interaction: true

execution:
  console: true
  player: true
  accept-leading-slash: true
  use-placeholders: true
  log-commands: false
  require-permission: false

cooldown:
  enabled: false
  seconds: 1.0
  type: "player"           # player | block
  separate-click-types: true

sounds:
  enabled: true
  set-command:
    enabled: true
    sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
    volume: 1.0
    pitch: 1.0

placeholders:
  enabled: true
  player: true
  uuid: true
  world: true
  coordinates: true

storage:
  file: "blocks.yml"
  auto-save:
    enabled: true
    interval-minutes: 5
  reload-on-plugin-reload: true
  validate-on-load: true
  backup-on-save: false
  debug-logging: false
```

**Sound names** accept three notations — invalid names are logged and ignored:

```yaml
sound: "ENTITY_EXPERIENCE_ORB_PICKUP"       # Bukkit constant
sound: "entity.experience_orb.pickup"       # Minecraft key
sound: "minecraft:block.note_block.bell"    # namespaced key
```

**Messages** support `&a` colour codes, `&#RRGGBB` hex colours, the `{prefix}` placeholder,
and can be disabled per message (`''`, `-` or `disabled`). Missing keys automatically fall back
to the bundled defaults.

---

## 💾 Data storage

`blocks.yml` — saved immediately on every change, written atomically (temp file + atomic move)
through a single coalesced background writer:

```yaml
blocks:
  - world: world
    world-uuid: aafa890c-647e-4342-9ebd-8bb1441fa97d
    x: 12
    y: -60
    z: 7
    execution: CONSOLE     # CONSOLE | PLAYER
    click: BOTH            # RIGHT | LEFT | BOTH
    command: say Hello World
    created-by: MamaD_TA
    created-at: 1793892693573
```

- Entries of worlds that are not loaded are **kept** and rebound automatically when the world
  loads (Multiverse-friendly).
- Invalid entries are skipped with a warning instead of corrupting the file.
- All pending changes are flushed synchronously on shutdown.

---

## 🔍 How it works

```
Player clicks a block
   └─ PlayerInteractEvent (LEFT/RIGHT_CLICK_BLOCK)
        ├─ is a command bound to this exact block?  (O(1) map lookup: world UUID + xyz)
        ├─ off-hand duplicate? same-tick duplicate? → ignore
        ├─ click mode enabled in config for this click type?
        ├─ world allowed?
        ├─ cooldown active?                        → message + sound
        ├─ placeholders resolved, leading slash stripped
        └─ execute: console  → Bukkit.dispatchCommand(console, cmd)
                    player   → player.performCommand(cmd)
```

Configured blocks are additionally protected by an interaction cancel plus
`BlockDamageEvent` / `BlockBreakEvent` guards, so a left-click-activated block can never be
broken by accident.

---

## 🧱 Compatibility

- Compiled against `paper-api:1.20.1-R0.1-SNAPSHOT`, `api-version: '1.20'`, Java 17 bytecode.
- **No NMS**, no `CraftBukkit`, no version-specific code.
- Sounds are played through key-based Adventure sounds, so they keep working on newer Paper
  versions where `org.bukkit.Sound` is no longer an enum.
- Only long-stable events are used: `PlayerInteractEvent`, `BlockDamageEvent`,
  `BlockBreakEvent`, `WorldLoadEvent`.
- Tested on **Paper 1.20.1 (build 196)** with a real client — assignment, left/right/both,
  console/player execution, placeholders, removal, cooldowns, world filter, block protection,
  non-OP access and reloads all verified.

---

## 📂 Project structure

```
TA_BlockTrigger/
├── pom.xml
├── README.md
├── docs/
│   ├── CONFIGURATION.md        every config option explained
│   └── GUIDE.md                full user/administrator guide
└── src/main/
    ├── java/com/mamadta/tablocktrigger/
    │   ├── TABlockTrigger.java              main class, lifecycle, reload, tasks
    │   ├── command/    BtCommand, BtTabCompleter
    │   ├── config/     ConfigManager, MessageManager, SoundManager
    │   ├── manager/    BlockCommandManager, CooldownManager, PlaceholderManager
    │   ├── execution/  CommandDispatcher
    │   ├── listener/   BlockInteractionListener, WorldListener
    │   ├── storage/    BlockStorage
    │   ├── model/      BlockCommand, BlockKey, ExecutionType, ClickMode
    │   └── util/       TextUtil
    └── resources/
        ├── plugin.yml
        ├── config.yml
        ├── messages.yml
        └── blocks.yml
```

---

## 🩺 Troubleshooting

| Symptom | Fix |
|---|---|
| `/bt` says no permission | You are not OP while `permissions.op-only: true`. Use `/op <name>`, or grant `tablocktrigger.admin` and set `permissions.use-permission-node: true`. |
| "not looking at a block within N blocks" | Look directly at the block face; raise `settings.max-selection-distance` if needed. |
| Clicking does nothing | Check the block's click mode, `interactions.right-click` / `left-click` / `both`, the world filter and `settings.enabled`. Set `settings.debug: true` for detailed console output. |
| No sounds | Check `sounds.enabled` and the slot's `enabled`; watch for "Unknown sound" warnings in the console. |
| Block can be broken | Set `interactions.prevent-block-breaking: true`. |
| Config changes ignored | Run `/bt reload` or restart the server. |

---

## 📚 Documentation

- **[docs/GUIDE.md](docs/GUIDE.md)** — full administrator guide (features, behaviour, storage,
  multi-version notes).
- **[docs/CONFIGURATION.md](docs/CONFIGURATION.md)** — every option of `config.yml`,
  `messages.yml` and `blocks.yml`.

---

<div align="center">

**Author:** MamaD_TA

If this plugin helps your server, a ⭐ on the repo is appreciated.

</div>
