package com.mamadta.tablocktrigger.config;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.model.ClickMode;
import com.mamadta.tablocktrigger.model.ExecutionType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads, validates and caches every option of {@code config.yml}.
 *
 * <p>All values are cached in fields on load so that hot paths (block interactions) never
 * have to touch the YAML file. Every value is validated: invalid entries are logged with a
 * helpful warning and replaced by a sensible default instead of breaking the plugin.</p>
 */
public final class ConfigManager {

    private final TABlockTrigger plugin;

    // ---------------------------------------------------------------- settings
    private boolean pluginEnabled = true;
    private int maxSelectionDistance = 10;
    private ExecutionType defaultExecutionType = ExecutionType.CONSOLE;
    private ClickMode defaultClickMode = ClickMode.RIGHT;
    private boolean debug = false;

    // ------------------------------------------------------------- permissions
    private boolean opOnly = true;
    private boolean usePermissionNode = false;
    private String adminPermissionNode = "tablocktrigger.admin";
    private boolean opsBypass = true;

    // ------------------------------------------------------------------ worlds
    private String worldMode = "all";
    private final List<String> worldList = new ArrayList<>();
    private boolean runInFilteredWorlds = false;

    // ------------------------------------------------------------ interactions
    private boolean rightClickEnabled = true;
    private boolean leftClickEnabled = true;
    private boolean bothClickEnabled = true;
    private boolean preventBlockBreaking = true;
    private boolean preventOffhandDuplicates = true;
    private boolean preventDuplicateInteractions = true;
    private boolean allowNormalInteraction = true;
    private boolean runWhenEventCancelled = false;

    // --------------------------------------------------------------- execution
    private boolean consoleExecutionEnabled = true;
    private boolean playerExecutionEnabled = true;
    private boolean acceptLeadingSlash = true;
    private boolean usePlaceholders = true;
    private boolean logCommands = false;
    private boolean showFailedMessages = true;
    private boolean requireExecutionPermission = false;
    private String executionPermissionNode = "tablocktrigger.use";

    // ---------------------------------------------------------------- cooldown
    private boolean cooldownEnabled = false;
    private double cooldownSeconds = 1.0D;
    private boolean cooldownPerBlock = false;
    private boolean cooldownSeparateClickTypes = true;

    // ------------------------------------------------------------ placeholders
    private boolean placeholdersEnabled = true;
    private boolean placeholderPlayer = true;
    private boolean placeholderUuid = true;
    private boolean placeholderWorld = true;
    private boolean placeholderCoordinates = true;

    // ----------------------------------------------------------------- storage
    private String storageFileName = "blocks.yml";
    private boolean autoSaveEnabled = true;
    private int autoSaveIntervalMinutes = 5;
    private boolean storageReloadOnPluginReload = true;
    private boolean validateOnLoad = true;
    private boolean backupOnSave = false;
    private boolean storageDebugLogging = false;

    public ConfigManager(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    // =======================================================================
    //  Loading / validation
    // =======================================================================

    /** (Re)loads and validates {@code config.yml}. */
    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // ---------------------------------------------------------- settings
        pluginEnabled = getBoolean(config, "settings.enabled", true);
        maxSelectionDistance = getInt(config, "settings.max-selection-distance", 10, 1, 100);
        defaultExecutionType = getExecutionType(config, "settings.default-execution-type", ExecutionType.CONSOLE);
        defaultClickMode = getClickMode(config, "settings.default-click-type", ClickMode.RIGHT);
        debug = getBoolean(config, "settings.debug", false);

        // ------------------------------------------------------- permissions
        opOnly = getBoolean(config, "permissions.op-only", true);
        usePermissionNode = getBoolean(config, "permissions.use-permission-node", false);
        adminPermissionNode = getString(config, "permissions.permission-node", "tablocktrigger.admin");
        opsBypass = getBoolean(config, "permissions.ops-bypass", true);
        if (adminPermissionNode.isBlank()) {
            warn("permissions.permission-node is empty, falling back to 'tablocktrigger.admin'.");
            adminPermissionNode = "tablocktrigger.admin";
        }
        if (!opOnly && !usePermissionNode) {
            warn("Both permissions.op-only and permissions.use-permission-node are disabled. "
                    + "Only holders of '" + adminPermissionNode + "' may use /bt.");
        }

        // ------------------------------------------------------------ worlds
        worldMode = getString(config, "worlds.mode", "all").toLowerCase(Locale.ROOT);
        if (!worldMode.equals("all") && !worldMode.equals("whitelist") && !worldMode.equals("blacklist")) {
            warn("worlds.mode must be one of: all, whitelist, blacklist. Using 'all'.");
            worldMode = "all";
        }
        worldList.clear();
        for (String entry : getStringList(config, "worlds.list")) {
            if (entry != null && !entry.isBlank()) {
                worldList.add(entry.trim());
            }
        }
        if (worldMode.equals("whitelist") && worldList.isEmpty()) {
            warn("worlds.mode is 'whitelist' but worlds.list is empty - block commands are disabled in every world.");
        }
        runInFilteredWorlds = getBoolean(config, "worlds.run-in-filtered-worlds", false);

        // ------------------------------------------------------ interactions
        rightClickEnabled = getBoolean(config, "interactions.right-click", true);
        leftClickEnabled = getBoolean(config, "interactions.left-click", true);
        bothClickEnabled = getBoolean(config, "interactions.both", true);
        preventBlockBreaking = getBoolean(config, "interactions.prevent-block-breaking", true);
        preventOffhandDuplicates = getBoolean(config, "interactions.prevent-offhand-duplicates", true);
        preventDuplicateInteractions = getBoolean(config, "interactions.prevent-duplicate-interactions", true);
        allowNormalInteraction = getBoolean(config, "interactions.allow-normal-interaction", true);
        runWhenEventCancelled = getBoolean(config, "interactions.run-when-event-cancelled", false);

        // --------------------------------------------------------- execution
        consoleExecutionEnabled = getBoolean(config, "execution.console", true);
        playerExecutionEnabled = getBoolean(config, "execution.player", true);
        acceptLeadingSlash = getBoolean(config, "execution.accept-leading-slash", true);
        usePlaceholders = getBoolean(config, "execution.use-placeholders", true);
        logCommands = getBoolean(config, "execution.log-commands", false);
        showFailedMessages = getBoolean(config, "execution.show-failed-messages", true);
        requireExecutionPermission = getBoolean(config, "execution.require-permission", false);
        executionPermissionNode = getString(config, "execution.permission-node", "tablocktrigger.use");
        if (executionPermissionNode.isBlank()) {
            warn("execution.permission-node is empty, falling back to 'tablocktrigger.use'.");
            executionPermissionNode = "tablocktrigger.use";
        }

        // ---------------------------------------------------------- cooldown
        cooldownEnabled = getBoolean(config, "cooldown.enabled", false);
        cooldownSeconds = getDouble(config, "cooldown.seconds", 1.0D, 0.0D, 86_400D);
        if (cooldownSeconds <= 0.0D && cooldownEnabled) {
            warn("cooldown.seconds is 0 while the cooldown is enabled - it will have no effect.");
        }
        String cooldownType = getString(config, "cooldown.type", "player").toLowerCase(Locale.ROOT);
        if (!cooldownType.equals("player") && !cooldownType.equals("block")) {
            warn("cooldown.type must be 'player' or 'block'. Using 'player'.");
            cooldownType = "player";
        }
        cooldownPerBlock = cooldownType.equals("block");
        cooldownSeparateClickTypes = getBoolean(config, "cooldown.separate-click-types", true);

        // ------------------------------------------------------ placeholders
        placeholdersEnabled = getBoolean(config, "placeholders.enabled", true);
        placeholderPlayer = getBoolean(config, "placeholders.player", true);
        placeholderUuid = getBoolean(config, "placeholders.uuid", true);
        placeholderWorld = getBoolean(config, "placeholders.world", true);
        placeholderCoordinates = getBoolean(config, "placeholders.coordinates", true);

        // ----------------------------------------------------------- storage
        String fileName = getString(config, "storage.file", "blocks.yml");
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".yml")) {
            warn("storage.file must end with .yml - using 'blocks.yml'.");
            fileName = "blocks.yml";
        }
        storageFileName = fileName;
        autoSaveEnabled = getBoolean(config, "storage.auto-save.enabled", true);
        autoSaveIntervalMinutes = getInt(config, "storage.auto-save.interval-minutes", 5, 1, 1440);
        storageReloadOnPluginReload = getBoolean(config, "storage.reload-on-plugin-reload", true);
        validateOnLoad = getBoolean(config, "storage.validate-on-load", true);
        backupOnSave = getBoolean(config, "storage.backup-on-save", false);
        storageDebugLogging = getBoolean(config, "storage.debug-logging", false);
    }

    // =======================================================================
    //  Permission helpers
    // =======================================================================

    /**
     * Checks whether a sender may manage block commands.
     *
     * <p>Rules (all configurable): the console always may; operators may when
     * {@code permissions.ops-bypass} or {@code permissions.op-only} is enabled; otherwise
     * the configured permission node is required.</p>
     *
     * @param sender command sender to check
     * @return {@code true} when the sender is an administrator
     */
    public boolean isAdministrator(CommandSender sender) {
        if (sender == null) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            // Console, command blocks, etc. - always trusted.
            return true;
        }
        if (opsBypass && player.isOp()) {
            return true;
        }
        if (opOnly && player.isOp()) {
            return true;
        }
        if (usePermissionNode) {
            return player.hasPermission(adminPermissionNode);
        }
        if (!opOnly) {
            // Neither OP-only nor node-based: still require the node so the command is
            // never open to everyone by accident.
            return player.hasPermission(adminPermissionNode);
        }
        return false;
    }

    /**
     * @param player player about to trigger a block command
     * @return {@code true} when the player is allowed to trigger block commands
     */
    public boolean canTriggerCommands(Player player) {
        return !requireExecutionPermission || player.hasPermission(executionPermissionNode);
    }

    /**
     * Checks the configured world filter.
     *
     * @param worldName name of the world
     * @return {@code true} when the world is handled by the plugin
     */
    public boolean isWorldAllowed(String worldName) {
        if (worldName == null) {
            return false;
        }
        switch (worldMode) {
            case "whitelist":
                return containsWorld(worldName);
            case "blacklist":
                return !containsWorld(worldName);
            default:
                return true;
        }
    }

    private boolean containsWorld(String worldName) {
        for (String entry : worldList) {
            if (entry.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a click mode is enabled for a given click.
     *
     * @param mode      configured click mode of the block
     * @param leftClick whether the player left-clicked
     * @return {@code true} when the interaction should be handled
     */
    public boolean isClickEnabled(ClickMode mode, boolean leftClick) {
        if (mode == ClickMode.BOTH) {
            return bothClickEnabled && (leftClick ? leftClickEnabled : rightClickEnabled);
        }
        if (mode == ClickMode.LEFT) {
            return leftClickEnabled;
        }
        return rightClickEnabled;
    }

    // =======================================================================
    //  Getters
    // =======================================================================

    public FileConfiguration raw() {
        return plugin.getConfig();
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public int getMaxSelectionDistance() {
        return maxSelectionDistance;
    }

    public ExecutionType getDefaultExecutionType() {
        return defaultExecutionType;
    }

    public ClickMode getDefaultClickMode() {
        return defaultClickMode;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isOpOnly() {
        return opOnly;
    }

    public boolean isUsePermissionNode() {
        return usePermissionNode;
    }

    public String getAdminPermissionNode() {
        return adminPermissionNode;
    }

    public boolean isOpsBypass() {
        return opsBypass;
    }

    public String getWorldMode() {
        return worldMode;
    }

    public List<String> getWorldList() {
        return List.copyOf(worldList);
    }

    public boolean isRunInFilteredWorlds() {
        return runInFilteredWorlds;
    }

    public boolean isRightClickEnabled() {
        return rightClickEnabled;
    }

    public boolean isLeftClickEnabled() {
        return leftClickEnabled;
    }

    public boolean isBothClickEnabled() {
        return bothClickEnabled;
    }

    public boolean isPreventBlockBreaking() {
        return preventBlockBreaking;
    }

    public boolean isPreventOffhandDuplicates() {
        return preventOffhandDuplicates;
    }

    public boolean isPreventDuplicateInteractions() {
        return preventDuplicateInteractions;
    }

    public boolean isAllowNormalInteraction() {
        return allowNormalInteraction;
    }

    public boolean isRunWhenEventCancelled() {
        return runWhenEventCancelled;
    }

    public boolean isConsoleExecutionEnabled() {
        return consoleExecutionEnabled;
    }

    public boolean isPlayerExecutionEnabled() {
        return playerExecutionEnabled;
    }

    public boolean isAcceptLeadingSlash() {
        return acceptLeadingSlash;
    }

    public boolean isUsePlaceholdersEnabled() {
        return usePlaceholders;
    }

    public boolean isLogCommands() {
        return logCommands;
    }

    public boolean isShowFailedMessages() {
        return showFailedMessages;
    }

    public boolean isRequireExecutionPermission() {
        return requireExecutionPermission;
    }

    public String getExecutionPermissionNode() {
        return executionPermissionNode;
    }

    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isCooldownPerBlock() {
        return cooldownPerBlock;
    }

    public boolean isCooldownSeparateClickTypes() {
        return cooldownSeparateClickTypes;
    }

    public boolean isPlaceholdersEnabled() {
        return placeholdersEnabled;
    }

    public boolean isPlaceholderPlayer() {
        return placeholderPlayer;
    }

    public boolean isPlaceholderUuid() {
        return placeholderUuid;
    }

    public boolean isPlaceholderWorld() {
        return placeholderWorld;
    }

    public boolean isPlaceholderCoordinates() {
        return placeholderCoordinates;
    }

    public String getStorageFileName() {
        return storageFileName;
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public int getAutoSaveIntervalMinutes() {
        return autoSaveIntervalMinutes;
    }

    public boolean isStorageReloadOnPluginReload() {
        return storageReloadOnPluginReload;
    }

    public boolean isValidateOnLoad() {
        return validateOnLoad;
    }

    public boolean isBackupOnSave() {
        return backupOnSave;
    }

    public boolean isStorageDebugLogging() {
        return storageDebugLogging;
    }

    // =======================================================================
    //  Safe value access
    // =======================================================================

    private void warn(String message) {
        plugin.getLogger().warning("[config] " + message);
    }

    private boolean getBoolean(FileConfiguration config, String path, boolean def) {
        Object raw = config.get(path);
        if (raw == null) {
            return def;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof String value) {
            if (value.equalsIgnoreCase("true")) {
                return true;
            }
            if (value.equalsIgnoreCase("false")) {
                return false;
            }
        }
        warn("Invalid boolean at '" + path + "' (" + raw + ") - using default: " + def);
        return def;
    }

    private int getInt(FileConfiguration config, String path, int def, int min, int max) {
        Object raw = config.get(path);
        if (raw == null) {
            return def;
        }
        int value;
        try {
            value = raw instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException exception) {
            warn("Invalid number at '" + path + "' (" + raw + ") - using default: " + def);
            return def;
        }
        if (value < min || value > max) {
            int clamped = Math.max(min, Math.min(max, value));
            warn("Value '" + path + "' (" + value + ") is out of range [" + min + ", " + max + "] - using " + clamped + ".");
            return clamped;
        }
        return value;
    }

    private double getDouble(FileConfiguration config, String path, double def, double min, double max) {
        Object raw = config.get(path);
        if (raw == null) {
            return def;
        }
        double value;
        try {
            value = raw instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(raw).trim());
        } catch (NumberFormatException exception) {
            warn("Invalid number at '" + path + "' (" + raw + ") - using default: " + def);
            return def;
        }
        if (value < min || value > max) {
            double clamped = Math.max(min, Math.min(max, value));
            warn("Value '" + path + "' (" + value + ") is out of range [" + min + ", " + max + "] - using " + clamped + ".");
            return clamped;
        }
        return value;
    }

    private String getString(FileConfiguration config, String path, String def) {
        Object raw = config.get(path);
        if (raw == null) {
            return def;
        }
        if (raw instanceof String value) {
            return value.trim();
        }
        warn("Invalid text at '" + path + "' (" + raw + ") - using default: " + def);
        return def;
    }

    private List<String> getStringList(FileConfiguration config, String path) {
        Object raw = config.get(path);
        if (raw == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object entry : list) {
                if (entry != null) {
                    out.add(String.valueOf(entry));
                }
            }
            return out;
        }
        if (raw instanceof ConfigurationSection section) {
            for (String key : section.getKeys(false)) {
                out.add(key);
            }
            return out;
        }
        if (raw instanceof String value) {
            return List.of(value);
        }
        warn("Invalid list at '" + path + "' - using an empty list.");
        return List.of();
    }

    private ExecutionType getExecutionType(FileConfiguration config, String path, ExecutionType def) {
        Object raw = config.get(path);
        if (raw == null) {
            return def;
        }
        ExecutionType parsed = ExecutionType.fromString(String.valueOf(raw));
        if (parsed == null) {
            warn("Invalid execution type at '" + path + "' (" + raw + ") - using default: " + def.id());
            return def;
        }
        return parsed;
    }

    private ClickMode getClickMode(FileConfiguration config, String path, ClickMode def) {
        Object raw = config.get(path);
        if (raw == null) {
            return def;
        }
        ClickMode parsed = ClickMode.fromString(String.valueOf(raw));
        if (parsed == null) {
            warn("Invalid click type at '" + path + "' (" + raw + ") - using default: " + def.id());
            return def;
        }
        return parsed;
    }
}
