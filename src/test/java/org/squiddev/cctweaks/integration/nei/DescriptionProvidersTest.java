package org.squiddev.cctweaks.integration.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.integration.nei.DescriptionHelpers.IDocProvider;

/**
 * The NEI handlers cannot be rendered headlessly, but everything that decides <em>what</em> is
 * shown is plain logic: the provider chain, the language-key loop and its fallbacks.
 */
public class DescriptionProvidersTest {

    private static final class FixedProvider implements IDocProvider {

        private final List<String> pages;

        FixedProvider(List<String> pages) {
            this.pages = pages;
        }

        @Override
        public List<String> getPages(ItemStack stack) {
            return pages;
        }
    }

    private final ItemStack stack = new ItemStack(new Item());

    @Test
    public void firstNonEmptyProviderWins() {
        DescriptionHelpers helpers = new DescriptionHelpers();
        helpers.add(new FixedProvider(null));
        helpers.add(new FixedProvider(Collections.<String>emptyList()));
        helpers.add(new FixedProvider(Arrays.asList("second")));
        helpers.add(new FixedProvider(Arrays.asList("third")));

        assertEquals(Collections.singletonList("second"), helpers.getPages(stack));
    }

    @Test
    public void noProviderMeansNoPages() {
        DescriptionHelpers helpers = new DescriptionHelpers();
        assertEquals(null, helpers.getPages(stack));
        assertEquals(
            0,
            helpers.providers()
                .size());
    }

    @Test
    public void defaultChainPrefersResourcesOverLanguageKeys() {
        List<IDocProvider> providers = DescriptionHelpers.INSTANCE.providers();
        assertEquals(2, providers.size());
        assertTrue(providers.get(0) instanceof ResourceDocProvider, "shipped docs come first");
        assertTrue(providers.get(1) instanceof LanguageDocProvider);
    }

    @Test
    public void languageProviderStopsAtTheFirstMissingKey() {
        RecordingTranslator translator = new RecordingTranslator("first page", null, "never read");
        LanguageDocProvider provider = new LanguageDocProvider(translator);

        List<String> pages = provider.getPages(stack);
        assertEquals(Collections.singletonList("first page"), pages);
        assertEquals(2, translator.calls.size(), "it should stop once a key is missing");
    }

    @Test
    public void languageProviderTriesTheItemNameAfterTheStackName() {
        // Every key is missing, so the provider stops after the first lookup.
        RecordingTranslator translator = new RecordingTranslator();
        LanguageDocProvider provider = new LanguageDocProvider(translator);

        provider.getPages(stack);

        assertTrue(
            translator.calls.get(0)
                .endsWith(".information.0"),
            "the specific key is tried first");
        assertTrue(
            translator.calls.get(0)
                .contains(stack.getUnlocalizedName()),
            "the specific key is based on the stack name");
    }

    @Test
    public void languageProviderConvertsEscapedNewlines() {
        RecordingTranslator translator = new RecordingTranslator(
            "line one" + LanguageDocProvider.NEWLINE_ESCAPE + "line two");

        List<String> pages = new LanguageDocProvider(translator).getPages(stack);

        assertEquals(1, pages.size());
        assertEquals("line one\n\nline two", pages.get(0));
    }

    @Test
    public void languageProviderWithNoTranslationsIsEmpty() {
        LanguageDocProvider provider = new LanguageDocProvider(new RecordingTranslator());
        assertTrue(
            provider.getPages(stack)
                .isEmpty(),
            "no keys means no pages, not null");
    }

    @Test
    public void shippedDocsStillResolveThroughTheDefaultChain() {
        // A stack with no bundled resource doc and no language keys gets no pages.
        assertEquals(null, DescriptionHelpers.getTranslate(stack), "an undocumented stack has no pages");
    }

    @Test
    public void neIConfigReportsTheModIdentity() {
        NEICCTweaksConfig config = new NEICCTweaksConfig();
        assertTrue(
            config.getName()
                .contains("CCTweaks"));
        assertEquals(org.squiddev.cctweaks.CCTweaks.VERSION, config.getVersion());
        assertFalse(
            config.getName()
                .isEmpty());
    }

    /**
     * Answers the first N keys from a fixed list (which may contain {@code null} to stop early) and
     * then returns {@code null}, so a bug in the provider's loop shows up as a failed assertion
     * rather than an unbounded list.
     */
    private static final class RecordingTranslator implements LanguageDocProvider.Translator {

        final List<String> calls = new ArrayList<String>();
        private final List<String> answers;

        RecordingTranslator(String... answers) {
            this.answers = Arrays.asList(answers);
        }

        @Override
        public String translate(String specific, String generic) {
            int index = calls.size();
            calls.add(specific);
            return index < answers.size() ? answers.get(index) : null;
        }
    }
}
