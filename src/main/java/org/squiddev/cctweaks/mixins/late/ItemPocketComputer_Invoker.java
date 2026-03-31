package org.squiddev.cctweaks.mixins.late;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;

@Mixin(value = ItemPocketComputer.class, remap = false)
public interface ItemPocketComputer_Invoker {

    @Invoker("createServerComputer")
    ServerComputer invokeCreateServerComputer(World world, IInventory inventory, ItemStack stack);
}

