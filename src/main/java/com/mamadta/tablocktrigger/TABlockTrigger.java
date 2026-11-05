package com.mamadta.tablocktrigger;

import com.mamadta.tablocktrigger.command.BtCommand;
import com.mamadta.tablocktrigger.command.BtTabCompleter;
import com.mamadta.tablocktrigger.config.ConfigManager;
import com.mamadta.tablocktrigger.config.MessageManager;
import com.mamadta.tablocktrigger.config.SoundManager;
import com.mamadta.tablocktrigger.execution.CommandDispatcher;
import com.mamadta.tablocktrigger.listener.BlockInteractionListener;
import com.mamadta.tablocktrigger.listener.WorldListener;
import com.mamadta.tablocktrigger.manager.BlockCommandManager;
import com.mamadta.tablocktrigger.manager.CooldownManager;
import com.mamadta.tablocktrigger.manager.PlaceholderManager;
import com.mamadta.tablocktrigger.storage.BlockStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * TA_BlockTrigger - assign commands to blocks and run them through player interactions.
 *
 * <p>Main class: wires every manager together, registers the command, the listeners and the
 * housekeeping tasks, and provides a full in-place reload used by {@code /bt reload}.</p>
 *
 * @author MamaD_TA
 */
public final class TABlockTrigger extends JavaPlugin {

    private static TABlockTrigger instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private SoundManager soundManager;
    private PlaceholderManager placeholderManager;
    private CooldownManager cooldownManager;
    private BlockCommandManager blockCommandManager;
    private BlockStorage blockStorage;
    private CommandDispatcher commandDispatcher;

    private BukkitTask autoSaveTask;
    private BukkitTask housekeepingTask;

    /** Result of a plugin reload, used by {@code /bt reload}. */
    public record ReloadResult(int blocks, long millis) {
    }

    // =======================================================================
    //  Lifecycle
    // =======================================================================

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        // ---------------------------------------------------- configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();

        messageManager = new MessageManager(this);
        messageManager.load();

        soundManager = new SoundManager(this);
        soundManager.load(getConfig());

        // -------------------------------------------------------- managers
        placeholderManager = new PlaceholderManager(this);
        cooldownManager = new CooldownManager(this);
        commandDispatcher = new CommandDispatcher(this);

        // --------------------------------------------------------- storage
        blockStorage = new BlockStorage(this);
        blockStorage.initialize();

        blockCommandManager = new BlockCommandManager(this);
        int loaded = blockCommandManager.loadFromStorage();

        // -------------------------------------------------------- listeners
        getServer().getPluginManager().registerEvents(new BlockInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);

        // ---------------------------------------------------------- command
        PluginCommand command = getCommand("bt");
        if (command == null) {
            getLogger().severe("Command 'bt' is missing from plugin.yml - the plugin cannot work!");
        } else {
            command.setExecutor(new BtCommand(this));
            command.setTabCompleter(new BtTabCompleter(this));
        }

        // ----------------------------------------------------------- tasks
        startTasks();

        // ------------------------------------------------------------ log
        if (configManager.isPluginEnabled()) {
            getLogger().info("TA_BlockTrigger v" + getPluginMeta().getVersion() + " enabled - "
                    + loaded + " block command(s) loaded in " + (System.currentTimeMillis() - start) + " ms.");
        } else {
            getLogger().warning("TA_BlockTrigger is DISABLED through settings.enabled in config.yml. "
                    + "Block commands will not run until you set it to true and use /bt reload.");
        }
        if (configManager.isDebug()) {
            getLogger().info("[debug] Selection distance: " + configManager.getMaxSelectionDistance()
                    + ", default execution: " + configManager.getDefaultExecutionType().id()
                    + ", default click: " + configManager.getDefaultClickMode().id()
                    + ", sounds: " + soundManager.describe());
        }
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
        if (blockStorage != null) {
            // Flushes every pending change synchronously before the server stops.
            blockStorage.shutdown();
        }
        getLogger().info("TA_BlockTrigger disabled.");
        instance = null;
    }

    // =======================================================================
    //  Reload
    // =======================================================================

    /**
     * Reloads {@code config.yml}, {@code messages.yml} and (optionally) the block data file.
     *
     * <p>Existing block commands are never lost: pending changes are flushed before the
     * reload starts and the in-memory data is written back afterwards.</p>
     *
     * @return statistics about the reload
     */
    public ReloadResult reloadPlugin() {
        long start = System.currentTimeMillis();

        stopTasks();

        configManager.load();
        messageManager.load();
        soundManager.load(getConfig());

        int blocks = blockCommandManager.reloadStorage();

        startTasks();

        long millis = System.currentTimeMillis() - start;
        getLogger().info("Configuration reloaded in " + millis + " ms (" + blocks + " block command(s) active).");
        if (!configManager.isPluginEnabled()) {
            getLogger().warning("TA_BlockTrigger is disabled through settings.enabled - block commands will not run.");
        }
        return new ReloadResult(blocks, millis);
    }

    // =======================================================================
    //  Tasks
    // =======================================================================

    private void startTasks() {
        if (configManager.isAutoSaveEnabled()) {
            long ticks = Math.max(20L, configManager.getAutoSaveIntervalMinutes() * 60L * 20L);
            autoSaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                // Safety net: rewrites the data file with the current in-memory state.
                if (blockCommandManager != null) {
                    blockCommandManager.saveAll();
                }
            }, ticks, ticks);
        }

        // Periodic housekeeping keeps the cooldown maps small (no memory leaks).
        housekeepingTask = getServer().getScheduler().runTaskTimer(this,
                () -> cooldownManager.cleanup(), 600L, 600L);
    }

    private void stopTasks() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (housekeepingTask != null) {
            housekeepingTask.cancel();
            housekeepingTask = null;
        }
    }

    // =======================================================================
    //  Accessors
    // =======================================================================

    /** @return the plugin instance (may be {@code null} while disabled). */
    public static TABlockTrigger getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public BlockCommandManager getBlockCommandManager() {
        return blockCommandManager;
    }

    public BlockStorage getBlockStorage() {
        return blockStorage;
    }

    public CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }
}
