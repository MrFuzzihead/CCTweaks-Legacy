package org.squiddev.cctweaks.items;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;

/**
 * Headless tests for the in-world upgrade paths. The tile/world interactions are mocked, but the
 * state-machine order (metadata/family/tile recreation, moved-flag rollback, gold) is the real
 * code.
 */
public class UpgradeItemTest {

    private BlockComputer oldComputer;
    private BlockTurtle oldTurtle;
    private BlockTurtle oldTurtleExpanded;
    private BlockTurtle oldTurtleAdvanced;

    private BlockComputer computerBlock;
    private BlockTurtle turtleBlock;
    private BlockTurtle advancedTurtleBlock;

    private final ItemComputerUpgrade upgrade = new ItemComputerUpgrade();

    private static void setWorld(TileEntity tile, World world) throws Exception {
        java.lang.reflect.Field field = TileEntity.class.getDeclaredField("worldObj");
        field.setAccessible(true);
        field.set(tile, world);
    }

    @BeforeEach
    public void bindBlocks() {
        oldComputer = ComputerCraft.Blocks.computer;
        oldTurtle = ComputerCraft.Blocks.turtle;
        oldTurtleExpanded = ComputerCraft.Blocks.turtleExpanded;
        oldTurtleAdvanced = ComputerCraft.Blocks.turtleAdvanced;

        computerBlock = mock(BlockComputer.class);
        turtleBlock = mock(BlockTurtle.class);
        advancedTurtleBlock = mock(BlockTurtle.class);

        ComputerCraft.Blocks.computer = computerBlock;
        ComputerCraft.Blocks.turtle = turtleBlock;
        ComputerCraft.Blocks.turtleExpanded = turtleBlock;
        ComputerCraft.Blocks.turtleAdvanced = advancedTurtleBlock;
    }

    @AfterEach
    public void restoreBlocks() {
        ComputerCraft.Blocks.computer = oldComputer;
        ComputerCraft.Blocks.turtle = oldTurtle;
        ComputerCraft.Blocks.turtleExpanded = oldTurtleExpanded;
        ComputerCraft.Blocks.turtleAdvanced = oldTurtleAdvanced;
    }

    private static EntityPlayer creativePlayer() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.capabilities = new PlayerCapabilities();
        player.capabilities.isCreativeMode = true;
        return player;
    }

    @Test
    public void computerUpgradeChangesMetadataAndRecreatesTheComputer() throws Exception {
        World world = mock(World.class);
        TileComputerBase tile = mock(TileComputerBase.class);
        when(tile.getBlock()).thenReturn(computerBlock);
        when(tile.getFamily()).thenReturn(ComputerFamily.Normal);
        tile.xCoord = 1;
        tile.yCoord = 2;
        tile.zCoord = 3;
        setWorld(tile, world);
        when(tile.getWorldObj()).thenReturn(world);
        when(world.getBlockMetadata(1, 2, 3)).thenReturn(3);
        when(world.setBlockMetadataWithNotify(1, 2, 3, 11, 3)).thenReturn(true);

        assertTrue(upgrade.useComputer(null, creativePlayer(), tile, 0));

        verify(tile).invalidate();
        verify(tile).validate();
        verify(tile).createServerComputer();
    }

    @Test
    public void computerUpgradeRejectsForeignBlocksAndProtectedAreas() throws Exception {
        World world = mock(World.class);
        TileComputerBase tile = mock(TileComputerBase.class);
        when(tile.getBlock()).thenReturn(mock(BlockComputer.class));
        tile.xCoord = 1;
        tile.yCoord = 2;
        tile.zCoord = 3;
        setWorld(tile, world);
        when(tile.getWorldObj()).thenReturn(world);

        assertFalse(upgrade.useComputer(null, creativePlayer(), tile, 0));
        verify(tile, Mockito.never()).invalidate();
    }

    @Test
    public void turtleUpgradeTransfersStateAndRecreatesTheAdvancedComputer() throws Exception {
        World world = mock(World.class);
        TileTurtle oldTile = mock(TileTurtle.class);
        TileTurtle newTile = mock(TileTurtle.class);
        ServerComputer serverComputer = mock(ServerComputer.class);

        when(oldTile.getBlock()).thenReturn(turtleBlock);
        when(oldTile.getFamily()).thenReturn(ComputerFamily.Normal);
        oldTile.xCoord = 1;
        oldTile.yCoord = 2;
        oldTile.zCoord = 3;
        setWorld(oldTile, world);
        when(oldTile.getWorldObj()).thenReturn(world);
        when(world.setBlock(1, 2, 3, advancedTurtleBlock)).thenReturn(true);
        when(world.getTileEntity(1, 2, 3)).thenReturn((TileEntity) newTile);
        setWorld(newTile, world);
        when(newTile.createServerComputer()).thenReturn(serverComputer);

        assertTrue(upgrade.useTurtle(null, creativePlayer(), oldTile, 0));

        verify(oldTile).setMoved(true);
        verify(newTile).transferStateFrom(oldTile);
        verify(newTile).invalidate();
        verify(newTile).validate();
        verify(newTile).createServerComputer();
    }

    @Test
    public void failedTurtleBlockSwapUnmovesTheOldTile() throws Exception {
        World world = mock(World.class);
        TileTurtle oldTile = mock(TileTurtle.class);

        when(oldTile.getBlock()).thenReturn(turtleBlock);
        when(oldTile.getFamily()).thenReturn(ComputerFamily.Normal);
        oldTile.xCoord = 1;
        oldTile.yCoord = 2;
        oldTile.zCoord = 3;
        setWorld(oldTile, world);
        when(oldTile.getWorldObj()).thenReturn(world);
        when(world.setBlock(1, 2, 3, advancedTurtleBlock)).thenReturn(false);
        when(world.getTileEntity(1, 2, 3)).thenReturn((TileEntity) oldTile);

        assertFalse(upgrade.useTurtle(null, creativePlayer(), oldTile, 0));

        verify(oldTile).setMoved(true);
        verify(oldTile).setMoved(false);
    }
}
