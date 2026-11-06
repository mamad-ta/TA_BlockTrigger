# SpigotMC upload kit — TA_BlockTrigger v1.1.0

همه‌ی فیلدهایی که فرم اسپیگات می‌خواد، آماده‌ی کپی. اول منابع (فایل + لینک)، بعد متن‌ها.

---

## 0. منابع آماده

**فایل آپلود (فقط همین رو آپلود کن):**
```
TA_BlockTrigger-1.1.0.jar        74,857 bytes (73 KB)
```

**لینک دانلود گیت‌هاب (Source / External download):**
```
https://github.com/mamad-ta/TA_BlockTrigger/releases/download/v1.1.0/TA_BlockTrigger-1.1.0.jar
```

**صفحه‌ی ریلیز:**
```
https://github.com/mamad-ta/TA_BlockTrigger/releases/tag/v1.1.0
```

**سورس‌کد:**
```
https://github.com/mamad-ta/TA_BlockTrigger
```

**SHA256:**
```
831ff422093b8208573159e5701d53f51d95420cdbc0fef58f5b74db053bec6c
```

---

## 1. فیلدهای فرم اسپیگات

| فیلد اسپیگات | مقدار دقیق |
|---|---|
| **Resource name** | `TA_BlockTrigger` |
| **Tag line** | `Bind any command to any block. One click, and it runs.` |
| **Category** | `Tools and Utilities` |
| **Native Minecraft Version** | `1.20` |
| **Tested Minecraft Versions** | `1.20`, `1.21` |
| **Languages** | `English` |
| **Donation link** | *(خالی بذار یا لینک خودت)* |
| **External download URL** | `https://github.com/mamad-ta/TA_BlockTrigger/releases/download/v1.1.0/TA_BlockTrigger-1.1.0.jar` |
| **Premium** | `Free` |
| **Source code URL** | `https://github.com/mamad-ta/TA_BlockTrigger` |

**Tag ها:** `paper`, `spigot`, `bukkit`, `block`, `command`, `trigger`, `click`, `button`, `admin`, `utility`

**باگ‌ترکر:** `https://github.com/mamad-ta/TA_BlockTrigger/issues`

---

## 2. متن اصلی (BBCode برای ادیتور اسپیگات)

> ادیتور اسپیگات BBCode می‌خواد، نه Markdown. این آماده‌ست — کپی کن و پیست کن.

```
[CENTER][SIZE=6][B]TA_BlockTrigger[/B][/SIZE]
[SIZE=4]Bind any command to any block. One click, and it runs.[/SIZE]

[SIZE=3]Paper · Spigot · CraftBukkit — Minecraft 1.20.1 and newer[/SIZE][/CENTER]

[HR][/HR]

[B][SIZE=5]What is it?[/SIZE][/B]

A lightweight, fully configurable plugin that lets administrators attach a command to one
exact block. When a player interacts with that block, the command runs — either as the console
or as the clicking player.

Every block is identified by [B]world UUID + X/Y/Z[/B], so two identical stone blocks in the
same world can hold completely different commands. Blocks without a binding are never touched:
normal gameplay is unaffected.

[HR][/HR]

[B][SIZE=5]Features[/SIZE][/B]

[LIST]
[*][B]Exact block binding[/B] — ray-traced, configurable selection distance
[*][B]Two execution types[/B] — [I]console[/I] (runs as the server) or [I]player[/I] (runs as the clicker)
[*][B]Three click modes[/B] — right click, left click, or both
[*][B]Smart argument parser[/B] — first argument = execution type, last argument = click type, everything in between is kept verbatim (spaces included)
[*][B]Built-in placeholders[/B] — {player}, {uuid}, {world}, {x}, {y}, {z} — each individually switchable
[*][B]Cooldowns[/B] — per player or per block, optionally separate for left/right clicks
[*][B]Configurable sounds[/B] — 7 slots, each with enable, name, volume and pitch
[*][B]Full message control[/B] — messages.yml with colour codes, &#RRGGBB hex colours and a per-message kill switch
[*][B]Block protection[/B] — configured blocks cannot be broken by players
[*][B]World filter[/B] — all, whitelist or blacklist
[*][B]Live reload[/B] — /bt reload re-applies everything without a restart
[*][B]Reliable storage[/B] — immediate, atomic, coalesced background writes; world-UUID based entries survive renamed or late-loading worlds
[*][B]No dependencies[/B] — O(1) lookups, cached config, no per-tick work, no NMS
[/LIST]

[HR][/HR]

[B][SIZE=5]Commands[/SIZE][/B]

[CODE]
/bt <console|player> <command ...> [right|left|both]
/bt <command ...> [right|left|both]      (uses the default execution type)
/bt remove
/bt reload
/bt help
[/CODE]

[B]Aliases:[/B] ctb, tbt, blocktrigger, tablocktrigger, cmdtoblock

[B]Tab completion[/B] is supported for the first argument (console, player, remove, reload, help)
and for the click type (right, left, both).

[HR][/HR]

[B][SIZE=5][COLOR=#4a90d9]Quick start[/COLOR][/SIZE][/B]

[CODE]
1. Look directly at the block you want to use.
2. /bt console say Hello World right
3. Right-click the block → the console says "Hello World".
[/CODE]

[B]More examples[/B]

[TABLE]
[TR][TD][B]Command[/B][/TD][TD][B]Result[/B][/TD][/TR]
[TR][TD]/bt console eco give {player} 100 both[/TD][TD]left or right click → console gives 100 to the clicker[/TD][/TR]
[TR][TD]/bt player spawn right[/TD][TD]right click → the player runs /spawn[/TD][/TR]
[TR][TD]/bt player warp pvp left[/TD][TD]left click → the player runs /warp pvp[/TD][/TR]
[TR][TD]/bt remove[/TD][TD]removes the command from the targeted block[/TD][/TR]
[TR][TD]/bt reload[/TD][TD]reloads all configuration and block data[/TD][/TR]
[/TABLE]

[HR][/HR]

[B][SIZE=5][COLOR=#4a90d9]Permissions[/COLOR][/SIZE][/B]

[TABLE]
[TR][TD][B]Node[/B][/TD][TD][B]Default[/B][/TD][TD][B]Purpose[/B][/TD][/TR]
[TR][TD]tablocktrigger.admin[/TD][TD]op[/TD][TD]use /bt in permission-node mode[/TD][/TR]
[TR][TD]tablocktrigger.use[/TD][TD]true[/TD][TD]trigger block commands when execution.require-permission is true[/TD][/TR]
[/TABLE]

Management is [B]OP-only out of the box[/B] (permissions.op-only: true). The permission model is
fully configurable — OP only, permission node only, or both, with an ops-bypass switch.
Non-OP players can never configure anything, but they [B]can[/B] click configured blocks.

[HR][/HR]

[B][SIZE=5][COLOR=#4a90d9]Placeholders[/COLOR][/SIZE][/B]

[TABLE]
[TR][TD][B]Placeholder[/B][/TD][TD][B]Replaced with[/B][/TD][/TR]
[TR][TD]{player}[/TD][TD]name of the clicking player[/TD][/TR]
[TR][TD]{uuid}[/TD][TD]UUID of the clicking player[/TD][/TR]
[TR][TD]{world}[/TD][TD]world the player is in[/TD][/TR]
[TR][TD]{x} {y} {z}[/TD][TD]the player's block coordinates[/TD][/TR]
[/TABLE]

[HR][/HR]

[B][SIZE=5][COLOR=#4a90d9]Installation[/COLOR][/SIZE][/B]

[LIST=1]
[*]Drop [B]TA_BlockTrigger-1.1.0.jar[/B] into your server's plugins/ folder.
[*]Restart the server. The plugin generates config.yml, messages.yml and blocks.yml.
[*]Configure it and run /bt reload — or just start binding blocks right away.
[/LIST]

[HR][/HR]

[B][SIZE=5][COLOR=#4a90d9]Compatibility[/COLOR][/SIZE][/B]

[LIST]
[*][B]Paper[/B], [B]Spigot[/B] and [B]CraftBukkit[/B] — one single jar for all three
[*]Minecraft [B]1.20.1 and newer[/B], including the 1.21.x line
[*]Java 17+ (Java 21 on 1.20.5+ / 1.21.x servers)
[*][B]No NMS[/B], no CraftBukkit internals, no version-specific code
[*]Built against spigot-api, so no Paper-only API can slip in
[/LIST]

[B]Not supported:[/B] Folia (commands dispatch on the main thread), versions older than 1.20.1.

[HR][/HR]

[B][SIZE=5][COLOR=#4a90d9]Links[/COLOR][/SIZE][/B]

[LIST]
[*][B]Source code:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger[/URL]
[*][B]Releases / changelog:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger/releases[/URL]
[*][B]Full administrator guide:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger/blob/main/docs/GUIDE.md[/URL]
[*][B]Configuration reference:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger/blob/main/docs/CONFIGURATION.md[/URL]
[*][B]Bug reports:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger/issues[/URL]
[/LIST]

[HR][/HR]

[CENTER][SIZE=3]If this plugin helps your server, a ⭐ on the repo is appreciated.[/SIZE][/CENTER]
```

---

## 3. نسخه‌ی Markdown (برای README یا دیسکورد)

```markdown
# TA_BlockTrigger

**Bind any command to any block. One click, and it runs.**

Paper · Spigot · CraftBukkit — Minecraft 1.20.1+

⬇️ **Download:** [TA_BlockTrigger-1.1.0.jar](https://github.com/mamad-ta/TA_BlockTrigger/releases/download/v1.1.0/TA_BlockTrigger-1.1.0.jar) (73 KB)
📦 **Releases:** https://github.com/mamad-ta/TA_BlockTrigger/releases
💻 **Source:** https://github.com/mamad-ta/TA_BlockTrigger

## What is it?

A lightweight Paper/Spigot plugin that lets administrators attach a command to one exact
block. When a player interacts with that block, the command runs — as the console or as the
clicking player. Every block is identified by world UUID + X/Y/Z, so two identical blocks can
hold different commands. Unconfigured blocks are never touched.

## Features

- 🎯 **Exact block binding** — ray-traced, configurable distance
- 🖥️ **Two execution types** — console or player
- 🖱️ **Three click modes** — right, left, both
- 🧩 **Smart argument parser** — first arg = execution type, last arg = click type, middle kept verbatim
- 🔤 **Placeholders** — `{player}` `{uuid}` `{world}` `{x}` `{y}` `{z}`
- ⏱️ **Cooldowns** — per player or per block
- 🔊 **7 sound slots** — enable, name, volume, pitch
- 💬 **Full message control** — colour codes, `&#RRGGBB` hex, per-message kill switch
- 🔐 **OP-only by default** — configurable permission model
- 💾 **Atomic storage** — survives crashes, renames and late-loading worlds
- ♻️ **Live reload** — `/bt reload`
- 🪶 **No dependencies** — no NMS, O(1) lookups

## Commands

```
/bt <console|player> <command ...> [right|left|both]
/bt <command ...> [right|left|both]
/bt remove
/bt reload
/bt help
```

Aliases: `ctb`, `tbt`, `blocktrigger`, `tablocktrigger`, `cmdtoblock`

## Quick start

```
1. Look directly at the block you want to use.
2. /bt console say Hello World right
3. Right-click the block → the console says "Hello World".
```

## Permissions

| Node | Default | Purpose |
|---|---|---|
| `tablocktrigger.admin` | `op` | use `/bt` in permission-node mode |
| `tablocktrigger.use` | `true` | trigger blocks when `execution.require-permission: true` |

## Requirements

| | |
|---|---|
| Server | Paper, Spigot or CraftBukkit — 1.20.1+ |
| Java | 17+ (21 on 1.20.5+ / 1.21.x) |
| Dependencies | None |

## Install

1. Drop `TA_BlockTrigger-1.1.0.jar` into `plugins/`
2. Restart the server
3. Bind your first block with `/bt console say Hello right`

## Links

- [Source code](https://github.com/mamad-ta/TA_BlockTrigger)
- [Releases](https://github.com/mamad-ta/TA_BlockTrigger/releases)
- [Administrator guide](https://github.com/mamad-ta/TA_BlockTrigger/blob/main/docs/GUIDE.md)
- [Configuration reference](https://github.com/mamad-ta/TA_BlockTrigger/blob/main/docs/CONFIGURATION.md)
```

---

## 4. متن کوتاه (برای فیلد Tag line یا توضیح کوتاه)

> `Bind any command to any block. One click, and it runs.`

> `Attach any command to any block — console or player execution, right/left/both click, placeholders, cooldowns, sounds. Paper · Spigot · CraftBukkit 1.20.1+`

---

## 5. راهنمای قدم‌به‌قدم آپلود

1. برو به **https://www.spigotmc.org/resources/add**
2. **Category:** `Tools and Utilities` → **Continue**
3. **Resource name:** `TA_BlockTrigger`
4. **Tag line:** از بخش ۴ کپی کن
5. **File:** `TA_BlockTrigger-1.1.0.jar` رو آپلود کن
6. **External download:** لینک گیت‌هاب بالا رو بذار (یا خالی بذار تا فقط خود اسپیگات هاست کنه)
7. **Description:** BBCode بخش ۲ رو کپی کن
8. **Tags:** `paper`, `spigot`, `bukkit`, `command`, `block`, `trigger`, `click`
9. **Tested versions:** `1.20` و `1.21` رو تیک بزن
10. **Version:** `1.1.0`
11. **Source code:** لینک گیت‌هاب
12. **Donation / Bug tracker** رو پر کن یا رد کن
13. ✅ تأیید قوانین → **Submit**

---

## 6. نکات مهم قبل از Submit

| نکته | توضیح |
|---|---|
| **لوگو / آیکون** | اسپیگات تصویر می‌خواد. یه بنر ۷۵۰×۲۵۰ یا آیکون ۹۶×۹۶ آماده کن — اگه خواستی بگو، می‌سازم. |
| **اسکرین‌شات** | حداقل یه تصویر از `/bt help` یا یه بلاک بایندشده. اسپیگات معمولاً بدون تصویر کمتر تأیید می‌کنه. |
| **ادیتور BBCode** | توی ادیتور اسپیگات حالت **BBCode** رو انتخاب کن، نه WYSIWYG. |
| **External download** | اگه این رو پر کنی، خود اسپیگات فایل رو هاست نمی‌کنه و از لینک تو دانلود می‌شه — آمار دانلودت هم نمیاد. توصیه: **فایل رو خودت آپلود کن** و لینک گیت‌هاب رو فقط تو توضیحات بذار. |
| **نسخه‌ی Native** | `1.20` بذار (نه 1.20.1) — چون `api-version: '1.20'` هست و اسپیگات با فرمت x.y می‌شناسه. |
| **اولین تأیید** | اسپیگات منابع جدید رو معمولاً ۲۴–۴۸ ساعت بررسی می‌کنه. |
