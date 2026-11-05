package com.mamadta.tablocktrigger.command;

import com.mamadta.tablocktrigger.TABlockTrigger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tab completion for {@code /bt}.
 *
 * <p>Suggestions:</p>
 * <ul>
 *   <li>first argument: {@code console}, {@code player}, {@code remove}, {@code reload},
 *       {@code help}</li>
 *   <li>last argument while defining a command: {@code right}, {@code left}, {@code both}</li>
 * </ul>
 */
public final class BtTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("console", "player", "remove", "reload", "help");
    private static final List<String> CLICK_TYPES = List.of("right", "left", "both");

    private final TABlockTrigger plugin;

    public BtTabCompleter(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Do not leak suggestions to players that may not use the command at all.
        if (!plugin.getConfigManager().isAdministrator(sender)) {
            return List.of();
        }

        if (args.length == 0) {
            return filter(SUB_COMMANDS, "");
        }

        if (args.length == 1) {
            return filter(SUB_COMMANDS, args[0]);
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (!first.equals("console") && !first.equals("player")) {
            // /bt remove | reload | help take no further arguments.
            return List.of();
        }

        // While defining a command the last argument is the click type.
        return filter(CLICK_TYPES, args[args.length - 1]);
    }

    /** Case-insensitive prefix filter. */
    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
