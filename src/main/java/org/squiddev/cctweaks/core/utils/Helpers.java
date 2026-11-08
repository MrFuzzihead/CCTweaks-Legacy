package org.squiddev.cctweaks.core.utils;

import net.minecraft.util.StatCollector;

/**
 * Helper methods for various things
 */
public class Helpers {

    /**
     * Translate any variant of a string
     *
     * @param def     The fallback string
     * @param strings The strings to try to translate
     * @return The first translateable string or the default
     */
    public static String translateOrDefault(String def, String... strings) {
        for (String string : strings) {
            if (StatCollector.canTranslate(string)) return StatCollector.translateToLocal(string);
        }

        return def;
    }

}
