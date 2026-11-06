# CCTweaks-Legacy — Agent Reference

This file is a machine-readable reference for AI coding agents working in this repository.
It documents the feature catalogue, bugfixes, every Mixin/AT/reflection/bytecode entry point,
and the complete runtime flow of the mod. Update this file whenever new mixins, features, or
architectural decisions are made.

---

## Project Identity

| Property | Value |
|----------|-------|
| Mod ID | `cctweaks` |
| Root Package | `org.squiddev.cctweaks` |
| Minecraft | 1.7.10 (Forge 10.13.4.1614) |
| Hard dependency | ComputerCraft `[3.2.0,)` — the CC source lives in `dan200/` |
| CC runtime requirement | Java 17+ with **lwjgl3ify** — CC 3.2.0 declares `required-after:lwjgl3ify` in `mcmod.info` |
| Cobalt package | `dan200.computercraft.shadow.org.squiddev.cobalt.*` (CC 3.x shadows and relocates Cobalt 0.9.9) |
| Mixin framework | GTNH UniMixins (`0.8.5-GTNH`) |
| Mixin package | `org.squiddev.cctweaks.mixins` |
| Mixin JSON (early) | `src/main/resources/mixins.cctweaks.json` — empty mixins list (package only) |
| Mixin JSON (late) | `src/main/resources/mixins.cctweaks.late.json` — all 10 mixins applied after CC loads |
| Access Transformers | **None** (`accessTransformersFile =` is blank in `gradle.properties`) |
| Bytecode / core mod | **None** |

---

## Feature Catalogue

| # | Feature | Key Source Files |
|---|---------|-----------------|
| 1 | **Wired Network Extension** — replaces CC's cable topology with a generic `INetworkNode` graph so any block can join a cable network | `mixins/late/TileCable_Mixin.java`, `mixins/late/BlockCable_Mixin.java`, `core/network/controller/NetworkController.java`, `core/network/NetworkRegistry.java` |
| 2 | **Network-Aware Cable Rendering** — cable arms extend to any `IWorldNetworkNode`, not just CC tiles | `mixins/late/CableBlockRenderingHandler_Mixin.java` |
| 3 | **Wireless Bridge Block** (meta 0 of `BlockNetworked`) — UUID/integer-bound block that wirelessly links two wired networks; `ItemDataCard` persists bindings | `blocks/network/TileNetworkedWirelessBridge.java`, `core/network/bridge/NetworkBinding.java`, `items/ItemDataCard.java` |
| 4 | **Wireless Bridge Turtle Upgrade** — turtle peripheral with `bindFromCard`/`bindToCard`/`bindToBlock`/`bindFromBlock` Lua API | `turtle/TurtleUpgradeWirelessBridge.java` |
| 5 | **Computer Upgrade Item** — upgrades Normal computer → Advanced in-world (transfers state); Normal turtle → Advanced (costs 7 gold ingots) | `items/ItemComputerUpgrade.java` |
| 6 | **Debug Wand** — displays tile/peripheral/network info in chat, injects Cobalt `DebugLib` into running computers, shows network visualiser overlay | `items/ItemDebugger.java`, `core/visualiser/NetworkPlayerWatcher.java`, `client/render/RenderNetworkOverlay.java` |
| 7 | **Tool Host Turtle Upgrades** — Normal and Advanced variants; turtles hold and swing arbitrary tools as interactions | `turtle/TurtleUpgradeToolHost.java`, `turtle/TurtleUpgradeToolManipulator.java` |
| 8 | **Extended Turtle Fuel** — ~~removed: CC 3.3.0 ships IC2/RF refuel handlers natively (`TurtleRefuelHandler` registry); CCTweaks fuel registry, integrations and `TurtleRefuelCommand_Mixin` deleted~~ | — |
| 9 | **Full-block Wired Modem** (meta 1 of `BlockNetworked`) — directional modem replacement as a full cube | `blocks/network/TileNetworkedModem.java` |
| 10 | **ForgeMultipart Integration** — cable and modem as multipart pieces; `ItemCable.onItemUse` tries multipart placement first | `mixins/late/ItemCable_Mixin.java`, `integration/multipart/network/PartCable.java`, `integration/multipart/network/PartModem.java` |
| 11 | **OpenPeripheral Network Integration** — injects `INetworkAccess` into OP method calls; proxies attach/detach through `WrappedPeripheral` | `mixins/late/AdapterPeripheral_Mixin.java`, `mixins/late/ModuleComputerCraft_Mixin.java`, `mixins/late/PeripheralProxy_Mixin.java` |
| 12 | **OpenPeripheral Inventory Adapter** — registers `AdapterNetworkedInventory` so OP exposes networked inventories | `integration/openperipheral/OpenPeripheralIntegration.java`, `integration/openperipheral/AdapterNetworkedInventory.java` |
| 13 | **Peripherals++ Pocket Wireless Bridge** — wireless bridge pocket upgrade via P++ registry | `integration/peripheralspp/PocketWirelessBinding.java`, `integration/peripheralspp/PocketAccess.java` |
| 14 | **Portal Gun Turtle Interaction** — registers portal gun item as a swing/use turtle tool interaction | `integration/PortalGunIntegration.java` |
| 15 | **PeripheralAPI Network Injection** — hooks `PeripheralWrapper.attach`/`detach` so CC peripherals receive `INetworkedPeripheral` events | `mixins/late/PeripheralAPI_Mixin.java` |

---

## Bugfixes Made

| Fix | Location |
|-----|----------|
| Cable tile completely rewritten: peripheral enable/disable sync (`m_peripheralAccessAllowed`) for OpenPeripheral's `PeripheralProxy`, correct NBT round-trip, proper `destroy()`/`onChunkUnload()` sequencing, deferred `connect()` via `FmlEvents.schedule` to survive same-tick placement/removal | `mixins/late/TileCable_Mixin.java` |
| `isCable` broadened so any `IWorldNetworkNode` registers as a connectable cable node (was CC-only hardcoded check) | `mixins/late/BlockCable_Mixin.java` |
| Cable rendering uses `NetworkAPI.helpers().canConnect()` instead of CC-private block-state logic, fixing rendering for custom network nodes | `mixins/late/CableBlockRenderingHandler_Mixin.java` |
| `PeripheralWrapper.detach` hook placed at `@At("HEAD")` (before CC fires its own cleanup) to prevent race condition | `mixins/late/PeripheralAPI_Mixin.java` |
| Neighbor-change peripheral type captured in a `ThreadLocal` before the update runs and consumed after, preventing stale type reads during modem toggle | `mixins/late/TileCable_Mixin.java` — `StorePeripheralType` / `UsePeripheralType` |

---

## Mixin Inventory

All mixins are **late phase** (applied after CC and OP are loaded). There are no early-phase
mixins. The Mixin enum declaration is in `mixins/Mixins.java`.

### Behavioral Mixins (8)

| Class | Target | Strategy | Purpose |
|-------|--------|----------|---------|
| `TileCable_Mixin` | `dan200.computercraft.shared.peripheral.modem.TileCable` | 14× `@Overwrite` + 2× `@Inject` | Full rewrite of CC cable tile; implements `IWorldNetworkNodeHost`, `IWorldPosition` |
| `BlockCable_Mixin` | `dan200.computercraft.shared.peripheral.common.BlockCable` | `@Overwrite` (`isCable`) | Broadens cable connectivity to any network node |
| `ItemCable_Mixin` | `dan200.computercraft.shared.peripheral.common.ItemCable` | `@Overwrite` (`onItemUse`) | Adds ForgeMultipart placement before vanilla placement |
| `CableBlockRenderingHandler_Mixin` | `dan200.computercraft.client.proxy.ComputerCraftProxyClient$CableBlockRenderingHandler` | `@Overwrite` (`renderWorldBlock`) | Network-aware cable + modem rendering |
| `PeripheralAPI_Mixin` | `dan200.computercraft.core.apis.PeripheralAPI$PeripheralWrapper` | `@Inject` on `attach`/`detach` + interface impl | Fires `INetworkedPeripheral` callbacks; implements `INetworkAccess.getPeripheralsOnNetwork` via `getAvailablePeripherals()` |
| `PeripheralProxy_Mixin` | `openperipheral.addons.peripheralproxy.WrappedPeripheral` | Interface impl (`INetworkedPeripheral`, `IPeripheralProxy`) | Delegates network events through OP's peripheral wrapper |
| `AdapterPeripheral_Mixin` | `openperipheral.interfaces.cc.wrappers.AdapterPeripheral` | `@Overwrite` (`prepareCall`) + interface impl | Injects `INetworkAccess` environment into every OP method call |
| `ModuleComputerCraft_Mixin` | `openperipheral.interfaces.cc.ModuleComputerCraft` | `@Inject` (`init`, `@At("RETURN")`) | Registers `INetworkAccess.class` as a OP provided environment via `MethodSelector.addProvidedEnv` |

### Accessor Interfaces (2, both OpenPeripheral)

| Interface | Target Class | Exposed Members | Used By |
|-----------|-------------|-----------------|---------|
| `ClassMethodsComposer_Accessor` | `ClassMethodsComposer` (OP) | `selector` → `Predicate<IMethodExecutor>` | `ModuleComputerCraft_Mixin` |
| `ComposedMethodsFactory_Accessor` | `ComposedMethodsFactory` (OP) | `composer` → `ClassMethodsComposer` | `ModuleComputerCraft_Mixin` |

### Invoker Interfaces (0)

> None — both removed when CC 3.3.0 widened `transferStateFrom` and `createServerComputer` to public.

### Access Transformers
> **None.** `accessTransformersFile` is blank in `gradle.properties`. All private access is
> handled by the Accessor/Invoker mixin interfaces listed above.

### Reflection
> **None.** Fully migrated to mixins per the completed plan in `docs/remove-reflection.md`.
> Verified: zero `import java.lang.reflect` occurrences in the source tree.

### Bytecode Manipulation
> **None.** No `IClassTransformer`, ASM visitors, or FML core mod present.

---

## ComputerCraft 3.2.0 Compatibility

CC 3.2.0 is built with `jvmDowngrader` (base classes are Java 8 class file version 52, no
`META-INF/versions` overlay), so CCTweaks still builds with `enableModernJavaSyntax = jabel`.
Four things differ from 2.x and must be preserved:

1. **Cobalt is relocated and shadowed.** `org.squiddev.cobalt.*` does not exist on the classpath.
   Import `dan200.computercraft.shadow.org.squiddev.cobalt.*`. Cobalt is now 0.9.9, where
   `DebugLib` has a private constructor and is installed with `DebugLib.add(state)`
   (`ItemDebugger`). `@Accessor("state")` still matches — the field descriptor follows the
   relocation.
2. **Blocking Lua is gone.** `ILuaContext` has `executeMainThreadTask` (returning `MethodResult`)
   and `issueMainThreadTask` only — no `pullEvent`/`pullEventRaw`/`yield`. A method that waits
   returns a `MethodResult`; the Lua bridge suspends the coroutine (see
   `CobaltMachine.CobaltCallback`).
3. **Delegated calls return `Object`.** `ArgumentDelegator.delegatePeripheral`/
   `delegateLuaObject` return `Object[]` *or* a `MethodResult`.

Because `callMethod` still declares `Object[]`, a suspension is carried in the single-element
sentinel array `new Object[] { methodResult }`. `core/utils/LuaResults.wrap(Object)` does that
(and mirrors CC's own `r instanceof Object[] ? r : new Object[] { r }` from
`PeripheralAPI.callMethod`). Used by `ToolManipulatorPeripheral.canUse`/`canSwing` and the
`getPeripheralsOnNetwork` call path in `BasicModemPeripheral` — a remote peripheral reached
through the network now suspends correctly instead of being flattened to `Object[]`.

### Development environment

* Use `./gradlew runServer25` / `runClient25` (or the plain `runServer`/`runClient`, which
  `build.gradle.kts` aliases onto them). The Java-8 run tasks put no lwjgl3ify on the classpath and
  die at `MissingModsException: lwjgl3ify` — the 1.7.10 Forge convention targets Java 8, but
  ComputerCraft 3.2.0 requires lwjgl3ify, which requires Java 17+.
* `PeripheralsPlusPlus` is `compileOnly` in `dependencies.gradle`. P++ 2286103's coremod
  transformer patches `ItemPocketComputer.createServerComputer` at hardcoded offsets that no
  longer match CC 3.2.0's method body, producing `VerifyError: Bad local variable type` and
  taking all of ComputerCraft down with it. Restore `devOnlyNonPublishable` when P++ ships a
  CC 3.2.0 build.
* Verified against `3.2.0`: all 20 mixins load and, of the 19 non-client targets, every one
  applies without error when loaded (checked by forcing `Class.forName` on each target on a
  dedicated server). `CableBlockRenderingHandler_Mixin` is client-only and needs an in-game
  check.

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
  addModule(ItemDataCard)           items/ItemDataCard.java
  addModule(BlockNetworked)         blocks/network/BlockNetworked.java
  addModule(BlockDebug)             blocks/debug/BlockDebug.java
  addModule(MultipartIntegration)   integration/multipart/MultipartIntegration.java
  addModule(OpenPeripheralInteg.)   integration/openperipheral/OpenPeripheralIntegration.java
  addModule(PeripheralHostProvider) core/peripheral/PeripheralHostProvider.java
  addModule(DefaultTurtleProviders) core/turtle/DefaultTurtleProviders.java
  addModule(TurtleUpgradeWireless)  turtle/TurtleUpgradeWirelessBridge.java
  addModule(PeripheralsPlusPlusInteg.)
  addModule(PortalGunIntegration)
  addModule(NetworkPlayerWatcher)   core/visualiser/NetworkPlayerWatcher.java
  addModule(RenderNetworkOverlay)   client/render/RenderNetworkOverlay.java
```

---

## Runtime Flow

```
JVM Startup
  └─ Mixin subsystem (GTNH UniMixins)
       Applies all 10 late-phase mixins to CC + OP class bytecode
       before any CC/OP class is first used.

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
            • Integration canLoad() checks (mod present?)

FMLInitializationEvent  →  CCTweaks.init()
  └─ Registry.init()
       └─ IModule.init() for each module
            • Crafting recipes added
            • ComputerCraft.registerTurtleUpgrade(...)
            • 
            • P++ pocket upgrade registered

FMLPostInitializationEvent  →  CCTweaks.postInit()
  └─ Registry.postInit()
       └─ IModule.postInit() for each module
            • OpenPeripheral: register AdapterNetworkedInventory
              (must be post-init so OP's API is ready)

FMLServerStartedEvent / FMLServerStoppedEvent
  └─ DelayedTasks.reset()
     NetworkBindings.reset()
     NetworkPlayerWatcher.reset()

─── Server Runtime ──────────────────────────────────────────────────

ServerTickEvent (Phase.START)
  ├─ DelayedTasks.update()            ← CC Lua task scheduling
  └─ Drain FmlEvents.serverQueue      ← deferred Runnables (e.g. cable.connect())

TileCable lifecycle  (logic lives in TileCable_Mixin)
  ├─ validate()
  │    └─ FmlEvents.schedule(cable.connect())   ← deferred one tick
  ├─ onNeighbourChange()
  │    ├─ @Inject HEAD: ThreadLocal.set(peripheralType)
  │    └─ @Inject TAIL: if WiredModemWithCable && type changed
  │         → modem.updateEnabled() → network.invalidateNode() → updateAnim()
  ├─ updateEntity()   → modem.modem.pollChanged() → updateAnim()
  ├─ onActivate()     → modem.toggleEnabled() → invalidateNode() → updateAnim()
  ├─ readFromNBT()    → lazy-store tag if world == null, else readLazyNBT()
  ├─ writeToNBT()     → write modem.isEnabled() + modem.id
  └─ destroy() / onChunkUnload()  → modem.destroy() + cable.destroy()

NetworkController  (core/network/controller/NetworkController.java)
  ├─ Node connects
  │    └─ NodeScanner BFS → assimilateNode() or mergeController()
  ├─ invalidateNode(node)
  │    └─ re-scan node's peripherals
  │       compute MapDifference(old, new peripherals)
  │       fire INetworkedPeripheral.networkInvalidated() on all nodes
  └─ transmitPacket(sender, packet)
       └─ route Packet to every IReceiver in range via node graph

PeripheralAPI_Mixin  (per CC computer, fires inside CC tick)
  ├─ PeripheralWrapper.attach @TAIL
  │    └─ INetworkedPeripheral.attachToNetwork(this=INetworkAccess, side)
  └─ PeripheralWrapper.detach @HEAD
       └─ INetworkedPeripheral.detachFromNetwork(this=INetworkAccess, side)

─── Client Runtime ──────────────────────────────────────────────────

ClientTickEvent (Phase.START)
  └─ Drain FmlEvents.clientQueue

ItemDebugger.onUpdate  (every tick while held)
  └─ RayTrace → NetworkPlayerWatcher.update(player, x, y, z)
       └─ if controller changed → VisualisationPacket.send() to player

RenderNetworkOverlay  (IClientModule, clientInit registers renderer)
  └─ Draws node/link overlay using VisualisationData from last packet
```

---

## API Surface (`api/` package)

| Interface | Role |
|-----------|------|
| `INetworkNode` | A node that can join a CCTweaks network; exposes connected nodes + peripherals |
| `IWorldNetworkNode` | `INetworkNode` + world position |
| `IWorldNetworkNodeHost` | TileEntity / part that owns an `IWorldNetworkNode` |
| `INetworkController` | The controller for a connected component; manages peripheral map + packet routing |
| `INetworkAccess` | View of the network given to a peripheral (peripherals-on-network, transmit) |
| `INetworkedPeripheral` | Peripheral that wants attach/detach/invalidate callbacks |
| `INetworkCompatiblePeripheral` | Marker — peripheral can be included in a network's peripheral map |
| `INetworkHelpers` | Utility — can two positions connect? |
| `INetworkRegistry` | Singleton — maps world positions to nodes, fires `isNode` / `getNode` |
| `Packet` | Value object: channel, replyChannel, payload, senderObject |
| `ITurtleRegistry` | Registry of `ITurtleInteraction` implementations |
| `IDataCard` | Item that can store a network binding (UUID + optional integer ID) |

Public API entry point: `CCTweaksAPI.instance()` → `ICCTweaksAPI`.

---

## Test Coverage

| Test Class | What Is Tested |
|------------|----------------|
| `core/network/NetworkTest.java` | Node connect/disconnect, controller merge/split, peripheral propagation, packet routing |
| `core/network/PacketTest.java` | Packet construction and equality |
| `core/peripheral/PeripheralHelpersTest.java` | `PeripheralHelpers` utility methods |

**Gap:** Mixin-dependent behaviour (cable tile lifecycle, turtle refuel, computer upgrade,
OP injection) has no automated coverage and requires in-game integration testing.

---

## Key Config Flags (CCTweaks.*  statics)

| Flag | Default | Effect |
|------|---------|--------|
| `computerUpgradeEnabled` | true | Enable Computer Upgrade item |
| `debugWandEnabled` | true | Enable Debug Wand item |
| `cbMultipart` | true | Enable ForgeMultipart integration |
| `openPeripheralInventories` | true | Register OP inventory adapter |
| `wbEnabled` | true | Enable Wireless Bridge block |
| `turtleEnabled` | true | Enable Wireless Bridge turtle upgrade |
| `pocketEnabled` | true | Enable Wireless Bridge pocket upgrade (P++) |
| `thEnabled` | true | Enable Tool Host turtle upgrade |
| `advanced` | true | Enable Advanced Tool Host variant |

---

## Known Fragile Points

1. **`TileCable_Accessor` static `@Accessor`** — static interface accessor methods require
   Java 8 static interface methods. Confirmed working under `0.8.5-GTNH`. If the mixin
   version changes, fall back to Access Transformers for `s_cableIcons`/`s_modemIcons`.

2. **`PeripheralWrapper_Accessor.this$0`** — accesses the synthetic outer-class field name.
   This is not in the Mixin spec but works under GTNH mixins. Will break if CC is recompiled
   with a different inner-class naming strategy (e.g., `val$0` or similar).

3. **`AdapterPeripheral_Mixin.prepareCall` `@Overwrite`** — completely replaces the OP method.
   Any OP update that changes `prepareCall`'s signature or semantics will cause a silent
   breakage. The `@author`/`@reason` Javadoc documents this intentionally.

4. **`TileCable_Mixin` `@Overwrite` count (14 methods)** — the cable tile is almost entirely
   replaced. Any CC update to `TileCable` must be audited against all 14 overwritten methods.

---

## Docs

| File | Contents |
|------|----------|
| `docs/remove-reflection.md` | Completed migration plan: 13 reflection sites → mixins |
| `docs/remove-luaj.md` | Completed migration plan: LuaJ → Cobalt in `ItemDebugger` |
| `docs/migrate-mixins-to-source.md` | Plan: move all 20 late-phase mixins into native CC / OP source (11 steps, covers all behavioral mixins, accessor/invoker elimination, and OP changes) |
| `AGENTS.md` | **This file** — comprehensive agent reference |

