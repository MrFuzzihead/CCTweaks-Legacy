package org.squiddev.cctweaks.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.squiddev.cctweaks.CCTweaks;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The in-game configuration screen for CCTweaks.
 *
 * <p>
 * Every option is shown on a single page. The values are edited directly in the live
 * {@link Configuration} (Forge does this for us), so closing the screen calls
 * {@link CCTweaks#saveConfiguration()} to write the file and refresh the static config fields.
 * </p>
 *
 * <p>
 * The turtle upgrade IDs are registered during init, so changing them needs a game restart; the
 * screen flags every entry as needing one, which is the safe assumption.
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GuiCCTweaksConfig extends GuiConfig {

    /**
     * The categories shown, in display order. "turtle.toolhost" is a nested category.
     */
    static final String[] CATEGORIES = new String[] { "computer", "testing", "turtle.toolhost" };

    public GuiCCTweaksConfig(GuiScreen parent) {
        super(parent, getConfigElements(), CCTweaks.MODID, false, true, getTitle());
    }

    static String getTitle() {
        return StatCollector.translateToLocal("gui.config." + CCTweaks.MODID + ".title");
    }

    /**
     * Build the flat list of config entries shown on the screen.
     *
     * @return The elements to display, or an empty list if the config has not been loaded yet
     */
    static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        Configuration config = CCTweaks.getConfiguration();
        if (config == null) return elements;

        for (String category : CATEGORIES) {
            for (Property property : config.getCategory(category)
                .getOrderedValues()) {
                addProperty(elements, property);
            }
        }

        return elements;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void addProperty(List<IConfigElement> elements, Property property) {
        elements.add(new ConfigElement(property));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        CCTweaks.saveConfiguration();
    }
}
