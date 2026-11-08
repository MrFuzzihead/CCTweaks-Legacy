package org.squiddev.cctweaks.blocks.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.items.ItemMultiBlock;

/**
 * The debug block is the only multi-metadata block in the mod: meta 0 is the debug peripheral and
 * every other meta is deliberately unsupported. The block is gated behind {@code debugItems}.
 */
public class BlockDebugTest {

    private final BlockDebug block = new BlockDebug();
    private boolean debugItems;

    @BeforeEach
    public void remember() {
        debugItems = CCTweaks.debugItems;
    }

    @AfterEach
    public void restore() {
        CCTweaks.debugItems = debugItems;
    }

    @Test
    public void metaZeroIsThePeripheral() {
        assertSame(
            TileDebugPeripheral.class,
            block.createNewTileEntity(mock(World.class), 0)
                .getClass());
        assertNull(block.createNewTileEntity(mock(World.class), 1), "only meta 0 is implemented");
    }

    @Test
    public void droppingKeepsTheMetadata() {
        assertEquals(0, block.damageDropped(0));
        assertEquals(1, block.damageDropped(1));
    }

    @Test
    public void unlocalisedNamesDistinguishThePeripheral() {
        assertTrue(
            block.getUnlocalizedName(0)
                .endsWith(".peripheral"));
        assertEquals(block.getUnlocalizedName(), block.getUnlocalizedName(1));
    }

    @Test
    public void onlyOneCreativeSubtype() {
        List<Object> stacks = new ArrayList<Object>();
        block.getSubBlocks(null, mock(CreativeTabs.class), stacks);

        assertEquals(1, stacks.size());
        // Block.blockRegistry is not bootstrapped in a test JVM, so the stack's item is null and
        // only the fields the constructor sets can be asserted.
        assertEquals(1, ((ItemStack) stacks.get(0)).stackSize);
    }

    @Test
    public void loadingIsGatedByTheDebugConfig() {
        CCTweaks.debugItems = false;
        assertTrue(!block.canLoad(), "the debug block is off by default");

        CCTweaks.debugItems = true;
        assertTrue(block.canLoad());
    }

    @Test
    public void theItemBlockRequiresAMultiBlock() {
        ItemMultiBlock item = new ItemMultiBlock(block);

        assertEquals(64, item.getItemStackLimit());
        assertEquals(0, item.getMaxDamage());
        assertTrue(item.getHasSubtypes());
        assertEquals(3, item.getMetadata(3), "metadata passes through");
        assertEquals(block.getUnlocalizedName(5), item.getUnlocalizedName(new ItemStack(item, 1, 5)));
    }

    @Test
    public void theItemBlockRejectsPlainBlocks() {
        assertThrows(RuntimeException.class, () -> new ItemMultiBlock(new PlainBlock()));
    }

    @Test
    public void theDebugBlockIsAMultiBlock() {
        // BlockDebug overrides preInit to register ItemMultiBlock; ItemMultiBlock's constructor
        // enforces that contract, so this is the same check the registration path relies on.
        assertTrue(block instanceof org.squiddev.cctweaks.blocks.IMultiBlock);

        ItemMultiBlock item = new ItemMultiBlock(block);
        assertTrue(
            item.getUnlocalizedName(new ItemStack(item, 1, 0))
                .endsWith(".peripheral"));
    }

    @Test
    public void iconLookupIsClientOnlyAndGuarded() {
        // No IIconRegister in a headless JVM, so both icons are null and the lookup must not throw.
        assertEquals(null, block.getIcon(0, 0));
        assertEquals(null, block.getIcon(0, 1));
    }

    @Test
    public void creativeTabIsTheComputerCraftOne() {
        assertEquals(org.squiddev.cctweaks.CCTweaks.getCreativeTab(), block.getCreativeTabToDisplayOn());
    }

    /**
     * A block that is not an {@code IMultiBlock}, used to check {@link ItemMultiBlock}'s contract.
     */
    private static final class PlainBlock extends Block {

        PlainBlock() {
            super(Material.rock);
        }
    }

    @Test
    public void subBlocksIgnoreTheTick() {
        // getSubBlocks is called with a null Item by the debug command; it must tolerate that.
        List<Object> stacks = new ArrayList<Object>();
        block.getSubBlocks(null, null, stacks);
        assertEquals(1, stacks.size());
    }
}
