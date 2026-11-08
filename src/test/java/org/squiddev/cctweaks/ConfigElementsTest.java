package org.squiddev.cctweaks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.core.config.ConfigElements;

import cpw.mods.fml.client.config.IConfigElement;

/**
 * The config GUI itself cannot be loaded headlessly (it extends {@code GuiScreen}), so the listing
 * logic lives in {@link ConfigElements}. These tests cover the two things that can silently break
 * the screen: an option that is read from the config but never listed, and the display order.
 */
public class ConfigElementsTest {

    private final Set<String> categoriesRead = new LinkedHashSet<String>();

    private boolean computerUpgradeEnabled;
    private boolean computerUpgradeCrafting;
    private boolean debugItems;
    private boolean advanced;
    private int advancedUpgradeId;
    private boolean thCrafting;
    private int digFactor;
    private boolean thEnabled;
    private int upgradeId;

    @BeforeEach
    public void remember() {
        computerUpgradeEnabled = CCTweaks.computerUpgradeEnabled;
        computerUpgradeCrafting = CCTweaks.computerUpgradeCrafting;
        debugItems = CCTweaks.debugItems;
        advanced = CCTweaks.advanced;
        advancedUpgradeId = CCTweaks.advancedUpgradeId;
        thCrafting = CCTweaks.thCrafting;
        digFactor = CCTweaks.digFactor;
        thEnabled = CCTweaks.thEnabled;
        upgradeId = CCTweaks.upgradeId;
    }

    @AfterEach
    public void restore() {
        CCTweaks.computerUpgradeEnabled = computerUpgradeEnabled;
        CCTweaks.computerUpgradeCrafting = computerUpgradeCrafting;
        CCTweaks.debugItems = debugItems;
        CCTweaks.advanced = advanced;
        CCTweaks.advancedUpgradeId = advancedUpgradeId;
        CCTweaks.thCrafting = thCrafting;
        CCTweaks.digFactor = digFactor;
        CCTweaks.thEnabled = thEnabled;
        CCTweaks.upgradeId = upgradeId;
    }

    @Test
    public void nullConfigurationProducesNoElements() {
        assertEquals(
            0,
            ConfigElements.of(null)
                .size());
    }

    /**
     * Every category {@link CCTweaks#readConfig(Configuration)} reads has to appear in the GUI.
     * Adding an option under a new category without adding it to {@link ConfigElements#CATEGORIES}
     * would otherwise hide it from every player.
     */
    @Test
    public void everyCategoryReadFromTheConfigIsShown() {
        Configuration config = mock(Configuration.class);
        when(config.get(anyString(), anyString(), anyBoolean())).thenAnswer(invocation -> {
            categoriesRead.add(invocation.getArgument(0));
            return property(invocation.getArgument(1), "true", Property.Type.BOOLEAN);
        });
        when(config.get(anyString(), anyString(), anyInt())).thenAnswer(invocation -> {
            categoriesRead.add(invocation.getArgument(0));
            return property(invocation.getArgument(1), "10", Property.Type.INTEGER);
        });

        CCTweaks.readConfig(config);

        assertTrue(!categoriesRead.isEmpty(), "readConfig should have read some options");
        assertEquals(new LinkedHashSet<String>(Arrays.asList(ConfigElements.CATEGORIES)), categoriesRead);
    }

    @Test
    public void elementsFollowCategoryAndPropertyOrder() {
        // Build (and stub) the categories before stubbing the configuration: nested stubbing
        // inside thenReturn(...) is what Mockito's UnfinishedStubbingException complains about.
        ConfigCategory computer = category("computer", "computerUpgradeEnabled", "computerUpgradeCrafting");
        ConfigCategory testing = category("testing", "debugItems");
        ConfigCategory toolhost = category("turtle.toolhost", "digFactor", "thEnabled");

        Configuration config = mock(Configuration.class);
        when(config.getCategory("computer")).thenReturn(computer);
        when(config.getCategory("testing")).thenReturn(testing);
        when(config.getCategory("turtle.toolhost")).thenReturn(toolhost);

        List<String> names = new ArrayList<String>();
        for (IConfigElement element : ConfigElements.of(config)) names.add(element.getName());

        assertEquals(
            Arrays.asList("computerUpgradeEnabled", "computerUpgradeCrafting", "debugItems", "digFactor", "thEnabled"),
            names);
    }

    @Test
    public void titleKeyIsStable() {
        assertEquals("gui.config.cctweaks.title", ConfigElements.titleKey());
    }

    private static ConfigCategory category(String name, String... properties) {
        List<Property> values = new ArrayList<Property>();
        for (String property : properties) {
            values.add(property(property, "true", Property.Type.BOOLEAN));
        }

        ConfigCategory category = mock(ConfigCategory.class);
        when(category.getName()).thenReturn(name);
        when(category.getOrderedValues()).thenReturn(values);
        return category;
    }

    private static Property property(String name, String value, Property.Type type) {
        Property property = new Property(name, value, type);
        property.setLanguageKey("gui.config.cctweaks.test." + name);
        return property;
    }
}
