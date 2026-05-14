# Calitina PVP

A Minecraft 1.21.11 Folia plugin that adds an Ancient Core / Fatal Core PVP progression system.

## Overview

Players kill Wardens to obtain **Ancient Cores** (远古核心), use them to farm special boss drops, craft **Fatal Cores** (必杀核心), and forge instant-kill weapons that bypass Totems of Undying.

## Progression Flow

```
Warden ──1%──▶ Ancient Core (远古核心)
                   │
                   │ right-click sonic boom
                   ▼
         Ender Dragon ──30%──▶ Resonant Dragon Egg (受远古之力共鸣的龙蛋)
         Wither       ──30%──▶ Resonant Nether Star (受远古之力共鸣的下界之星)
                   │
                   │ crafting table (9-slot recipe)
                   ▼
            Fatal Core (必杀核心)
                   │
                   │ anvil + weapon
                   ▼
            Fatal Weapon (必杀武器)
                   │
                   │ melee attack
                   ▼
              INSTANT KILL (any mob, any player, ignores Totem)
```

## Items

### Ancient Core (远古核心)
- **Source**: Warden death (default 1% chance, configurable)
- **Model**: Echo Shard
- **Use**: Right-click fires a teal Warden-style sonic boom (60% of Warden damage)
- **Durability**: 100 uses (configurable)
- Drops as a single item (non-stackable by default, stacks if obtained creatively)

### Resonant Dragon Egg (受远古之力共鸣的龙蛋)
- **Source**: Ender Dragon killed with Ancient Core (last hit must be Ancient Core sonic boom)
- **Drop chance**: 30% (configurable)
- **Drop location**: Fixed at `(0.5, 65, 0.5)` in The End, zero velocity
- **Model**: Dragon Egg (right-click placement blocked)

### Resonant Nether Star (受远古之力共鸣的下界之星)
- **Source**: Wither killed with Ancient Core (last hit must be Ancient Core sonic boom)
- **Drop chance**: 30% (configurable, separate from dragon egg chance)
- **Model**: Nether Star

### Fatal Core (必杀核心)
- **Source**: Crafted in crafting table or crafter
- **Recipe** (3×3):
  ```
  Star  Egg  Star
  Egg  Core  Egg
  Star  Egg  Star
  ```
  - Star = Resonant Nether Star
  - Egg = Resonant Dragon Egg
  - Core = Ancient Core (any durability)
- **Standalone use**: Right-click fires a red instant-kill sonic boom (bypasses Totem)
- **Uses**: 10 (configurable)
- Crafting is dynamic — works in both crafting table and crafter

### Fatal Weapon (必杀武器)
- **Source**: Combine any weapon + Fatal Core in an anvil (10 level cost)
- **Supported weapons**: Sword, Bow, Crossbow, Axe, Trident, Mace
- **Effect**: Melee attacks instantly kill any entity, including players with Totem of Undying
- **Uses**: Transferred from the Fatal Core's remaining uses
  - **Mace** gets bonus: proportional to 13 max (e.g., 5-use core → 7 uses on Mace)
  - **Other weapons**: direct transfer (e.g., 5-use core → 5 uses)
- Weapon shatters when uses reach 0
- Remaining uses shown in lore and action bar

## Config

All values in `config.yml`:

| Key | Default | Description |
|-----|---------|-------------|
| `warden-drop-chance` | 1.0 | Warden Ancient Core drop chance (%) |
| `ancient-core-damage-percent` | 60 | Ancient Core damage as % of Warden sonic boom (10 base) |
| `ancient-core-durability` | 100 | Ancient Core uses before breaking |
| `dragon-egg-drop-chance` | 30.0 | Resonant Dragon Egg drop chance (%) |
| `nether-star-drop-chance` | 30.0 | Resonant Nether Star drop chance (%) |
| `fatal-core-base-uses` | 10 | Base Fatal Core uses |
| `fatal-core-specialty-bonus` | 3 | Bonus uses for Mace |
| `fatal-core-sonic-range` | 15 | Fatal Core sonic boom range (blocks) |
| `fatal-core-sonic-damage` | 15.0 | Fatal Core sonic boom damage (obsolete — now instant kill) |

## Compatibility

### dragon-egg-plugin
Fully compatible. Both plugins listen to `EntityDeathEvent` for Ender Dragon but at different priorities:
- `dragon-egg-plugin` (NORMAL): places vanilla dragon egg block at portal
- `calitina-pvp` (MONITOR): drops custom Resonant Dragon Egg item at `(0.5, 65, 0.5)`

No conflicts — they operate on different items and locations.

## Technical Notes

- **Folia compatible**: Uses `ConcurrentHashMap` for shared state, no `BukkitScheduler` calls
- **Stack-safe**: Stacked Ancient Cores / Fatal Cores consume 1 durability per use by splitting the stack
- **Anvil stack-safe**: Only 1 Fatal Core consumed from stacked anvil right slot
- **Dynamic crafting**: Uses `PrepareItemCraftEvent` + `InventoryClickEvent` (LOWEST) pattern
- **PDC-based**: All custom items identified via Persistent Data Container, not lore matching

## Build

```bash
./gradlew build
# Output: build/libs/calitina-pvp-1.0-SNAPSHOT.jar
```

Requires Java 21, Paper API 1.21.11.
