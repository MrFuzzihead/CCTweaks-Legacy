package org.squiddev.cctweaks.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ServerComputer;

/**
 * The wand reports why it could not inject. Without this a silent failure is indistinguishable
 * from a missing feature, which is exactly how it was reported in-game.
 */
public class ItemDebuggerTest {

    private boolean requiresOp;

    @BeforeEach
    public void save() {
        requiresOp = org.squiddev.cctweaks.CCTweaks.debugWandRequiresOp;
        org.squiddev.cctweaks.CCTweaks.debugWandRequiresOp = false;
    }

    @AfterEach
    public void restore() {
        org.squiddev.cctweaks.CCTweaks.debugWandRequiresOp = requiresOp;
    }

    private static String message(EntityPlayer player) {
        ArgumentCaptor<IChatComponent> captor = ArgumentCaptor.forClass(IChatComponent.class);
        Mockito.verify(player)
            .addChatMessage(captor.capture());
        return captor.getValue()
            .getUnformattedText();
    }

    private static EntityPlayer player() {
        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        Mockito.when(player.canCommandSenderUseCommand(Mockito.anyInt(), Mockito.anyString()))
            .thenReturn(true);
        return player;
    }

    private static TileComputerBase tile(ServerComputer computer, boolean usable) {
        TileComputerBase tile = Mockito.mock(TileComputerBase.class);
        Mockito.when(tile.isUsable(Mockito.any(EntityPlayer.class), Mockito.anyBoolean()))
            .thenReturn(usable);
        Mockito.when(tile.getServerComputer())
            .thenReturn(computer);
        return tile;
    }

    @Test
    public void reportsOutOfRangeComputer() {
        EntityPlayer player = player();
        assertFalse(new ItemDebugger().useComputer(null, player, tile(null, false), 0));
        assertTrue(message(player).contains("out of reach"));
    }

    @Test
    public void reportsUnstartedComputer() {
        EntityPlayer player = player();
        assertFalse(new ItemDebugger().useComputer(null, player, tile(null, true), 0));
        assertTrue(message(player).contains("not been started"));
    }

    @Test
    public void reportsSwitchedOffComputer() {
        Computer computer = Mockito.mock(Computer.class);
        Mockito.when(computer.isOn())
            .thenReturn(false);

        ServerComputer serverComputer = Mockito.mock(ServerComputer.class);
        Mockito.when(serverComputer.getComputer())
            .thenReturn(computer);

        EntityPlayer player = player();
        assertFalse(new ItemDebugger().useComputer(null, player, tile(serverComputer, true), 0));
        assertTrue(message(player).contains("Turn the computer on"));
    }

    @Test
    public void reportsUnsupportedLuaMachine() {
        Computer computer = Mockito.mock(Computer.class);
        Mockito.when(computer.isOn())
            .thenReturn(true);
        Mockito.when(computer.getLuaMachine())
            .thenReturn(null);

        ServerComputer serverComputer = Mockito.mock(ServerComputer.class);
        Mockito.when(serverComputer.getComputer())
            .thenReturn(computer);

        EntityPlayer player = player();
        assertFalse(new ItemDebugger().useComputer(null, player, tile(serverComputer, true), 0));
        assertTrue(message(player).contains("Unsupported Lua machine"));
    }

    @Test
    public void reportsMissingOperatorPermission() {
        org.squiddev.cctweaks.CCTweaks.debugWandRequiresOp = true;

        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        Mockito.when(player.canCommandSenderUseCommand(Mockito.anyInt(), Mockito.anyString()))
            .thenReturn(false);

        assertFalse(new ItemDebugger().useComputer(null, player, tile(null, true), 0));
        assertTrue(message(player).contains("operator"));
    }

    @Test
    public void operatorCheckIsSkippedWhenDisabled() {
        EntityPlayer player = Mockito.mock(EntityPlayer.class);
        Mockito.when(player.canCommandSenderUseCommand(Mockito.anyInt(), Mockito.anyString()))
            .thenReturn(false);

        // Falls through to the range check rather than the permission check.
        assertFalse(new ItemDebugger().useComputer(null, player, tile(null, false), 0));
        assertTrue(message(player).contains("out of reach"));
        Mockito.verify(player, Mockito.never())
            .canCommandSenderUseCommand(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void genericTilesPrintPeripheralInformation() {
        EntityPlayer player = player();
        World world = Mockito.mock(World.class);
        net.minecraft.tileentity.TileEntity tile = Mockito.mock(net.minecraft.tileentity.TileEntity.class);
        Mockito.when(tile.getWorldObj())
            .thenReturn(world);
        Mockito.when(tile.getBlockType())
            .thenReturn(Mockito.mock(net.minecraft.block.Block.class));

        assertFalse(new ItemDebugger().useGeneric(null, player, tile, 0));

        ArgumentCaptor<IChatComponent> captor = ArgumentCaptor.forClass(IChatComponent.class);
        Mockito.verify(player)
            .addChatMessage(captor.capture());
        List<IChatComponent> messages = captor.getAllValues();
        assertTrue(
            messages.get(0)
                .getUnformattedText()
                .startsWith("Tile: "));
        // A plain tile with no peripheral only reports the tile itself.
        assertEquals(1, messages.size());
    }
}
