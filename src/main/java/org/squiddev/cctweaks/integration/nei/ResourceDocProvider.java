package org.squiddev.cctweaks.integration.nei;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import org.squiddev.cctweaks.CCTweaks;

/**
 * Reads documentation pages from {@code assets/cctweaks/docs/<lang>/<item|tile>/<name>.txt}.
 *
 * <p>
 * Pages are separated by a line containing only {@code ***}. Two markup tokens are supported,
 * mirroring the format the docs were originally written in: {@code ${**}} for bold and
 * {@code ${*}} for italic. Any other token is stripped, so a doc can carry a placeholder without
 * leaking it to the screen.
 * </p>
 */
public class ResourceDocProvider implements DescriptionHelpers.IDocProvider {

    private static final String ROOT = "/assets/cctweaks/docs/";
    private static final String FALLBACK_LANGUAGE = "en_US";

    private final Map<String, List<String>> cache = new HashMap<String, List<String>>();
    private String language;

    public ResourceDocProvider() {
        this(null);
    }

    /**
     * @param language The language directory to read, or null for the client's current language.
     */
    public ResourceDocProvider(String language) {
        this.language = language;
    }

    private String getLanguage() {
        if (language != null) return language;
        try {
            String current = net.minecraft.client.Minecraft.getMinecraft().gameSettings.language;
            return current == null ? FALLBACK_LANGUAGE : current;
        } catch (RuntimeException e) {
            // No client (unit tests, a dedicated server): the fallback is the only sane choice.
            return FALLBACK_LANGUAGE;
        }
    }

    @Override
    public List<String> getPages(ItemStack stack) {
        String path = docsPath(stack);
        if (path == null) return null;

        List<String> cached = cache.get(path);
        if (cached == null) {
            cached = read(path);
            cache.put(path, cached);
        }

        return cached.isEmpty() ? null : cached;
    }

    /**
     * The docs path for a stack, without a language or extension, or null if the stack has no name.
     */
    static String docsPath(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        return docsPath(stack.getUnlocalizedName(), stack.getItem() instanceof ItemBlock);
    }

    /**
     * The docs path for an unlocalised name.
     *
     * <p>
     * An unlocalised name looks like {@code item.cctweaks.debugger} or
     * {@code tile.cctweaks.debugBlock}: the leading {@code item.}/{@code tile.} selects the docs
     * directory and the mod id is dropped, matching how the resources are laid out.
     * </p>
     *
     * @param unlocalizedName The stack's unlocalised name
     * @param blockItem       Whether the item places a block, used when the name carries no prefix
     * @return A path such as {@code item/debugger}, or null if the name is unusable
     */
    static String docsPath(String unlocalizedName, boolean blockItem) {
        if (unlocalizedName == null || unlocalizedName.isEmpty()) return null;

        String name = unlocalizedName;
        String folder = null;
        if (name.startsWith("item.")) {
            folder = "item";
            name = name.substring("item.".length());
        } else if (name.startsWith("tile.")) {
            folder = "tile";
            name = name.substring("tile.".length());
        }

        String namespace = CCTweaks.MODID + ".";
        if (name.startsWith(namespace)) name = name.substring(namespace.length());
        if (name.isEmpty() || "null".equals(name)) return null;

        if (folder == null) folder = blockItem ? "tile" : "item";
        return folder + "/" + name;
    }

    private List<String> read(String path) {
        String language = getLanguage();
        String text = readResource(ROOT + language + "/" + path + ".txt");
        if (text == null && !FALLBACK_LANGUAGE.equals(language)) {
            text = readResource(ROOT + FALLBACK_LANGUAGE + "/" + path + ".txt");
        }

        return text == null ? Collections.<String>emptyList() : parse(text);
    }

    private static String readResource(String path) {
        InputStream stream = ResourceDocProvider.class.getResourceAsStream(path);
        if (stream == null) return null;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line)
                    .append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            CCTweaks.logger.warn("Could not read documentation resource " + path, e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Split a documentation file into pages and convert its markup.
     *
     * @param text The raw file contents
     * @return One entry per page; empty pages are dropped
     */
    static List<String> parse(String text) {
        List<String> pages = new ArrayList<String>();
        if (text == null) return pages;

        StringBuilder page = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if ("***".equals(line.trim())) {
                addPage(pages, page);
                page.setLength(0);
            } else {
                if (page.length() > 0) page.append('\n');
                page.append(line);
            }
        }
        addPage(pages, page);

        return pages;
    }

    private static void addPage(List<String> pages, StringBuilder page) {
        String formatted = format(page.toString()).trim();
        if (!formatted.isEmpty()) pages.add(formatted);
    }

    /**
     * Convert the documentation markup to Minecraft formatting codes.
     */
    static String format(String text) {
        // ${**} -> bold, ${*} -> italic, any other token is dropped.
        return text.replace("${**}", "\u00a7l")
            .replace("${*}", "\u00a7o")
            .replaceAll("\\$\\{[^}]*\\}", "");
    }
}
