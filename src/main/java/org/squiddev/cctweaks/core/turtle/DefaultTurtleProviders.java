package org.squiddev.cctweaks.core.turtle;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.api.turtle.AbstractTurtleInteraction;
import org.squiddev.cctweaks.core.registry.Module;

import dan200.computercraft.api.turtle.ITurtleAccess;

/**
 * Registers turtle related things
 */
public class DefaultTurtleProviders extends Module {

    @Override
    public void init() {
        // Add bucket using provider
        CCTweaksAPI.instance()
            .turtleRegistry()
            .registerInteraction(new AbstractTurtleInteraction() {

                @Override
                public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack,
                    ForgeDirection direction, MovingObjectPosition hit) {
                    if (!FluidContainerRegistry.isBucket(stack)) return false;

                    ChunkCoordinates coords = turtle.getPosition();
                    int x = coords.posX + direction.offsetX;
                    int y = coords.posY + direction.offsetY;
                    int z = coords.posZ + direction.offsetZ;
                    Block block = turtle.getWorld()
                        .getBlock(x, y, z);

                    if (block == null || block.isAir(turtle.getWorld(), x, y, z)) {
                        return FluidContainerRegistry.isFilledContainer(stack);
                    } else if (block instanceof IFluidBlock || block instanceof BlockLiquid
                        || block.getMaterial()
                            .isLiquid()) {
                                return FluidContainerRegistry.isEmptyContainer(stack);
                            } else {
                                return false;
                            }
                }
            });
    }
}
