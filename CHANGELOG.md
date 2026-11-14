# Changelog

All notable changes to TA_BlockTrigger are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] — 2026-11-05

The "runs everywhere" release. Same jar, same config, three server implementations.

### Added
- **Spigot and CraftBukkit support.** The plugin is now built against `spigot-api` instead of
  `paper-api`, so a single jar runs on Paper, Spigot and CraftBukkit.
- Internal tick counter owned by the plugin, replacing the Paper-only `Bukkit.getCurrentTick()`.
  Cooldowns and the 30-second housekeeping pass use it.
- Sound resolution through the Bukkit sound registry, so configured sounds keep working on
  servers where the enum-based API is gone.

### Changed
- `api-version` raised from `1.19` to `1.20` — the previous value made the plugin unloadable
  on 1.20.1 servers.
- Colour handling is done with `ChatColor` and manual hex expansion (`&x&r&r&g&g&b&b`) instead of
  Adventure's `LegacyComponentSerializer`.
- README and `docs/GUIDE.md` now describe the three supported platforms and the version table.

### Removed
- `getPluginMeta()` (Paper 1.20+) — replaced with the equivalent data from `plugin.yml`.
- `net.kyori.adventure.*` imports — the plugin has no external dependencies at runtime.

### Fixed
- Plugin failed to load on servers that did not ship the Paper API surface.

### Compatibility
| Server | 1.20.1 | 1.20.2–1.20.4 | 1.20.5+ / 1.21.x |
|---|---|---|---|
| Paper | ✅ | ✅ | ✅ (Java 21) |
| Spigot | ✅ | ✅ | ✅ (Java 21) |
| CraftBukkit | ✅ | ✅ | ✅ (Java 21) |
| Folia | ❌ | ❌ | ❌ |

`config.yml`, `messages.yml` and `blocks.yml` from 1.0.x load unchanged — no migration needed.

---

## [1.0.1] — 2026-11-04

### Fixed
- `api-version: '1.19'` in `plugin.yml` caused a hard refusal to load on 1.20.1+ servers. Set to
  `'1.20'`. No Java code changed — same command syntax, config keys, storage format and
  permissions as 1.0.0.

---

## [1.0.0] — 2026-11-03

First public release.

### Added
- Bind any command to an exact block (world + X/Y/Z) by looking at it and running `/bt`.
- Execution as `console` (full power) or as the `player` (normal permissions).
- Click modes: `right`, `left` or `both`.
- Placeholders: `{player}`, `{uuid}`, `{world}`, `{x}`, `{y}`, `{z}`.
- Per-player and per-block cooldowns.
- Configurable sounds and messages, including hex colours.
- Block protection — configured blocks cannot be broken.
- World filter and a configurable interaction distance.
- Atomic, coalesced persistence in `blocks.yml`, with `/bt reload` for live config changes.
- Tab completion for `/bt`.
- `docs/GUIDE.md` and `docs/CONFIGURATION.md`.

### Requirements
- Paper 1.20.1+, Java 17+.

[1.1.0]: https://github.com/mamad-ta/TA_BlockTrigger/releases/tag/v1.1.0
[1.0.1]: https://github.com/mamad-ta/TA_BlockTrigger/releases/tag/v1.0.1
[1.0.0]: https://github.com/mamad-ta/TA_BlockTrigger/releases/tag/v1.0.0
