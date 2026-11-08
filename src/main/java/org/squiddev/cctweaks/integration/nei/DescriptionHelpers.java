package org.squiddev.cctweaks.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Documentation lookup: asks each {@link IDocProvider} in turn and returns the first non-empty
 * page list.
 *
 * <p>
 * The provider list is held on an instance (the static {@link #INSTANCE} is the one the mod uses)
 * so a test can exercise the ordering rules without mutating global state.
 * </p>
 */
public class DescriptionHelpers {

    public interface IDocProvider {

        List<String> getPages(ItemStack stack);
    }

    /**
     * The provider chain used by the NEI handler.
     */
    public static final DescriptionHelpers INSTANCE = new DescriptionHelpers();

    static {
        // Resource docs win: they are the files the mod ships, and the language-file keys are only
        // a fallback for packs that want to translate documentation without shipping a jar
        // resource.
        INSTANCE.add(new ResourceDocProvider());
        INSTANCE.add(new LanguageDocProvider());
    }

    private final List<IDocProvider> providers = new ArrayList<IDocProvider>();

    public static List<String> getTranslate(ItemStack stack) {
        return INSTANCE.getPages(stack);
    }

    /**
     * @param stack The stack to document
     * @return The first non-empty page list, or {@code null} if no provider knows the item
     */
    public List<String> getPages(ItemStack stack) {
        for (IDocProvider provider : providers) {
            List<String> lines = provider.getPages(stack);
            if (lines != null && lines.size() > 0) return lines;
        }

        return null;
    }

    public void add(IDocProvider provider) {
        providers.add(provider);
    }

    /**
     * @return A copy of the provider chain, in lookup order
     */
    public List<IDocProvider> providers() {
        return new ArrayList<IDocProvider>(providers);
    }
}
