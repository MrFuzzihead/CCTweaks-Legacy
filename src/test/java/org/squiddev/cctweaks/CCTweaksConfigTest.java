package org.squiddev.cctweaks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CCTweaksConfigTest {

    private int digFactor;
    private int upgradeId;
    private int advancedUpgradeId;
    private boolean enabled;
    private boolean advanced;

    @BeforeEach
    public void save() {
        digFactor = CCTweaks.digFactor;
        upgradeId = CCTweaks.upgradeId;
        advancedUpgradeId = CCTweaks.advancedUpgradeId;
        enabled = CCTweaks.thEnabled;
        advanced = CCTweaks.advanced;
    }

    @AfterEach
    public void restore() {
        CCTweaks.digFactor = digFactor;
        CCTweaks.upgradeId = upgradeId;
        CCTweaks.advancedUpgradeId = advancedUpgradeId;
        CCTweaks.thEnabled = enabled;
        CCTweaks.advanced = advanced;
    }

    @Test
    public void invalidToolHostValuesAreClampedOrDisableTheFeature() {
        CCTweaks.digFactor = 0;
        CCTweaks.upgradeId = 1;
        CCTweaks.advancedUpgradeId = 1;
        CCTweaks.thEnabled = true;
        CCTweaks.advanced = true;

        CCTweaks.validateToolHostConfig();

        assertEquals(1, CCTweaks.digFactor);
        assertFalse(CCTweaks.thEnabled);
        assertFalse(CCTweaks.advanced);
    }

    @Test
    public void duplicateUpgradeIdsDisableTheAdvancedManipulator() {
        CCTweaks.digFactor = 10;
        CCTweaks.upgradeId = 332;
        CCTweaks.advancedUpgradeId = 332;
        CCTweaks.thEnabled = true;
        CCTweaks.advanced = true;

        CCTweaks.validateToolHostConfig();

        assertFalse(CCTweaks.advanced);
    }

    @Test
    public void excessiveDigFactorIsClamped() {
        CCTweaks.digFactor = 1000;
        CCTweaks.upgradeId = 332;
        CCTweaks.advancedUpgradeId = 333;
        CCTweaks.thEnabled = true;
        CCTweaks.advanced = true;

        CCTweaks.validateToolHostConfig();

        assertEquals(100, CCTweaks.digFactor);
    }
}
