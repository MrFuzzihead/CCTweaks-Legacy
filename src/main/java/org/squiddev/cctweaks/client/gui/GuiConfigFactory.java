package org.squiddev.cctweaks.client.gui;

import java.util.Collections;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Hooks {@link GuiCCTweaksConfig} into the "Config" button on the Mods list.
 *
 * <p>
 * This is loaded by name from {@code @Mod(guiFactory = ...)}, so it is never class-loaded on a
 * dedicated server (where {@link GuiScreen} does not exist).
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GuiConfigFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GuiCCTweaksConfig.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }
}
