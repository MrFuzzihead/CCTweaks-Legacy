package org.squiddev.cctweaks.turtle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.registry.Registry;

import dan200.computercraft.api.turtle.TurtleUpgradeType;

/**
 * Metadata of the two turtle upgrades. The ids come from the config, the adjectives are language
 * keys and the crafting items are metadata-coded, so each of them can silently break the upgrade
 * registration or show an untranslated name in the turtle GUI.
 */
public class TurtleUpgradeMetadataTest {

    private final TurtleUpgradeToolHost host = new TurtleUpgradeToolHost();
    private final TurtleUpgradeToolManipulator manipulator = new TurtleUpgradeToolManipulator();

    @Test
    public void idsComeFromTheConfig() {
        assertEquals(CCTweaks.upgradeId, host.getUpgradeID());
        assertEquals(CCTweaks.advancedUpgradeId, manipulator.getUpgradeID());
        assertTrue(host.getUpgradeID() != manipulator.getUpgradeID(), "the two upgrades need distinct ids");
    }

    @Test
    public void bothAreToolUpgrades() {
        assertEquals(TurtleUpgradeType.Tool, host.getType());
        assertEquals(TurtleUpgradeType.Tool, manipulator.getType());
    }

    @Test
    public void craftingItemsCarryTheRightMetadata() {
        assertEquals(
            Registry.itemToolHost,
            host.getCraftingItem()
                .getItem());
        assertEquals(
            0,
            host.getCraftingItem()
                .getItemDamage());

        assertEquals(
            Registry.itemToolHost,
            manipulator.getCraftingItem()
                .getItem());
        assertEquals(
            1,
            manipulator.getCraftingItem()
                .getItemDamage());
        assertEquals(1, manipulator.getCraftingItem().stackSize);
    }

    @Test
    public void onlyTheManipulatorIsAPeripheral() {
        assertTrue(manipulator.alsoPeripheral(), "the manipulator exposes tool_manipulator");
        assertNull(host.createPeripheral(null, null), "the plain tool host is not a peripheral");
    }

    @Test
    public void adjectivesAreTranslated() throws Exception {
        Properties lang = new Properties();
        try (InputStream stream = getClass().getResourceAsStream("/assets/cctweaks/lang/en_US.lang")) {
            assertTrue(stream != null, "en_US.lang should be on the classpath");
            lang.load(stream);
        }

        for (String key : new String[] { host.getUnlocalisedAdjective(), manipulator.getUnlocalisedAdjective() }) {
            assertTrue(lang.containsKey(key), "missing translation for " + key);
            assertTrue(
                !lang.getProperty(key)
                    .isEmpty(),
                key + " should not be blank");
        }
    }
}
