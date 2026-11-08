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
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.core.turtle.TurtleRegistry;
import org.squiddev.cctweaks.turtle.ToolHostPlayer;
import org.squiddev.cctweaks.turtle.TurtleUpgradeToolHost;
import org.squiddev.cctweaks.turtle.TurtleUpgradeToolManipulator;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.permissions.ITurtlePermissionProvider;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleVerb;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
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
            check("bucket-fill", this::checkBucketFill);
            check("bucket-place", this::checkBucketPlace);
            check("bucket-place-supported-no-hit", this::checkBucketPlaceSupportedNoHit);
            check("bucket-place-unsupported-no-hit", this::checkBucketPlaceUnsupportedNoHit);
            check("bucket-not-a-source", this::checkBucketNotASource);
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
     * A tool host holding an empty bucket fills it from the fluid source in front of it. Driven
     * through the real {@link ITurtleRegistry}, so this also proves the interaction is registered.
     */
    private void checkBucketFill() throws Exception {
        int x = originX + 1, y = originY, z = originZ;
        world.setBlock(x, y, z, Blocks.water, 0, 3);

        SelfTestTurtle turtle = turtle(x, y + 1, z);
        turtle.setDirection(0);
        turtle.setSlot(0, new ItemStack(Items.bucket));

        TurtleCommandResult result = useBucket(turtle, 0);
        assertTrue(result != null, "the bucket interaction should handle an empty bucket on water");
        assertTrue(result.isSuccess(), "filling a bucket from a water source should succeed");

        assertTrue(world.isAirBlock(x, y, z), "the water source should have been drained");
        assertTrue(turtle.count(Items.water_bucket) == 1, "the turtle should hold a water bucket");
        assertTrue(turtle.count(Items.bucket) == 0, "the empty bucket should have been replaced");
        assertTrue(result.getResults()[0].equals("bucket"), "the result should name the bucket action");
    }

    /**
     * The other direction: a filled bucket empties into the space on the clicked side, like a
     * player right-clicking a wall.
     */
    private void checkBucketPlace() throws Exception {
        int x = originX + 5, y = originY, z = originZ;
        // The turtle sits on the block it is "clicking"; emptying a bucket targets the block on the
        // clicked side, i.e. the space the turtle occupies.
        world.setBlock(x, y, z, Blocks.stone, 0, 3);
        int placeY = y + 1;
        world.setBlockToAir(x, placeY, z);

        SelfTestTurtle turtle = turtle(x, placeY, z);
        turtle.setDirection(0);
        turtle.setSlot(0, new ItemStack(Items.water_bucket));

        TurtleCommandResult result = useBucket(turtle, 0);
        assertTrue(result != null, "the bucket interaction should handle a filled bucket");
        assertTrue(result.isSuccess(), "emptying a bucket against a solid block should succeed");

        Block placed = world.getBlock(x, placeY, z);
        assertTrue(
            placed.getMaterial()
                .isLiquid(),
            "water should have been placed on the clicked side, found " + placed.getUnlocalizedName());
        assertTrue(world.getBlock(x, y, z) == Blocks.stone, "the clicked block must not be replaced");
        assertTrue(turtle.count(Items.bucket) == 1, "the turtle should hold an empty bucket");
        assertTrue(turtle.count(Items.water_bucket) == 0, "the water bucket should have been replaced");
    }

    /**
     * No block hit, but the cell in front of the turtle has a floor one block below it: pouring
     * there is the same action as a player clicking the top face of that floor, so it is allowed.
     */
    private void checkBucketPlaceSupportedNoHit() throws Exception {
        int x = originX + 7, y = originY, z = originZ;
        // The floor the turtle pours onto: one block below and in front of it (NORTH is -Z).
        world.setBlock(x, y, z - 1, Blocks.stone, 0, 3);
        world.setBlockToAir(x, y + 1, z - 1);
        world.setBlock(x, y, z, Blocks.stone, 0, 3);

        SelfTestTurtle turtle = turtle(x, y + 1, z);
        turtle.setSlot(0, new ItemStack(Items.water_bucket));

        assertTrue(canBucketNoHit(turtle, ForgeDirection.NORTH), "canUse should be true over a supported cell");
        TurtleCommandResult result = useBucketNoHit(turtle, ForgeDirection.NORTH);
        assertTrue(result != null, "the bucket interaction should handle a filled bucket without a hit");
        assertTrue(result.isSuccess(), "pouring onto the floor in front should succeed");

        assertTrue(
            world.getBlock(x, y + 1, z - 1)
                .getMaterial()
                .isLiquid(),
            "water should have been placed in front of the turtle, on top of the floor");
        assertTrue(turtle.count(Items.bucket) == 1, "the turtle should hold an empty bucket");
    }

    /**
     * The same no-hit pour, but with nothing underneath the target cell: mid-air placement stays
     * refused, exactly as right-clicking the air would be for a player.
     */
    private void checkBucketPlaceUnsupportedNoHit() throws Exception {
        int x = originX + 9, y = originY, z = originZ;
        world.setBlockToAir(x, y, z - 1);
        world.setBlockToAir(x, y + 1, z - 1);
        world.setBlock(x, y, z, Blocks.stone, 0, 3);

        SelfTestTurtle turtle = turtle(x, y + 1, z);
        turtle.setSlot(0, new ItemStack(Items.water_bucket));

        assertTrue(!canBucketNoHit(turtle, ForgeDirection.NORTH), "canUse should be false over the void");
        assertTrue(useBucketNoHit(turtle, ForgeDirection.NORTH) == null, "nothing should handle a mid-air pour");
        assertTrue(world.isAirBlock(x, y + 1, z - 1), "no water should have been placed");
        assertTrue(turtle.count(Items.water_bucket) == 1, "the filled bucket should be untouched");
    }

    /**
     * Flowing water is not a source: the interaction must refuse rather than duplicating fluid.
     */
    private void checkBucketNotASource() throws Exception {
        int x = originX + 3, y = originY, z = originZ;
        world.setBlock(x, y, z, Blocks.stone, 0, 3);

        SelfTestTurtle turtle = turtle(x, y + 1, z);
        turtle.setDirection(0);
        turtle.setSlot(0, new ItemStack(Items.bucket));

        assertTrue(!canBucket(turtle, 0), "canUse should be false for a block with no fluid");
        assertTrue(useBucket(turtle, 0) == null, "nothing should handle a bucket on a solid block");
        assertTrue(turtle.count(Items.bucket) == 1, "the empty bucket should be untouched");
    }

    /**
     * Drive the bucket interaction the way the manipulator does: load the fake player, ask the
     * registry, unload (which writes the inventory back to the turtle).
     */
    private TurtleCommandResult useBucket(SelfTestTurtle turtle, int direction) throws Exception {
        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, direction, false);
        try {
            ItemStack stack = player.getItem(turtle);
            MovingObjectPosition hit = hitBelow(turtle);
            return TurtleRegistry.instance.use(turtle, null, player.asPlayer(), stack, ForgeDirection.DOWN, hit);
        } finally {
            player.unload(turtle);
        }
    }

    /**
     * Like {@link #useBucket}, but with no block hit at all - the modulator's ray found nothing,
     * e.g. the turtle is facing over a one-block drop.
     */
    private TurtleCommandResult useBucketNoHit(SelfTestTurtle turtle, ForgeDirection direction) throws Exception {
        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, 0, false);
        try {
            ItemStack stack = player.getItem(turtle);
            return TurtleRegistry.instance.use(turtle, null, player.asPlayer(), stack, direction, null);
        } finally {
            player.unload(turtle);
        }
    }

    private boolean canBucketNoHit(SelfTestTurtle turtle, ForgeDirection direction) {
        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, 0, false);
        try {
            ItemStack stack = player.getItem(turtle);
            return TurtleRegistry.instance.canUse(turtle, player.asPlayer(), stack, direction, null);
        } finally {
            player.unload(turtle);
        }
    }

    private boolean canBucket(SelfTestTurtle turtle, int direction) {
        ToolHostPlayer player = new ToolHostPlayer(turtle);
        player.load(turtle, direction, false);
        try {
            ItemStack stack = player.getItem(turtle);
            return TurtleRegistry.instance
                .canUse(turtle, player.asPlayer(), stack, ForgeDirection.DOWN, hitBelow(turtle));
        } finally {
            player.unload(turtle);
        }
    }

    /**
     * A hit on the top face of the block directly below the turtle, which is what the manipulator
     * would find looking down.
     */
    private static MovingObjectPosition hitBelow(SelfTestTurtle turtle) {
        ChunkCoordinates position = turtle.getPosition();
        int x = position.posX, y = position.posY - 1, z = position.posZ;
        return new MovingObjectPosition(x, y, z, 1, Vec3.createVectorHelper(x + 0.5, y + 1, z + 0.5));
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
