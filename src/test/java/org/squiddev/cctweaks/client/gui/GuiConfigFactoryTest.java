package org.squiddev.cctweaks.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.CCTweaks;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;

/**
 * The config GUI needs a live client to render, but the wiring FML relies on can be checked
 * headlessly.
 *
 * <p>
 * Careful: {@code GuiScreen}'s static initialiser needs LWJGL, so the screen class must only be
 * <em>loaded</em> here (class literals, reflection), never initialised.
 * </p>
 */
public class GuiConfigFactoryTest {

    @Test
    public void modAnnotationPointsAtAWorkingFactory() throws Exception {
        IModGuiFactory factory = (IModGuiFactory) Class.forName(CCTweaks.GUI_FACTORY)
            .getDeclaredConstructor()
            .newInstance();

        assertSame(GuiCCTweaksConfig.class, factory.mainConfigGuiClass());
        assertTrue(
            factory.runtimeGuiCategories()
                .isEmpty(),
            "there are no runtime-only options");
        assertTrue(
            GuiConfig.class.isAssignableFrom(factory.mainConfigGuiClass()),
            "FML expects an IModGuiFactory to hand it a GuiConfig");
    }

    @Test
    public void screenExposesTheElementsAndCategoriesTheFactoryReliesOn() throws Exception {
        // Class.forName(..., false, ...) loads without initialising: the static initialiser pulls in
        // LWJGL via GuiScreen.
        Class<?> screen = Class.forName(
            "org.squiddev.cctweaks.client.gui.GuiCCTweaksConfig",
            false,
            GuiConfigFactoryTest.class.getClassLoader());

        Field categories = screen.getDeclaredField("CATEGORIES");
        assertEquals(String[].class, categories.getType());
        assertTrue(Modifier.isStatic(categories.getModifiers()));

        Method elements = screen.getDeclaredMethod("getConfigElements");
        assertTrue(Modifier.isStatic(elements.getModifiers()));
        assertEquals(java.util.List.class, elements.getReturnType());

        Method title = screen.getDeclaredMethod("getTitle");
        assertEquals(String.class, title.getReturnType());
    }
}
