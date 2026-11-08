package org.squiddev.cctweaks.turtle;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import org.squiddev.cctweaks.core.turtle.TurtleRegistry;
import org.squiddev.cctweaks.core.utils.LuaResults;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaEnvironment;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.lua.lib.LuaEnvironment;
import dan200.computercraft.shared.turtle.core.InteractDirection;
import dan200.computercraft.shared.util.WorldUtil;

public class ToolManipulatorPeripheral implements IPeripheral {

    private static final String[] METHOD_NAMES = new String[] { "use", "useUp", "useDown", "canUse", "canUseUp",
        "canUseDown", "swing", "swingUp", "swingDown", "canSwing", "canSwingUp", "canSwingDown", };

    /**
     * A task that does nothing, used only to obtain a delayed completion event.
     */
    private static final ILuaTask NOOP_TASK = new ILuaTask() {

        @Override
        public Object[] execute() {
            return null;
        }
    };

    private final ITurtleAccess access;
    private final ToolHostPlayer player;
    private final TurtleSide side;

    public ToolManipulatorPeripheral(ITurtleAccess access, ToolHostPlayer player, TurtleSide side) {
        this.access = access;
        this.player = player;
        this.side = side;
    }

    @Override
    public String getType() {
        return "tool_manipulator";
    }

    @Override
    public String[] getMethodNames() {
        return METHOD_NAMES;
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] args)
        throws LuaException, InterruptedException {
        switch (method) {
            case 0:
                return use(computer, context, InteractDirection.Forward, args);
            case 1:
                return use(computer, context, InteractDirection.Up, args);
            case 2:
                return use(computer, context, InteractDirection.Down, args);
            case 3:
                return LuaResults.wrap(canUse(computer, context, InteractDirection.Forward, args));
            case 4:
                return LuaResults.wrap(canUse(computer, context, InteractDirection.Up, args));
            case 5:
                return LuaResults.wrap(canUse(computer, context, InteractDirection.Down, args));
            case 6:
                return swing(computer, context, InteractDirection.Forward, args);
            case 7:
                return swing(computer, context, InteractDirection.Up, args);
            case 8:
                return swing(computer, context, InteractDirection.Down, args);
            case 9:
                return LuaResults.wrap(canSwing(computer, context, InteractDirection.Forward, args));
            case 10:
                return LuaResults.wrap(canSwing(computer, context, InteractDirection.Up, args));
            case 11:
                return LuaResults.wrap(canSwing(computer, context, InteractDirection.Down, args));
        }

        throw new LuaException("No such method " + method);
    }

    static int parseDuration(Object[] args) throws LuaException {
        if (args.length <= 0 || args[0] == null) return 0;
        if (!(args[0] instanceof Number)) throw new LuaException("Expected number for argument #1");

        int duration = ((Number) args[0]).intValue();
        if (duration < 0) throw new LuaException("Duration must be >= 0");
        return duration;
    }

    static boolean parseSneak(Object[] args, int index) throws LuaException {
        if (args.length <= index || args[index] == null) return false;
        if (args[index] instanceof Boolean) return (Boolean) args[index];
        throw new LuaException("Expected boolean for argument #" + (index + 1));
    }

    static boolean isActionDisabled(String action) {
        return isActionDisabled(action, ComputerCraft.turtleDisabledActions);
    }

    static boolean isActionDisabled(String action, String[] disabledActions) {
        for (String disabled : disabledActions) {
            if (disabled != null && disabled.equalsIgnoreCase(action)) return true;
        }
        return false;
    }

    static Object[] disabled(String action) {
        return new Object[] { false, "Turtle action \"" + action + "\" is disabled" };
    }

    // region Use
    public Object[] use(final IComputerAccess computer, ILuaContext context, InteractDirection direction, Object[] args)
        throws LuaException, InterruptedException {
        if (isActionDisabled("tool")) return disabled("tool");

        final int duration = parseDuration(args);
        final boolean sneak = parseSneak(args, 1);
        final int dir = direction.toWorldDir(access);

        MethodResult result = context.executeMainThreadTask(new ILuaTask() {

            @Override
            public Object[] execute() throws LuaException {
                return doUse(computer, dir, sneak, duration);
            }
        });

        // DelayedTask only applies its delay *after* the task has already run and, because
        // LuaEnvironment.sleep() discards the MethodResult that executeTask() returns, it never
        // suspended at all. Chain the wait onto the task result ourselves instead.
        return LuaResults.wrap(afterDelay(computer, result, duration));
    }

    /**
     * Chain a {@code duration}-tick wait onto the result of a main-thread task.
     *
     * <p>
     * The Lua bridge resumes a suspended call from its {@code MethodResult} callback, and accepts
     * another {@code MethodResult} from that callback to keep suspending -- so waiting for the task
     * and then for the delay is expressed as two nested waits.
     * </p>
     *
     * @param computer The computer to queue the delay task on
     * @param result   The result of the action, which may itself still be suspended
     * @param duration The number of ticks to wait afterwards, or 0 for no wait
     * @return A result that yields {@code result}'s values once the delay has elapsed
     */
    static MethodResult afterDelay(IComputerAccess computer, MethodResult result, int duration) throws LuaException {
        if (duration <= 0) return result;

        if (result.isImmediate()) return waitThen(computer, duration, result.getResults());

        return MethodResult.pullEvent(event -> {
            MethodResult next = result.resumeWith(event);
            // Not our event yet: keep waiting on the original result.
            if (next == null) return null;
            // A nested wait we did not set up; let it finish rather than dropping it.
            if (!next.isImmediate()) return next;
            return waitThen(computer, duration, next.getResults());
        });
    }

    /**
     * Wait {@code duration} ticks by queueing a no-op delayed task, then yield {@code values}.
     */
    private static MethodResult waitThen(IComputerAccess computer, int duration, Object[] values) throws LuaException {
        long id = LuaEnvironment.instance.issueTask(computer, NOOP_TASK, duration);
        return MethodResult.pullEvent(
            event -> MethodResult.matches(event, ILuaEnvironment.EVENT_NAME, id) ? MethodResult.of(values) : null);
    }

    public Object[] doUse(IComputerAccess computer, int direction, boolean sneak, int duration) throws LuaException {
        player.load(access, direction, sneak);

        ForgeDirection fDirection = ForgeDirection.VALID_DIRECTIONS[direction];
        MovingObjectPosition hit = findHit(fDirection, 0.65);
        ItemStack stack = player.getItem(access);
        World world = player.worldObj;

        try {
            if (stack != null) {
                TurtleCommandResult result = TurtleRegistry.instance
                    .use(access, computer, player, stack, fDirection, hit);
                if (result != null) return toObjectArray(result);
            }

            if (hit != null) {
                switch (hit.typeOfHit) {
                    case ENTITY:
                        if (stack != null && player.interactWith(hit.entityHit)) {
                            return new Object[] { true, "entity", "interact" };
                        }
                        break;
                    case BLOCK: {
                        Object[] result = tryUseOnBlock(world, hit, stack, resolveSide(hit));
                        if (result != null) return result;
                    }
                }
            }

            if (stack != null && !ForgeEventFactory
                .onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, world)
                .isCanceled()) {
                player.posX += fDirection.offsetX * 0.6;
                player.posY += fDirection.offsetY * 0.6;
                player.posZ += fDirection.offsetZ * 0.6;

                duration = Math.min(duration, stack.getMaxItemUseDuration());
                ItemStack old = stack.copy();
                ItemStack result = stack.useItemRightClick(player.worldObj, player);

                boolean using = player.isUsingItem();
                ItemStack inUse = player.getItemInUse();
                if (using && inUse != null && !ForgeEventFactory.onUseItemStop(player, inUse, duration)) {
                    inUse.onPlayerStoppedUsing(player.worldObj, player, inUse.getMaxItemUseDuration() - duration);
                    player.clearItemInUse();
                }

                if (using || !ItemStack.areItemStacksEqual(old, result)) {
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, result);
                    return new Object[] { true, "item", "use" };
                } else {
                    return new Object[] { false };
                }
            }
        } finally {
            player.clearItemInUse();
            player.unload(access);
        }

        return new Object[] { false };
    }

    public Object[] tryUseOnBlock(World world, MovingObjectPosition hit, ItemStack stack, int side) {
        int x = hit.blockX;
        int y = hit.blockY;
        int z = hit.blockZ;
        if (!world.getBlock(x, y, z)
            .isAir(world, x, y, z)) {
            if (ComputerCraft.turtlesObeyBlockProtection && !ComputerCraft.isBlockEditable(world, x, y, z, player)) {
                return new Object[] { false, "Cannot use protected block" };
            }

            if (ForgeEventFactory
                .onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, x, y, z, side, world)
                .isCanceled()) {
                return new Object[] { true, "block", "interact" };
            }

            Object[] result = onPlayerRightClick(stack, x, y, z, side, hit.hitVec);
            if (result != null) return result;
        }

        return null;
    }

    public Object[] onPlayerRightClick(ItemStack stack, int x, int y, int z, int side, Vec3 look) {
        float xCoord = (float) look.xCoord - (float) x;
        float yCoord = (float) look.yCoord - (float) y;
        float zCoord = (float) look.zCoord - (float) z;
        World world = player.worldObj;

        if (stack != null && stack.getItem() != null
            && stack.getItem()
                .onItemUseFirst(stack, player, world, x, y, z, side, xCoord, yCoord, zCoord)) {
            return new Object[] { true, "item", "use" };
        }

        if (!player.isSneaking() || stack == null
            || stack.getItem()
                .doesSneakBypassUse(world, x, y, z, player)) {
            if (world.getBlock(x, y, z)
                .onBlockActivated(world, x, y, z, player, side, xCoord, yCoord, zCoord)) {
                return new Object[] { true, "block", "interact" };
            }
        }

        if (stack == null) return null;

        if (stack.getItem() instanceof ItemBlock) {
            if (isActionDisabled("place")) return disabled("place");

            ItemBlock itemBlock = (ItemBlock) stack.getItem();

            ForgeDirection direction = ForgeDirection.VALID_DIRECTIONS[side];
            int placeX = x + direction.offsetX;
            int placeY = y + direction.offsetY;
            int placeZ = z + direction.offsetZ;

            // Mirrors ItemBlock's own rule: a replaceable block (tall grass, snow layer) is
            // replaced in place instead of placing against its side.
            if (world.getBlock(x, y, z)
                .isReplaceable(world, x, y, z)) {
                placeX = x;
                placeY = y;
                placeZ = z;
            }

            if (ComputerCraft.turtlesObeyBlockProtection
                && !ComputerCraft.isBlockEditable(world, placeX, placeY, placeZ, player)) {
                return new Object[] { false, "Cannot place protected block" };
            }

            if (!world
                .canPlaceEntityOnSide(itemBlock.field_150939_a, placeX, placeY, placeZ, false, side, null, stack)) {
                return null;
            }
        }

        if (stack.tryPlaceItemIntoWorld(player, world, x, y, z, side, xCoord, yCoord, zCoord)) {
            if (stack.stackSize <= 0) {
                MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, stack));
            }

            return new Object[] { true, "place" };
        }

        return null;
    }
    // endregion

    // region Swing
    public Object[] swing(final IComputerAccess computer, ILuaContext context, InteractDirection direction,
        Object[] args) throws LuaException, InterruptedException {
        if (isActionDisabled("tool")) return new Object[] { false, "Turtle action \"tool\" is disabled" };

        final boolean sneak = parseSneak(args, 0);
        final int dir = direction.toWorldDir(access);

        return access.executeCommand(context, new ITurtleCommand() {

            @Override
            public TurtleCommandResult execute(ITurtleAccess iTurtleAccess) {
                try {
                    return doSwing(computer, dir, sneak);
                } catch (LuaException e) {
                    return TurtleCommandResult.failure(e.getMessage());
                }
            }
        });
    }

    public TurtleCommandResult doSwing(IComputerAccess computer, int direction, boolean sneak) throws LuaException {
        player.load(access, direction, sneak);

        ForgeDirection fDirection = ForgeDirection.VALID_DIRECTIONS[direction];
        ItemStack stack = player.getItem(access);
        TurtleAnimation animation = side == TurtleSide.Left ? TurtleAnimation.SwingLeftTool
            : TurtleAnimation.SwingRightTool;
        MovingObjectPosition hit = findHit(fDirection, 0.65);

        try {
            if (stack != null) {
                TurtleCommandResult result = TurtleRegistry.instance
                    .swing(access, computer, player, stack, fDirection, hit);
                if (result != null) {
                    if (result.isSuccess()) access.playAnimation(animation);
                    return result;
                }
            }

            if (hit != null) {
                switch (hit.typeOfHit) {
                    case ENTITY: {
                        if (isActionDisabled("attack"))
                            return TurtleCommandResult.failure("Turtle action \"attack\" is disabled");

                        TurtleCommandResult result = player.attack(access, hit.entityHit);
                        if (result.isSuccess()) access.playAnimation(animation);
                        return result;
                    }
                    case BLOCK: {
                        if (isActionDisabled("dig"))
                            return TurtleCommandResult.failure("Turtle action \"dig\" is disabled");

                        TurtleCommandResult result = player.dig(access, direction);
                        if (result.isSuccess()) access.playAnimation(animation);
                        return result;
                    }
                }
            }
        } finally {
            player.clearItemInUse();
            player.unload(access);
        }

        return TurtleCommandResult.failure("Nothing to do here");
    }
    // endregion

    // region Can use
    public MethodResult canUse(final IComputerAccess computer, ILuaContext context, InteractDirection dir,
        Object[] args) throws LuaException, InterruptedException {
        if (isActionDisabled("tool")) return MethodResult.of(false);

        final int direction = dir.toWorldDir(access);
        return context.executeMainThreadTask(new ILuaTask() {

            @Override
            public Object[] execute() throws LuaException {
                player.load(access, direction, false);

                ItemStack stack = player.getHeldItem();
                ForgeDirection fDirection = ForgeDirection.VALID_DIRECTIONS[direction];
                MovingObjectPosition hit = findHit(fDirection, 0.65);

                try {
                    if (stack != null) {
                        boolean result = TurtleRegistry.instance.canUse(access, player, stack, fDirection, hit);
                        if (result) return new Object[] { true };
                    }

                    if (hit == null) {
                        // Nothing to click: only an item that does something on a right-click counts.
                        return new Object[] { stack != null && hasRightClickBehaviour(stack) };
                    }

                    switch (hit.typeOfHit) {
                        case ENTITY:
                            return new Object[] { stack != null && hit.entityHit != null };
                        case BLOCK:
                            return new Object[] { canUseOnBlock(hit, stack, resolveSide(hit)) };
                        default:
                            return new Object[] { false };
                    }
                } finally {
                    player.unload(access);
                }
            }
        });
    }

    /**
     * Whether {@link #onPlayerRightClick} would do something with this block and stack.
     *
     * <p>
     * Block activation cannot be probed without side effects -- {@code onBlockActivated} opens the
     * GUI it is asked about -- so a block only counts as usable if it has a tile entity (which is
     * what makes an interaction worth starting), or as placeable if we are holding an item block.
     * </p>
     */
    boolean canUseOnBlock(MovingObjectPosition hit, ItemStack stack, int side) {
        World world = player.worldObj;
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        Block block = world.getBlock(x, y, z);

        if (block == null || block.isAir(world, x, y, z)) return false;
        if (ComputerCraft.turtlesObeyBlockProtection && !ComputerCraft.isBlockEditable(world, x, y, z, player)) {
            return false;
        }

        if (stack != null && stack.getItem() instanceof ItemBlock) {
            ForgeDirection direction = ForgeDirection.VALID_DIRECTIONS[side];
            int placeX = x + direction.offsetX;
            int placeY = y + direction.offsetY;
            int placeZ = z + direction.offsetZ;

            if (block.isReplaceable(world, x, y, z)) {
                placeX = x;
                placeY = y;
                placeZ = z;
            }

            if (ComputerCraft.turtlesObeyBlockProtection
                && !ComputerCraft.isBlockEditable(world, placeX, placeY, placeZ, player)) {
                return false;
            }

            ItemBlock itemBlock = (ItemBlock) stack.getItem();
            if (world
                .canPlaceEntityOnSide(itemBlock.field_150939_a, placeX, placeY, placeZ, false, side, null, stack)) {
                return true;
            }
        }

        if (!player.isSneaking() || stack == null
            || stack.getItem()
                .doesSneakBypassUse(world, x, y, z, player)) {
            int metadata = world.getBlockMetadata(x, y, z);
            return block.hasTileEntity(metadata);
        }

        return false;
    }

    /**
     * Whether an item has right-click behaviour when there is no block in front of the turtle.
     *
     * <p>
     * Vanilla offers no way to ask an item without running it, so this only rules out the case we
     * can be certain about: an item block needs a surface to place against, and there is none.
     * </p>
     */
    static boolean hasRightClickBehaviour(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        return !(stack.getItem() instanceof ItemBlock);
    }

    public MethodResult canSwing(final IComputerAccess computer, ILuaContext context, InteractDirection dir,
        Object[] args) throws LuaException, InterruptedException {
        if (isActionDisabled("tool")) return MethodResult.of(false);

        final int direction = dir.toWorldDir(access);
        return context.executeMainThreadTask(new ILuaTask() {

            @Override
            public Object[] execute() throws LuaException {
                player.load(access, direction, false);

                ItemStack stack = player.getHeldItem();
                ForgeDirection fDirection = ForgeDirection.VALID_DIRECTIONS[direction];
                MovingObjectPosition hit = findHit(fDirection, 0.65);

                try {
                    if (hit != null) {
                        if (hit.entityHit != null && isActionDisabled("attack")) return new Object[] { false };
                        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && isActionDisabled("dig"))
                            return new Object[] { false };
                    }

                    if (stack != null) {
                        boolean result = TurtleRegistry.instance.canSwing(access, player, stack, fDirection, hit);
                        if (result) return new Object[] { true };
                    }

                    // TiC (and other modded) weapons handle damage in onLeftClickEntity and do not
                    // expose a vanilla attack-damage attribute. Any entity in range can be swung at.
                    if (hit != null && hit.entityHit != null) return new Object[] { true };

                    if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        Block block = access.getWorld()
                            .getBlock(hit.blockX, hit.blockY, hit.blockZ);
                        int meta = access.getWorld()
                            .getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
                        if (block.canHarvestBlock(player, meta)) return new Object[] { true };
                    }

                    return new Object[] { false };
                } finally {
                    player.unload(access);
                }
            }
        });
    }
    // endregion

    /**
     * The face of the hit block that the interaction should use.
     *
     * <p>
     * {@link MovingObjectPosition#sideHit} is normally correct, but it is {@code -1} when the ray
     * started inside a block (a turtle pressed against a full block). Deriving the face from the
     * hit vector is the only reliable answer then. The old code retried with
     * {@link ForgeDirection#OPPOSITES} instead, which could activate or place against the far face
     * of the block, or fire twice.
     * </p>
     *
     * @param hit The block hit to inspect
     * @return A {@link ForgeDirection} ordinal, 0-5
     */
    static int resolveSide(MovingObjectPosition hit) {
        if (hit.sideHit >= 0 && hit.sideHit < ForgeDirection.VALID_DIRECTIONS.length) return hit.sideHit;
        if (hit.hitVec == null) return ForgeDirection.UP.ordinal();

        double x = hit.hitVec.xCoord - hit.blockX;
        double y = hit.hitVec.yCoord - hit.blockY;
        double z = hit.hitVec.zCoord - hit.blockZ;

        // Nearest face wins; the ray inside the block is closest to the face it entered through.
        double distance = Math.min(Math.min(x, 1 - x), Math.min(y, 1 - y));
        int side = distance == x ? ForgeDirection.WEST.ordinal()
            : distance == 1 - x ? ForgeDirection.EAST.ordinal()
                : distance == y ? ForgeDirection.DOWN.ordinal() : ForgeDirection.UP.ordinal();

        double zDistance = Math.min(z, 1 - z);
        if (zDistance < distance) {
            side = zDistance == z ? ForgeDirection.NORTH.ordinal() : ForgeDirection.SOUTH.ordinal();
        }

        return side;
    }

    public MovingObjectPosition findHit(ForgeDirection facing, double range) {
        Vec3 origin = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
        Vec3 blockCenter = origin.addVector(facing.offsetX * 0.51, facing.offsetY * 0.51, facing.offsetZ * 0.51);
        Vec3 target = blockCenter.addVector(facing.offsetX * range, facing.offsetY * range, facing.offsetZ * range);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(origin, target);

        // WorldUtil treats its third argument as a direction and multiplies it by the distance,
        // so passing the endpoint here traces a ray into the distance instead of towards the
        // target block. Pass the relative vector and a distance of 1 instead.
        Vec3 direction = Vec3.createVectorHelper(
            target.xCoord - origin.xCoord,
            target.yCoord - origin.yCoord,
            target.zCoord - origin.zCoord);
        Entity entity = WorldUtil.rayTraceEntities(player.worldObj, origin, direction, 1.0);

        if (entity != null) {
            return new MovingObjectPosition(entity);
        } else {
            return hit;
        }
    }

    @Override
    public void attach(IComputerAccess computer) {}

    @Override
    public void detach(IComputerAccess computer) {}

    @Override
    public boolean equals(Object other) {
        return other == this
            || (other instanceof ToolManipulatorPeripheral && side == ((ToolManipulatorPeripheral) other).side
                && access.equals(((ToolManipulatorPeripheral) other).access));
    }

    @Override
    public int hashCode() {
        return 31 * access.hashCode() + side.hashCode();
    }

    @Override
    public boolean equals(IPeripheral other) {
        return equals((Object) other);
    }

    public static Object[] toObjectArray(TurtleCommandResult result) {
        if (result.isSuccess()) {
            Object[] resultVals = result.getResults();
            if (resultVals == null) {
                return new Object[] { true };
            } else {
                Object[] returnVals = new Object[resultVals.length + 1];
                returnVals[0] = true;
                System.arraycopy(resultVals, 0, returnVals, 1, resultVals.length);
                return returnVals;
            }
        } else {
            String message = result.getErrorMessage();
            return message == null ? new Object[] { false } : new Object[] { false, message };
        }
    }
}
