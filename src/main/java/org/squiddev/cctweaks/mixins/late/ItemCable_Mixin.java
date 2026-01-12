package org.squiddev.cctweaks.mixins.late;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TItemMultiPart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.ItemCable;
import dan200.computercraft.shared.peripheral.common.ItemPeripheralBase;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.squiddev.cctweaks.integration.multipart.network.PartCable;
import org.squiddev.cctweaks.integration.multipart.network.PartModem;

@Mixin(ItemCable.class)
public abstract class ItemCable_Mixin extends ItemPeripheralBase implements TItemMultiPart {

    protected ItemCable_Mixin(Block block) {
        super(block);
    }

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
        if (!this.func_150936_a(world, x, y, z, side, player, stack)) {
            return false;
        } else {
            PeripheralType type = this.getPeripheralType(stack);
            Block existing = world.getBlock(x, y, z);
            if (existing == ComputerCraft.Blocks.cable) {
                PeripheralType existingType = ComputerCraft.Blocks.cable.getPeripheralType(world, x, y, z);
                if (existingType == PeripheralType.WiredModem && type == PeripheralType.Cable) {
                    if (stack.stackSize > 0) {
                        int existingDirection = ComputerCraft.Blocks.cable.getDirection(world, x, y, z);
                        world.setBlockMetadataWithNotify(x, y, z, existingDirection + 6, 3);
                        world.playSoundEffect((double)x + (double)0.5F, (double)y + (double)0.5F, (double)z + (double)0.5F, ComputerCraft.Blocks.cable.stepSound.getBreakSound(), (ComputerCraft.Blocks.cable.stepSound.getVolume() + 1.0F) / 2.0F, ComputerCraft.Blocks.cable.stepSound.getPitch() * 0.8F);
                        --stack.stackSize;
                        TileEntity tile = world.getTileEntity(x, y, z);
                        if (tile instanceof TileCable cable) {
                            cable.networkChanged();
                        }

                        return true;
                    }

                    return false;
                }
            }

            if (existing != net.minecraft.init.Blocks.air && (type == PeripheralType.Cable || existing.isSideSolid(world, x, y, z, ForgeDirection.getOrientation(side)))) {
                int offsetX = x + Facing.offsetsXForSide[side];
                int offsetY = y + Facing.offsetsYForSide[side];
                int offsetZ = z + Facing.offsetsZForSide[side];
                Block offsetExisting = world.getBlock(offsetX, offsetY, offsetZ);
                if (offsetExisting == ComputerCraft.Blocks.cable) {
                    PeripheralType offsetExistingType = ComputerCraft.Blocks.cable.getPeripheralType(world, offsetX, offsetY, offsetZ);
                    if (offsetExistingType == PeripheralType.Cable && type == PeripheralType.WiredModem) {
                        if (stack.stackSize > 0) {
                            int direction = Facing.oppositeSide[side];
                            world.setBlockMetadataWithNotify(offsetX, offsetY, offsetZ, direction + 6, 3);
                            world.playSoundEffect((double)offsetX + (double)0.5F, (double)offsetY + (double)0.5F, (double)offsetZ + (double)0.5F, ComputerCraft.Blocks.cable.stepSound.getBreakSound(), (ComputerCraft.Blocks.cable.stepSound.getVolume() + 1.0F) / 2.0F, ComputerCraft.Blocks.cable.stepSound.getPitch() * 0.8F);
                            --stack.stackSize;
                            TileEntity tile = world.getTileEntity(offsetX, offsetY, offsetZ);
                            if (tile instanceof TileCable cable) {
                                cable.networkChanged();
                            }

                            return true;
                        }

                        return false;
                    }

                    if (offsetExistingType == PeripheralType.WiredModem && type == PeripheralType.Cable) {
                        if (stack.stackSize > 0) {
                            int offsetExistingDirection = ComputerCraft.Blocks.cable.getDirection(world, offsetX, offsetY, offsetZ);
                            world.setBlockMetadataWithNotify(offsetX, offsetY, offsetZ, offsetExistingDirection + 6, 3);
                            world.playSoundEffect((double)offsetX + (double)0.5F, (double)offsetY + (double)0.5F, (double)offsetZ + (double)0.5F, ComputerCraft.Blocks.cable.stepSound.getBreakSound(), (ComputerCraft.Blocks.cable.stepSound.getVolume() + 1.0F) / 2.0F, ComputerCraft.Blocks.cable.stepSound.getPitch() * 0.8F);
                            --stack.stackSize;
                            TileEntity tile = world.getTileEntity(offsetX, offsetY, offsetZ);
                            if (tile instanceof TileCable cable) {
                                cable.networkChanged();
                            }

                            return true;
                        }

                        return false;
                    }
                }
            }

            return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        }
    }
}
