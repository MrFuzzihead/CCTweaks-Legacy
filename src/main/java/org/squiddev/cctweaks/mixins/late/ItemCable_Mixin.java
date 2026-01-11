package org.squiddev.cctweaks.mixins.late;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TItemMultiPart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.ItemCable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.squiddev.cctweaks.integration.multipart.network.PartCable;
import org.squiddev.cctweaks.integration.multipart.network.PartModem;

@Mixin(ItemCable.class)
public abstract class ItemCable_Mixin implements TItemMultiPart {
    @Override
    public double getHitDepth(Vector3 hit, int side) {
        return hit.copy().scalarProject(Rotation.axes[side]) + (side % 2 ^ 1);
    }

    @Override
    public TMultiPart newPart(ItemStack stack, EntityPlayer player, World world, BlockCoord pos, int side, Vector3 hit) {
        PeripheralType type = ((ItemCable) (Object) this).getPeripheralType(stack);
        return switch (type) {
            case Cable -> MultiPartRegistry.createPart(PartCable.NAME, false);
            case WiredModem -> new PartModem(Facing.oppositeSide[side]);
            default -> null;
        };
    }

    @Unique
    public boolean cCTweaks_Legacy$place(ItemStack item, EntityPlayer player, World world, BlockCoord pos, int side, Vector3 hit) {
        TMultiPart part = newPart(item, player, world, pos, side, hit);
        if (part == null || !TileMultipart.canPlacePart(world, pos, part)) return false;
        if (!world.isRemote) TileMultipart.addPart(world, pos, part);
        if (!player.capabilities.isCreativeMode) item.stackSize--;
        return true;
    }

    /**
     * @author MrFuzzihead
     * @reason Overwrite onItemUse to add multipart placement logic. Calls the original logic for vanilla placement.
     */
    @Overwrite(remap = false)
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ) {
        Block block = world.getBlock(x, y, z);

        // Try multipart placement first
        BlockCoord pos = new BlockCoord(x, y, z);
        Vector3 hit = new Vector3(hitX, hitY, hitZ);
        double d = getHitDepth(hit, side);

        if (d < 1 && cCTweaks_Legacy$place(stack, player, world, pos, side, hit)) return true;
        if (block.isAir(world, x, y, z) && cCTweaks_Legacy$nativePlace(stack, player, world, x, y, z, side, hitX, hitY, hitZ)) return true;
        if (cCTweaks_Legacy$place(stack, player, world, pos.offset(side), side, hit)) return true;

        // Fallback to vanilla logic
        return cCTweaks_Legacy$nativePlace(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    /**
     * Calls the original ItemCable#onItemUse logic.
     * This is a stub and will be merged by the patcher or handled by a shadow method.
     */
    @Unique
    public boolean cCTweaks_Legacy$nativePlace(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                                               float hitX, float hitY, float hitZ) {
        throw new AbstractMethodError("nativePlace should be implemented by the patcher or shadowed");
    }
}
