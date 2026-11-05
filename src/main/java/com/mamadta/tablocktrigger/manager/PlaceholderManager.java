package com.mamadta.tablocktrigger.manager;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Replaces the built-in placeholders inside configured commands.
 *
 * <p>Supported placeholders (each one can be switched off in {@code config.yml}):</p>
 * <ul>
 *   <li>{@code {player}} - name of the player who clicked the block</li>
 *   <li>{@code {uuid}} - UUID of the player</li>
 *   <li>{@code {world}} - name of the world the player is currently in</li>
 *   <li>{@code {x}}, {@code {y}}, {@code {z}} - the player's block coordinates</li>
 * </ul>
 */
public final class PlaceholderManager {

    private final TABlockTrigger plugin;

    public PlaceholderManager(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies every enabled placeholder to a command.
     *
     * @param command raw command from the storage file
     * @param player  player triggering the command
     * @return the processed command (unchanged when placeholders are disabled)
     */
    public String apply(String command, Player player) {
        if (command == null || command.isEmpty() || player == null) {
            return command;
        }
        ConfigManager config = plugin.getConfigManager();
        // Both switches must allow placeholders: the master switch in the "placeholders"
        // section and the execution level switch in the "execution" section.
        if (!config.isPlaceholdersEnabled() || !config.isUsePlaceholdersEnabled()) {
            return command;
        }
        if (command.indexOf('{') < 0) {
            return command; // nothing to replace - fast path
        }

        String result = command;
        if (config.isPlaceholderPlayer()) {
            result = result.replace("{player}", player.getName());
        }
        if (config.isPlaceholderUuid()) {
            result = result.replace("{uuid}", player.getUniqueId().toString());
        }
        if (config.isPlaceholderWorld()) {
            result = result.replace("{world}", player.getWorld().getName());
        }
        if (config.isPlaceholderCoordinates()) {
            Location location = player.getLocation();
            result = result.replace("{x}", String.valueOf(location.getBlockX()))
                    .replace("{y}", String.valueOf(location.getBlockY()))
                    .replace("{z}", String.valueOf(location.getBlockZ()));
        }
        return result;
    }

    /** @return the documentation of the supported placeholders (used by {@code /bt help}). */
    public String describeSupported() {
        return "{player}, {uuid}, {world}, {x}, {y}, {z}";
    }
}
