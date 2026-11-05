package com.mamadta.tablocktrigger.execution;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.config.ConfigManager;
import com.mamadta.tablocktrigger.config.MessageManager;
import com.mamadta.tablocktrigger.config.SoundManager;
import com.mamadta.tablocktrigger.manager.CooldownManager;
import com.mamadta.tablocktrigger.model.BlockCommand;
import com.mamadta.tablocktrigger.model.ExecutionType;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Executes the command bound to a block, honouring permissions, cooldowns,
 * placeholders and every execution related setting.
 */
public final class CommandDispatcher {

    private final TABlockTrigger plugin;

    public CommandDispatcher(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs the command of a configured block for the given player.
     *
     * @param player    player who clicked the block
     * @param block     clicked block
     * @param command   configured block command
     * @param leftClick whether the interaction was a left click
     * @return {@code true} when the command was executed successfully
     */
    public boolean execute(Player player, Block block, BlockCommand command, boolean leftClick) {
        ConfigManager config = plugin.getConfigManager();
        MessageManager messages = plugin.getMessageManager();
        SoundManager sounds = plugin.getSoundManager();
        CooldownManager cooldowns = plugin.getCooldownManager();

        // ------------------------------------------------------- permission
        if (!config.canTriggerCommands(player)) {
            messages.send(player, "execution.no-permission");
            sounds.play(player, SoundManager.SoundKey.NO_PERMISSION);
            debug("Blocked " + player.getName() + " (missing permission) from using " + command.locationString());
            return false;
        }

        // -------------------------------------------------------- cooldown
        double remaining = cooldowns.getRemainingSeconds(player, block, command, leftClick);
        if (remaining > 0.0D) {
            messages.send(player, "cooldown.active",
                    "seconds", formatSeconds(remaining),
                    "player", player.getName());
            sounds.play(player, SoundManager.SoundKey.COOLDOWN);
            return false;
        }

        // ---------------------------------------------------- placeholders
        String rawCommand = command.getCommand();
        String resolved = plugin.getPlaceholderManager().apply(rawCommand, player);
        if (resolved == null || resolved.isBlank()) {
            return false;
        }
        String finalCommand = stripLeadingSlash(resolved);

        // ------------------------------------------------------- execution
        boolean success;
        try {
            if (command.getExecutionType() == ExecutionType.CONSOLE) {
                if (!config.isConsoleExecutionEnabled()) {
                    messages.send(player, "execution.type-disabled", "type", "console");
                    return false;
                }
                success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            } else {
                if (!config.isPlayerExecutionEnabled()) {
                    messages.send(player, "execution.type-disabled", "type", "player");
                    return false;
                }
                success = player.performCommand(finalCommand);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Could not execute block command '" + finalCommand + "': " + throwable.getMessage());
            success = false;
        }

        // ------------------------------------------------------- feedback
        if (config.isLogCommands()) {
            plugin.getLogger().info("[command] " + player.getName() + " triggered "
                    + command.getExecutionType().id() + " command '" + finalCommand + "' at "
                    + command.locationString());
        } else {
            debug(player.getName() + " triggered " + command.getExecutionType().id()
                    + " command '" + finalCommand + "' at " + command.locationString());
        }

        if (!success && config.isShowFailedMessages()) {
            messages.send(player, "execution.failed", "command", finalCommand);
        }

        if (success) {
            cooldowns.apply(player, block, command, leftClick);
            sounds.play(player, SoundManager.SoundKey.CLICK);
        }
        return success;
    }

    /** Commands are always dispatched without a leading slash. */
    private String stripLeadingSlash(String command) {
        String result = command.trim();
        while (result.startsWith("/")) {
            result = result.substring(1).trim();
        }
        return result;
    }

    /** Formats a remaining cooldown with one decimal for a friendly message. */
    public static String formatSeconds(double seconds) {
        if (seconds < 10.0D) {
            return String.format(java.util.Locale.ROOT, "%.1f", seconds);
        }
        return String.valueOf((long) Math.ceil(seconds));
    }

    private void debug(String message) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[dispatch] " + message);
        }
    }
}
