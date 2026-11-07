package org.squiddev.cctweaks.core.turtle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.squiddev.cctweaks.api.turtle.ITurtleInteraction;
import org.squiddev.cctweaks.api.turtle.ITurtleRegistry;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

public class TurtleRegistry implements ITurtleRegistry, ITurtleInteraction {

    public static final TurtleRegistry instance = new TurtleRegistry();

    private final Multimap<Item, ITurtleInteraction> itemInteractions = MultimapBuilder.hashKeys()
        .arrayListValues()
        .build();
    private final List<ITurtleInteraction> genericInteractions = new ArrayList<ITurtleInteraction>();

    @Override
    public void registerInteraction(ITurtleInteraction interaction) {
        if (interaction == null) throw new IllegalArgumentException("interaction cannot be null");
        genericInteractions.add(interaction);
    }

    @Override
    public void registerInteraction(Item item, ITurtleInteraction interaction) {
        if (item == null) throw new IllegalArgumentException("item cannot be null");
        if (interaction == null) throw new IllegalArgumentException("interaction cannot be null");
        itemInteractions.put(item, interaction);
    }

    @Override
    public TurtleCommandResult swing(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack,
        ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
        if (stack == null || stack.getItem() == null) return null;

        for (ITurtleInteraction interaction : itemInteractions.get(stack.getItem())) {
            TurtleCommandResult result = interaction.swing(turtle, computer, player, stack, direction, hit);
            if (result != null) return result;
        }

        for (ITurtleInteraction interaction : genericInteractions) {
            TurtleCommandResult result = interaction.swing(turtle, computer, player, stack, direction, hit);
            if (result != null) return result;
        }

        return null;
    }

    @Override
    public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack,
        ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
        if (stack == null || stack.getItem() == null) return null;

        for (ITurtleInteraction interaction : itemInteractions.get(stack.getItem())) {
            TurtleCommandResult result = interaction.use(turtle, computer, player, stack, direction, hit);
            if (result != null) return result;
        }

        for (ITurtleInteraction interaction : genericInteractions) {
            TurtleCommandResult result = interaction.use(turtle, computer, player, stack, direction, hit);
            if (result != null) return result;
        }

        return null;
    }

    @Override
    public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
        MovingObjectPosition hit) {
        if (stack == null || stack.getItem() == null) return false;

        for (ITurtleInteraction interaction : itemInteractions.get(stack.getItem())) {
            boolean result = interaction.canUse(turtle, player, stack, direction, hit);
            if (result) return true;
        }

        for (ITurtleInteraction interaction : genericInteractions) {
            boolean result = interaction.canUse(turtle, player, stack, direction, hit);
            if (result) return true;
        }

        return false;
    }

    @Override
    public boolean canSwing(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
        MovingObjectPosition hit) {
        if (stack == null || stack.getItem() == null) return false;

        for (ITurtleInteraction interaction : itemInteractions.get(stack.getItem())) {
            boolean result = interaction.canSwing(turtle, player, stack, direction, hit);
            if (result) return true;
        }

        for (ITurtleInteraction interaction : genericInteractions) {
            boolean result = interaction.canSwing(turtle, player, stack, direction, hit);
            if (result) return true;
        }

        return false;
    }
}
