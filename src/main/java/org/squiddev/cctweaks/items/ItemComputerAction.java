package org.squiddev.cctweaks.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;

/**
 * Handles actions on computers
 */
public abstract class ItemComputerAction extends ItemBase {

    public ItemComputerAction(String itemName) {
        super(itemName);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            // Must return false here: PlayerControllerMP.onPlayerRightClick returns immediately
            // when onItemUseFirst returns true, before queueing the C08 block-placement packet,
            // so the server would never see the click and the action would never run. Returning
            // false lets the packet through; while sneaking, PlayerControllerMP also skips
            // onBlockActivated (doesSneakBypassUse is false) and TileComputerBase.onActivate
            // returns false when sneaking, so no GUI flickers open on the client either.
            return false;
        }

        if (!player.isSneaking()) return false;

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) return false;

        boolean result;
        if (tile instanceof TileComputerBase) {
            // Allow custom Turtle Actions
            if (tile instanceof TileTurtle) {
                result = useTurtle(stack, player, (TileTurtle) tile, side);
            } else {
                result = useComputer(stack, player, (TileComputerBase) tile, side);
            }
        } else {
            result = useGeneric(stack, player, tile, side);
        }

        if (result && shouldConsume(stack, player)) {
            if (!player.capabilities.isCreativeMode) {
                if (--stack.stackSize <= 0) player.destroyCurrentEquippedItem();
            }
        }

        return result;
    }

    /**
     * Whether a successful use should consume one item from the held stack. The debug wand
     * overrides this: its whole purpose is repeatable inspection.
     */
    protected boolean shouldConsume(ItemStack stack, EntityPlayer player) {
        return true;
    }

    protected abstract boolean useComputer(ItemStack stack, EntityPlayer player, TileComputerBase computerTile,
        int side);

    protected boolean useTurtle(ItemStack stack, EntityPlayer player, TileTurtle computerTile, int side) {
        return useComputer(stack, player, computerTile, side);
    }

    /**
     * Custom action on other tile types
     */
    protected boolean useGeneric(ItemStack stack, EntityPlayer player, TileEntity tile, int side) {
        return false;
    }
}
