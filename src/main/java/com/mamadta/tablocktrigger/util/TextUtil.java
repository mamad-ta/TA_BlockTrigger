package com.mamadta.tablocktrigger.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colour helpers.
 *
 * <p>Supports classic {@code &0-&f} / {@code &k-&o} codes as well as hex colours in the
 * {@code &#RRGGBB} format. Hex colours are translated to the {@code &x&r&r&g&g&b&b} form
 * understood by the legacy serializer, which keeps the same code working on every
 * Paper version without relying on version specific APIs.</p>
 */
public final class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private TextUtil() {
    }

    /**
     * Converts a configuration string (with colour codes) into a component.
     *
     * @param input raw text from configuration
     * @return coloured component, never {@code null}
     */
    public static Component color(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(translateHex(input));
    }

    /**
     * Converts {@code &#RRGGBB} sequences into the {@code &x&R&R&G&G&B&B} format.
     *
     * @param input raw text
     * @return translated text
     */
    public static String translateHex(String input) {
        if (input == null || input.indexOf('#') < 0) {
            return input == null ? "" : input;
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
        return PLAIN.serialize(color(input));
    }
}
