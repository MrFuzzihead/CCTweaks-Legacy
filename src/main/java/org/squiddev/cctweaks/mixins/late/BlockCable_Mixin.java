package org.squiddev.cctweaks.mixins.late;

import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.squiddev.cctweaks.api.network.NetworkAPI;

import dan200.computercraft.shared.peripheral.common.BlockCable;

@Mixin(BlockCable.class)
public abstract class BlockCable_Mixin {

    /**
     * @author MrFuzzihead
     * @reason Patches
     * {@link dan200.computercraft.shared.peripheral.common.BlockCable#isCable(IBlockAccess, int, int, int)
     */
    @Overwrite(remap = false)
    public static boolean isCable(IBlockAccess world, int x, int y, int z) {
        return NetworkAPI.registry()
            .isNode(world, x, y, z);
    }
}
