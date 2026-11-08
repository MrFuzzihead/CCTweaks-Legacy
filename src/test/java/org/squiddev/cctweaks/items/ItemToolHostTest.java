package org.squiddev.cctweaks.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.CCTweaks;

public class ItemToolHostTest {

    private boolean thEnabled;
    private boolean advanced;

    @BeforeEach
    public void saveConfig() {
        thEnabled = CCTweaks.thEnabled;
        advanced = CCTweaks.advanced;
    }

    @AfterEach
    public void restoreConfig() {
        CCTweaks.thEnabled = thEnabled;
        CCTweaks.advanced = advanced;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<ItemStack> subtypes() {
        List list = new ArrayList<ItemStack>();
        new ItemToolHost().getSubItems(null, null, list);
        return list;
    }

    @Test
    public void disabledUpgradeHasNoCreativeEntries() {
        CCTweaks.thEnabled = false;
        CCTweaks.advanced = true;

        assertTrue(subtypes().isEmpty());
    }

    @Test
    public void baseUpgradeIsListed() {
        CCTweaks.thEnabled = true;
        CCTweaks.advanced = false;

        List<ItemStack> stacks = subtypes();
        assertEquals(1, stacks.size());
        assertEquals(
            0,
            stacks.get(0)
                .getItemDamage());
    }

    @Test
    public void advancedUpgradeIsListedWhenEnabled() {
        CCTweaks.thEnabled = true;
        CCTweaks.advanced = true;

        List<ItemStack> stacks = subtypes();
        assertEquals(2, stacks.size());
        assertEquals(
            0,
            stacks.get(0)
                .getItemDamage());
        assertEquals(
            1,
            stacks.get(1)
                .getItemDamage());
    }
}
