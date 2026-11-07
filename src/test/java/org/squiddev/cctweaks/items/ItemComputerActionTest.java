package org.squiddev.cctweaks.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dan200.computercraft.shared.computer.blocks.TileComputerBase;

public class ItemComputerActionTest {

    private static ItemStack stack() {
        return new ItemStack(new Item(), 2);
    }

    private static EntityPlayer player(boolean sneaking) {
        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        Mockito.when(player.isSneaking())
            .thenReturn(sneaking);
        return player;
    }

    @Test
    public void clientAlwaysLetsPacketThrough() {
        // PlayerControllerMP.onPlayerRightClick returns immediately when onItemUseFirst returns
        // true, before queueing the C08 block-placement packet. Returning true on the client
        // would mean the server never sees the click and the action never runs, so the client
        // must always return false and let the server do the work.
        ItemComputerAction action = Mockito.mock(ItemComputerAction.class, Mockito.CALLS_REAL_METHODS);
        World world = Mockito.mock(World.class);
        world.isRemote = true;

        TileComputerBase computer = Mockito.mock(TileComputerBase.class);
        Mockito.when(world.getTileEntity(0, 0, 0))
            .thenReturn(computer);

        assertFalse(action.onItemUseFirst(stack(), player(true), world, 0, 0, 0, 0, 0, 0, 0));
        assertFalse(action.onItemUseFirst(stack(), player(false), world, 0, 0, 0, 0, 0, 0, 0));

        Mockito.when(world.getTileEntity(0, 0, 0))
            .thenReturn(Mockito.mock(TileEntity.class));
        assertFalse(action.onItemUseFirst(stack(), player(true), world, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    public void debugWandDoesNotConsume() {
        ItemDebugger debugger = Mockito.mock(ItemDebugger.class, Mockito.CALLS_REAL_METHODS);
        assertFalse(debugger.shouldConsume(stack(), null));
    }

    @Test
    public void defaultActionConsumes() {
        ItemComputerAction action = Mockito.mock(ItemComputerAction.class, Mockito.CALLS_REAL_METHODS);
        assertTrue(action.shouldConsume(stack(), null));
    }

    @Test
    public void successfulUseConsumesOneItem() {
        ItemComputerAction action = Mockito.mock(ItemComputerAction.class, Mockito.CALLS_REAL_METHODS);
        World world = Mockito.mock(World.class);
        world.isRemote = false;

        TileComputerBase computer = Mockito.mock(TileComputerBase.class);
        Mockito.when(world.getTileEntity(0, 0, 0))
            .thenReturn(computer);
        Mockito.doReturn(true)
            .when(action)
            .useComputer(
                Mockito.any(ItemStack.class),
                Mockito.any(EntityPlayer.class),
                Mockito.any(TileComputerBase.class),
                Mockito.anyInt());

        EntityPlayer player = player(true);
        player.capabilities = new net.minecraft.entity.player.PlayerCapabilities();
        ItemStack stack = stack();

        assertTrue(action.onItemUseFirst(stack, player, world, 0, 0, 0, 0, 0, 0, 0));
        assertEquals(1, stack.stackSize);
    }
}
