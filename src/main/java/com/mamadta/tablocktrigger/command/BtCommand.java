package com.mamadta.tablocktrigger.command;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.config.ConfigManager;
import com.mamadta.tablocktrigger.config.MessageManager;
import com.mamadta.tablocktrigger.config.SoundManager;
import com.mamadta.tablocktrigger.model.BlockCommand;
import com.mamadta.tablocktrigger.model.ClickMode;
import com.mamadta.tablocktrigger.model.ExecutionType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles the {@code /bt} management command.
 *
 * <p>Syntax:</p>
 * <pre>
 *   /bt &lt;console|player&gt; &lt;command ...&gt; [right|left|both]
 *   /bt &lt;command ...&gt; [right|left|both]      (uses the configured default execution type)
 *   /bt remove
 *   /bt reload
 *   /bt help
 * </pre>
 *
 * <p>The command parser keeps the first argument (execution type) and the last argument
 * (click type) separate while preserving the whole command - including spaces - in the
 * middle. Both arguments are optional and fall back to the configured defaults.</p>
 */
public final class BtCommand implements CommandExecutor {

    private final TABlockTrigger plugin;

    public BtCommand(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return handleCommand(sender, label, args);
        } catch (Throwable throwable) {
            // Never let an unexpected problem break the command; report it instead.
            plugin.getLogger().severe("Unexpected error while handling /" + label + ": " + throwable);
            plugin.getMessageManager().send(sender, "general.error",
                    "error", throwable.getClass().getSimpleName()
                            + (throwable.getMessage() == null ? "" : ": " + throwable.getMessage()));
            return true;
        }
    }

    private boolean handleCommand(CommandSender sender, String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();
        MessageManager messages = plugin.getMessageManager();

        // ---------------- permission -------------------------------------
        if (!config.isAdministrator(sender)) {
            messages.send(sender, "general.no-permission");
            if (sender instanceof Player player) {
                plugin.getSoundManager().play(player, SoundManager.SoundKey.NO_PERMISSION);
            }
            return true;
        }

        if (args.length == 0) {
            messages.send(sender, "general.usage", "label", label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "help", "?" -> messages.sendList(sender, "help.lines",
                    "distance", String.valueOf(config.getMaxSelectionDistance()),
                    "default-execution", config.getDefaultExecutionType().id(),
                    "default-click", config.getDefaultClickMode().id(),
                    "placeholders", plugin.getPlaceholderManager().describeSupported());
            case "remove" -> handleRemove(sender);
            case "console", "player" -> handleSet(sender, args);
            default -> {
                // Allows /bt <command ...> [click] without an explicit execution type:
                // the configured default execution type is used.
                if (!config.isPluginEnabled()) {
                    messages.send(sender, "general.plugin-disabled");
                    return true;
                }
                handleSet(sender, args);
            }
        }
        return true;
    }

    // =======================================================================
    //  /bt <console|player> <command> [click]
    // =======================================================================

    private void handleSet(CommandSender sender, String[] args) {
        ConfigManager config = plugin.getConfigManager();
        MessageManager messages = plugin.getMessageManager();
        SoundManager sounds = plugin.getSoundManager();

        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.player-only");
            return;
        }
        if (!config.isPluginEnabled()) {
            messages.send(player, "general.plugin-disabled");
            return;
        }

        // ---------------- parse execution type (first argument) ----------
        List<String> tokens = new ArrayList<>(List.of(args));
        ExecutionType executionType = config.getDefaultExecutionType();
        if (!tokens.isEmpty()) {
            ExecutionType parsed = ExecutionType.fromString(tokens.get(0));
            if (parsed != null) {
                executionType = parsed;
                tokens.remove(0);
            }
        }

        if (tokens.isEmpty()) {
            invalidCommand(sender);
            return;
        }

        // ---------------- parse click type (last argument) ---------------
        ClickMode clickMode = config.getDefaultClickMode();
        if (tokens.size() > 1) {
            ClickMode parsed = ClickMode.fromString(tokens.get(tokens.size() - 1));
            if (parsed != null) {
                clickMode = parsed;
                tokens.remove(tokens.size() - 1);
            }
        }

        String rawCommand = String.join(" ", tokens).trim();
        if (rawCommand.isEmpty()) {
            invalidCommand(sender);
            return;
        }

        // ---------------- leading slash ---------------------------------
        if (rawCommand.startsWith("/")) {
            if (!config.isAcceptLeadingSlash()) {
                messages.send(player, "set.leading-slash");
                sounds.play(player, SoundManager.SoundKey.INVALID_COMMAND);
                return;
            }
            rawCommand = rawCommand.substring(1).trim();
            if (rawCommand.isEmpty()) {
                invalidCommand(sender);
                return;
            }
        }

        // ---------------- target block ----------------------------------
        int distance = config.getMaxSelectionDistance();
        Block target = player.getTargetBlockExact(distance, FluidCollisionMode.NEVER);
        if (target == null || target.getType().isAir()) {
            messages.send(player, "select.no-block", "distance", String.valueOf(distance));
            sounds.play(player, SoundManager.SoundKey.INVALID_COMMAND);
            return;
        }

        // ---------------- execution type enabled ------------------------
        if (executionType == ExecutionType.CONSOLE && !config.isConsoleExecutionEnabled()) {
            messages.send(player, "set.execution-disabled", "type", executionType.id());
            sounds.play(player, SoundManager.SoundKey.INVALID_COMMAND);
            return;
        }
        if (executionType == ExecutionType.PLAYER && !config.isPlayerExecutionEnabled()) {
            messages.send(player, "set.execution-disabled", "type", executionType.id());
            sounds.play(player, SoundManager.SoundKey.INVALID_COMMAND);
            return;
        }

        // ---------------- click type enabled ----------------------------
        boolean clickAllowed = switch (clickMode) {
            case RIGHT -> config.isRightClickEnabled();
            case LEFT -> config.isLeftClickEnabled();
            case BOTH -> config.isBothClickEnabled()
                    && (config.isLeftClickEnabled() || config.isRightClickEnabled());
        };
        if (!clickAllowed) {
            messages.send(player, "set.click-disabled", "click", clickMode.id());
            sounds.play(player, SoundManager.SoundKey.INVALID_COMMAND);
            return;
        }

        // ---------------- store -----------------------------------------
        BlockCommand blockCommand = BlockCommand.fromBlock(target, executionType, clickMode, rawCommand, player.getName());
        BlockCommand previous = plugin.getBlockCommandManager().set(blockCommand);

        messages.send(player, "set.success",
                "world", blockCommand.getWorldName(),
                "x", String.valueOf(blockCommand.getX()),
                "y", String.valueOf(blockCommand.getY()),
                "z", String.valueOf(blockCommand.getZ()),
                "block", target.getType().name(),
                "execution", executionType.id(),
                "click", clickMode.id(),
                "command", rawCommand,
                "replaced", previous == null ? "false" : "true");
        sounds.play(player, SoundManager.SoundKey.SET_COMMAND);
    }

    // =======================================================================
    //  /bt remove
    // =======================================================================

    private void handleRemove(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
        MessageManager messages = plugin.getMessageManager();
        SoundManager sounds = plugin.getSoundManager();

        if (!(sender instanceof Player player)) {
            messages.send(sender, "general.player-only");
            return;
        }
        if (!config.isPluginEnabled()) {
            messages.send(player, "general.plugin-disabled");
            return;
        }

        int distance = config.getMaxSelectionDistance();
        Block target = player.getTargetBlockExact(distance, FluidCollisionMode.NEVER);
        if (target == null || target.getType().isAir()) {
            messages.send(player, "select.no-block", "distance", String.valueOf(distance));
            sounds.play(player, SoundManager.SoundKey.INVALID_COMMAND);
            return;
        }

        BlockCommand removed = plugin.getBlockCommandManager().remove(target);
        if (removed == null) {
            messages.send(player, "remove.no-command",
                    "world", target.getWorld().getName(),
                    "x", String.valueOf(target.getX()),
                    "y", String.valueOf(target.getY()),
                    "z", String.valueOf(target.getZ()));
            sounds.play(player, SoundManager.SoundKey.INVALID_COMMAND);
            return;
        }

        messages.send(player, "remove.success",
                "world", removed.getWorldName(),
                "x", String.valueOf(removed.getX()),
                "y", String.valueOf(removed.getY()),
                "z", String.valueOf(removed.getZ()),
                "block", target.getType().name(),
                "execution", removed.getExecutionType().id(),
                "click", removed.getClickMode().id(),
                "command", removed.getCommand());
        sounds.play(player, SoundManager.SoundKey.REMOVE_COMMAND);
    }

    // =======================================================================
    //  /bt reload
    // =======================================================================

    private void handleReload(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        try {
            TABlockTrigger.ReloadResult result = plugin.reloadPlugin();
            messages.send(sender, "reload.success",
                    "time", String.valueOf(result.millis()),
                    "blocks", String.valueOf(result.blocks()));
            if (sender instanceof Player player) {
                plugin.getSoundManager().play(player, SoundManager.SoundKey.RELOAD);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().severe("Reload failed: " + throwable.getMessage());
            messages.send(sender, "reload.failed", "error", String.valueOf(throwable.getMessage()));
            if (sender instanceof Player player) {
                plugin.getSoundManager().play(player, SoundManager.SoundKey.INVALID_COMMAND);
            }
        }
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

    private void invalidCommand(CommandSender sender) {
        plugin.getMessageManager().send(sender, "set.invalid-command");
        if (sender instanceof Player player) {
            plugin.getSoundManager().play(player, SoundManager.SoundKey.INVALID_COMMAND);
        }
    }
}
