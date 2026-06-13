# FarmDesk

**Unified farmOS frontend for Android — offline-first farm management suite**

FarmDesk is a single-file Android app that combines GoatDesk, StockDesk, and BirdDesk into one APK. Manage livestock health, farm inventory, and poultry flocks from one app — all syncing to a [farmOS](https://farmos.org) instance, all working offline first.

Built for real farm conditions: older Android hardware, outdoor use, intermittent connectivity, and crews who need simple tools that just work.

---

## Included Modules

| Module | What it does |
|--------|-------------|
| 🐐 GoatDesk | Livestock health logs — weight, vitals, medication, procedures, births, tasks |
| 📦 StockDesk | Farm inventory — restock, usage, running totals synced to farmOS |
| 🐦 BirdDesk | Poultry — bird roster, egg collection by nest box, feed, health, mortality |

Modules can be enabled or disabled independently. Each works standalone or alongside the others. The GoatDesk ↔ StockDesk bridge automatically deducts medication inventory when a med log is saved.

---

## Features (all modules)

- Offline-first — log anything without a connection; push to farmOS when ready
- High-contrast UI designed for outdoor use on older Android phones and tablets
- One farmOS connection shared across all modules
- Local-first sync queue — nothing goes to the network until you tap Sync
- Photos stored as device files; only IDs in localStorage (quota-safe)
- Universal Android back button support
- No farm-specific hardcoding — point it at any farmOS instance and go

---

## Requirements

- farmOS 2.x with JSON:API enabled
- OAuth2 credentials with `farm_manager` scope
- Android 6+ (tested on Samsung Galaxy devices, including older hardware)

---

## Installation

Download the latest APK from [Releases](https://github.com/ltlfarm/FarmDesk/releases) and sideload it onto your device.

> **Upgrading:** If the signing key ever changes between versions, Samsung devices will block the upgrade. Uninstall first. **Sync to farmOS before uninstalling** — local-only data will be lost.

---

## Setup

1. Open FarmDesk and tap **Settings → General → FarmOS Sync**
2. Enter your farmOS URL (e.g. `https://yourfarm.farmos.net`), username, and password
3. Enable the modules you want to use
4. Tap **Fetch** in each module to import your existing farmOS data
5. Start logging

---

## farmOS Integration Notes

- All write operations require `farm_manager` OAuth scope
- `farm_worker` scope returns 0 logs for non-owners — grant trusted workers Manager role in farmOS if needed
- Task completion is always a PATCH on the existing log — never a new POST
- Log timestamps are Unix integer strings
- Animal lists filter by `animal_type` term — set types in farmOS to keep mixed-species farms clean
- Inventory writes follow the farmOS sequence: POST quantity → POST log with `target_revision_id`

---

## Architecture

FarmDesk is a single HTML file (`farmdesk.html`) with all CSS and JS inline — no external dependencies, no build step beyond the APK wrapper. Each module is a self-contained section with a unique prefix (`sd_` for Stock, `bd_` for Birds). Adding a new module means adding its HTML section, CSS block, JS section, and one registry entry.

The APK is a thin Android WebView shell (`MainActivity.java`) that adds camera access, file storage, and JavaScript bridges.

---

## Suite Repositories

| App | Repo | Status |
|-----|------|--------|
| FarmDesk (unified) | `github.com/ltlfarm/FarmDesk` | ✅ Active |
| GoatDesk (standalone) | `github.com/ltlfarm/goatdesk` | ✅ Active |
| StockDesk (standalone) | `github.com/ltlfarm/stockdesk` | ✅ Active |
| BirdDesk (standalone) | `github.com/ltlfarm/birddesk` | ✅ Active |

APKs build automatically via GitHub Actions on every push to `main`.

---

## For Other Farms

Nothing in FarmDesk is hardcoded to any specific farm. Point it at your farmOS instance, fetch your animals and inventory, and it works. Animal types, inventory categories, and units all come from your farmOS taxonomy.

FarmDesk was built on a small off-grid homestead running a self-hosted Pi4 and older Samsung hardware. If it works there, it'll work anywhere.
