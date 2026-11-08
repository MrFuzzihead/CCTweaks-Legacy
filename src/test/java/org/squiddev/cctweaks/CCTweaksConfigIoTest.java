package org.squiddev.cctweaks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link CCTweaks#readConfig(Configuration)}, which is the code path shared by the initial
 * load and the config GUI (the GUI edits {@link Property} values in memory, then
 * {@link CCTweaks#saveConfiguration()} saves and re-reads them).
 *
 * <p>
 * {@link Configuration} cannot be instantiated outside a running game — its constructor reads the
 * Minecraft home directory out of FML — so the configuration and its properties are mocked. That is
 * enough: the interesting logic is which keys are read, the defaults, the validation and the
 * language keys.
 * </p>
 */
public class CCTweaksConfigIoTest {

    private final List<String> languageKeys = new ArrayList<String>();

    private boolean computerUpgradeEnabled;
    private boolean computerUpgradeCrafting;
    private boolean debugWandEnabled;
    private boolean debugWandRequiresOp;
    private boolean debugItems;
    private boolean advanced;
    private int advancedUpgradeId;
    private boolean thCrafting;
    private int digFactor;
    private boolean thEnabled;
    private int upgradeId;

    @BeforeEach
    public void save() {
        computerUpgradeEnabled = CCTweaks.computerUpgradeEnabled;
        computerUpgradeCrafting = CCTweaks.computerUpgradeCrafting;
        debugWandEnabled = CCTweaks.debugWandEnabled;
        debugWandRequiresOp = CCTweaks.debugWandRequiresOp;
        debugItems = CCTweaks.debugItems;
        advanced = CCTweaks.advanced;
        advancedUpgradeId = CCTweaks.advancedUpgradeId;
        thCrafting = CCTweaks.thCrafting;
        digFactor = CCTweaks.digFactor;
        thEnabled = CCTweaks.thEnabled;
        upgradeId = CCTweaks.upgradeId;
        languageKeys.clear();
    }

    @AfterEach
    public void restore() {
        CCTweaks.computerUpgradeEnabled = computerUpgradeEnabled;
        CCTweaks.computerUpgradeCrafting = computerUpgradeCrafting;
        CCTweaks.debugWandEnabled = debugWandEnabled;
        CCTweaks.debugWandRequiresOp = debugWandRequiresOp;
        CCTweaks.debugItems = debugItems;
        CCTweaks.advanced = advanced;
        CCTweaks.advancedUpgradeId = advancedUpgradeId;
        CCTweaks.thCrafting = thCrafting;
        CCTweaks.digFactor = digFactor;
        CCTweaks.thEnabled = thEnabled;
        CCTweaks.upgradeId = upgradeId;
    }

    /**
     * A configuration whose properties behave like Forge's: a property that has not been written to
     * the file reports the default, a property that has reports the configured value.
     */
    private Configuration config(Map<String, Object> values) {
        Configuration config = mock(Configuration.class);
        when(config.get(anyString(), anyString(), anyBoolean())).thenAnswer(
            invocation -> property(
                values,
                invocation.getArgument(0),
                invocation.getArgument(1),
                invocation.getArgument(2)));
        when(config.get(anyString(), anyString(), anyInt())).thenAnswer(
            invocation -> property(
                values,
                invocation.getArgument(0),
                invocation.getArgument(1),
                invocation.getArgument(2)));
        return config;
    }

    private Property property(Map<String, Object> values, String category, String name, Object defaultValue) {
        String qualified = category + "." + name;
        Object value = values.containsKey(qualified) ? values.get(qualified) : defaultValue;

        Property property = mock(Property.class);
        if (value instanceof Boolean) {
            when(property.getBoolean(anyBoolean())).thenReturn((Boolean) value);
        } else {
            when(property.getInt(anyInt())).thenReturn((Integer) value);
        }
        when(property.setLanguageKey(anyString())).thenAnswer(invocation -> {
            languageKeys.add(invocation.getArgument(0));
            return property;
        });
        return property;
    }

    private static Map<String, Object> values(Object... pairs) {
        Map<String, Object> values = new HashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put((String) pairs[i], pairs[i + 1]);
        }
        return values;
    }

    @Test
    public void defaultsMatchTheShippedConfig() {
        CCTweaks.computerUpgradeEnabled = false;
        CCTweaks.computerUpgradeCrafting = false;
        CCTweaks.debugWandEnabled = false;
        CCTweaks.debugWandRequiresOp = true;
        CCTweaks.debugItems = true;
        CCTweaks.advanced = false;
        CCTweaks.advancedUpgradeId = 1;
        CCTweaks.thCrafting = false;
        CCTweaks.digFactor = 99;
        CCTweaks.thEnabled = false;
        CCTweaks.upgradeId = 2;

        CCTweaks.readConfig(config(values()));

        assertTrue(CCTweaks.computerUpgradeEnabled);
        assertTrue(CCTweaks.computerUpgradeCrafting);
        assertTrue(CCTweaks.debugWandEnabled);
        assertFalse(CCTweaks.debugWandRequiresOp);
        assertFalse(CCTweaks.debugItems);
        assertTrue(CCTweaks.advanced);
        assertEquals(333, CCTweaks.advancedUpgradeId);
        assertTrue(CCTweaks.thCrafting);
        assertEquals(10, CCTweaks.digFactor);
        assertTrue(CCTweaks.thEnabled);
        assertEquals(332, CCTweaks.upgradeId);
    }

    @Test
    public void configuredValuesAreApplied() {
        CCTweaks.readConfig(
            config(
                values(
                    "computer.computerUpgradeEnabled",
                    false,
                    "computer.computerUpgradeCrafting",
                    false,
                    "computer.debugWandEnabled",
                    false,
                    "computer.debugWandRequiresOp",
                    true,
                    "testing.debugItems",
                    true,
                    "turtle.toolhost.advanced",
                    false,
                    "turtle.toolhost.thCrafting",
                    false,
                    "turtle.toolhost.digFactor",
                    42,
                    "turtle.toolhost.thEnabled",
                    false,
                    "turtle.toolhost.upgradeId",
                    400,
                    "turtle.toolhost.advancedUpgradeId",
                    401)));

        assertFalse(CCTweaks.computerUpgradeEnabled);
        assertFalse(CCTweaks.computerUpgradeCrafting);
        assertFalse(CCTweaks.debugWandEnabled);
        assertTrue(CCTweaks.debugWandRequiresOp);
        assertTrue(CCTweaks.debugItems);
        assertFalse(CCTweaks.advanced);
        assertFalse(CCTweaks.thCrafting);
        assertEquals(42, CCTweaks.digFactor);
        assertFalse(CCTweaks.thEnabled);
        assertEquals(400, CCTweaks.upgradeId);
        assertEquals(401, CCTweaks.advancedUpgradeId);
    }

    @Test
    public void validationStillRunsWhenReloading() {
        CCTweaks.readConfig(
            config(
                values(
                    "turtle.toolhost.digFactor",
                    0,
                    "turtle.toolhost.upgradeId",
                    5,
                    "turtle.toolhost.advancedUpgradeId",
                    5)));

        assertEquals(1, CCTweaks.digFactor, "digFactor should be clamped");
        assertFalse(CCTweaks.thEnabled, "an out-of-range upgrade ID should disable the tool host");
        assertFalse(CCTweaks.advanced, "an out-of-range upgrade ID should disable the advanced upgrade");
    }

    @Test
    public void everyOptionHasATranslatedLanguageKey() throws IOException {
        CCTweaks.readConfig(config(values()));

        assertEquals(11, languageKeys.size(), "every config option should get a language key");

        Set<String> lang = loadLang();
        for (String key : languageKeys) {
            assertTrue(key.startsWith("gui.config." + CCTweaks.MODID + "."), "unexpected key " + key);
            assertTrue(lang.contains(key), "missing translation for " + key);
        }

        assertTrue(
            languageKeys.contains("gui.config.cctweaks.computer.debugWandRequiresOp"),
            "the debug wand op gate should be translated");
        assertTrue(
            languageKeys.contains("gui.config.cctweaks.turtle.toolhost.digFactor"),
            "nested categories keep their dotted name in the key");
        assertTrue(lang.contains("gui.config.cctweaks.title"), "the config screen needs a title");
    }

    @Test
    public void savingWithoutALoadedConfigIsANoOp() {
        // Unit tests never run preInit, so there is no live configuration to save: saving must be
        // a no-op rather than an NPE (the config GUI is only reachable after preInit in game).
        assertNull(CCTweaks.getConfiguration());
        CCTweaks.saveConfiguration();
        assertNull(CCTweaks.getConfiguration());
    }

    private static Set<String> loadLang() throws IOException {
        InputStream stream = CCTweaksConfigIoTest.class.getClassLoader()
            .getResourceAsStream("assets/cctweaks/lang/en_US.lang");
        if (stream == null) throw new IOException("en_US.lang is missing from the jar resources");

        Set<String> keys = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                int split = line.indexOf('=');
                if (split > 0) keys.add(line.substring(0, split));
            }
        } finally {
            reader.close();
        }
        return keys;
    }
}
