# SpigotMC — متن کوتاه

## فیلدهای فرم

| فیلد | مقدار |
|---|---|
| Resource name | `TA_BlockTrigger` |
| Tag line | `Bind any command to any block. One click, and it runs.` |
| Category | `Tools and Utilities` |
| Native version | `1.20` |
| Tested versions | `1.20`, `1.21` |
| Version | `1.1.0` |
| Tags | `paper`, `spigot`, `bukkit`, `command`, `block`, `trigger`, `click` |
| Source | `https://github.com/mamad-ta/TA_BlockTrigger` |
| دانلود | `https://github.com/mamad-ta/TA_BlockTrigger/releases/download/v1.1.0/TA_BlockTrigger-1.1.0.jar` |

---

## توضیحات (این رو کپی کن)

```
[CENTER][IMG]https://raw.githubusercontent.com/mamad-ta/TA_BlockTrigger/main/assets/logo-512.png[/IMG]

[SIZE=6][B]TA_BlockTrigger[/B][/SIZE]
[SIZE=4]Bind any command to any block. One click, and it runs.[/SIZE]

Paper · Spigot · CraftBukkit — 1.20.1+
[/CENTER]

[HR][/HR]

[B]Assign a command to one exact block.[/B] When a player clicks it, the command runs — as the
console or as the player. Every block is identified by world UUID + X/Y/Z, so two identical
blocks can do completely different things. Unconfigured blocks are never touched.

[HR][/HR]

[B][SIZE=5]Features[/SIZE][/B]

[LIST]
[*]Exact block binding (ray-traced, configurable distance)
[*]Execution as [B]console[/B] or as the [B]player[/B]
[*]Click modes: [B]right[/B], [B]left[/B], [B]both[/B]
[*]Placeholders: {player} {uuid} {world} {x} {y} {z}
[*]Cooldowns per player or per block
[*]7 configurable sound slots
[*]Full message control with colour + hex codes
[*]Block protection, world filter, live /bt reload
[*]No dependencies, no NMS
[/LIST]

[HR][/HR]

[B][SIZE=5]Commands[/SIZE][/B]

[CODE]
/bt <console|player> <command ...> [right|left|both]
/bt remove
/bt reload
/bt help
[/CODE]

[B]Example:[/B] look at a block and run
[CODE]/bt console eco give {player} 100 both[/CODE]
Now left- or right-clicking that block gives the player 100.

[HR][/HR]

[B][SIZE=5]Install[/SIZE][/B]

[LIST=1]
[*]Drop TA_BlockTrigger-1.1.0.jar into plugins/
[*]Restart the server
[*]Bind your first block with /bt
[/LIST]

[HR][/HR]

[B][SIZE=5]Links[/SIZE][/B]

[LIST]
[*][B]Download:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger/releases/download/v1.1.0/TA_BlockTrigger-1.1.0.jar[/URL]
[*][B]Source code:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger[/URL]
[*][B]Guide:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger/blob/main/docs/GUIDE.md[/URL]
[/LIST]
```

---

## نسخه‌ی حتی کوتاه‌تر (اگه این کوتاه‌ترش رو می‌خوای)

```
[CENTER][B][SIZE=6]TA_BlockTrigger[/SIZE][/B]
Bind any command to any block. One click, and it runs.

Paper · Spigot · CraftBukkit 1.20.1+[/CENTER]

[HR][/HR]

Look at a block, run [B]/bt console eco give {player} 100 both[/B] — now every click on that
block pays the player 100.

[LIST]
[*]Console or player execution
[*]Right / left / both click
[*]Placeholders: {player} {uuid} {world} {x} {y} {z}
[*]Cooldowns, sounds, block protection, world filter
[*]Live reload with /bt reload
[*]No dependencies, no NMS
[/LIST]

[B]Install:[/B] drop the jar in plugins/ → restart → done.

[B]Commands:[/B] /bt &lt;console|player&gt; &lt;command&gt; [right|left|both] · /bt remove · /bt reload · /bt help

[HR][/HR]

[B]Download:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger/releases/download/v1.1.0/TA_BlockTrigger-1.1.0.jar[/URL]
[B]Source:[/B] [URL]https://github.com/mamad-ta/TA_BlockTrigger[/URL]
```

---

## لوگو

فایل: `assets/logo.png` (۱۰۲۴×۱۰۲۴)

اسپیگات تصویر رو از URL می‌گیره، پس باید آپلودش کنی. سه راه:

1. **آواتار ریسورس** رو مستقیم از فایل آپلود کن (اسپیگات فایل لوکال قبول می‌کنه)
2. آپلود به [imgur](https://imgur.com) یا [ibb.co](https://imgbb.com) و گذاشتن لینک
3. گذاشتن تو خود ریپو + لینک raw:
   `https://raw.githubusercontent.com/mamad-ta/TA_BlockTrigger/main/assets/logo.png`

راه ۳ از قبل توی متن گذاشته شده — پس فقط کافیه فایل‌ها توی ریپو باشن (که الان هستن):

```
https://raw.githubusercontent.com/mamad-ta/TA_BlockTrigger/main/assets/logo-512.png   ← برای BBCode
https://raw.githubusercontent.com/mamad-ta/TA_BlockTrigger/main/assets/logo-96.png    ← آواتار
https://raw.githubusercontent.com/mamad-ta/TA_BlockTrigger/main/assets/logo.png       ← کامل ۱۰۲۴
```

⚠️ **توجه:** raw.githubusercontent بعضی وقت‌ها کند یا rate-limit میشه. اگه تصویر لود نشد،
فایل `assets/logo-512.png` رو روی [imgbb](https://imgbb.com) آپلود کن و لینک مستقیمش رو بذار.
