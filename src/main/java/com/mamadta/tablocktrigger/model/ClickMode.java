package com.mamadta.tablocktrigger.model;

import java.util.Locale;

/**
 * Which mouse click(s) trigger the command bound to a block.
 */
public enum ClickMode {

    /** Only a right click triggers the command. */
    RIGHT,

    /** Only a left click triggers the command. */
    LEFT,

    /** Both left and right click trigger the command. */
    BOTH;

    /**
     * Parses the click mode (case-insensitive).
     *
     * @param input raw argument
     * @return the matching mode or {@code null} when the input is unknown
     */
    public static ClickMode fromString(String input) {
        if (input == null) {
            return null;
        }
        switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "right":
            case "right-click":
            case "rightclick":
                return RIGHT;
            case "left":
            case "left-click":
            case "leftclick":
                return LEFT;
            case "both":
            case "any":
            case "all":
                return BOTH;
            default:
                return null;
        }
    }

    /** Lower-case name used in configuration files and messages. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @param leftClick whether the interaction was a left click
     * @return {@code true} when this mode handles the given click
     */
    public boolean matches(boolean leftClick) {
        return switch (this) {
            case RIGHT -> !leftClick;
            case LEFT -> leftClick;
            case BOTH -> true;
        };
    }
}
