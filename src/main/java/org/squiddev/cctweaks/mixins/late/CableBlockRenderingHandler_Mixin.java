package org.squiddev.cctweaks.mixins.late;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.squiddev.cctweaks.api.network.INetworkHelpers;
import org.squiddev.cctweaks.api.network.NetworkAPI;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.FixedRenderBlocks;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.BlockCable;

@Mixin(targets = "dan200.computercraft.client.proxy.ComputerCraftProxyClient$CableBlockRenderingHandler")
public abstract class CableBlockRenderingHandler_Mixin {

    @Unique
    private static final double cCTweaks_Legacy$MIN = 0.375;
    @Unique
    private static final double cCTweaks_Legacy$MAX = 1 - cCTweaks_Legacy$MIN;
    @Unique
    private static FixedRenderBlocks cCTweaks_Legacy$fixedRenderBlocks;

    /**
     * @author CCTweaks
     * @reason Replace cable rendering with network-aware logic
     */
    @Overwrite(remap = false)
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelID,
        RenderBlocks renderblocks) {
        if (modelID == ComputerCraft.Blocks.cable.blockRenderID) {
            if (cCTweaks_Legacy$fixedRenderBlocks == null) cCTweaks_Legacy$fixedRenderBlocks = new FixedRenderBlocks();

            BlockCable cable = (BlockCable) block;
            PeripheralType type = cable.getPeripheralType(world, x, y, z);

            if (type == PeripheralType.Cable || type == PeripheralType.WiredModemWithCable) {
                cCTweaks_Legacy$fixedRenderBlocks.setWorld(world);
                cCTweaks_Legacy$fixedRenderBlocks.setRenderBounds(
                    cCTweaks_Legacy$MIN,
                    cCTweaks_Legacy$MIN,
                    cCTweaks_Legacy$MIN,
                    cCTweaks_Legacy$MAX,
                    cCTweaks_Legacy$MAX,
                    cCTweaks_Legacy$MAX);
                cCTweaks_Legacy$fixedRenderBlocks.renderStandardBlock(block, x, y, z);
                int modemDir;
                if (type == PeripheralType.WiredModemWithCable) {
                    modemDir = cable.getDirection(world, x, y, z);
                } else {
                    modemDir = -1;
                }
                INetworkHelpers helpers = NetworkAPI.helpers();
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    if (dir.ordinal() == modemDir || helpers.canConnect(world, x, y, z, dir)) {
                        cCTweaks_Legacy$fixedRenderBlocks.setRenderBounds(
                            dir.offsetX == -1 ? 0 : dir.offsetX == 1 ? cCTweaks_Legacy$MAX : cCTweaks_Legacy$MIN,
                            dir.offsetY == -1 ? 0 : dir.offsetY == 1 ? cCTweaks_Legacy$MAX : cCTweaks_Legacy$MIN,
                            dir.offsetZ == -1 ? 0 : dir.offsetZ == 1 ? cCTweaks_Legacy$MAX : cCTweaks_Legacy$MIN,
                            dir.offsetX == -1 ? cCTweaks_Legacy$MIN : dir.offsetX == 1 ? 1 : cCTweaks_Legacy$MAX,
                            dir.offsetY == -1 ? cCTweaks_Legacy$MIN : dir.offsetY == 1 ? 1 : cCTweaks_Legacy$MAX,
                            dir.offsetZ == -1 ? cCTweaks_Legacy$MIN : dir.offsetZ == 1 ? 1 : cCTweaks_Legacy$MAX);
                        cCTweaks_Legacy$fixedRenderBlocks.renderStandardBlock(block, x, y, z);
                    }
                }

                block.setBlockBoundsBasedOnState(world, x, y, z);
            }
            if (type == PeripheralType.WiredModem || type == PeripheralType.WiredModemWithCable) {
                BlockCable.renderAsModem = true;
                block.setBlockBoundsBasedOnState(world, x, y, z);
                cCTweaks_Legacy$fixedRenderBlocks.setWorld(world);
                cCTweaks_Legacy$fixedRenderBlocks.setRenderBoundsFromBlock(block);
                cCTweaks_Legacy$fixedRenderBlocks.renderStandardBlock(block, x, y, z);
                BlockCable.renderAsModem = false;
                block.setBlockBoundsBasedOnState(world, x, y, z);
            }
            return true;
        }
        return false;
    }
}
