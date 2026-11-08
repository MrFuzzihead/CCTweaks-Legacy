package org.squiddev.cctweaks.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.config.ConfigElements;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The in-game configuration screen for CCTweaks.
 *
 * <p>
 * Every option is shown on a single page. The values are edited directly in the live
 * {@link net.minecraftforge.common.config.Configuration} (Forge does this for us), so closing the
 * screen calls {@link CCTweaks#saveConfiguration()} to write the file and refresh the static config
 * fields.
 * </p>
 *
 * <p>
 * The turtle upgrade IDs are registered during init, so changing them needs a game restart; the
 * screen flags every entry as needing one, which is the safe assumption.
 * </p>
 *
 * <p>
 * Everything here needs LWJGL and so cannot run in a test. The part that is worth testing (which
 * options are listed, and in what order) lives in {@link ConfigElements}.
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GuiCCTweaksConfig extends GuiConfig {

    /**
     * The categories shown, in display order. See {@link ConfigElements#CATEGORIES}.
     */
    static final String[] CATEGORIES = ConfigElements.CATEGORIES;

    public GuiCCTweaksConfig(GuiScreen parent) {
        super(parent, getConfigElements(), CCTweaks.MODID, false, true, getTitle());
    }

    static String getTitle() {
        return StatCollector.translateToLocal(ConfigElements.titleKey());
    }

    /**
     * Build the flat list of config entries shown on the screen.
     *
     * @return The elements to display, or an empty list if the config has not been loaded yet
     */
    static List<IConfigElement> getConfigElements() {
        return ConfigElements.of(CCTweaks.getConfiguration());
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        CCTweaks.saveConfiguration();
    }
}
