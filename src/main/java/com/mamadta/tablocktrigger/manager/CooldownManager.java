package com.mamadta.tablocktrigger.manager;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.config.ConfigManager;
import com.mamadta.tablocktrigger.model.BlockCommand;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles click cooldowns and the duplicate-interaction guard.
 *
 * <p>Cooldowns can be tracked per player (default) or per block, optionally separately for
 * left and right clicks. Entries are stored in concurrent maps and cleaned up periodically
 * so the maps cannot grow without bound.</p>
 */
public final class CooldownManager {

    private final TABlockTrigger plugin;

    /** key -> epoch millis when the cooldown expires. */
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    /** key -> server tick of the last processed interaction. */
    private final Map<String, Integer> recentInteractions = new ConcurrentHashMap<>();

    public CooldownManager(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    // =======================================================================
    //  Duplicate interaction guard
    // =======================================================================

    /**
     * Detects two identical interactions (same player, block and click) inside one tick,
     * which prevents double execution through main-hand/off-hand or duplicate events.
     *
     * @param player    interacting player
     * @param block     clicked block
     * @param leftClick whether the interaction was a left click
     * @return {@code true} when this interaction was already handled in the same tick
     */
    public boolean isDuplicate(Player player, Block block, boolean leftClick) {
        String key = player.getUniqueId() + "|" + block.getWorld().getUID() + "|"
                + block.getX() + "," + block.getY() + "," + block.getZ() + "|" + (leftClick ? "l" : "r");
        int tick = Bukkit.getCurrentTick();
        Integer previous = recentInteractions.put(key, tick);
        return previous != null && previous == tick;
    }

    // =======================================================================
    //  Cooldowns
    // =======================================================================

    /**
     * @param player    player about to trigger the command
     * @param block     clicked block
     * @param command   block command being triggered
     * @param leftClick whether the interaction was a left click
     * @return remaining cooldown in seconds, {@code 0} when the player may execute the command
     */
    public double getRemainingSeconds(Player player, Block block, BlockCommand command, boolean leftClick) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isCooldownEnabled() || config.getCooldownSeconds() <= 0.0D) {
            return 0.0D;
        }
        Long expiresAt = cooldowns.get(buildKey(player, block, command, leftClick));
        if (expiresAt == null) {
            return 0.0D;
        }
        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            return 0.0D;
        }
        return remainingMillis / 1000.0D;
    }

    /**
     * Starts (or refreshes) the cooldown for an executed command.
     *
     * @param player    player who executed the command
     * @param block     clicked block
     * @param command   triggered block command
     * @param leftClick whether the interaction was a left click
     */
    public void apply(Player player, Block block, BlockCommand command, boolean leftClick) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isCooldownEnabled() || config.getCooldownSeconds() <= 0.0D) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + (long) Math.ceil(config.getCooldownSeconds() * 1000.0D);
        cooldowns.put(buildKey(player, block, command, leftClick), expiresAt);
    }

    private String buildKey(Player player, Block block, BlockCommand command, boolean leftClick) {
        ConfigManager config = plugin.getConfigManager();
        StringBuilder builder = new StringBuilder();
        if (config.isCooldownPerBlock()) {
            builder.append(command.getKey().worldId()).append('|')
                    .append(command.getX()).append(',').append(command.getY()).append(',').append(command.getZ());
        } else {
            builder.append(player.getUniqueId());
        }
        if (config.isCooldownSeparateClickTypes()) {
            builder.append('|').append(leftClick ? "left" : "right");
        }
        return builder.toString();
    }

    // =======================================================================
    //  Housekeeping
    // =======================================================================

    /** Removes expired cooldowns and stale duplicate markers. */
    public void cleanup() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);

        int currentTick = Bukkit.getCurrentTick();
        recentInteractions.entrySet().removeIf(entry -> currentTick - entry.getValue() > 20);
    }

    /** Clears every tracked cooldown (used on disable/reload). */
    public void clearAll() {
        cooldowns.clear();
        recentInteractions.clear();
    }

    /** @return amount of currently tracked cooldown entries (debug/diagnostics). */
    public int size() {
        return cooldowns.size();
    }
}
