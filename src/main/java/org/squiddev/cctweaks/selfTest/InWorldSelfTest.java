package org.squiddev.cctweaks.selfTest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.turtle.ToolHostPlayer;
import org.squiddev.cctweaks.turtle.TurtleUpgradeToolHost;
import org.squiddev.cctweaks.turtle.TurtleUpgradeToolManipulator;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.permissions.ITurtlePermissionProvider;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleVerb;
import dan200.computercraft.core.lua.ILuaMachine;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;

/**
 * In-world self test.
 *
 * <p>
 * The unit tests cover everything that can run without a Minecraft bootstrap. They cannot cover
 * {@link ToolHostPlayer}, block/tile placement or the upgrade items, because those need a real
 * {@code WorldServer}, a real {@code ItemInWorldManager} and real ComputerCraft tiles. This class
 * fills that gap: it builds a small scratch platform near the world spawn, drives the real code
 * against the real world, and restores every block it touched.
 * </p>
 *
 * <p>
 * It is invoked either from {@code /cctweaks selftest} or automatically at server start when
 * {@code -Dcctweaks.selftest=true} is set (see {@code scripts/headless-smoke.sh}). Every check is
 * independent: a throwing check is recorded as a failure and the rest still run.
 * </p>
 */
public final class InWorldSelfTest {

    /**
     * Prefix of every line this class writes to the log. The headless harness greps for it.
     */
    public static final String MARKER = "CCTWEAKS-SELFTEST";

    /**
     * Horizontal/vertical extent of the scratch area, relative to the origin. The zone is cleared
     * (set to air) before the checks run and restored to the original blocks afterwards.
     */
    private static final int MIN_X = -1, MAX_X = 7;
    private static final int MIN_Y = -1, MAX_Y = 2;
    private static final int MIN_Z = -1, MAX_Z = 1;

    private final WorldServer world;
    private final int originX;
    private final int originY;
    private final int originZ;

    /**
     * The permission provider is a global hook that cannot be unregistered, so it is installed
     * once and denies only while {@link #denyEnabled} is set for one coordinate.
     */
    private static volatile boolean denyEnabled;
    private static volatile int denyX, denyY, denyZ;
    private static boolean denyProviderRegistered;

    private final List<String> failures = new ArrayList<String>();
    private final List<String> notes = new ArrayList<String>();
    private int passed;

    private InWorldSelfTest(WorldServer world, int originX, int originY, int originZ) {
        this.world = world;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
    }

    /**
     * Build a self test around the world spawn: a few blocks up and a few blocks away, so it never
     * runs inside terrain, and close enough to spawn that the chunks are already loaded.
     */
    public static InWorldSelfTest atSpawn(WorldServer world) {
        ChunkCoordinates spawn = world.getSpawnPoint();
        int x = spawn.posX + 8;
        int z = spawn.posZ + 8;
        int y = world.getTopSolidOrLiquidBlock(x, z) + 4;
        return new InWorldSelfTest(world, x, y, z);
    }

    public static InWorldSelfTest at(WorldServer world, int x, int y, int z) {
        return new InWorldSelfTest(world, x, y, z);
    }

    /**
     * Run every check, restoring the world afterwards.
     *
     * @return The number of failed checks (0 on success)
     */
    public int run() {
        CCTweaks.logger.info(
            "{} start at ({}, {}, {}) in dimension {}",
            MARKER,
            originX,
            originY,
            originZ,
            world.provider.dimensionId);

        Block[] blocks = new Block[area()];
        int[] metadata = new int[area()];
        boolean tiles = snapshot(blocks, metadata);

        try {
            clearArea();
            if (!tiles) {
                notes.add("scratch area contained tile entities; they were not restored");
            }

            check("toolhost-inventory", this::checkInventoryRoundTrip);
            check("toolhost-attributes", this::checkAttributeModifiers);
            check("toolhost-dig-stone", this::checkDigStone);
            check("toolhost-dig-unbreakable", this::checkDigUnbreakable);
            check("toolhost-dig-nothing", this::checkDigNothing);
            check("toolhost-dig-protected", this::checkDigProtected);
            check("toolhost-attack", this::checkAttack);
            check("toolhost-upgrade-dig", this::checkUpgradeDig);
            check("toolhost-upgrade-disabled", this::checkUpgradeDisabled);
            check("toolhost-manipulator-peripheral", this::checkManipulatorPeripheral);
            check("debug-wand-refused-off", this::checkDebugWandOff);
            check("debug-wand-inject", this::checkDebugWandInject);
            check("upgrade-computer", this::checkUpgradeComputer);
            check("upgrade-turtle-creative", this::checkUpgradeTurtleCreative);
            check("upgrade-turtle-gold", this::checkUpgradeTurtleGold);
            check("upgrade-unsupported-tile", this::checkUpgradeUnsupported);
            check("registry-modules", this::checkRegistry);
        } catch (Throwable throwable) {
            CCTweaks.logger.error("{} aborted", MARKER, throwable);
            failures.add("aborted: " + throwable);
        } finally {
            restore(blocks, metadata);
        }

        for (String note : notes) CCTweaks.logger.info("{} NOTE {}", MARKER, note);
        CCTweaks.logger.info("{} RESULT {} passed, {} failed", MARKER, passed, failures.size());
        for (String failure : failures) CCTweaks.logger.error("{} FAIL {}", MARKER, failure);

        return failures.size();
    }

    // region Checks

    private void checkInventoryRoundTrip() {
        SelfTestTurtle turtle = turtle(originX, originY, originZ);
        ItemStack stone = new ItemStack(Blocks.stone, 3);
        turtle.setSlot(0, stone);

        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, 3, false);
        assertTrue(player.getItem(turtle) != null, "held item should be the turtle's selected slot");
        assertTrue(player.getItem(turtle).stackSize == 3, "held item should keep its stack size");

        // The fake player must start clean outside the turtle's slots, or unloading would duplicate.
        assertTrue(player.inventory.getStackInSlot(20) == null, "fake inventory should start empty");

        player.inventory.setInventorySlotContents(20, new ItemStack(Items.apple, 2));
        player.unload(turtle);

        assertTrue(turtle.getSlot(0) != null && turtle.getSlot(0).stackSize == 3, "turtle slot should survive");
        assertTrue(turtle.count(Items.apple) == 2, "overflow items should be stored in the turtle");
    }

    private void checkAttributeModifiers() {
        SelfTestTurtle turtle = turtle(originX, originY, originZ);
        turtle.setSlot(0, new ItemStack(Items.diamond_sword));

        ToolHostPlayer player = new ToolHostPlayer(turtle);
        double base = player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.attackDamage)
            .getAttributeValue();

        player.load(turtle, 3, false);
        double armed = player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.attackDamage)
            .getAttributeValue();
        player.unload(turtle);
        double unloaded = player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.attackDamage)
            .getAttributeValue();

        assertTrue(armed > base, "a diamond sword should add attack damage (" + base + " -> " + armed + ")");
        assertTrue(
            Math.abs(unloaded - base) < 1e-6,
            "unloading should remove the attribute modifiers (" + unloaded + " != " + base + ")");
    }

    private void checkDigStone() {
        int x = originX, y = originY - 1, z = originZ;
        world.setBlock(x, y, z, Blocks.stone, 0, 3);

        SelfTestTurtle turtle = turtle(originX, originY, originZ);
        turtle.setSlot(0, new ItemStack(Items.diamond_pickaxe));

        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, 0, false);

        int calls = 0;
        TurtleCommandResult result = null;
        while (calls < 60 && !world.isAirBlock(x, y, z)) {
            result = player.dig(turtle, 0);
            calls++;
            assertTrue(result.isSuccess(), "dig should report success while breaking a block");
        }
        player.unload(turtle);

        assertTrue(world.isAirBlock(x, y, z), "stone should be broken after " + calls + " dig calls");
        assertTrue(
            turtle.count(Item.getItemFromBlock(Blocks.cobblestone)) > 0,
            "the cobblestone drop should end up in the turtle, not the world");
        notes.add("dig-stone took " + calls + " call(s)");
    }

    private void checkDigUnbreakable() {
        int x = originX, y = originY - 1, z = originZ;
        world.setBlock(x, y, z, Blocks.bedrock, 0, 3);

        SelfTestTurtle turtle = turtle(originX, originY, originZ);
        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, 0, false);
        TurtleCommandResult result = player.dig(turtle, 0);
        player.unload(turtle);

        assertTrue(!result.isSuccess(), "bedrock must not be diggable");
        assertTrue(!world.isAirBlock(x, y, z), "bedrock must survive");
    }

    private void checkDigNothing() {
        int x = originX, y = originY - 1, z = originZ;
        world.setBlockToAir(x, y, z);

        SelfTestTurtle turtle = turtle(originX, originY, originZ);
        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, 0, false);
        TurtleCommandResult result = player.dig(turtle, 0);
        player.unload(turtle);

        assertTrue(!result.isSuccess(), "digging air must fail");
    }

    /**
     * Registers a permission provider that denies exactly the scratch position, then digs there.
     * This is the T4 regression: the tool host must consult {@code ComputerCraft.isBlockEditable}
     * just like CC's own {@code TurtleTool} does.
     */
    private void checkDigProtected() {
        final int x = originX, y = originY - 1, z = originZ;
        world.setBlock(x, y, z, Blocks.stone, 0, 3);

        registerDenyProvider();

        final boolean previous = ComputerCraft.turtlesObeyBlockProtection;
        ComputerCraft.turtlesObeyBlockProtection = true;
        denyX = x;
        denyY = y;
        denyZ = z;
        denyEnabled = true;

        try {
            SelfTestTurtle turtle = turtle(originX, originY, originZ);
            turtle.setSlot(0, new ItemStack(Items.diamond_pickaxe));
            ToolHostPlayer player = new ToolHostPlayer(turtle);
            player.load(turtle, 0, false);
            TurtleCommandResult result = player.dig(turtle, 0);
            player.unload(turtle);

            assertTrue(!result.isSuccess(), "a protected block must not be diggable");
            assertTrue(!world.isAirBlock(x, y, z), "the protected block must survive");
        } finally {
            denyEnabled = false;
            ComputerCraft.turtlesObeyBlockProtection = previous;
        }
    }

    private static synchronized void registerDenyProvider() {
        if (denyProviderRegistered) return;
        denyProviderRegistered = true;

        ComputerCraft.registerPermissionProvider(new ITurtlePermissionProvider() {

            @Override
            public boolean isBlockEnterable(net.minecraft.world.World world, int x, int y, int z) {
                return true;
            }

            @Override
            public boolean isBlockEditable(net.minecraft.world.World world, int x, int y, int z) {
                return !(denyEnabled && x == denyX && y == denyY && z == denyZ);
            }
        });
    }

    private void checkAttack() {
        int x = originX + 2, z = originZ;
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(x + 0.5, originY, z + 0.5);
        world.spawnEntityInWorld(zombie);

        SelfTestTurtle turtle = turtle(originX, originY, originZ);
        turtle.setSlot(0, new ItemStack(Items.diamond_sword));

        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, 3, false);
        try {
            int hits = 0;
            // `isDead` is only set once the 20-tick death animation finishes, which needs world
            // ticks. Health is the immediate signal that the attack landed and killed the mob.
            while (zombie.getHealth() > 0 && hits < 40) {
                // The world is not ticking between hits, so EntityLivingBase's 10-tick
                // invulnerability window would swallow every attack after the first one.
                zombie.hurtResistantTime = 0;
                player.attack(turtle, zombie);
                hits++;
            }

            assertTrue(
                zombie.getHealth() <= 0,
                "a diamond sword should kill a zombie within 40 hits (" + hits
                    + " hit(s), "
                    + zombie.getHealth()
                    + " hp left)");
            notes.add("attack killed the zombie in " + hits + " hit(s)");
        } finally {
            player.unload(turtle);
            if (zombie.getHealth() > 0) zombie.setDead();
        }

        int flesh = turtle.count(Items.rotten_flesh);
        if (flesh == 0) {
            // Zombie loot is randomly skipped by `EntityZombie.dropFewItems`, so this is a note
            // rather than a failure: a missing drop does not mean the consumer is broken.
            notes.add("attack produced no rotten flesh (loot is randomised)");
        }
    }

    /**
     * The turtle-facing entry point: {@code turtle.dig()} on a turtle with the tool host upgrade.
     */
    private void checkUpgradeDig() {
        int x = originX, y = originY - 1, z = originZ;
        world.setBlock(x, y, z, Blocks.stone, 0, 3);

        SelfTestTurtle turtle = turtle(originX, originY, originZ);
        turtle.setSlot(0, new ItemStack(Items.diamond_pickaxe));

        TurtleUpgradeToolHost upgrade = new TurtleUpgradeToolHost();
        TurtleCommandResult result = null;
        int calls = 0;
        while (calls < 40 && !world.isAirBlock(x, y, z)) {
            result = upgrade.useTool(turtle, TurtleSide.Left, TurtleVerb.Dig, 0);
            calls++;
            assertTrue(result != null, "useTool must never return null");
            assertTrue(result.isSuccess(), "the tool host upgrade should dig a stone block");
        }

        assertTrue(world.isAirBlock(x, y, z), "the tool host upgrade should break stone (" + calls + " calls)");
        assertTrue(
            turtle.count(Item.getItemFromBlock(Blocks.cobblestone)) > 0,
            "the drop should be stored in the turtle");
    }

    private void checkUpgradeDisabled() {
        boolean previous = CCTweaks.thEnabled;
        CCTweaks.thEnabled = false;
        try {
            TurtleCommandResult result = new TurtleUpgradeToolHost()
                .useTool(turtle(originX, originY, originZ), TurtleSide.Left, TurtleVerb.Dig, 0);

            assertTrue(result != null, "a disabled tool host must still return a result");
            assertTrue(!result.isSuccess(), "a disabled tool host must refuse to dig");
        } finally {
            CCTweaks.thEnabled = previous;
        }
    }

    private void checkManipulatorPeripheral() {
        TurtleUpgradeToolManipulator upgrade = new TurtleUpgradeToolManipulator();
        assertTrue(upgrade.alsoPeripheral(), "the manipulator must be mounted as a peripheral");

        IPeripheral peripheral = upgrade.createPeripheral(turtle(originX, originY, originZ), TurtleSide.Right);
        assertTrue(peripheral != null, "the manipulator upgrade should create a peripheral");
        assertTrue("tool_manipulator".equals(peripheral.getType()), "the peripheral type should be tool_manipulator");
        assertTrue(peripheral.getMethodNames().length == 12, "the manipulator should expose 12 methods");

        assertTrue(
            new TurtleUpgradeToolHost().createPeripheral(turtle(originX, originY, originZ), TurtleSide.Left) == null,
            "the plain tool host is not a peripheral");
    }

    /**
     * The wand must refuse a computer that has not been switched on yet, with a distinct message
     * (the return value is what a caller sees; the message itself is unit tested).
     */
    /**
     * A fake player standing next to {@code (x, y, z)}, which is what a player using the wand looks
     * like: the wand's {@code isUsable} range check fails for a player still at the origin.
     */
    private ToolHostPlayer wandPlayer(int x, int y, int z) {
        ToolHostPlayer player = new ToolHostPlayer(turtle(originX, originY, originZ));
        player.capabilities.isCreativeMode = true;
        player.setSneaking(true);
        player.setPosition(x + 0.5, y + 0.5, z + 0.5);
        return player;
    }

    private void checkDebugWandOff() {
        int x = originX + 4, y = originY, z = originZ;
        TileComputerBase computer = placeComputer(x, y, z);

        ToolHostPlayer player = wandPlayer(x, y, z);
        assertTrue(computer.isUsable(player, false), "the stand-in player should be within reach of the computer");

        boolean used = Registry.itemDebugger
            .onItemUseFirst(new ItemStack(Registry.itemDebugger), player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f);

        assertTrue(!used, "the debug wand must not report success on a computer that is off");
    }

    /**
     * Injects the Cobalt debug library into a running computer, which is the one code path that
     * cannot be unit tested: it needs a started {@code ServerComputer} with a real Cobalt machine.
     */
    private void checkDebugWandInject() throws Exception {
        int x = originX + 4, y = originY, z = originZ;
        ServerComputer server = placeComputer(x, y, z).getServerComputer();
        assertTrue(server != null, "the computer should have a server instance");
        assertTrue(!server.isOn(), "the computer should start switched off");

        server.turnOn();

        // The world is not ticking, so the tile never calls ServerComputer.update() - the call
        // that advances the computer's state machine. Do it here, and sleep so CC's own computer
        // thread can build the Lua machine in between. Sleeping is safe: this is a diagnostic
        // path, not a tick-critical one.
        long deadline = System.currentTimeMillis() + 20000;
        while (server.getComputer() == null || !server.getComputer()
            .isOn()
            || server.getComputer()
                .getLuaMachine() == null) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("the computer did not start within 20 seconds");
            }
            server.update();
            Thread.sleep(20);
        }

        ILuaMachine machine = server.getComputer()
            .getLuaMachine();
        assertTrue(machine != null, "a running computer should have a Lua machine");
        assertTrue(server.isOn(), "the computer should report that it is on");

        ToolHostPlayer player = wandPlayer(x, y, z);
        ItemStack wand = new ItemStack(Registry.itemDebugger);
        boolean used = Registry.itemDebugger.onItemUseFirst(wand, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f);
        assertTrue(used, "the debug wand should inject into a running computer");

        // The wand is a reusable tool: a successful use must not consume it.
        assertTrue(wand.stackSize == 1, "the debug wand must not be consumed");

        // A second injection must work as well (idempotent library install).
        assertTrue(
            Registry.itemDebugger.onItemUseFirst(wand, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f),
            "injecting twice should still succeed");
    }

    /**
     * Place a computer block and make sure its {@link ServerComputer} exists. A freshly placed
     * tile is only validated (and its server computer created) on the next world tick, which the
     * self test does not get, so this creates it directly - the same call CC makes from
     * {@code validate()} and that the upgrade item makes after an in-place upgrade.
     */
    private TileComputerBase placeComputer(int x, int y, int z) {
        world.setBlockToAir(x, y, z);
        world.setBlock(x, y, z, ComputerCraft.Blocks.computer, 0, 3);

        TileEntity tile = world.getTileEntity(x, y, z);
        assertTrue(tile instanceof TileComputerBase, "placing a computer block should create a computer tile");

        TileComputerBase computer = (TileComputerBase) tile;
        if (computer.getServerComputer() == null) computer.createServerComputer();
        return computer;
    }

    private void checkUpgradeComputer() {
        int x = originX + 4, y = originY, z = originZ;
        world.setBlock(x, y, z, ComputerCraft.Blocks.computer, 0, 3);

        TileEntity tile = world.getTileEntity(x, y, z);
        assertTrue(tile instanceof TileComputerBase, "placing a computer block should create a computer tile");
        assertTrue(
            ((TileComputerBase) tile).getFamily() == ComputerFamily.Normal,
            "the placed computer should start off normal");

        ToolHostPlayer player = new ToolHostPlayer(turtle(originX, originY, originZ));
        player.capabilities.isCreativeMode = true;
        player.setSneaking(true);

        ItemStack upgrade = new ItemStack(Registry.itemComputerUpgrade);
        boolean used = Registry.itemComputerUpgrade
            .onItemUseFirst(upgrade, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f);

        assertTrue(used, "the computer upgrade should apply to a normal computer");
        assertTrue((world.getBlockMetadata(x, y, z) & 8) != 0, "the advanced metadata bit should be set");

        TileEntity upgraded = world.getTileEntity(x, y, z);
        assertTrue(upgraded instanceof TileComputerBase, "the tile should survive the in-place upgrade");
        assertTrue(
            ((TileComputerBase) upgraded).getFamily() == ComputerFamily.Advanced,
            "the tile should now be an advanced computer");
        assertTrue(upgraded == tile, "the in-place upgrade must keep the same tile instance");

        // A second application must be a no-op, not a second upgrade.
        assertTrue(
            !Registry.itemComputerUpgrade.onItemUseFirst(upgrade, player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f),
            "an advanced computer must not be upgradeable again");
    }

    private void checkUpgradeTurtleCreative() {
        int x = originX + 6, y = originY, z = originZ;
        world.setBlock(x, y, z, ComputerCraft.Blocks.turtle, 0, 3);

        TileEntity tile = world.getTileEntity(x, y, z);
        assertTrue(tile instanceof TileTurtle, "placing a turtle block should create a turtle tile");

        ToolHostPlayer player = new ToolHostPlayer(turtle(originX, originY, originZ));
        player.capabilities.isCreativeMode = true;
        player.setSneaking(true);

        boolean used = Registry.itemComputerUpgrade
            .onItemUseFirst(new ItemStack(Registry.itemComputerUpgrade), player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f);

        assertTrue(used, "the computer upgrade should apply to a normal turtle");
        assertTrue(
            world.getBlock(x, y, z) == ComputerCraft.Blocks.turtleAdvanced,
            "the turtle block should be replaced with the advanced turtle");

        TileEntity upgraded = world.getTileEntity(x, y, z);
        assertTrue(upgraded instanceof TileTurtle, "the upgraded block should still be a turtle tile");
        assertTrue(
            ((TileTurtle) upgraded).getFamily() == ComputerFamily.Advanced,
            "the turtle should report the advanced family");
    }

    private void checkUpgradeTurtleGold() {
        int x = originX + 6, y = originY, z = originZ;
        world.setBlock(x, y, z, ComputerCraft.Blocks.turtle, 0, 3);
        assertTrue(world.getBlock(x, y, z) == ComputerCraft.Blocks.turtle, "the turtle should be normal again");

        ToolHostPlayer player = new ToolHostPlayer(turtle(originX, originY, originZ));
        player.capabilities.isCreativeMode = false;
        player.setSneaking(true);
        player.inventory.setInventorySlotContents(0, null);

        // No gold: must be refused without consuming the upgrade or changing the block.
        assertTrue(
            !Registry.itemComputerUpgrade.onItemUseFirst(
                new ItemStack(Registry.itemComputerUpgrade),
                player,
                world,
                x,
                y,
                z,
                1,
                0.5f,
                0.5f,
                0.5f),
            "the turtle upgrade should be refused without 7 gold");
        assertTrue(
            world.getBlock(x, y, z) == ComputerCraft.Blocks.turtle,
            "a refused upgrade must not replace the turtle");

        // Seven ingots in seven slots (the recipe counts slots, see CraftingComputerUpgrade).
        for (int i = 1; i <= 7; i++) player.inventory.setInventorySlotContents(i, new ItemStack(Items.gold_ingot));

        boolean used = Registry.itemComputerUpgrade
            .onItemUseFirst(new ItemStack(Registry.itemComputerUpgrade), player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f);

        assertTrue(used, "the turtle upgrade should succeed with 7 gold ingots");
        assertTrue(
            world.getBlock(x, y, z) == ComputerCraft.Blocks.turtleAdvanced,
            "the turtle should be advanced after paying");
        int gold = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.gold_ingot) gold += stack.stackSize;
        }
        assertTrue(gold == 0, "all seven gold ingots should have been consumed (" + gold + " left)");
    }

    /**
     * The upgrade must refuse a tile it does not understand instead of consuming the item.
     */
    private void checkUpgradeUnsupported() {
        int x = originX + 4, y = originY, z = originZ;
        world.setBlock(x, y, z, Blocks.chest, 0, 3);
        world.setBlockToAir(x, y + 1, z);

        ToolHostPlayer player = new ToolHostPlayer(turtle(originX, originY, originZ));
        player.capabilities.isCreativeMode = true;
        player.setSneaking(true);

        boolean used = Registry.itemComputerUpgrade
            .onItemUseFirst(new ItemStack(Registry.itemComputerUpgrade), player, world, x, y, z, 1, 0.5f, 0.5f, 0.5f);
        assertTrue(!used, "a chest must not be upgradeable");
        assertTrue(world.getBlock(x, y, z) == Blocks.chest, "the chest must be left alone");
    }

    private void checkRegistry() {
        assertTrue(CCTweaks.digFactor >= 1, "digFactor should be validated to at least 1");
        assertTrue(
            Registry.modules()
                .size() >= 5,
            "the built-in modules should be registered");
        assertTrue(Registry.itemComputerUpgrade != null, "the computer upgrade item should exist");
    }

    // endregion

    // region Helpers

    private interface Check {

        void run() throws Exception;
    }

    private void check(String name, Check check) {
        long start = System.nanoTime();
        try {
            check.run();
            passed++;
            CCTweaks.logger.info("{} PASS {} ({} ms)", MARKER, name, millis(start));
        } catch (Throwable throwable) {
            failures.add(name + ": " + throwable);
            CCTweaks.logger.error("{} FAIL {} ({} ms)", MARKER, name, millis(start), throwable);
        }
    }

    private static long millis(long start) {
        return (System.nanoTime() - start) / 1000000L;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private SelfTestTurtle turtle(int x, int y, int z) {
        SelfTestTurtle turtle = new SelfTestTurtle(world, 16).setPosition(x, y, z);
        turtle.setDirection(3);
        return turtle;
    }

    private int area() {
        return (MAX_X - MIN_X + 1) * (MAX_Y - MIN_Y + 1) * (MAX_Z - MIN_Z + 1);
    }

    private int index(int dx, int dy, int dz) {
        int sx = MAX_X - MIN_X + 1, sy = MAX_Y - MIN_Y + 1;
        return ((dx - MIN_X) * sy + (dy - MIN_Y)) * (MAX_Z - MIN_Z + 1) + (dz - MIN_Z);
    }

    private boolean snapshot(Block[] blocks, int[] metadata) {
        boolean clean = true;
        for (int dx = MIN_X; dx <= MAX_X; dx++) {
            for (int dy = MIN_Y; dy <= MAX_Y; dy++) {
                for (int dz = MIN_Z; dz <= MAX_Z; dz++) {
                    int x = originX + dx, y = originY + dy, z = originZ + dz;
                    if (world.getTileEntity(x, y, z) != null) clean = false;
                    blocks[index(dx, dy, dz)] = world.getBlock(x, y, z);
                    metadata[index(dx, dy, dz)] = world.getBlockMetadata(x, y, z);
                }
            }
        }
        return clean;
    }

    private void clearArea() {
        for (int dx = MIN_X; dx <= MAX_X; dx++) {
            for (int dy = MIN_Y; dy <= MAX_Y; dy++) {
                for (int dz = MIN_Z; dz <= MAX_Z; dz++) {
                    world.setBlockToAir(originX + dx, originY + dy, originZ + dz);
                }
            }
        }
    }

    private void restore(Block[] blocks, int[] metadata) {
        try {
            for (int dx = MIN_X; dx <= MAX_X; dx++) {
                for (int dy = MIN_Y; dy <= MAX_Y; dy++) {
                    for (int dz = MIN_Z; dz <= MAX_Z; dz++) {
                        world.setBlock(
                            originX + dx,
                            originY + dy,
                            originZ + dz,
                            blocks[index(dx, dy, dz)],
                            metadata[index(dx, dy, dz)],
                            3);
                    }
                }
            }
        } catch (Throwable throwable) {
            CCTweaks.logger.error("{} could not restore the scratch area", MARKER, throwable);
        }
    }

    // endregion
}
