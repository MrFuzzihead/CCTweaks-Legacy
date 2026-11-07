# CCTweaks-Legacy — Agent Reference

This file is a machine-readable reference for AI coding agents working in this repository.
It documents the feature catalogue, bugfixes, mixin/AT/reflection/bytecode usage and the
complete runtime flow of the mod. Update this file whenever features or architecture change.

---

## Project Identity

| Property | Value |
|----------|-------|
| Mod ID | `cctweaks` |
| Root Package | `org.squiddev.cctweaks` |
| Minecraft | 1.7.10 (Forge 10.13.4.1614) |
| Hard dependency | ComputerCraft `[3.3.0,)` — the CC source lives in `dan200/` |
| CC runtime requirement | Java 17+ with **lwjgl3ify** — CC 3.x declares `required-after:lwjgl3ify` in its `mcmod.info` |
| Cobalt package | `dan200.computercraft.shadow.org.squiddev.cobalt.*` (CC 3.x shadows and relocates Cobalt 0.9.9) |
| **Mixins** | **None.** `usesMixins = false`; no `mixins.*.json`, no `@Mixin`, no `TargetMods`/`Mixins` enum, no UniMixins dependency |
| Access Transformers | **One file**: `src/main/resources/META-INF/CCTweaks_at.cfg`, wired via `accessTransformersFile = CCTweaks_at.cfg` (emits `FMLAT` in the jar manifest) |
| Bytecode / core mod | **None** |

**CCTweaks patches nothing.** It is a plain Forge mod built entirely on ComputerCraft's public
API. Everything it used to patch, replace or extend in CC (cable tile, cable block, cable
rendering, cable item, peripheral wrapper, turtle refuel) is either shipped by CC 3.x itself or
was deleted. OpenPeripheral is no longer required or touched.

---

## Feature Catalogue

| # | Feature | Key Source Files |
|---|---------|-----------------|
| 1 | **Computer Upgrade Item** — upgrades Normal computer → Advanced in-world (transfers state); Normal turtle → Advanced (costs 7 gold ingots) | `items/ItemComputerUpgrade.java`, `items/CraftingComputerUpgrade.java` |
| 2 | **Debug Wand** — prints tile/peripheral/peripheral-binding info in chat; right-clicking a computer injects Cobalt's `DebugLib` into its running Lua state | `items/ItemDebugger.java`, `blocks/debug/TileDebugPeripheral.java` |
| 3 | **Debug Block** — one debug peripheral tile (`meta 0`, `debugPeripheral`), gated by `debugItems` | `blocks/debug/BlockDebug.java` |
| 4 | **Tool Host Turtle Upgrades** — Normal (`Gripping`) and Advanced (`Manipulative`) variants; turtles hold and swing arbitrary tools as interactions | `turtle/TurtleUpgradeToolHost.java`, `turtle/TurtleUpgradeToolManipulator.java`, `turtle/ToolHostPlayer.java`, `turtle/ToolManipulatorPeripheral.java` |
| 5 | **Portal Gun Turtle Interaction** — registers the portal gun item as a swing/use turtle tool interaction | `integration/PortalGunIntegration.java` |
| 6 | **NEI Documentation** — language-file-backed docs rendered in NEI recipe pages | `integration/nei/*` |

### Removed Features (do not re-add without a CC gap to fill)

| Feature | Why removed |
|---------|-------------|
| Extended turtle fuel (EU/RF refuel) | CC ships IC2/RF handlers natively via `TurtleRefuelHandler` |
| Full-block wired modem (`TileNetworkedModem`) | CC ships `BlockWiredModemFull`/`TileWiredModemFull` (six-face modem hub) |
| `TileCable_Mixin`, `BlockCable_Mixin` | CC 3.3.0 supplies `isPeripheralAccessAllowed()`/`getAttachedPeripheralID()` and rescans on neighbour change itself |
| Multipart cable + modem (`PartCable`, `PartModem`, `PartModemWithCableIntermediate`, `PartWirelessBridge`, `RegisterBlockPart`, `MultipartIntegration`, `ItemPart`) | Deferred: CC plans its own multipart cables |
| Wireless bridge block, turtle upgrade, P++ pocket upgrade, `ItemDataCard`/`IDataCard` | Dropped with the network featureset; CC's ender modem covers computer↔computer wireless |
| CCTweaks network graph (`api/network/*`, `core/network/**` incl. controller/cable/modem/bridge, `NetworkAPI`, `NetworkRegistry`, `NetworkHelpers`) | Had no node producers left once cables, modems, multipart and the bridge were gone |
| Network visualiser + debug block metas 1/2 (`core/visualiser/*`, `RenderNetworkOverlay`, `TileDebugNetworkedPeripheral`, `TileDebugNode`) | Debug tooling for the removed graph |
| OpenPeripheral integration (`AdapterNetworkedInventory`, `OpenPeripheralIntegration`, 5 OP mixins) | Its only purpose was `ARG_NETWORK`; CC's `InventoryPeripheral` now offers `pushItems`/`pullItems` on every `IInventory` |
| `PeripheralAPI_Mixin` + `PeripheralWrapper` accessors | Replaced by CC's own `IPeripheral.attach`/`detach` lifecycle |
| All CC accessor/invoker mixins | CC 3.3.0 widened/renamed every member they exposed |

---

## Bugfixes Made

> None remain. Every fix CCTweaks once carried (cable tile rewrite, `isCable` broadening,
> neighbour-change type capture, `PeripheralWrapper.detach` race, turtle refuel routing) lived in
> a mixin that CC 3.x has since made unnecessary.

---

## Mixin / AT / Reflection Inventory

| Mechanism | Status |
|-----------|--------|
| Mixins | **None.** Deleted: `mixins/` package, `mixins.cctweaks.json`, `mixins.cctweaks.late.json`, UniMixins `LateMixinsLoader`, `Mixins` enum, `TargetMods` enum. `usesMixins = false` in `gradle.properties`. |
| Access Transformers | **One**, load-bearing: `META-INF/CCTweaks_at.cfg` widens `ItemInWorldManager.field_73094_o` (`durabilityRemainingOnBlock`) to public for `ToolHostPlayer`. `gradle.properties` sets `accessTransformersFile = CCTweaks_at.cfg` so FML emits `FMLAT` in the jar manifest — **do not delete the file or blank that property**: `ToolHostPlayer` stops compiling, and (if the property is blanked while the file remains) production builds ship without `FMLAT` and fail at runtime with `IllegalAccessError`. |
| Reflection | **None.** Zero `import java.lang.reflect` in the tree. |
| Bytecode / core mod | **None.** |

If a future feature genuinely needs to patch CC, re-enable `usesMixins`, re-add the GTNH
`MixinBuilder` scaffolding, and follow `IMixins`/`MixinBuilder` conventions.

---

## ComputerCraft 3.x Compatibility

CC 3.x is built with `jvmDowngrader` (base classes are Java 8 class file version 52, no
`META-INF/versions` overlay), so CCTweaks still builds with `enableModernJavaSyntax = jabel`.
Three things differ from 2.x and must be preserved:

1. **Cobalt is relocated and shadowed.** `org.squiddev.cobalt.*` does not exist on the classpath.
   Import `dan200.computercraft.shadow.org.squiddev.cobalt.*`. Cobalt is now 0.9.9, where
   `DebugLib` has a private constructor and is installed with `DebugLib.add(state)`
   (`ItemDebugger`).
2. **Blocking Lua is gone.** `ILuaContext` has `executeMainThreadTask` (returning `MethodResult`)
   and `issueMainThreadTask` only — no `pullEvent`/`pullEventRaw`/`yield`. A method that waits
   returns a `MethodResult`; the Lua bridge suspends the coroutine.
3. **Delegated calls return `Object`.** `ArgumentDelegator.delegatePeripheral`/
   `delegateLuaObject` return `Object[]` *or* a `MethodResult`.

Because `callMethod` still declares `Object[]`, a suspension is carried in the single-element
sentinel array `new Object[] { methodResult }`. `core/utils/LuaResults.wrap(Object)` does that
(mirrors CC's own `r instanceof Object[] ? r : new Object[] { r }` from
`PeripheralAPI.callMethod`). Used by `ToolManipulatorPeripheral`.

Facts worth remembering about CC's peripheral API:

* `IComputerAccess.getAvailablePeripherals()` is keyed by **side name** (`Computer.s_sideNames`),
  and `getAttachmentName()` returns that same side string — it throws if the computer is detached.
* `DefaultPeripheralProvider` wraps any `IInventory` tile in `InventoryPeripheral`
  (`size`/`list`/`getItemDetail`/`getItemLimit`/`pushItems`/`pullItems`), so an inventory's
  peripheral is `IPeripheralTargeted` at the tile.

### Development environment

* Use `./gradlew runServer25` / `runClient25` (or the plain `runServer`/`runClient`, which
  `build.gradle.kts` aliases onto them). The Java-8 run tasks put no lwjgl3ify on the classpath and
  die at `MissingModsException: lwjgl3ify`.
* `dependencies.gradle` after the audit:
  * `api` — `Computercraft-Legacy` only (CC internals are used directly: `Computer`, `CobaltMachine`,
    `TileComputerBase`, `LuaEnvironment`, turtle/pocket factories).
  * `implementation` — `CodeChickenCore` (`codechicken.lib.gui.GuiDraw` in the NEI handler) and
    `NotEnoughItems` (`codechicken.nei.*`).
  * `devOnlyNonPublishable` — `PortalGun` + `iChunUtil` (PortalGun's hard dep); dev-only because the
    integration is `@Optional`.
  * `testCompile` — JUnit 4.
  Removed with their features: ForgeMultipart, Peripherals++, OpenPeripheralCore/Addons,
  OpenModsLib, CollectiveFramework (only P++ needed it).

---

## Module System

All registerable content implements `IModule` (or `IClientModule`).
`Registry` holds a static `Set<IModule>` and sequences `preInit → init → postInit`.
Client-only modules are wrapped in `RegisterWrapperClient` (uses `@SideOnly(CLIENT)` on `clientInit`).

```
Registry (static initialiser — runs when CCTweaks.preInit is called)
  addModule(ItemComputerUpgrade)    items/ItemComputerUpgrade.java
  addModule(ItemDebugger)           items/ItemDebugger.java
  addModule(ItemToolHost)           items/ItemToolHost.java
  addModule(BlockDebug)             blocks/debug/BlockDebug.java
  addModule(PeripheralHostProvider) core/peripheral/PeripheralHostProvider.java
  addModule(DefaultTurtleProviders) core/turtle/DefaultTurtleProviders.java
  addModule(PortalGunIntegration)   integration/PortalGunIntegration.java
```

---

## Runtime Flow

```
JVM Startup
  └─ No mixin subsystem, no core mod. One AT (`CCTweaks_at.cfg`). CCTweaks is otherwise an ordinary Forge mod.

FMLPreInitializationEvent  →  CCTweaks.preInit()
  ├─ Load config file → populate CCTweaks.* static config fields
  ├─ Register FmlEvents on FMLCommonHandler bus
  │    ServerTickEvent → DelayedTasks.update() + drain serverQueue
  │    ClientTickEvent → drain clientQueue
  ├─ Register McEvents on MinecraftForge.EVENT_BUS
  ├─ Create SimpleNetworkWrapper channel "cctweaks"
  └─ Registry.preInit()
       └─ IModule.preInit() for each module
            • Items/blocks registered with GameRegistry

FMLInitializationEvent  →  CCTweaks.init()
  └─ Registry.init()
       └─ IModule.init() for each module
            • Crafting recipes added
            • ComputerCraft.registerTurtleUpgrade(...) (tool host upgrades)
            • ComputerCraftAPI.registerPeripheralProvider(PeripheralHostProvider)

FMLPostInitializationEvent  →  CCTweaks.postInit()
  └─ Registry.postInit() (no module currently implements it)

FMLServerStartedEvent / FMLServerStoppedEvent
  └─ DelayedTasks.reset()

─── Server Runtime ──────────────────────────────────────────────────

ServerTickEvent (Phase.START)
  ├─ DelayedTasks.update()            ← CC Lua task scheduling
  └─ Drain FmlEvents.serverQueue      ← deferred Runnables

─── Client Runtime ──────────────────────────────────────────────────

ClientTickEvent (Phase.START)
  └─ Drain FmlEvents.clientQueue
```

---

## API Surface (`api/` package)

| Interface | Role |
|-----------|------|
| `IPeripheralHelpers` | `getBasePeripheral` / `getTarget` unwrapping of peripheral proxies and `IPeripheralTargeted` |
| `IPeripheralProxy` | Peripheral that wraps a base peripheral (`getBasePeripheral`) |
| `IPeripheralHost` | TileEntity that supplies its own peripherals (`PeripheralHostProvider` registers a CC provider for it) |
| `IWorldPosition` | x/y/z + world position (implemented by `TileBase`) |
| `ITurtleRegistry` / `ITurtleInteraction` / `AbstractTurtleInteraction` | Custom turtle interactions (used for buckets, tool host, portal gun) |
| `ICCTweaksAPI` | `CCTweaksAPI.instance()` entry point: `turtleRegistry()`, `peripheralHelpers()`, `luaEnvironment()` |

`ICCTweaksAPI` no longer exposes `networkRegistry()`/`networkHelpers()`.

---

## Test Coverage

| Test Class | What Is Tested |
|------------|----------------|
| `core/peripheral/PeripheralHelpersTest.java` | `PeripheralHelpers` unwrapping: base-peripheral chains, targeted peripherals, proxy + target combinations, null-target fallbacks |

Removed with their subjects: `NetworkTest`, `PacketTest`, `ComputerNetworkAccessTest`,
`core/network/mock`.

**Gap:** in-world behaviour (computer upgrade, tool host, debug wand) has no automated coverage
and needs integration testing in a dev client.

---

## Key Config Flags (CCTweaks.* statics)

| Flag | Default | Effect |
|------|---------|--------|
| `computerUpgradeEnabled` | true | Enable Computer Upgrade item |
| `computerUpgradeCrafting` | true | Enable its crafting recipe |
| `debugWandEnabled` | true | Enable Debug Wand item |
| `debugItems` | false | Enable the debug block |
| `thEnabled` | true | Enable Tool Host turtle upgrade |
| `advanced` | true | Enable the Advanced Tool Manipulator variant |
| `thCrafting` | true | Enable Tool Host crafting |
| `advancedUpgradeId` | 333 | Advanced tool manipulator upgrade id |

Removed with their features: `cbMultipart`, `openPeripheralInventories`, `wbEnabled`,
`wbCrafting`, `turtleEnabled`, `pocketEnabled`, `pocketId`, `fullBlockModemCrafting`,
`extendedControllerValidation`, `euRefuelAmount`, `fluxRefuelAmount`.

---

## Known Fragile Points

1. **No mixins, no reflection, one AT.** A CC or Forge update can only break CCTweaks through
   normal API changes (peripheral API, turtle upgrade API, `ItemInWorldManager`, Cobalt internals
   used by `ItemDebugger`) plus the single AT below.
2. **The AT is the only remaining bytecode-level coupling** — a name/visibility change to
   `ItemInWorldManager.durabilityRemainingOnBlock` breaks `ToolHostPlayer` at compile time (the AT
   will simply stop matching).

3. **`ItemDebugger` reaches into Cobalt** (`CobaltMachine.getLuaState()` → `DebugLib.add(state)`).
   That is the one remaining "internal" dependency: if CC changes its Lua machine or Cobalt's
   `DebugLib` visibility again, only the debug wand's injector breaks (guarded by
   `instanceof`/try-catch, returns `false`).
4. **`ToolHostPlayer` emulates `ItemInWorldManager`** (including
   `durabilityRemainingOnBlock`), so a Forge/vanilla mining-behaviour change affects tool host dig
   speed and durability.
5. **No in-game config GUI.** `guiFactory` was blanked: it pointed at a non-existent
   `client.gui.GuiConfigFactory`. Config is edited via `config/cctweaks.cfg` directly; the
   `gui.config.cctweaks.*` lang keys are kept for a future factory.

---

## Docs

| File | Contents |
|------|----------|
| `docs/remove-reflection.md` | Completed migration plan: 13 reflection sites → mixins (mixins since deleted) |
| `docs/remove-luaj.md` | Completed migration plan: LuaJ → Cobalt in `ItemDebugger` |
| `docs/migrate-mixins-to-source.md` | Historical: move late-phase mixins into native CC / OP source — **superseded**, all mixins are now deleted |
| `AGENTS.md` | **This file** — comprehensive agent reference |
