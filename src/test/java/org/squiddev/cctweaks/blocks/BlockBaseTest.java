package org.squiddev.cctweaks.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

/**
 * {@link BlockBase} is the glue between a block and its {@link TileBase}: every hook has to
 * tolerate a missing or mismatched tile, because a stale block reference outliving its tile is
 * normal in Minecraft.
 */
public class BlockBaseTest {

    private static final class TestTile extends TileBase {

        int created;
        int destroyed;
        int activated;
        int neighbours;
        boolean activateResult;

        @Override
        public void create() {
            created++;
        }

        @Override
        public void destroy() {
            destroyed++;
        }

        @Override
        public boolean onActivated(EntityPlayer player, int side) {
            activated++;
            return activateResult;
        }

        @Override
        public void onNeighborChanged() {
            neighbours++;
        }
    }

    private static final class TestBlock extends BlockBase<TestTile> {

        TestTile lastTile;

        TestBlock() {
            super("testBlock", Material.rock, TestTile.class);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return lastTile = new TestTile();
        }
    }

    private final TestBlock block = new TestBlock();

    @Test
    public void constructorConfiguresTheBlock() {
        assertEquals("testBlock", block.name);
        assertEquals(TestTile.class, block.klass);
        assertEquals(2.0f, block.getBlockHardness(null, 0, 0, 0), 0.001f);
        assertTrue(
            block.getUnlocalizedName()
                .contains("cctweaks.testBlock"));
        assertTrue(
            block.getUnlocalizedName()
                .endsWith("testBlock"));
        assertTrue(block.canLoad(), "blocks are always loadable");
    }

    @Test
    public void createNewTileEntityMakesTheRightTile() {
        assertTrue(block.createNewTileEntity(null, 0) instanceof TestTile);
    }

    @Test
    public void getTileIgnoresForeignAndMissingTiles() {
        TestTile tile = new TestTile();
        IBlockAccess world = mock(IBlockAccess.class);
        when(world.getTileEntity(1, 2, 3)).thenReturn(tile);
        assertSame(tile, block.getTile(world, 1, 2, 3));

        // A tile of another type (a modded block left behind) must not be cast.
        when(world.getTileEntity(4, 5, 6)).thenReturn(mock(TileEntity.class));
        assertNull(block.getTile(world, 4, 5, 6));

        assertNull(block.getTile(world, 7, 8, 9));
    }

    @Test
    public void activationIsDelegated() {
        TestTile tile = new TestTile();
        tile.activateResult = true;
        World world = mock(World.class);
        when(world.getTileEntity(1, 2, 3)).thenReturn(tile);

        assertTrue(block.onBlockActivated(world, 1, 2, 3, null, 0, 0.5f, 0.5f, 0.5f));
        assertEquals(1, tile.activated);

        assertFalse(block.onBlockActivated(world, 4, 5, 6, null, 0, 0.5f, 0.5f, 0.5f), "no tile means no activation");
    }

    @Test
    public void neighbourChangesAreServerOnly() {
        TestTile tile = new TestTile();
        World world = mock(World.class);
        when(world.getTileEntity(1, 2, 3)).thenReturn(tile);

        block.onNeighborBlockChange(world, 1, 2, 3, null);
        assertEquals(1, tile.neighbours, "the server should notify the tile");

        block.onNeighborChange(world, 1, 2, 3, 1, 3, 3);
        assertEquals(2, tile.neighbours, "the server should notify the tile once more");
    }

    @Test
    public void clientNeighbourChangesAreIgnored() {
        TestTile tile = new TestTile();
        World world = mock(World.class);
        // isRemote is a field on World, not a method, so it is assigned rather than stubbed.
        world.isRemote = true;
        when(world.getTileEntity(1, 2, 3)).thenReturn(tile);

        block.onNeighborBlockChange(world, 1, 2, 3, null);
        block.onNeighborChange(world, 1, 2, 3, 1, 3, 3);

        assertEquals(0, tile.neighbours, "the client must not re-scan peripherals");
    }

    @Test
    public void breakingTheBlockDestroysTheTileOnce() {
        TestTile tile = new TestTile();
        World world = mock(World.class);
        when(world.getTileEntity(1, 2, 3)).thenReturn(tile);

        block.breakBlock(world, 1, 2, 3, null, 0);
        assertEquals(1, tile.destroyed);

        // A second break (chunk unload after removal) must not destroy again.
        block.breakBlock(world, 1, 2, 3, null, 0);
        assertEquals(1, tile.destroyed);
    }

    @Test
    public void breakingAMissingTileIsHarmless() {
        // A stale block reference outliving its tile must not throw.
        block.breakBlock(mock(World.class), 1, 2, 3, null, 0);
    }

    @Test
    public void tileLifecycleHooksAreNoOps() {
        // Module lifecycle: blocks register themselves in preInit and have nothing to do after.
        block.init();
        block.postInit();
    }
}
