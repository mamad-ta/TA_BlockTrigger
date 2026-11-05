package com.mamadta.tablocktrigger.config;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads, formats and sends every message defined in {@code messages.yml}.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>{@code &a} style colour codes and {@code &#RRGGBB} hex colours.</li>
 *   <li>A configurable {@code prefix} available through the {@code {prefix}} placeholder.</li>
 *   <li>A message can be disabled by setting it to an empty string, {@code "-"} or
 *       {@code "disabled"} - it is then never sent.</li>
 *   <li>Missing keys fall back to the copy bundled inside the jar, so a broken or outdated
 *       {@code messages.yml} can never throw an exception.</li>
 * </ul>
 */
public final class MessageManager {

    private static final String DISABLED_LITERAL = "disabled";

    private final TABlockTrigger plugin;
    private final Set<String> missingWarnings = ConcurrentHashMap.newKeySet();

    private YamlConfiguration messages = new YamlConfiguration();
    private YamlConfiguration defaults = new YamlConfiguration();
    private File file;
    private String prefix = "";

    public MessageManager(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    // =======================================================================
    //  Loading
    // =======================================================================

    /** Creates (when missing) and loads {@code messages.yml} plus the bundled defaults. */
    public void load() {
        loadBundledDefaults();

        if (file == null) {
            file = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(file);
        // Fill in keys the administrator may not have (e.g. after an update).
        messages.setDefaults(defaults);

        prefix = raw("prefix");
        if (prefix == null || isDisabledToken(prefix)) {
            prefix = "";
        }
    }

    private void loadBundledDefaults() {
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[messages] Could not read the bundled messages.yml: " + exception.getMessage());
        }
    }

    // =======================================================================
    //  Raw access
    // =======================================================================

    /**
     * @param path message key
     * @return raw configured text or {@code null} when unknown
     */
    public String raw(String path) {
        String value = messages.getString(path);
        if (value == null) {
            value = defaults.getString(path);
            if (value == null && missingWarnings.add(path)) {
                plugin.getLogger().warning("[messages] Missing message key '" + path + "' - using the key itself.");
            }
            if (value == null) {
                return null;
            }
        }
        return value;
    }

    /** @param path message key @return raw list value (may be empty) */
    public List<String> rawList(String path) {
        List<String> list = messages.getStringList(path);
        if (list.isEmpty()) {
            list = defaults.getStringList(path);
        }
        return list;
    }

    /**
     * @param path message key
     * @return {@code true} when the message is configured as disabled/empty
     */
    public boolean isDisabled(String path) {
        String value = raw(path);
        return value == null || isDisabledToken(value);
    }

    private boolean isDisabledToken(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.equals("-") || trimmed.equalsIgnoreCase(DISABLED_LITERAL);
    }

    // =======================================================================
    //  Sending
    // =======================================================================

    /**
     * Sends a message key to a receiver.
     *
     * @param receiver         receiver of the message
     * @param path             message key in {@code messages.yml}
     * @param replacements     pairs of {@code placeholder, value}
     */
    public void send(CommandSender receiver, String path, String... replacements) {
        if (receiver == null) {
            return;
        }
        String message = format(path, replacements);
        if (message == null) {
            return;
        }
        receiver.sendMessage(TextUtil.color(message));
    }

    /**
     * Sends a list-based message (e.g. the help page).
     *
     * @param receiver     receiver of the message
     * @param path         message key holding a string list
     * @param replacements pairs of {@code placeholder, value}
     */
    public void sendList(CommandSender receiver, String path, String... replacements) {
        if (receiver == null) {
            return;
        }
        List<String> lines = rawList(path);
        if (lines.isEmpty()) {
            String single = format(path, replacements);
            if (single != null) {
                receiver.sendMessage(TextUtil.color(single));
            }
            return;
        }
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                receiver.sendMessage(TextUtil.color(""));
                continue;
            }
            receiver.sendMessage(TextUtil.color(apply(line, replacements)));
        }
    }

    /**
     * Builds the final, coloured source string of a message.
     *
     * @param path         message key
     * @param replacements placeholder pairs
     * @return the formatted message or {@code null} when the message is disabled
     */
    public String format(String path, String... replacements) {
        String value = raw(path);
        if (value == null) {
            return path;
        }
        if (isDisabledToken(value)) {
            return null;
        }
        String withPrefix = value.replace("{prefix}", prefix);
        return apply(withPrefix, replacements);
    }

    /** @return the configured prefix (with colour codes) */
    public String getPrefix() {
        return prefix;
    }

    private String apply(String text, String... replacements) {
        if (replacements == null || replacements.length == 0) {
            return text;
        }
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("[messages] Placeholder replacements must be given in key/value pairs.");
        }
        String result = text;
        int limit = replacements.length - (replacements.length % 2);
        for (int i = 0; i < limit; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            if (key == null) {
                continue;
            }
            result = result.replace("{" + key + "}", value == null ? "" : value);
        }
        return result;
    }

    /**
     * Convenience helper building a replacement map for {@link #send}.
     *
     * @return an empty map builder
     */
    public static Map<String, String> placeholders() {
        return new LinkedHashMap<>();
    }

    /** @return the loaded {@code messages.yml} for diagnostic purposes */
    public YamlConfiguration getConfiguration() {
        return messages;
    }

    /** @return all message keys currently available (for debugging). */
    public List<String> keys() {
        return new ArrayList<>(messages.getKeys(true));
    }
}
