# CCTweaks-Legacy — codebase audit

Audited the full tree (~4,500 lines across 60 Java files), cross-checked every CC interaction against the
**ComputerCraft-Legacy 3.3.0 sources**, ran `./gradlew test`, compiled against a newer NEI/CCC pair, built the
jar (verified `FMLAT: CCTweaks_at.cfg`), and booted `runServer25`.

Current baseline: **13 main packages, 2 unit tests**, no mixins/reflection beyond the API loader, one AT. Most of
the remaining risk is in the Tool Host/Manipulator and the two upgrade items.

---

## 1. Correctness bugs

### 1.1 Tool Manipulator / Tool Host (highest impact)

| # | Issue | Evidence | Impact |
|---|-------|----------|--------|
| T1 | **Entity ray trace is wrong.** `findHit` passes the ray *endpoint* as the direction to `WorldUtil.rayTraceEntities`, which does `vecEnd = vecStart + vecDir * distance`. | `turtle/ToolManipulatorPeripheral.java:403-418`; CC `WorldUtil.java:42-49` | At any non-origin coordinate, `use`/`swing`/`canSwing` on an entity traces a completely different ray, so entity interactions basically never work. |
| T2 | **Held-item attribute modifiers are never applied.** The fake `TurtlePlayer` overrides `onUpdate()` to no-op, so `EntityLivingBase.onUpdate` never applies equipment attribute modifiers; `attackTargetEntityWithCurrentItem` then uses base attack damage (1). | `turtle/ToolHostPlayer.java:181-233`; `FakePlayer.onUpdate()`; `EntityLivingBase.java:1780-1810` | Diamond swords, axes, TiC weapons lose all non-enchantment damage; only Sharpness (via `EnchantmentHelper`) still works. |
| T3 | **Instant-break blocks drop into the world, not the turtle.** The drop consumer is installed only around `uncheckedTryHarvestBlock`. `ItemInWorldManager.onBlockClicked` can harvest immediately (`f >= 1`) for hardness-0 blocks (grass, torches, flowers). | `turtle/ToolHostPlayer.java:118-142`; `ItemInWorldManager.java:153-217` | Drops from soft blocks are lost to the turtle; `dig()` still reports success. |
| T4 | **Block protection bypass.** `dig` calls `ItemInWorldManager` directly and never checks `ComputerCraft.turtlesObeyBlockProtection` / `ComputerCraft.isBlockEditable`. CC's own `TurtleTool.dig` does. | `turtle/ToolHostPlayer.java:118-148`; `TurtleTool.java:169-193` | Tool-host turtles can break blocks in spawn protection / claims (GriefPrevention etc.) where normal turtles cannot. |
| T5 | **Placement/activation protection bypass.** `onPlayerRightClick` places blocks and activates blocks without `isBlockEditable`; CC's `TurtlePlaceCommand` checks. | `turtle/ToolManipulatorPeripheral.java:194-262`; `TurtlePlaceCommand.java:265-270` | Same permission bypass for placing and right-clicking in protected areas. |
| T6 | **`turtleDisabledActions` bypass.** `swing`/`use` are peripheral calls, not turtle commands, so the config list (`dig`, `place`, `attack`, `tool`) is never consulted. | `turtle/ToolManipulatorPeripheral.java:91-400`; `TurtleToolCommand.java:34-37` | Admins disabling turtle actions can still be bypassed via the manipulator peripheral. |
| T7 | **`use(duration)` never waits.** `task.delay = duration` is set inside the task body; `DelayedTask.execute` reads `delay` *before* the task runs. Also `LuaEnvironment.sleep` discards its `MethodResult`, so even a correctly set delay would not suspend. | `ToolManipulatorPeripheral.java:115-121, 168`; CC `DelayedTask.java:12-16`, `LuaEnvironment.java:72-75` | `duration` only affects `onPlayerStoppedUsing`; it does not hold the Lua call. |
| T8 | **`equals` ignores the turtle side.** Left and right manipulators on the same turtle compare equal. | `ToolManipulatorPeripheral.java:427-440` | Semantically wrong; currently harmless because CC keys by side, but any equality-based lookup/collection is wrong. |
| T9 | **Cached `ToolHostPlayer` keeps the old world.** `players` is keyed by `ITurtleAccess`; `TurtleBrain` survives `teleportTo` to another dimension, but `ToolHostPlayer.worldObj`/`theItemInWorldManager.theWorld` are fixed at construction. | `TurtleUpgradeToolHost.java:27,80-84`; `ToolHostPlayer.java:41-44`; `TurtleBrain.java:439-474` | After a dimension change, digging/placing/drops happen in the old world. |
| T10 | **Drop-consumer leaks.** `attack`/`dig` register a consumer and remove it without `finally`; an exception leaves `captureDrops = true` and the map entry behind. | `ToolHostPlayer.java:88-96, 132-136`; `McEvents.java:37-62` | Entity never drops loot; permanent map entry; world reload leaks. |
| T11 | **Drop location mutation.** `dig` mutates the `coordinates` object returned by `getPlayerCoordinates()` to point at the target block; overflow drops are then spawned at the block, not the turtle. | `ToolHostPlayer.java:102-106`; `getConsumer` `:55-67` | Overflow items spawn in the wrong place. |
| T12 | **`canUse` only checks registered interactions.** It never reports whether vanilla use/placement would succeed, despite the docs saying it should. | `ToolManipulatorPeripheral.java:336-361` | `canUse` returns false for ordinary block/place interactions. |
| T13 | **Opposite-side fallback in `tryUseOnBlock`.** Tries `hit.sideHit` and `ForgeDirection.OPPOSITES[sideHit]`, which can activate/place against the far face. | `ToolManipulatorPeripheral.java:147-154` | Wrong-face interactions; possible double use. |
| T14 | **Unknown method returns `null`.** `callMethod` falls through to `return null` instead of a `LuaException`. | `ToolManipulatorPeripheral.java:58-88` | Lua gets `nil` instead of a clear error. |
| T15 | **Inventory-size assumption.** `load`/`unload` copy `i < turtleSize` into a 36-slot `InventoryPlayer` without guarding `turtleSize > 36`. | `ToolHostPlayer.java:196-228` | Custom turtles with >36 slots throw `ArrayIndexOutOfBoundsException`. |

**Concrete fixes**

```java
// T1
Vec3 dir = target.subtract(origin);
Entity entity = WorldUtil.rayTraceEntities(player.worldObj, origin, dir, 1.0);
```

```java
// T3/T4/T10
if (ComputerCraft.turtlesObeyBlockProtection
    && !ComputerCraft.isBlockEditable(world, x, y, z, this)) {
    return TurtleCommandResult.failure("Cannot break protected block");
}
IWorldPosition position = new WorldPosition(world, x, y, z);
McEvents.addBlockConsumer(position, getConsumer(turtle));
try {
    /* whole progress loop, including onBlockClicked() */
} finally {
    McEvents.removeBlockConsumer(position);
}
```

```java
// T2
private ItemStack loadedStack;
public void load(...) {
    ...
    loadedStack = turtle.getInventory().getStackInSlot(turtle.getSelectedSlot());
    if (loadedStack != null) getAttributeMap().applyAttributeModifiers(loadedStack.getAttributeModifiers());
}
public void unload(...) {
    if (loadedStack != null) getAttributeMap().removeAttributeModifiers(loadedStack.getAttributeModifiers());
    loadedStack = null;
    ...
}
```

For T7, either delete `task.delay = duration` and document that duration controls item use, or chain two
`LuaEnvironment.executeTask` calls (task 1 performs the use; task 2 is delayed by `duration` and returns the
captured result).

---

### 1.2 Upgrade items and crafting

| # | Issue | Evidence | Impact |
|---|-------|----------|--------|
| U1 | **In-world turtle upgrade stays a Normal computer.** `setMoved(true)` keeps the old `ServerComputer` in the registry; `createServerComputer()` returns it, so the terminal was constructed with `ComputerFamily.Normal`. `ServerTerminal.isColour` is fixed at construction. | `items/ItemComputerUpgrade.java:79-121`; CC `ServerComputer.java:33-39`, `ServerTerminal.java:14-25,64-66`; `TermAPI.java:191` | Advanced turtle block, but `term.isColour()` is false and the terminal renders greyscale. |
| U2 | **In-world computer upgrade loses the running computer.** The old block is replaced; `BlockGeneric.breakBlock` unloads the registry before `transferStateFrom`, so the old `m_instanceID` is already `-1`. ID/label/on are copied, but the instance/terminal/program are not. | `ItemComputerUpgrade.java:57-75`; CC `BlockGeneric.java:90-101`, `TileComputerBase.java:385-397` | Program/terminal scrollback lost; documentation says “transfers state”. Could be acceptable, but should be documented, or use a metadata-only in-place upgrade. |
| U3 | **Turtle overlays/hats are lost when crafting an advanced turtle.** `TurtleItemFactory.create(..., null, null)` is passed instead of `turtle.getOverlay(stack)` / `getHatOverlay(stack)`. | `items/CraftingComputerUpgrade.java:46-55`; `ITurtleItem.java:19-21` | Custom turtle overlays/hats disappear. |
| U4 | **Pocket upgrades are lost.** Only `getHasModem` (ID 1) is preserved; ender modem, speaker, chat box, NPC upgrades and their `upgradeData` are dropped. | `CraftingComputerUpgrade.java:56-60`; `ItemPocketComputer.java:541-573`; `PocketAPI.java:163-185` | Crafting an advanced pocket computer erases the installed upgrade and its NBT. |
| U5 | **Unsupported `IComputerItem` implementation matches but yields `null`.** `matches` accepts any `IComputerItem` with `Normal` family; `getCraftingResult` returns `null` for anything that is not `ItemComputer`/`ItemTurtleBase`/`ItemPocketComputer`. | `CraftingComputerUpgrade.java:31-63, 88-95` | The custom recipe can shadow a valid recipe and leave the output slot empty. |
| U6 | **`getRecipeOutput()` returns `null`.** | `CraftingComputerUpgrade.java:118-121` | Other recipe browsers/crafting mods may NPE or hide the recipe. Return a representative advanced computer stack. |
| U7 | **Gold is extracted before the upgrade is known to succeed.** If `setBlock`/tile transfer fails, the 7 ingots are gone. | `ItemComputerUpgrade.java:88-110` | Gold loss on failure (rare). Check count first, extract after success, or refund. |
| U8 | **Modded computer/turtle tiles are converted to CC blocks.** `useComputer` hardcodes `ComputerCraft.Blocks.computer`; `useTurtle` hardcodes `turtleAdvanced`. A modded `TileComputerBase`/`TileTurtle` subclass (or custom block) is replaced, losing its data. | `ItemComputerUpgrade.java:58-59, 103-104` | Compatibility/data-loss bug on modded computers/turtles. Restrict to CC's exact tile/block classes. |
| U9 | **`setBlock` failure/self-transfer not handled.** If `setBlock` returns false, `getTileEntity` returns the old tile; `transferStateFrom(this)` sets the tile's own `m_instanceID = -1`, and the item is consumed. | `ItemComputerUpgrade.java:58-68, 100-113` | Rare but catastrophic: normal computer/turtle left invalidated with the upgrade consumed. |
| U10 | **Protection not checked.** `world.setBlock` bypasses spawn/claim protection; a player can replace computers/turtles inside protected regions. | `ItemComputerUpgrade.java:48-122` | Griefing/security issue on servers. |
| U11 | **Gold count semantics are slot-based.** Seven ingots in one slot count as one; the recipe requires seven occupied slots. This is deliberate (vanilla crafting consumes one per slot, so stacking would be a free upgrade) but undocumented. | `CraftingComputerUpgrade.java:72-110` | Confusing UX; players with a stack of gold see no match. Add a comment or require exact stacks. |
| U12 | **Advanced subtypes appear when disabled.** `getSubItems` always adds meta 1, even if `CCTweaks.advanced` is false; it also lists both items when `thEnabled` is false. | `items/ItemToolHost.java:36-40` | Dead creative entries for disabled features. |

**Recommended upgrade path**

For **computers**, avoid replacing the block at all: change the metadata in place, then invalidate/validate the tile
so a new Advanced `ServerComputer` is created:

```java
int metadata = world.getBlockMetadata(x, y, z);
world.setBlockMetadataWithNotify(x, y, z, metadata + 8, 3);
computerTile.invalidate();   // unloads the Normal registry entry
computerTile.validate();     // clears tileEntityInvalid
computerTile.createServerComputer(); // recreates with the Advanced family
computerTile.updateBlock();
```

This preserves the tile/label/ID/on-state and still gives the Advanced colour; the running program is restarted
either way.

For **turtles**, keep `transferStateFrom` + `setMoved`, but force a new `ServerComputer` after the block swap:

```java
newTurtle.transferStateFrom(computerTile);
newTurtle.invalidate();   // remove the old Normal instance
newTurtle.validate();
newTurtle.createServerComputer(); // Advanced family, same computer ID
newTurtle.updateBlock();
```

For **crafting**, pass `getOverlay`/`getHatOverlay`, and copy the pocket `upgrade` and `upgradeData` NBT keys.
Restrict matching to CC's exact item classes (or explicitly support the modded ones) so `matches` and
`getCraftingResult` agree.

---

### 1.3 Debug wand

| # | Issue | Evidence | Impact |
|---|-------|----------|--------|
| D1 | **The debug wand is consumed on success.** `useComputer` returns true and `ItemComputerAction` decrements the stack; `useGeneric` returns false, so the wand only disappears when injection works. | `items/ItemDebugger.java:41-61`; `items/ItemComputerAction.java:42-46` | In survival the wand is destroyed on first successful use. Add a `consumeOnSuccess()` hook and override to `false` in `ItemDebugger`. |
| D2 | **Security: any player with the item can inject `debug` into any computer.** `DebugLib.add` installs `debug.getlocal/setlocal/getupvalue/setupvalue/sethook/setfenv`, which is a sandbox-quality escape. The wand has no recipe (creative only), but packs can add one, and `debugWandEnabled` defaults true. | `ItemDebugger.java:45-59`; Cobalt `DebugLib.add` | Privilege escalation / griefing on multiplayer servers. Require op (`canCommandSenderUseCommand(2, ...)`), or default `debugWandEnabled` to false, or both. |
| D3 | **Thread race.** `getLuaMachine()`/`getLuaState()` and `DebugLib.add(state)` are called from the server thread while the Cobalt worker thread can be executing the same state. `CobaltMachine.state` is not synchronized/volatile. | `ItemDebugger.java:45-59`; CC/Cobalt state | Potential Lua state corruption under load. If CC exposes no “run on computer thread” hook, document this as debug-only and consider requiring the computer to be off/inaccessible. |
| D4 | **Errors are swallowed, and the wand consumes on partial success.** The `catch (NullPointerException)` is redundant; there is no log; no `isUsable`/range check. | `ItemDebugger.java:55-59, 65-111` | Hard to diagnose; can affect computers the player should not reach. Log at debug, use `computerTile.isUsable(player, false)`. |

---

### 1.4 Events, lifecycles, peripherals

| # | Issue | Evidence |
|---|-------|----------|
| E1 | `McEvents.onEntityLivingDrops` ignores `event.isCanceled()`. A protection mod that cancels `LivingDropsEvent` still has the drops moved into the turtle. | `core/McEvents.java:89-97`; `LivingDropsEvent` is `@Cancelable` |
| E2 | `TileBase` destroys twice: `BlockContainer.breakBlock` invalidates the tile (`TileBase.invalidate` → `destroy`) and `BlockBase.breakBlock` calls `tile.destroy()` again. `onChunkUnload` + `invalidate` can do the same. | `blocks/BlockBase.java:51-57`; `blocks/TileBase.java:48-69` |
| E3 | `TileBase.validate()` does not call `super.validate()`, so `tileEntityInvalid` is never cleared after revalidation. | `blocks/TileBase.java:48-53`; `TileEntity.java:238-241` |
| E4 | `PeripheralHelpers.getBasePeripheral`/`getTarget` loop forever on a proxy cycle (`getBasePeripheral()` returns itself). | `core/peripheral/PeripheralHelpers.java:12-40` |
| E5 | `PeripheralProxy` is not thread-safe: `instance` is non-volatile, `mounts` is a `HashSet`, and `getBasePeripheral` iterates `mounts` while `attach`/`detach` can mutate it. | `core/peripheral/PeripheralProxy.java:20-44,63-78` |
| E6 | `Registry` uses a `HashSet` (non-deterministic order) and iterates it while `addModule` can mutate it from a module hook → `ConcurrentModificationException`. | `core/registry/Registry.java:26,54-96` |
| E7 | `FmlEvents` uses a singleton `instance` that `schedule` dereferences; a scheduled task that throws propagates out of the server tick. The client queue/`onConfigChanged` are dead. | `core/FmlEvents.java:16-67` |
| E8 | `McEvents` static maps are never cleared on server stop; entities with `captureDrops=true` leak across world reloads. | `core/McEvents.java:28-29` |
| E9 | `PeripheralHostProvider` is registered in `init`, after CC’s `DefaultPeripheralProvider` (registered in CC preInit). Any `IPeripheralHost` tile that is also an `IInventory` is shadowed by `InventoryPeripheral`; the host peripheral is never used. | `core/peripheral/PeripheralHostProvider.java:19-29`; CC `DefaultPeripheralProvider.java:38-63` |
| E10 | `PortalGunIntegration` implements `swing`/`use` but not `canSwing`/`canUse`, so the manipulator reports false for portal guns even though the actions work. | `integration/PortalGunIntegration.java:38-57`; `AbstractTurtleInteraction.java:29-45` |
| E11 | `ItemComputerAction` returns `false` on the client when sneaking (`world.isRemote` early return). `PlayerControllerMP` then performs the normal block activation (flickers/opens a menu) while the server performs the upgrade and consumes the item. | `items/ItemComputerAction.java:20-25`; `PlayerControllerMP.java:367-377` |
| E12 | Config values are not validated: `digFactor <= 0` makes `dig` a no-op that reports success; `upgradeId < 64`, duplicate IDs, or `upgradeId == advancedUpgradeId` make CC throw during `init` and crash the game. | `CCTweaks.java:110-154`; CC `CCTurtleProxyCommon.java:70-81,186-198` |

**Quick fixes**

```java
// E11
if (world.isRemote) return player.isSneaking(); // suppress client-side block use only
if (!player.isSneaking()) return false;
```

```java
// E1
if (event.isCanceled()) return;
```

```java
// E4
Set<IPeripheral> seen = Collections.newSetFromMap(new IdentityHashMap<>());
while (peripheral != null && seen.add(peripheral)) { ... }
```

```java
// E2/E3
private boolean destroyed;
@Override public void validate() { super.validate(); destroyed = false; create(); }
public final void destroyOnce() { if (!destroyed) { destroyed = true; destroy(); } }
```

---

## 2. Architecture / refactors worth doing

1. **The `DelayedTasks` tick is load-bearing.** CC 3.3.0 never calls `DelayedTasks.update()` anywhere;
   `FmlEvents.onServerTick` is the only caller. That means CCTweaks is silently keeping CC’s
   `ILuaEnvironment.issueTask/sleep/executeTask` alive, and if another mod ever ticks it too, all delays halve.
   Document this in `AGENTS.md`, add a test that schedules a task and ticks `FmlEvents`, and raise it upstream in
   Computercraft-Legacy (it belongs in `ComputerCraftProxyCommon.onServerTick`).
2. **Make the turtle/tool logic testable.** `ToolManipulatorPeripheral` and `ToolHostPlayer` are untestable because
   they construct a `FakePlayer`/`WorldServer`. Extract:
   - argument parsing (`duration`, `sneak`) into a pure helper;
   - an `IToolHostPlayer` interface implemented by `ToolHostPlayer`, injected into `ToolManipulatorPeripheral`;
   - hit resolution into a `HitResolver` taking an `IBlockAccess`/world.

   Then a `StubTurtleAccess` + `StubLuaContext` can test `use`/`swing`/`canUse`/`canSwing` without a server.
3. **Replace `Registry`’s static initializer** with explicit module construction in `preInit`/a factory. This
   removes the `ComputerCraft.mainCreativeTab` static-init dependency and makes module lifecycle testable.
4. **Replace `FmlEvents`’ singleton** with a static `ConcurrentLinkedQueue` + instance tick handler, and add
   `reset()`.
5. **Delete the dead `IClientModule` path** or add a real client module. There are currently no implementations, so
   `RegisterWrapperClient`/`IClientModule` are pure dead weight.
6. **Modernise `CCTweaksAPI`’s loader**: `Class.newInstance()` is deprecated; use
   `getDeclaredConstructor().newInstance()`, catch `ReflectiveOperationException`/`ClassCastException`/
   `ExceptionInInitializerError`, and keep the `CoreNotFoundException` messages.
7. **Document the `IPeripheralHost` shadowing limitation** or move the provider registration earlier (not possible
   with a hard CC dependency; a CC-side change is the real fix).
8. **`TileLazyNBT` is dead** (removed with the network code); `BlockNotifyFlags` has one use; `FakeNetHandler` is
   205 lines of defensive overrides worth keeping but worth a comment.

---

## 3. Optimizations

- Cache `getMethodNames()` arrays in `ToolManipulatorPeripheral` and `TileDebugPeripheral.SidedPeripheral` (CC calls
  these frequently).
- `TurtleUpgradeToolManipulator.createPeripheral` allocates a new peripheral **every turtle tick** (CC calls it from
  `TurtleBrain.updatePeripherals`). Cache one per `(turtle, side)` or return the previous instance from the upgrade.
- Cache `ToolHostPlayer.getConsumer(turtle)` as a field instead of allocating an anonymous class per attack/dig.
- `ItemToolHost.getSubItems` should return nothing when `thEnabled` is false, and only meta 0 when `advanced` is
  false.
- `TurtleRegistry` fields should be `final`; use `Collections.emptyList()` / null guards.
- `Registry` → `LinkedHashSet` for deterministic module order.
- `InventoryUtils`/`Helpers`: remove dead methods (below) — less class loading and less to maintain.
- `DescriptionHandler.WIDTH` should be `static final`; `LanguageDocProvider` should stop at the first missing key (it
  already does) and cache per item if docs are added.
- `ItemComputerUpgrade.useComputer`: metadata-only upgrade avoids block replacement/particles and the phantom-tile
  window (U2).

---

## 4. Dead code and stale assets

| Item | Why dead |
|------|----------|
| `core/utils/InventoryUtils.java` (~240 of 276 lines) | Only `extractItems` is used; the rest is OpenMods copy-paste. |
| `core/utils/Helpers.java` (~100 of 114 lines) | Only `translateOrDefault` is used; `translateAny`, `twoWayCrafting`, `alternateCrafting`, `swapCharacters`, `nextId`, `equals`, `THREAD_PRIORITY` are unused. |
| `core/utils/IterableIterator.java` | No usages. |
| `core/utils/EntityPosition.java` | No usages. |
| `turtle/TurtlePosition.java` | No usages. |
| `core/turtle/LuaDirection.java` | No usages (replaced by `InteractDirection`). |
| `blocks/TileLazyNBT.java` | No subclasses; the lazy NBT feature was removed with the network code. |
| `items/ItemBase.getTag` | Unused. |
| `items/ItemToolHost.getMetadata(int)` | Pointless identity override. |
| `items/ToolHostPlayer.loadWholeInventory`/`unloadWholeInventory` | Deprecated wrappers, no callers. |
| `TurtleUpgradeToolHost.getItem(ITurtleAccess)` | Unused duplicate of `ToolHostPlayer.getItem`. |
| `IClientModule` + `Registry.RegisterWrapper*` | No client modules. |
| `FmlEvents.scheduleClient`/`clientQueue`/`onClientTick`/`onConfigChanged` | No producers/handlers. |
| `CCTweaks.NETWORK` | Channel created, no messages registered. |
| `CCTweaks.debug` | Config key never read. |
| `cctweaks:networkedBlock`, `cctweaks:dataCard`, `cctweaks:multipart` | Removed features, but the dev world still contains them; FML prompts on load. Needs a `FMLMissingMappingsEvent` handler (see §5). |
| `src/test/resources/.../core/network/data.json`, `core/patcher/*.lua` | Their tests were deleted in `acd88b6`. |
| `src/main/resources/assets/cctweaks/docs/en_US/**` | Nothing reads them; `LanguageDocProvider` reads `.information.N` lang keys, and none exist. |
| `gui.config.cctweaks.*` lang keys | No config GUI (documented as kept for a future factory). |
| `AGENTS.md` “Docs” table | References `docs/remove-reflection.md`, etc., but there is no `docs/` directory. |

**Estimate:** ~600–700 lines (≈13%) can be deleted with no behaviour change.

---

## 5. Features / gaps worth adding

1. **Restore NEI documentation.** The README/AGENTS claim NEI documentation, but `LanguageDocProvider` looks for
   `item.cctweaks.<name>.information.N` keys and `en_US.lang` has none. Either add those keys or add a
   `ResourceDocProvider` that reads `assets/cctweaks/docs/en_US/item/<name>.txt` / `tile/<name>.txt` (page `***`,
   bold `${**}` markup). The existing `debugger.txt` also describes the removed network visualiser — update it.
2. **Migration mappings for removed content.** The dev server log shows
   `Found a missing id from the world cctweaks:networkedBlock` / `dataCard` / `multipart` and an interactive FML
   prompt. Add an `FMLMissingMappingsEvent` handler on the FML bus that `warn()`s/`ignore()`s the old `cctweaks:*`
   names, so 2.x worlds load without a console prompt and the deletion is logged. Alternatively re-register hidden
   placeholder blocks/items to preserve the data. This is important for a modpack update.
3. **Config validation + config GUI.** Clamp `digFactor` (1–100), validate upgrade IDs (≥64, distinct, not
   colliding), and either add a `GuiConfigFactory` using the existing lang keys or drop the keys.
4. **Op-gate the debug wand**, or default `debugWandEnabled` to false; print a clear chat message when injection
   succeeds/fails.
5. **Enforce CC permissions in the tool host** (`isBlockEditable`, `turtleDisabledActions`) and add a config option
   for the manipulator’s placement/activation.
6. **Complete `canUse`** to mirror `doUse` (block activation/placeability), and add `canUse`/`canSwing` to
   `PortalGunIntegration`.
7. **Preserve data on upgrades**: overlays/hats, pocket upgrade ID + `upgradeData`, modded tile compatibility, and a
   chat/tooltip warning that upgrading restarts a running computer.
8. **Add a `tool_manipulator` API doc** (method list, arguments, return tuples) — currently only the NEI `.txt`
   describes duration/sneak.
9. **Dimension-change cache invalidation** for `ToolHostPlayer` (recreate when
   `player.worldObj != turtle.getWorld()`).
10. **Optional**: a `/cctweaks` command to inspect registered modules/config/upgrade IDs; useful on servers.

---

## 6. Testing plan

### 6.1 Unit tests (JUnit 5 + Mockito, no MC bootstrap)

Add `src/test/java/org/squiddev/cctweaks/testsupport/` with:

- `StubInventory` (fixed-size `IInventory`, stack-limit/dirty tracking)
- `StubTurtleAccess` (`ITurtleAccess`, synchronous `executeCommand`)
- `StubComputerAccess` (`IComputerAccess`, records queued events)
- `StubLuaContext` (runs `executeMainThreadTask` immediately and returns `MethodResult.of(...)`)
- `StubPeripheral` (configurable type/methods/attach log)
- Mockito mocks for `World`, `Block`, `IBlockAccess`, `MovingObjectPosition` where the test only needs a few
  methods.

Proposed classes:

| Test | Covers |
|------|--------|
| `PeripheralHelpersTest` (expand) | existing chains + **cycles**, null inputs, proxy+target |
| `PeripheralProxyTest` | lazy creation, attach-before-create replay, detach both states, default type, `equals` |
| `LuaResultsTest` | `wrap(Object[])`, `wrap(MethodResult)`, null |
| `WorldPositionTest` | equals/hash symmetry, different world/coords, mixed `IWorldPosition` |
| `TurtleRegistryTest` | item-specific before generic, first non-null wins, `canUse`/`canSwing` OR, null stack/registration |
| `ToolManipulatorPeripheralTest` | `toObjectArray`, method-name array, invalid method throws, duration/sneak parsing, `equals` includes side |
| `CraftingComputerUpgradeTest` | empty/None, normal computer + upgrade, two computers, advanced/Command rejected, turtle + <7 slots vs 7 slots, unsupported `IComputerItem` returns no match, recipe output non-null |
| `ItemComputerActionTest` | client sneak suppresses without effects; server not-sneaking; consumption; creative |
| `McEventsTest` | block consumer consumes+clears, drop chance applied once, entity consumer captures+clears, **canceled event ignored**, reset clears maps |
| `InventoryUtilsTest` | exact/partial/insufficient extraction, no mutation on failure, `markDirty`, new `countItems` |
| `TileLifecycleTest` | `validate`→create once; `invalidate`/`onChunkUnload`→destroy once; breakBlock+invalidate→one destroy; description NBT roundtrip |
| `TileDebugPeripheralTest` | per-side caching, out-of-range side, attach/detach set, `getSide` |
| `FmlEventsTest` | schedule runs next tick; throwing task logged, not propagated; reset clears; **`DelayedTasks.update` is ticked** |
| `LanguageDocProviderTest` | injected key lookup, pagination stops at first gap |
| `ApiSurfaceTest` | reflective pin of `ICCTweaksAPI`/`ITurtleRegistry`/`ITurtleInteraction` signatures (like the CC-Legacy `AddonApiSurfaceTest`) |
| `BuildArtifactTest` | built jar manifest contains `FMLAT: CCTweaks_at.cfg`; no `mixins.*.json`; `CCTweaks.DEPENDENCIES` matches `dependencies.gradle` CC version |

### 6.2 Headless harness / integration

- **Server smoke**: CI already runs `runServer` (aliased to `runServer25` by `addon.gradle.kts`). Add a test step
  that greps the server log for:
  - `cctweaks` in the FML mod list,
  - no `cctweaks`-related `ERROR`/`Exception`,
  - no missing-mapping prompt for a fresh world.
- **Optional Horizon-QA**: the GTNH reusable workflow already supports `horizonqa: true`. Add `@GameTest` cases for:
  - normal computer + upgrade → advanced, same ID/label, `term.isColour()`;
  - normal turtle + upgrade → advanced, same ID/inventory/fuel/overlays, `term.isColour()`;
  - tool host digs a grass/torch and the drop is in the turtle inventory;
  - tool host respects a claim (protection provider);
  - debug block side bindings.
- **Gradle**: add `tasks.withType<Test> { useJUnitPlatform() }` in `addon.gradle.kts` (otherwise JUnit 5 tests are
  silently not run), and a guard that fails if zero test XML files were produced (the CC-Legacy repo added exactly
  this for `verifyTestSuiteExecuted`).

### 6.3 Coverage

No JaCoCo task exists today. Add the JaCoCo plugin and publish `build/reports/jacoco/test/html`; target the pure
logic (helpers, registry, crafting matcher, upgrade NBT, events) first, then the fakes-backed manipulator paths.

---

## 7. Dependency audit

| Dependency | Current | Recommended | Notes |
|---|---|---|---|
| ComputerCraft-Legacy | `3.3.0:dev` | **3.3.0** | Latest tag. `main` has post-3.3.0 commits (fuel handlers, pocket upgrades, full modem), but no newer tag; bump when 3.4.0 lands. |
| CodeChickenCore | `1.4.10:dev` | **1.4.22:dev** | NEI 2.9.2 declares 1.4.22. 1.5.0 is newer but not what NEI 2.9.2 was built against; test before using it. |
| NotEnoughItems | `2.7.4-GTNH:dev` | **2.9.2-GTNH:dev** | Latest; I compiled the tree against 2.9.0 + CCC 1.4.22 successfully. One NEI deprecation warning in `DescriptionHandler`; clean it up with the newer API. |
| PortalGun | `curse.maven:PortalGun-229084:2480016` | **2480016** | Latest 1.7.10 file (4.0.0-beta-6-fix-1), confirmed via CurseForge widget. |
| iChunUtil | `curse.maven:iChunUtil-229060:2338149` | **2338149** | The widget lists 2338148 as the newest 1.7.10 file; verify 2338149 exists / is newer, otherwise pin 2338148. |
| JUnit | `junit:junit:4.13.1` | **JUnit 5.13.x** | Use `testImplementation` + `useJUnitPlatform()`; 4.13.2 if staying on JUnit 4. |
| Mockito | none | **4.11.0** (Java 8-safe) or **5.24.0** | Add for fakes/mocks. |
| GTNH settings plugin | `2.0.34` | **2.0.34** | Latest; ExampleMod is on 2.0.33. |
| Gradle wrapper | `9.7.1` | **9.7.1** | Matches ExampleMod; 9.9.0 is current but test GTNHGradle compatibility before bumping. |
| blowdryerTag | `0.2.2` | **0.2.2** | Matches ExampleMod. |
| Forge | `10.13.4.1614` | **10.13.4.1614** | Latest 1.7.10. |
| MCP mappings | `stable/12` | **stable/12** | Matches GTNH template. |

Also:
- `testCompile` is deprecated in Gradle 9 — switch to `testImplementation`.
- CI’s reusable workflow rejects `-pre` dependency versions; none are present.
- `./gradlew updateDependencies` exists if you want GTNH Maven bumps automated; review the diff, since it can pull
  pre-releases.

---

## 8. Suggested order of work

1. **P0 correctness (small, high value):** T1 entity ray, T2 attack attributes, T3 dig drop capture, T4/T5/T6
   protection & disabled actions, U1 turtle family, U3/U4 upgrade NBT, D1 wand consumption, E1 canceled drops.
2. **P0 release safety:** `FMLMissingMappingsEvent` for removed 2.x blocks/items; config ID/`digFactor` validation.
3. **P1 data integrity:** U2 metadata-only computer upgrade, U5/U6 crafting matcher/output, U8 modded tile guards,
   U7 gold ordering.
4. **P1 security:** D2 op gate/default-off, D3 documented race, T4/T5 permissions.
5. **P2 testability:** `IToolHostPlayer` extraction, `Stub*` harness, JUnit 5 + `useJUnitPlatform`, unit tests, CI
   server smoke/optional Horizon-QA.
6. **P3 cleanup:** delete dead code/resources, fix `TileBase` lifecycle, `Registry` ordering, `FmlEvents`
   reset/exception handling, `PeripheralHelpers` cycles, `PeripheralProxy` locking.
7. **P4 deps/docs:** NEI/CCC bump + verify in client, JUnit/Mockito, README/AGENTS corrections, NEI docs provider,
   config GUI.

The P0 list is about a dozen focused edits plus tests, and P0 + the test harness would make a good first PR.

---

## Batch 1 implementation status

Implemented on this branch (with 83 headless tests, `./gradlew build` clean, and a clean
`runServer25` bootstrap):

- **Tool Host / Manipulator:** entity ray direction fixed; held-item attributes applied; dig drop
  capture covers instant-break blocks and is removed in `finally`; `isBlockEditable` and
  `turtleDisabledActions` checks; placement protection; world-change player invalidation; side-aware
  equality; inventory-size guards; invalid-method `LuaException`; cached method table; argument
  parsing extracted and tested.
- **Upgrade items:** computer upgrade now changes the metadata family bit in place and recreates the
  `ServerComputer` (ID/label/on-state preserved); turtle upgrade recreates the advanced
  `ServerComputer` and rolls back the moved flag on failure; turtle overlays/hats and pocket
  `upgrade`/`upgradeData` are preserved; unsupported `IComputerItem`s are rejected at match time;
  `getRecipeOutput` is non-null; gold is counted before extraction and refunded on failure;
  protection checks; modded tile/block guards.
- **Debug wand:** no longer consumed; `isUsable` range check; failures logged.
- **Events/lifecycle:** `LivingDropsEvent` cancellation respected; `McEvents`/`FmlEvents` reset on
  server start/stop; `TileBase` destroys once; `Registry` uses `LinkedHashSet` and snapshots;
  `FmlEvents` exception isolation; `PeripheralHelpers` cycle guard; `PeripheralProxy` locking;
  `PortalGunIntegration.canUse`/`canSwing`; config
  validation; `FMLMissingMappingsEvent` handler for removed 2.x names.
- **Build/tests:** JUnit 5 + Mockito, `useJUnitPlatform`, 26 test classes/103 tests, headless
  `StubTurtleAccess`/`StubComputerAccess` harness, API/AT/dependency guard tests.
- **Dependencies:** NEI 2.9.2-GTNH, CodeChickenCore 1.4.22, JUnit 5.13.4, Mockito 4.11.0.

Remaining from the audit (next batches): T7 `use(duration)` wait semantics, T12 complete `canUse`,
T13 opposite-side fallback, D2/D3 debug-wand privilege/threading, E9 `IPeripheralHost` inventory
shadowing, NEI documentation provider, dead-code/resource cleanup, config GUI, and in-world
integration coverage (tool host mining/placement, debug wand injection, NEI GUI).

### Post-batch in-game corrections

- **E11 revert:** the batch-1 client-side suppression in `ItemComputerAction` was wrong — returning
  `true` from `onItemUseFirst` on the client makes `PlayerControllerMP.onPlayerRightClick` return
  before queueing the C08 packet, so the server never ran the upgrade/wand action. The client now
  always returns `false` (no GUI flicker: sneaking skips `onBlockActivated`, and
  `TileComputerBase.onActivate` returns `false` while sneaking). E11 as originally filed was a
  non-issue.
- **Portal gun click semantics:** `swing` now fires portal type 1 and `use` type 2, matching
  PortalGun's left=blue/right=orange keybind semantics. Previously `use` keyed off the held item
  (`ItemPortalGunBlue` → 1), so both actions fired blue with a blue gun.

## Batch 2 implementation status

- **Debug wand (D1–D4):** every failure path now reports a distinct chat message (out of range,
  not started, switched off, unsupported Lua machine, injection error) and success names the
  computer id, so a silent wand is diagnosable in-game. Added `computer.debugWandRequiresOp`
  (default false) as the op gate for D2; D3 (the `LuaState` write racing the computer thread) is
  documented in `AGENTS.md` rather than "fixed", because CC exposes no "run on the computer thread"
  hook. The NEI doc text no longer describes the deleted network visualiser.
- **T7 `use(duration)`:** `DelayedTask` never worked (it reads `delay` after the task has run, and
  `LuaEnvironment.sleep` drops the `MethodResult` that `executeTask` returns). `use` now chains two
  suspensions — the main-thread task, then a no-op delayed task — via `MethodResult.pullEvent`, so
  `duration` really holds the Lua call for that many ticks.
- **T12 `canUse`:** now mirrors `use`: registered interactions, an entity in range with an item,
  placeable item blocks (with `isBlockEditable`), and tile-entity blocks (block activation cannot be
  probed without side effects, which is documented on the method).
- **T13 opposite-side fallback:** gone. `resolveSide` keeps `hit.sideHit` when valid and otherwise
  derives the face from the hit vector, so a ray that starts inside a block no longer activates or
  places against the far face or fires twice.
- **NEI documentation:** added `ResourceDocProvider`, reading
  `assets/cctweaks/docs/<lang>/{item,tile}/<name>.txt` with `***` page breaks and `${**}`/`${*}`
  markup. This is the provider the shipped docs were written for; without it the NEI page was
  always empty because `en_US.lang` has no `.information.N` keys.
- **Dead code:** deleted `IterableIterator`, `EntityPosition`, `TurtlePosition`, `LuaDirection`,
  `TileLazyNBT`, `IClientModule` + the `Registry` client wrappers, the unused half of
  `InventoryUtils`/`Helpers`, `ItemBase.getTag`, `ItemToolHost.getMetadata`,
  `ToolHostPlayer.loadWholeInventory`/`unloadWholeInventory`, `TurtleUpgradeToolHost.getItem`, and
  the orphaned `src/test/resources` fixtures.

Still open: E9 (`IPeripheralHost` inventory shadowing — documented, needs a CC-side fix), config GUI,
and real in-world integration coverage.
