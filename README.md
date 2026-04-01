# CCTweaks-Legacy [![JitPack](https://jitpack.io/v/MrFuzzihead/CCTweaks-Legacy.svg)](https://jitpack.io/#MrFuzzihead/CCTweaks-Legacy)

Miscellaneous changes and extensions for ComputerCraft on Minecraft 1.7.10.

This is a port/continuation of [CCTweaks](https://github.com/SquidDev-CC/CCTweaks) targeting
the 1.7.10 GTNH modpack. It uses [GTNH UniMixins](https://github.com/GTNewHorizons/UniMixins)
instead of a core mod to patch ComputerCraft and OpenPeripheral at runtime.

## Features

### Networking
- **Wired Network API** — generic `INetworkNode` graph replaces CC's internal cable topology,
  allowing any block to join a wired network
- **Wireless Bridge** — links two wired networks wirelessly via UUID/integer bindings; persisted
  on a Data Card item; also available as a turtle upgrade and a pocket-computer upgrade
  (requires Peripherals++)
- **Full-block Wired Modem** — a full-cube directional modem that connects to peripherals on
  all six sides
- **Network Visualiser** — overlay showing the live node/link graph of the network under the
  debug wand cursor
- **ForgeMultipart Integration** — cable and modem available as multipart pieces (requires
  ChickenBones' ForgeMultipart)

### Turtles
- **Tool Host Upgrade** — Normal and Advanced variants; turtles can hold and swing any tool
- **Extended Refuel** — IC2 EU items (`IElectricItem`) and CoFH RF items
  (`IEnergyContainerItem`) accepted as turtle fuel
- **Portal Gun Integration** — portal gun registered as a swing/use turtle tool

### Computers
- **Computer Upgrade Item** — upgrades a Normal computer to Advanced in-world, transferring all
  state
- **Debug Wand** — shows tile/peripheral/network info in chat, injects Cobalt `DebugLib` into
  running computers

### Integration
- **OpenPeripheral** — injects `INetworkAccess` into every OP method call so peripherals can
  query and transmit on the wired network; proxies attach/detach events through
  `WrappedPeripheral`; registers a networked-inventory adapter
- **Peripherals++** — registers a Wireless Bridge pocket upgrade

## Requirements

| Dependency | Version |
|------------|---------|
| Minecraft | 1.7.10 |
| Minecraft Forge | 10.13.4.1614 |
| ComputerCraft | 2.0.0+ (GTNH fork) |
| GTNH UniMixins | 0.8.5-GTNH+ |
| ForgeMultipart | Optional |
| OpenPeripheral | Optional |
| Peripherals++ | Optional |
| IndustrialCraft 2 | Optional (EU refuel) |
| CoFH Core | Optional (RF refuel) |

## Building

This project uses the [GTNH Convention Gradle plugin](https://github.com/GTNewHorizons/GTNHGradle).
Java 17 is recommended (the build targets JVM 8 via Jabel).

```bash
# Clone
git clone https://github.com/<your-fork>/CCTweaks-Legacy
cd CCTweaks-Legacy

# Build (downloads all dependencies automatically)
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows

# Run the game client for testing
./gradlew runClient

# Run the game server for testing
./gradlew runServer
```

The output jar is placed in `build/libs/`.

## Configuration

All options are in `config/cctweaks.cfg`, generated on first launch.

| Category | Key | Default | Description |
|----------|-----|---------|-------------|
| `computer` | `computerUpgradeEnabled` | `true` | Enable Computer Upgrade item |
| `computer` | `computerUpgradeCrafting` | `true` | Enable crafting the upgrade |
| `computer` | `debugWandEnabled` | `true` | Enable Debug Wand item |
| `integration` | `cbMultipart` | `true` | Enable ForgeMultipart integration |
| `integration` | `openPeripheralInventories` | `true` | Register OP inventory adapter |
| `network` | `fullBlockModemCrafting` | `true` | Enable crafting full-block modems |
| `network.wirelessbridge` | `wbEnabled` | `true` | Enable Wireless Bridge block |
| `network.wirelessbridge` | `turtleEnabled` | `true` | Enable Wireless Bridge turtle upgrade |
| `network.wirelessbridge` | `pocketEnabled` | `true` | Enable Wireless Bridge pocket upgrade |
| `turtle` | `euRefuelAmount` | `25` | EU consumed per turtle fuel unit |
| `turtle` | `fluxRefuelAmount` | `100` | RF consumed per turtle fuel unit |
| `turtle.toolhost` | `thEnabled` | `true` | Enable Tool Host upgrade |
| `turtle.toolhost` | `advanced` | `true` | Enable Advanced Tool Host upgrade |

## API

CCTweaks exposes an API for other mods to register custom network nodes and turtle fuel sources:

```java
ICCTweaksAPI api = CCTweaksAPI.instance();

// Register a custom wired network node provider
api.networkRegistry().addNodeProvider(myProvider);

// Register a custom turtle fuel source
api.fuelRegistry().addProvider(myFuelProvider);
```

The API artifact is published alongside the main jar. See the `api/` source package for all
interfaces.

## Contributing

Bug reports and pull requests are welcome. If you find a bug that appears to relate to
ComputerCraft itself, please report it **here** rather than upstream — CCTweaks patches several
CC internals and is the more likely cause.

Please read `AGENTS.md` for a complete map of all mixin targets, the module system, and the
runtime flow before making structural changes.
