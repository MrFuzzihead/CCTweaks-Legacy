# CCTweaks-Legacy

[![](https://jitpack.io/v/MrFuzzihead/CCTweaks-Legacy.svg)](https://jitpack.io/#MrFuzzihead/CCTweaks-Legacy)
[![Build status](https://github.com/MrFuzzihead/CCTweaks-Legacy/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/MrFuzzihead/CCTweaks-Legacy/actions/workflows/build-and-test.yml)
[![Latest release](https://img.shields.io/github/v/release/MrFuzzihead/CCTweaks-Legacy?include_prereleases&sort=semver)](https://github.com/MrFuzzihead/CCTweaks-Legacy/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![ComputerCraft](https://img.shields.io/badge/ComputerCraft-3.3.0-2ea44f)](https://github.com/MrFuzzihead/Computercraft-Legacy)

Miscellaneous tweaks and extensions for ComputerCraft on Minecraft 1.7.10, maintained for GTNH.

## What this is

This repository is a **port/continuation of [CCTweaks](https://github.com/SquidDev-CC/CCTweaks)**,
the SquidDev-maintained add-on for ComputerCraft, targeting Minecraft 1.7.10 and the
[GTNH](https://www.gtnewhorizons.com/) modpack. It is built against
[Computercraft-Legacy](https://github.com/MrFuzzihead/Computercraft-Legacy) 3.3.0.

It is a much smaller mod than it was. As ComputerCraft 3.x shipped the things CCTweaks used to
patch or provide — the full-block wired modem, peripheral `attach`/`detach` lifecycle, inventory
`pushItems`/`pullItems`, extended turtle refueling — CCTweaks' network graph, mixins and
OpenPeripheral integration were removed (see the "Removed features" note under
[Features](#features)). What remains is built **entirely on ComputerCraft's public API**: no
mixins, no core mod, no bytecode manipulation. The single exception is one access transformer
that widens `ItemInWorldManager.durabilityRemainingOnBlock` to public for the Tool Host turtle
(`META-INF/CCTweaks_at.cfg`).

See [AGENTS.md](AGENTS.md) for the full agent/developer reference, including the removed-feature
record and why each was dropped.

## Features

- **Computer Upgrade Item** — upgrades a Normal computer to Advanced in place (ID, label and
  on-state are kept; the running program restarts); upgrades a Normal turtle to Advanced,
  preserving its brain, inventory, fuel, colour and overlays (costs 7 gold ingots).
- **Debug Wand** — prints tile/peripheral/peripheral-binding info in chat, and injects Cobalt's
  `DebugLib` into a running computer's Lua state. It is reused rather than consumed.
- **Debug Block** — a debug peripheral tile for testing peripheral bindings.
- **Tool Host Turtle Upgrades** — Normal (`Gripping`) and Advanced (`Manipulative`) variants;
  turtles can hold and swing arbitrary tools as interactions.
- **Portal Gun Turtle Interaction** — registers the portal gun item as a swing/use turtle tool.
- **NEI Documentation** — language-file-backed documentation rendered in Not Enough Items
  recipe pages.

Removed features (do not re-add without a ComputerCraft gap to fill): the CCTweaks wired network
graph and all its nodes (full-block wired modem, wireless bridge block/turtle/pocket upgrades,
multipart cables), network visualizer, extended EU/RF turtle refueling, and the OpenPeripheral
integration — ComputerCraft 3.x ships equivalents for all of these.

## Requirements

| Dependency       | Version                                  |
|------------------|------------------------------------------|
| Minecraft        | 1.7.10                                   |
| Minecraft Forge  | 10.13.4.1614                             |
| ComputerCraft    | [3.3.0, ) (`Computercraft-Legacy`)       |
| lwjgl3ify        | Required by ComputerCraft 3.x (Java 17+) |
| Not Enough Items | Optional (NEI documentation)             |
| PortalGun        | Optional (Portal Gun turtle interaction) |

## Compiling

Everything is handled by the `com.gtnewhorizons.gtnhconvention` plugin. Load the project in
IntelliJ and run `./gradlew build`, or use the wrapper from the command line:

```
./gradlew build          # compile, test, format check
./gradlew test           # tests only
./gradlew runServer      # dev server (Java 17+; ComputerCraft 3.x requires lwjgl3ify)
./gradlew spotlessApply  # apply formatting
```

The Gradle daemon is pinned to Java 25 (see `.java-version`); the mod itself targets Java 8
bytecode (via Jabel). The output jar is placed in `build/libs/`. `./gradlew test` runs the
headless JUnit 5 suite; `./gradlew runServer25` is the server smoke test.

## Configuration

All options are in `config/cctweaks.cfg`, generated on first launch. See
[AGENTS.md](AGENTS.md) for the current key list.

## Contributing

Bug reports and pull requests are welcome. If you find a bug that appears to relate to
ComputerCraft itself, please report it in the
[Computercraft-Legacy](https://github.com/MrFuzzihead/Computercraft-Legacy) repository rather
than here — CCTweaks is now a thin layer on ComputerCraft's public API and is unlikely to be
the cause.

Please read [AGENTS.md](AGENTS.md) before making structural changes.

## License

**MIT License** — see [LICENSE](LICENSE), inherited verbatim from upstream
[CCTweaks](https://github.com/SquidDev-CC/CCTweaks) (Copyright (c) 2015-2017 SquidDev).
ComputerCraft itself is distributed under the ComputerCraft Public License; see
[Computercraft-Legacy](https://github.com/MrFuzzihead/Computercraft-Legacy) for its terms.

## Credit

CCTweaks was created by [SquidDev](https://github.com/SquidDev). ComputerCraft was created by
[Daniel "dan200" Ratcliffe](https://github.com/dan200). This port is maintained by
[MrFuzzihead](https://github.com/MrFuzzihead) for Minecraft 1.7.10 / GTNH.
