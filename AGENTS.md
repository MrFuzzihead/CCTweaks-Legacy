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
| 1 | **Computer Upgrade Item** — upgrades Normal computer → Advanced in place (metadata family bit; ID, label and on-state kept; the running program restarts); Normal turtle → Advanced, transferring the brain/inventory/fuel/overlays and recreating the Advanced ServerComputer (costs 7 gold ingots) | `items/ItemComputerUpgrade.java`, `items/CraftingComputerUpgrade.java` |
| 2 | **Debug Wand** — prints tile/peripheral/peripheral-binding info in chat; shift-right-clicking a **running** computer injects Cobalt's `DebugLib` into its Lua state and reports success or the exact reason it failed in chat. Never consumed | `items/ItemDebugger.java`, `blocks/debug/TileDebugPeripheral.java` |
| 3 | **Debug Block** — one debug peripheral tile (`meta 0`, `debugPeripheral`), gated by `debugItems` | `blocks/debug/BlockDebug.java` |
| 4 | **Tool Host Turtle Upgrades** — Normal (`Gripping`) and Advanced (`Manipulative`) variants; turtles hold and swing arbitrary tools as interactions. The manipulator peripheral's `use(duration)` really waits `duration` ticks; `canUse` mirrors `use` | `turtle/TurtleUpgradeToolHost.java`, `turtle/TurtleUpgradeToolManipulator.java`, `turtle/ToolHostPlayer.java`, `turtle/ToolManipulatorPeripheral.java` |
| 5 | **Portal Gun Turtle Interaction** — registers the portal gun item as a swing/use turtle tool interaction | `integration/PortalGunIntegration.java` |
| 6 | **NEI Documentation** — `assets/cctweaks/docs/<lang>/{item,tile}/<name>.txt` rendered in NEI usage/recipe pages (`ResourceDocProvider`; `${**}` bold, `${*}` italic, `***` page break). `LanguageDocProvider` `.information.N` keys are the fallback | `integration/nei/*` |
| 7 | **In-game config GUI** — every option in Mods list → Config; edits Forge's live `Configuration`, then saves `config/cctweaks.cfg` and re-reads the statics on close | `client/gui/GuiConfigFactory.java`, `client/gui/GuiCCTweaksConfig.java`, `CCTweaks.getConfiguration/saveConfiguration` |
| 8 | **`/cctweaks` diagnostics** — operator command (level 2): `modules`, `config`, `selftest`. `selftest` runs the in-world checks (see Test Coverage) near the world spawn and restores the area; the same suite runs at server start when `-Dcctweaks.selftest=true` (dev harness/CI) | `commands/CommandCCTweaks.java`, `selfTest/InWorldSelfTest.java`, `selfTest/SelfTestTurtle.java` |

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

The original mixin-era fixes (cable tile rewrite, `isCable` broadening, neighbour-change type
capture, `PeripheralWrapper.detach` race, turtle refuel routing) are gone with the mixins. The
current tree still carries these ordinary-source fixes:

- Tool Host/Manipulator: entity ray direction, held-item attribute modifiers, drop capture for
  instant-break blocks, `isBlockEditable` and `turtleDisabledActions` checks, consumer cleanup in
  `finally`, world-change cache invalidation, side-aware peripheral equality.
- Upgrade items: Advanced turtle/computer `ServerComputer` family/colour recreation, turtle
  overlay/hat and pocket upgrade NBT preservation, unsupported-`IComputerItem` rejection,
  protection checks and rollback on failed block swaps.
- Debug wand: no longer consumed, uses the real player range check, logs injection failures.
- Events/lifecycle: cancelled `LivingDropsEvent` is respected; `McEvents`/`FmlEvents` reset on
  server stop; `TileBase` destroys exactly once; `PeripheralHelpers` stops on proxy cycles.

---

## Mixin / AT / Reflection Inventory

| Mechanism | Status |
|-----------|--------|
| Mixins | **None.** Deleted: `mixins/` package, `mixins.cctweaks.json`, `mixins.cctweaks.late.json`, UniMixins `LateMixinsLoader`, `Mixins` enum, `TargetMods` enum. `usesMixins = false` in `gradle.properties`. |
| Access Transformers | **One**, load-bearing: `META-INF/CCTweaks_at.cfg` widens `ItemInWorldManager.field_73094_o` (`durabilityRemainingOnBlock`) to public for `ToolHostPlayer`. `gradle.properties` sets `accessTransformersFile = CCTweaks_at.cfg` so FML emits `FMLAT` in the jar manifest — **do not delete the file or blank that property**: `ToolHostPlayer` stops compiling, and (if the property is blanked while the file remains) production builds ship without `FMLAT` and fail at runtime with `IllegalAccessError`. |
| Reflection | **No `java.lang.reflect` imports.** `CCTweaksAPI` still loads `core.API` through `Class.forName`, as it must to keep the API jar optional. |
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

All registerable content implements `IModule` (the `IClientModule`/`RegisterWrapperClient` path was
deleted - there are no client-only modules). `ModuleRegistry` sequences `preInit → init → postInit`
on a `LinkedHashSet` (registration order, duplicate-safe, snapshot per phase so a module that
registers another module cannot break the current pass). `Registry` is the static facade that owns
that instance and registers the built-in modules in its initialiser.

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
  ├─ Load config file → populate CCTweaks.* static config fields, keeping the Configuration
  │    object in CCTweaks.configuration so the client config GUI can save and re-read it
  ├─ Register FmlEvents on FMLCommonHandler bus
  │    ServerTickEvent → DelayedTasks.update() + drain serverQueue
  ├─ Register McEvents on MinecraftForge.EVENT_BUS
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

FMLServerStartingEvent  →  CCTweaks.onServerStarting()
  └─ register CommandCCTweaks  (/cctweaks modules|config|selftest)

FMLServerStartedEvent   →  CCTweaks.onServerStart()
  ├─ FmlEvents.reset() / McEvents.reset() / DelayedTasks.reset()
  └─ if -Dcctweaks.selftest=true → InWorldSelfTest.atSpawn(overworld).run()

FMLServerStoppedEvent   →  CCTweaks.onServerStopped()
  └─ FmlEvents.reset() / McEvents.reset() / DelayedTasks.reset()

─── Server Runtime ──────────────────────────────────────────────────

ServerTickEvent (Phase.START)
  ├─ DelayedTasks.update()            ← CC Lua task scheduling
  └─ Drain FmlEvents.serverQueue      ← deferred Runnables

─── Client Runtime ──────────────────────────────────────────────────

No client-side tick work: the client queue/handler was removed with the network featureset.
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

| Suite | What Is Tested |
|-------|----------------|
| `ApiSurfaceTest` | Public API signatures and `@Mod` dependency string |
| `BuildArtifactTest` | AT resource is shipped; `dependencies.gradle` CC version matches `CCTweaks.DEPENDENCIES` |
| `CCTweaksConfigTest` | Tool Host config clamping/ID validation |
| `CCTweaksConfigIoTest` | `readConfig` defaults/overrides/validation, one language key per option (and each key is translated in `en_US.lang`), `saveConfiguration` before pre-init |
| `core.FmlEventsTest` | Tick scheduling, exception isolation, reset, CC `DelayedTasks` tick, missing-mapping resolution |
| `core.McEventsTest` | Block/entity drop consumers, drop chance, cancellation, reset |
| `core.peripheral.PeripheralHelpersTest` | Chains, targets, nulls, proxy cycles |
| `core.peripheral.PeripheralProxyTest` | Lazy creation, attach replay, detach, default type, equality |
| `core.turtle.TurtleRegistryTest` | Item-before-generic dispatch, first-result wins, null handling, registration guards |
| `core.utils.InventoryUtilsTest` / `LuaResultsTest` / `WorldPositionTest` | Extraction/counting, Lua suspension wrapping, positions |
| `blocks.TileBaseLifecycleTest` | `create`/`destroy` exactly once across validate/invalidate/chunk unload |
| `blocks.debug.TileDebugPeripheralTest` | Per-side cache, bounds, `getSide`, computer tracking |
| `items.CraftingComputerUpgradeTest` | Matching, gold-slot semantics, unsupported items, pocket NBT preservation |
| `items.ItemComputerActionTest` | Client always defers to the server (packet flow), consumption hook, debug wand non-consumption |
| `items.ItemDebuggerTest` | Every wand failure path reports a distinct chat message; operator gate on/off; generic tile info |
| `items.ItemToolHostTest` | Creative subtype gating |
| `items.UpgradeItemTest` | In-place computer metadata/family recreation, turtle transfer/recreation, failed-swap rollback |
| `turtle.ToolManipulatorPeripheralTest` | Argument parsing, disabled actions, result shape, method table, side equality, hit-side resolution, `use(duration)` wait chaining, `canUse` item heuristics |
| `turtle.ToolManipulatorFindHitTest` | Entity ray ends at the target, not beyond it |
| `turtle.TurtleUpgradeMetadataTest` | Upgrade ids come from the config and stay distinct, both are tool upgrades, crafting item metadata, only the manipulator is a peripheral, adjectives are translated in `en_US.lang` |
| `turtle.ToolManipulatorActionsTest` | The full `use`/`swing`/`canUse`/`canSwing` decision tree through the `IToolHostPlayer` + `ToolHitResolver` seams: registered interaction first, entity interact, block activation, place gating, item use in air, disabled actions, animation only on success |
| `core.registry.ModuleRegistryTest` | Phase ordering, `canLoad` gating, duplicate registration, late registration catch-up, out-of-order/duplicate phase errors, immutable snapshot |
| `harness.HeadlessTurtleHarnessTest` | Stub turtle + stub computer drive the real registry with no server/world |
| `api.CCTweaksAPILoaderTest` | API instance stability; missing/wrong-type/constructor-less/throwing implementations raise `CoreNotFoundException` |
| `client.gui.GuiConfigFactoryTest` | `@Mod(guiFactory = ...)` names a real factory returning a `GuiConfig` subclass; the screen fields/methods it relies on exist (the class is loaded but never initialised, since `GuiScreen` needs LWJGL) |
| `core.peripheral.PeripheralHostProviderTest` | `IPeripheralHost` tiles are unwrapped per side; non-hosts, empty positions and declining hosts return null |
| `integration.nei.ResourceDocProviderTest` | Doc path derivation, page splitting, markup conversion, shipped resources readable |
| `integration.nei.DescriptionProvidersTest` | Provider-chain ordering (resources before language keys), first non-empty wins, the `information.N` loop stopping at the first missing key and its stack→item fallback, `
` → paragraph conversion, NEI config identity |
| `integration.PortalGunInteractionTest` | `swing` fires blue and `use` fires orange regardless of which gun is held (the left/right click regression), non-portal-gun and null stacks ignored, `canSwing`/`canUse` accept subclasses |
| `ConfigElementsTest` | Every category `readConfig` reads appears in the config GUI (so no option can be invisible), category/property display order, title key, null configuration |
| `blocks.BlockBaseTest` | Tile lookup tolerates foreign/missing tiles, activation and neighbour hooks, client-side neighbour suppression, destroy-once on break |
| `blocks.debug.BlockDebugTest` | Meta 0 is the peripheral, other metas unsupported, drop metadata, unlocalised names, creative subtypes, `debugItems` gating, `ItemMultiBlock` contract |
| `commands.CommandCCTweaksTest` | Command name/permission/usage, sub-command tab completion, help/modules/config output, unknown sub-command, world-less sender |
| `selfTest.SelfTestTurtleTest` | The runtime `ITurtleAccess` stub: slot IR, `count`, defensive position, direction/teleport, inert fuel/upgrades |

`./gradlew test` runs the JUnit 5 suite headlessly (209 tests / 36 classes at the time of writing).
`./gradlew build` also runs Spotless, Checkstyle, `verifyTestSuiteExecuted` (fails the build if the
test task produced zero XML results - a refactor that moves tests out of a detected package would
otherwise pass silently) and `jacocoTestReport`, which writes
`build/reports/jacoco/test/html/index.html` (~62% lines / ~61% branches at the time of writing). The
`selfTest` package is excluded from the report: it only runs inside the game JVM, which JaCoCo does
not instrument, and its results are asserted by the harness instead.

`PortalGun` is `devOnlyNonPublishable` (optional at runtime, needed at compile time for
`instanceof ItemPortalGun`), so `dependencies.gradle` also adds it to `testImplementation` +
`testRuntimeOnly` for `PortalGunInteractionTest`. `iChunUtil` is PortalGun's hard dependency.

Three headless-testing traps worth knowing before writing another test:

* `net.minecraft.init.Items`/`Blocks` are **not bootstrapped** in a plain JUnit JVM - every field is
  null, so `new ItemStack(Items.gold_ingot)` makes a stack whose `getItem()` is null and
  `getItemDamage()` NPEs. Construct items directly (`new Item()`), or assert only constructor-set
  fields such as `stackSize`.
* `World.isRemote` is a **field**, not a method: stub it by assignment (`world.isRemote = true`),
  never `when(world.isRemote)` - that silently becomes `when(false)`.
* Mockito forbids nested stubbing: build and stub helper mocks before passing them to
  `when(...).thenReturn(...)`.

`ToolManipulatorPeripheral` talks to its fake player only through `IToolHostPlayer` and finds hits
through `ToolHitResolver`; `ToolHostPlayer` and `ToolHitResolver.DEFAULT` are the in-game
implementations, and `testsupport.StubToolHostPlayer` plus a fixed-hit resolver let the action
decision tree run with no `WorldServer`.

`bash scripts/headless-smoke.sh` is the automated server harness (also run by the
`headless-smoke` job in `.github/workflows/build-and-test.yml`; it creates a dev-server `eula.txt`
if a fresh checkout has none). It runs
`./gradlew runServer25 -PsmokeTest`, which hands the dev server a stdin pipe so it can be told to
`stop` (a few seconds after FML logs `Done`, or after `SMOKE_STOP_AFTER` seconds), then asserts on
`run/server/logs/latest.log` + `fml-server-latest.log`: CCTweaks loaded, no CCTweaks
ERRORs/exceptions, no unresolved 2.x mappings, no mod-sorting failures, clean shutdown. It catches
runtime-only breakage (the AT stopped matching, `@Mod`/dependency change, a CC API rename).

By default the script also passes `-PselfTest`, which sets `-Dcctweaks.selftest=true` on the run
task; `CCTweaks.onServerStart` then runs `selfTest.InWorldSelfTest.atSpawn(world)` and logs one
`CCTWEAKS-SELFTEST PASS|FAIL <name>` line per check plus a `RESULT n passed, m failed` summary. The
script fails the run on any `FAIL` line or a missing `RESULT`. `SMOKE_SELFTEST=0` disables it.

What the self test covers (and nothing else does, because it needs a live `WorldServer`):

* `ToolHostPlayer` load/unload inventory round-trip (including overflow storage) and held-item
  attribute modifiers being applied and removed;
* `dig` on stone with a real `ItemInWorldManager` - block broken, cobblestone ending up in the
  turtle's inventory through the `McEvents` drop consumer;
* `dig` refusals: bedrock, air, and a block denied by an `ITurtlePermissionProvider`
  (`ComputerCraft.turtlesObeyBlockProtection`);
* `attack` killing a zombie (the world does not tick during the test, so the loop resets
  `hurtResistantTime` and waits on health, not `isDead` - `isDead` only flips after the 20-tick
  death animation);
* the computer upgrade applying in place (same tile instance, `+8` metadata, Advanced family,
  second application refused) and the turtle upgrade in creative and with 7 gold ingots, plus
  refusal on an unsupported tile;
* the turtle-facing tool host (`TurtleUpgradeToolHost.useTool(Dig)` and its disabled-config
  refusal) and `TurtleUpgradeToolManipulator.createPeripheral` returning the `tool_manipulator`
  peripheral with its 12 methods;
* the debug wand: refusal on a switched-off computer and a successful `DebugLib` injection into a
  **running** computer (the wand is not consumed and a second injection succeeds). The computer has
  to be booted by hand: the test calls `ServerComputer.update()` in a sleep loop, because the state
  machine only advances from the world tick, and gives CC's own computer thread time to build the
  Cobalt machine.

The permission provider used by `toolhost-dig-protected` is a global CC hook that cannot be
unregistered, so it is installed once and denies only while a single coordinate is armed.

It builds its own `selfTest.SelfTestTurtle` (a real-world `ITurtleAccess` over `InventoryBasic`),
because `testsupport.StubTurtleAccess` is test-source-set only. `SelfTestTurtle` is not
`ToolHostPlayer`'s production turtle: no computer, no peripheral, no fuel.

**Still needs a dev client:** the config GUI, NEI rendering, and Portal Gun interaction (PortalGun
is a dev-only dependency, so it is absent from the dedicated server).

Removed with their subjects: `NetworkTest`, `PacketTest`, `ComputerNetworkAccessTest`,
`core/network/mock`.

---

## Key Config Flags (CCTweaks.* statics)

| Flag | Default | Effect |
|------|---------|--------|
| `computerUpgradeEnabled` | true | Enable Computer Upgrade item |
| `computerUpgradeCrafting` | true | Enable its crafting recipe |
| `debugWandEnabled` | true | Enable Debug Wand item |
| `debugWandRequiresOp` | false | Require permission level 2 to inject the debug library (recommended on public servers) |
| `debugItems` | false | Enable the debug block |
| `thEnabled` | true | Enable Tool Host turtle upgrade |
| `advanced` | true | Enable the Advanced Tool Manipulator variant |
| `thCrafting` | true | Enable Tool Host crafting |
| `upgradeId` | 332 | Tool Host upgrade id (must be 64..32767) |
| `advancedUpgradeId` | 333 | Advanced tool manipulator upgrade id (must be distinct, 64..32767) |
| `digFactor` | 10 | Dig progress per Tool Host call (clamped to 1..100) |

Removed with their features: `cbMultipart`, `openPeripheralInventories`, `wbEnabled`,
`wbCrafting`, `turtleEnabled`, `pocketEnabled`, `pocketId`, `fullBlockModemCrafting`,
`extendedControllerValidation`, `euRefuelAmount`, `fluxRefuelAmount`.

---

## Known Fragile Points

1. **CCTweaks is the only ticker of CC's `DelayedTasks` list.** `FmlEvents.onServerTick` calls
   `DelayedTasks.update()`; CC 3.3.0's proxy ticks `MainThread` but not `DelayedTasks`. If CC
   starts ticking it too, every delayed task runs twice and delays halve. Keep the single call
   (and the `FmlEventsTest` assertion) until CC owns it.
2. **No mixins, no reflection imports, one AT.** A CC or Forge update can only break CCTweaks
   through normal API changes (peripheral API, turtle upgrade API, `ItemInWorldManager`, Cobalt
   internals used by `ItemDebugger`) plus the single AT below.
3. **The AT is the only remaining bytecode-level coupling** — a name/visibility change to
   `ItemInWorldManager.durabilityRemainingOnBlock` breaks `ToolHostPlayer` at compile time (the AT
   will simply stop matching).

4. **`ItemDebugger` reaches into Cobalt** (`CobaltMachine.getLuaState()` → `DebugLib.add(state)`).
   That is the one remaining "internal" dependency: if CC changes its Lua machine or Cobalt's
   `DebugLib` visibility again, only the debug wand's injector breaks (guarded by
   `instanceof`/try-catch, and the reason is now reported in chat).
   `DebugLib.add` writes into the live `LuaState` from the server thread while the computer's
   worker thread may be executing the same state (`CobaltMachine.state` is not volatile), so the
   injection is not synchronised — it is a debug tool, and `debugWandRequiresOp` exists to limit who
   can trigger it.
5. **`ToolHostPlayer` emulates `ItemInWorldManager`** (including
   `durabilityRemainingOnBlock`), so a Forge/vanilla mining-behaviour change affects tool host dig
   speed and durability.
6. **`PeripheralHostProvider` is registered in `init`**, after CC's `DefaultPeripheralProvider`
   (registered in CC's own preInit). CC asks providers in registration order, so an `IPeripheralHost`
   tile that is also an `IInventory` is shadowed by CC's `InventoryPeripheral` and the host
   peripheral is never returned. A CC-side fix is needed; do not rely on `IPeripheralHost` for
   inventory tiles.
7. **The config GUI is client-only by construction.** `CCTweaks.GUI_FACTORY` is a class-name
   string in `@Mod(guiFactory = ...)`, so FML never loads `client.gui.*` on a dedicated server.
   `GuiCCTweaksConfig` edits Forge's live `Configuration` (whose values are the `Property` objects);
   `onGuiClosed` → `CCTweaks.saveConfiguration()` saves the file and re-reads the statics.
   **Options that affect registration need a game restart** — the turtle upgrade IDs are registered
   during init — so the screen flags every option as "requires restart".
8. **The config GUI cannot be exercised headlessly.** `GuiScreen`'s static initialiser needs
   LWJGL, so `GuiConfigFactoryTest` only loads the screen class (class literals/reflection) and
   checks the factory wiring; the save-on-close path is covered up to `readConfig` by
   `CCTweaksConfigIoTest`.

---

## Docs

| File | Contents |
|------|----------|
| `AGENTS.md` | **This file** — comprehensive agent reference |
| `README.md` | User-facing overview, features and build instructions |
| `AUDIT.md` | Codebase audit (bugs, refactors, test/dependency plan) |
| `docs/tool-manipulator-api.md` | `tool_manipulator` peripheral reference: methods, arguments, result tuples, protection and disabled-action behaviour |
| `src/test/java/**` | Executable specification for the pure/headless behaviour |
| `scripts/headless-smoke.sh` | Automated dedicated-server smoke check (see Test Coverage) |

Older links to `docs/remove-reflection.md`, `docs/remove-luaj.md` and
`docs/migrate-mixins-to-source.md` referred to completed/pre-3.x migrations that no longer exist.
