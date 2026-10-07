package com.wintertodtautopilot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Local equipment-name registry, avoiding guesses from an item's appearance. */
final class WintertodtWarmClothing
{
    private static final Set<String> NAMES = load();
    private WintertodtWarmClothing() { }
    private static Set<String> load()
    {
        Set<String> names = new HashSet<>();
        InputStream stream = WintertodtWarmClothing.class.getResourceAsStream("warm-clothing.txt");
        if (stream == null) { throw new IllegalStateException("Missing warm clothing registry"); }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (!line.isEmpty() && !line.startsWith("#")) { names.add(line); }
            }
        }
        catch (IOException e) { throw new IllegalStateException("Cannot read warm clothing registry", e); }
        return names;
    }
    static boolean isWarm(String name)
    {
        if (name == null) { return false; }
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        if (NAMES.contains(normalized)) { return true; }
        // Charged/empty and trimmed/imbued variants of listed items share warmth behavior.
        normalized = normalized.replaceAll(" \\((t|i|or|empty|uncharged)\\)$", "");
        return NAMES.contains(normalized);
    }
}
