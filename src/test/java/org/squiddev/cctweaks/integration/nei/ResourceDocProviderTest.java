package org.squiddev.cctweaks.integration.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

public class ResourceDocProviderTest {

    private static final class NamedItem extends Item {

        private final String name;

        private NamedItem(String name) {
            this.name = name;
        }

        @Override
        public String getUnlocalizedName() {
            return name;
        }

        @Override
        public String getUnlocalizedName(ItemStack stack) {
            return name;
        }
    }

    /** ItemStack holds its item through a WeakReference, so tests must keep the item alive. */
    private static final java.util.List<Item> KEEP_ALIVE = new java.util.ArrayList<Item>();

    private static ItemStack itemStack(String unlocalizedName) {
        NamedItem item = new NamedItem(unlocalizedName);
        KEEP_ALIVE.add(item);
        return new ItemStack(item);
    }

    @Test
    public void splitsPagesOnSeparator() {
        List<String> pages = ResourceDocProvider.parse("first line\nsecond line\n\n***\n\nsecond page\n");
        assertEquals(2, pages.size());
        assertTrue(
            pages.get(0)
                .startsWith("first line"));
        assertTrue(
            pages.get(0)
                .contains("second line"));
        assertEquals("second page", pages.get(1));
    }

    @Test
    public void dropsEmptyPages() {
        assertEquals(
            1,
            ResourceDocProvider.parse("only page")
                .size());
        assertEquals(
            1,
            ResourceDocProvider.parse("***\npage\n***\n")
                .size());
        assertEquals(
            0,
            ResourceDocProvider.parse("")
                .size());
        assertEquals(
            0,
            ResourceDocProvider.parse(null)
                .size());
    }

    @Test
    public void convertsMarkup() {
        // ${**} -> bold, ${*} -> italic, unknown tokens dropped.
        assertEquals(
            "\u00a7lBold\u00a7o and \u00a7oitalic",
            ResourceDocProvider.format("${**}Bold${*} and ${*}italic"));
        assertEquals(
            "bold",
            ResourceDocProvider.format("${**}bold")
                .substring(2));
        assertEquals("text", ResourceDocProvider.format("${n}text"));
    }

    @Test
    public void buildsDocsPathFromUnlocalizedName() {
        assertEquals("item/computerUpgrade", ResourceDocProvider.docsPath(itemStack("cctweaks.computerUpgrade")));
        assertEquals("item/toolHost.advanced", ResourceDocProvider.docsPath(itemStack("cctweaks.toolHost.advanced")));
        assertNull(ResourceDocProvider.docsPath(null));
        assertNull(ResourceDocProvider.docsPath(itemStack("")));
    }

    @Test
    public void blockItemsUseTheTileFolder() {
        // The "tile." prefix picks the directory; a bare name falls back to the item type.
        assertEquals("tile/debugBlock", ResourceDocProvider.docsPath("tile.cctweaks.debugBlock", true));
        assertEquals("tile/debugBlock", ResourceDocProvider.docsPath("cctweaks.debugBlock", true));
        assertEquals("item/computerUpgrade", ResourceDocProvider.docsPath("cctweaks.computerUpgrade", false));
        assertNull(ResourceDocProvider.docsPath("item.cctweaks.", false));
        assertNull(ResourceDocProvider.docsPath((String) null, false));
    }

    @Test
    public void missingDocsReturnNull() {
        ResourceDocProvider provider = new ResourceDocProvider("xx_XX");
        assertNull(provider.getPages(itemStack("cctweaks.notADocumentedItem")));
    }

    @Test
    public void shippedDocsAreReadable() {
        // The mod ships these; a typo in a resource path would silently remove the NEI page.
        ResourceDocProvider provider = new ResourceDocProvider("en_US");
        List<String> pages = provider.getPages(itemStack("cctweaks.computerUpgrade"));
        assertTrue(pages != null && !pages.isEmpty(), "computerUpgrade docs should be present");

        // Cached: the same page list is handed out again.
        assertTrue(pages == provider.getPages(itemStack("cctweaks.computerUpgrade")));
    }
}
