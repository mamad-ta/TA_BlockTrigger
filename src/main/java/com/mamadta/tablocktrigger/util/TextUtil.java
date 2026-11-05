package com.mamadta.tablocktrigger.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colour helpers that only use the plain Bukkit API, so they work identically on
 * Paper, Spigot and CraftBukkit.
 *
 * <p>Supports classic {@code &0-&f} / {@code &k-&o} codes as well as hex colours in the
 * {@code &#RRGGBB} format. Hex colours are translated to the {@code &x&r&r&g&g&b&b} form,
 * which {@link ChatColor#translateAlternateColorCodes(char, String)} understands on every
 * server implementation.</p>
 */
public final class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private TextUtil() {
    }

    /**
     * Converts a configuration string (with colour codes) into a coloured Bukkit string.
     *
     * @param input raw text from configuration
     * @return coloured text, never {@code null}
     */
    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', translateHex(input));
    }

    /**
     * Converts {@code &#RRGGBB} sequences into the {@code &x&R&R&G&G&B&B} format.
     *
     * @param input raw text
     * @return translated text
     */
    public static String translateHex(String input) {
        if (input == null) {
            return "";
        }
        if (input.indexOf('#') < 0) {
            return input;
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder out = new StringBuilder(input.length() + 16);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (int i = 0; i < hex.length(); i++) {
                replacement.append('&').append(hex.charAt(i));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Strips every colour/formatting code - used for console logging.
     *
     * @param input text that may contain colour codes
     * @return plain text
     */
    public static String stripColor(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String translated = ChatColor.translateAlternateColorCodes('&', translateHex(input));
        return ChatColor.stripColor(translated);
    }
}
