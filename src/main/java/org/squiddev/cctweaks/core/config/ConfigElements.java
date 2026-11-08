package org.squiddev.cctweaks.core.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.squiddev.cctweaks.CCTweaks;

import cpw.mods.fml.client.config.IConfigElement;

/**
 * Builds the list of entries shown in the in-game config GUI.
 *
 * <p>
 * This lives outside the {@code client.gui} package on purpose: {@code GuiCCTweaksConfig} cannot be
 * class-loaded in a headless JVM (it extends {@code GuiScreen}, which needs LWJGL), so anything that
 * lives in it is untestable. The listing logic is the part worth testing - a config option that is
 * missing from {@link #CATEGORIES} silently never appears in the GUI - and it only needs Forge
 * types.
 * </p>
 */
public final class ConfigElements {

    /**
     * The categories shown, in display order. "turtle.toolhost" is a nested category. Every
     * category in {@code config/cctweaks.cfg} must appear here or its options are invisible in the
     * GUI; {@code ConfigElementsTest} enforces that.
     */
    public static final String[] CATEGORIES = new String[] { "computer", "testing", "turtle.toolhost" };

    private ConfigElements() {}

    /**
     * Flatten every property in {@link #CATEGORIES} into GUI elements.
     *
     * @param config The loaded configuration, or {@code null} before pre-init
     * @return The elements to display, or an empty list if the config has not been loaded yet
     */
    public static List<IConfigElement> of(Configuration config) {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        if (config == null) return elements;

        for (String category : CATEGORIES) {
            for (Property property : config.getCategory(category)
                .getOrderedValues()) {
                elements.add(create(property));
            }
        }

        return elements;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static IConfigElement create(Property property) {
        return new ConfigElement(property);
    }

    /**
     * @return The config GUI title, or its language key if it is not translated
     */
    public static String titleKey() {
        return "gui.config." + CCTweaks.MODID + ".title";
    }
}
