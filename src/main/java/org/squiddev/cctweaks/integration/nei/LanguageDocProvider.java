package org.squiddev.cctweaks.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.squiddev.cctweaks.core.utils.Helpers;

/**
 * Fallback documentation provider: reads {@code <item>.information.N} language keys.
 *
 * <p>
 * The keys are looked up per stack-specific name first and then per item name, so a pack can
 * document a whole item without repeating the text for each subtype, and stops at the first index
 * that has no translation.
 * </p>
 */
public class LanguageDocProvider implements DescriptionHelpers.IDocProvider {

    /**
     * A literal newline escape that translation files use for paragraph breaks.
     */
    static final String NEWLINE_ESCAPE = "\\n";

    /**
     * Looks up {@code specific} and falls back to {@code generic}. Returns {@code null} when
     * neither is translated.
     */
    public interface Translator {

        String translate(String specific, String generic);
    }

    private static final Translator DEFAULT = new Translator() {

        @Override
        public String translate(String specific, String generic) {
            return Helpers.translateOrDefault(null, specific, generic);
        }
    };

    private final Translator translator;

    public LanguageDocProvider() {
        this(DEFAULT);
    }

    LanguageDocProvider(Translator translator) {
        this.translator = translator;
    }

    @Override
    public List<String> getPages(ItemStack stack) {
        String name = stack.getUnlocalizedName() + ".information.", genericName = stack.getItem()
            .getUnlocalizedName() + ".information.";

        int index = 0;
        List<String> lines = new ArrayList<String>();
        while (true) {
            String translated = translator.translate(name + index, genericName + index);
            if (translated == null) break;

            lines.add(translated.replace(NEWLINE_ESCAPE, "\n\n"));
            index++;
        }

        return lines;
    }
}
