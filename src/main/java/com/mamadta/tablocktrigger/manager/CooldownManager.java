package com.mamadta.tablocktrigger.manager;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.config.ConfigManager;
import com.mamadta.tablocktrigger.model.BlockCommand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles click cooldowns and the duplicate-interaction guard.
 *
 * <p>Cooldowns can be tracked per player (default) or per block, optionally separately for
 * left and right clicks. Entries are stored in concurrent maps and cleaned up periodically
 * so the maps cannot grow without bound.</p>
 *
 * <p>The duplicate-interaction guard needs a notion of "current tick". {@code
 * Bukkit.getCurrentTick()} only exists on Paper, so this class keeps its own tick counter
 * instead, advanced once per server tick by the plugin's housekeeping task. That keeps the
 * plugin portable to Spigot and CraftBukkit at no cost.</p>
 */
public final class CooldownManager {

    private final TABlockTrigger plugin;

    /** key -> epoch millis when the cooldown expires. */
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    /** key -> plugin tick of the last processed interaction. */
    private final Map<String, Integer> recentInteractions = new ConcurrentHashMap<>();

    /**
     * Plugin-side tick counter, advanced by {@link #tick()} once per server tick.
     * Replaces Paper's {@code Bukkit.getCurrentTick()}, which does not exist on Spigot.
     */
    private final AtomicInteger currentTick = new AtomicInteger();

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
        int tick = currentTick.get();
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

        int tick = currentTick.get();
        recentInteractions.entrySet().removeIf(entry -> tick - entry.getValue() > 20);
    }

    /**
     * Advances the plugin-side tick counter by one.
     *
     * <p>Called once per server tick from the plugin's housekeeping task, which is the
     * portable replacement for Paper's {@code Bukkit.getCurrentTick()}.</p>
     */
    public void tick() {
        currentTick.incrementAndGet();
    }

    /** @return the current plugin-side tick (monotonic, starts at 0). */
    public int ticks() {
        return currentTick.get();
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
