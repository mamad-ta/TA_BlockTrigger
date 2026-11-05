package com.mamadta.tablocktrigger.model;

import java.util.Locale;

/**
 * How the command bound to a block is executed.
 */
public enum ExecutionType {

    /** The command runs as the server console (highest permissions). */
    CONSOLE,

    /** The command runs as the player who clicked the block. */
    PLAYER;

    /**
     * Parses the execution type (case-insensitive, a few tolerated aliases).
     *
     * @param input raw argument
     * @return the matching type or {@code null} when the input is unknown
     */
    public static ExecutionType fromString(String input) {
        if (input == null) {
            return null;
        }
        switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "console":
            case "server":
            case "asconsole":
                return CONSOLE;
            case "player":
            case "asplayer":
                return PLAYER;
            default:
                return null;
        }
    }

    /** Lower-case name used in configuration files and messages. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
