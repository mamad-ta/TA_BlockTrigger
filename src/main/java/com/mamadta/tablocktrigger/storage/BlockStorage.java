package com.mamadta.tablocktrigger.storage;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.config.ConfigManager;
import com.mamadta.tablocktrigger.model.BlockCommand;
import com.mamadta.tablocktrigger.model.ClickMode;
import com.mamadta.tablocktrigger.model.ExecutionType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Reads and writes the block command data file ({@code blocks.yml} by default).
 *
 * <p>Design goals:</p>
 * <ul>
 *   <li><b>Immediate persistence</b> - every change requests a save right away.</li>
 *   <li><b>No excessive I/O</b> - saves run on a single background thread and requests that
 *       arrive while a write is in progress are coalesced into one follow-up write.</li>
 *   <li><b>Data integrity</b> - the file is written to a temporary file first and then
 *       atomically moved over the real file, so a crash can never leave a half-written
 *       data file. Optional {@code .bak} backups are supported.</li>
 *   <li><b>Validation</b> - invalid entries are skipped with a warning instead of
 *       corrupting the in-memory state.</li>
 * </ul>
 */
public final class BlockStorage {

    private static final String HEADER = String.join("\n",
            "# ============================================================",
            "#  TA_BlockTrigger - block command storage",
            "#  Author: MamaD_TA",
            "# ------------------------------------------------------------",
            "#  Format:",
            "#    blocks:",
            "#      - world: \"world\"                 # world name",
            "#        world-uuid: \"<uuid>\"           # world UUID (primary identifier)",
            "#        x: 10",
            "#        y: 64",
            "#        z: -5",
            "#        execution: CONSOLE              # CONSOLE or PLAYER",
            "#        click: RIGHT                    # RIGHT, LEFT or BOTH",
            "#        command: \"say Hello World\"      # command without a leading slash",
            "#  This file is written by the plugin. Stop the server before editing it by hand.",
            "# ============================================================",
            "");

    private final TABlockTrigger plugin;
    private final ConfigManager config;
    private final Object writeLock = new Object();
    private final AtomicBoolean writeScheduled = new AtomicBoolean(false);

    private File file;
    private ExecutorService executor;
    private volatile List<BlockCommand> pendingSnapshot;

    public BlockStorage(TABlockTrigger plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    // =======================================================================
    //  Lifecycle
    // =======================================================================

    /** Resolves the data file, creates it when needed and starts the writer thread. */
    public void initialize() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Could not create the plugin data folder: " + folder.getAbsolutePath());
        }

        this.file = new File(folder, config.getStorageFileName());
        ensureFileExists();
        startExecutor();

        debug("Storage file: " + file.getAbsolutePath());
    }

    /**
     * Re-resolves the data file (the file name may have changed through a reload) and
     * restarts the writer thread. Pending changes are flushed to the previous file first.
     */
    public void reopen() {
        List<BlockCommand> current = plugin.getBlockCommandManager() == null
                ? List.of()
                : new ArrayList<>(plugin.getBlockCommandManager().all());
        flushSync(current);
        stopExecutor();

        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Could not create the plugin data folder: " + folder.getAbsolutePath());
        }
        this.file = new File(folder, config.getStorageFileName());
        ensureFileExists();
        startExecutor();
        debug("Storage file re-opened: " + file.getAbsolutePath());
    }

    private void ensureFileExists() {
        if (file != null && file.exists()) {
            return;
        }
        boolean copied = false;
        if ("blocks.yml".equalsIgnoreCase(config.getStorageFileName())) {
            // Ship the documented sample file from the jar when available.
            try {
                plugin.saveResource("blocks.yml", false);
                copied = file != null && file.exists();
            } catch (IllegalArgumentException ignored) {
                // Resource missing - fall through and create a fresh file.
            }
        }
        if (!copied) {
            writeFile(HEADER + "blocks: []" + System.lineSeparator(), List.of(), true);
        }
    }

    private void startExecutor() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "TA_BlockTrigger-Storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void stopExecutor() {
        ExecutorService current = executor;
        executor = null;
        if (current != null) {
            current.shutdown();
            try {
                if (!current.awaitTermination(5, TimeUnit.SECONDS)) {
                    current.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                current.shutdownNow();
            }
        }
    }

    /** Stops the writer thread (pending changes are flushed first). */
    public void shutdown() {
        flushSync(plugin.getBlockCommandManager() == null
                ? List.of()
                : plugin.getBlockCommandManager().all());
        stopExecutor();
    }

    /** @return the data file currently in use. */
    public File getFile() {
        return file;
    }

    // =======================================================================
    //  Loading
    // =======================================================================

    /**
     * Reads every block command from the data file.
     *
     * @return the loaded entries (never {@code null}); invalid entries are logged and skipped
     */
    public List<BlockCommand> loadAll() {
        List<BlockCommand> result = new ArrayList<>();
        if (file == null || !file.exists()) {
            debug("No storage file present - starting with an empty configuration.");
            return result;
        }

        YamlConfiguration data;
        try {
            data = YamlConfiguration.loadConfiguration(file);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + file.getName()
                    + " - no block commands were loaded. The file was left untouched.", exception);
            return result;
        }

        List<?> rawList = data.getList("blocks");
        if (rawList == null) {
            // Also accept the section based layout (world-uuid -> "x,y,z" -> settings).
            result.addAll(loadSectionLayout(data));
            debug("Loaded " + result.size() + " block command(s) from " + file.getName() + ".");
            return result;
        }

        int invalid = 0;
        for (Object entry : rawList) {
            if (!(entry instanceof Map<?, ?> map)) {
                invalid++;
                continue;
            }
            BlockCommand command = parseEntry(map);
            if (command == null) {
                invalid++;
            } else {
                result.add(command);
            }
        }

        debug("Loaded " + result.size() + " block command(s) from " + file.getName() + ".");
        if (invalid > 0) {
            plugin.getLogger().warning("[storage] Skipped " + invalid
                    + " invalid block command entr" + (invalid == 1 ? "y" : "ies")
                    + " in " + file.getName() + ". Enable storage.validate-on-load for details.");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private BlockCommand parseEntry(Map<?, ?> map) {
        Map<String, Object> entry = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, value) -> entry.put(String.valueOf(key).toLowerCase(), value));

        String worldName = asString(entry.get("world"), asString(entry.get("world-name"), ""));
        String worldId = asString(entry.get("world-uuid"), asString(entry.get("world-uuid-raw"), ""));
        if (worldId.isBlank() && !worldName.isBlank()) {
            worldId = com.mamadta.tablocktrigger.model.BlockKey.nameId(worldName);
        }

        Integer x = asInt(entry.get("x"));
        Integer y = asInt(entry.get("y"));
        Integer z = asInt(entry.get("z"));
        String command = asString(entry.get("command"), "");

        ExecutionType executionType = ExecutionType.fromString(asString(entry.get("execution"), ""));
        ClickMode clickMode = ClickMode.fromString(asString(entry.get("click"), ""));

        if (worldId.isBlank() || x == null || y == null || z == null || command.isBlank()
                || executionType == null || clickMode == null) {
            logInvalid(entry, "missing/invalid world, coordinates, execution type, click type or command");
            return null;
        }

        if (!config.isValidateOnLoad()) {
            // Still reject entries we cannot interpret - but keep logging minimal.
            return new BlockCommand(worldId, worldName.isBlank() ? "unknown" : worldName,
                    x, y, z, executionType, clickMode, command,
                    asString(entry.get("created-by"), ""), asLong(entry.get("created-at"), 0L));
        }

        if (worldName.isBlank()) {
            // No name stored - try to resolve it from the world UUID (if the world is loaded).
            org.bukkit.World world = resolveWorld(worldId);
            if (world != null) {
                worldName = world.getName();
            }
        }
        return new BlockCommand(worldId, worldName.isBlank() ? "unknown" : worldName,
                x, y, z, executionType, clickMode, command,
                asString(entry.get("created-by"), ""), asLong(entry.get("created-at"), 0L));
    }

    private void logInvalid(Map<String, Object> entry, String reason) {
        if (config.isValidateOnLoad()) {
            plugin.getLogger().warning("[storage] Invalid block command entry in " + file.getName()
                    + " (" + reason + "): " + entry);
        }
    }

    /** Reads the optional section based layout: {@code blocks.<world-uuid>.<x,y,z>.command}. */
    private List<BlockCommand> loadSectionLayout(YamlConfiguration data) {
        List<BlockCommand> result = new ArrayList<>();
        ConfigurationSection blocks = data.getConfigurationSection("blocks");
        if (blocks == null) {
            return result;
        }
        for (String worldId : blocks.getKeys(false)) {
            ConfigurationSection worldSection = blocks.getConfigurationSection(worldId);
            if (worldSection == null) {
                continue;
            }
            for (String coordinateKey : worldSection.getKeys(false)) {
                ConfigurationSection entry = worldSection.getConfigurationSection(coordinateKey);
                if (entry == null) {
                    continue;
                }
                Map<String, Object> map = new LinkedHashMap<>();
                map.putAll(entry.getValues(true));
                String[] parts = coordinateKey.split(",");
                if (parts.length == 3) {
                    map.putIfAbsent("x", parts[0].trim());
                    map.putIfAbsent("y", parts[1].trim());
                    map.putIfAbsent("z", parts[2].trim());
                }
                map.putIfAbsent("world-uuid", worldId);
                map.putIfAbsent("world", entry.getString("world", worldId));
                BlockCommand parsed = parseEntry(map);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
        }
        return result;
    }

    // =======================================================================
    //  Saving
    // =======================================================================

    /**
     * Requests an asynchronous save. Multiple requests made while a write is running are
     * coalesced into a single additional write.
     *
     * @param snapshot the current block commands
     */
    public void requestSave(Collection<BlockCommand> snapshot) {
        if (snapshot == null) {
            return;
        }
        pendingSnapshot = new ArrayList<>(snapshot);
        ExecutorService current = executor;
        if (current == null || current.isShutdown()) {
            writeSnapshot(pendingSnapshot);
            pendingSnapshot = null;
            return;
        }
        if (!writeScheduled.compareAndSet(false, true)) {
            return; // an existing write loop will pick up the pending snapshot
        }
        current.execute(() -> {
            try {
                while (true) {
                    List<BlockCommand> toWrite = pendingSnapshot;
                    if (toWrite == null) {
                        break;
                    }
                    pendingSnapshot = null;
                    writeSnapshot(toWrite);
                }
            } finally {
                writeScheduled.set(false);
                // A request may have arrived between the last check and the flag reset.
                ExecutorService latest = executor;
                if (pendingSnapshot != null && latest != null && !latest.isShutdown()) {
                    List<BlockCommand> leftover = pendingSnapshot;
                    pendingSnapshot = null;
                    if (leftover != null) {
                        requestSave(leftover);
                    }
                }
            }
        });
    }

    /**
     * Writes the given snapshot synchronously (used on disable and after reloads).
     *
     * @param snapshot block commands to persist
     */
    public void flushSync(Collection<BlockCommand> snapshot) {
        if (snapshot == null) {
            return;
        }
        writeSnapshot(new ArrayList<>(snapshot));
        pendingSnapshot = null;
    }

    private void writeSnapshot(List<BlockCommand> snapshot) {
        YamlConfiguration data = new YamlConfiguration();
        List<Map<String, Object>> entries = new ArrayList<>(snapshot.size());
        for (BlockCommand command : snapshot) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world", command.getWorldName());
            entry.put("world-uuid", command.getWorldId());
            entry.put("x", command.getX());
            entry.put("y", command.getY());
            entry.put("z", command.getZ());
            entry.put("execution", command.getExecutionType().name());
            entry.put("click", command.getClickMode().name());
            entry.put("command", command.getCommand());
            if (!command.getCreatedBy().isEmpty()) {
                entry.put("created-by", command.getCreatedBy());
            }
            if (command.getCreatedAt() > 0L) {
                entry.put("created-at", command.getCreatedAt());
            }
            entries.add(entry);
        }
        data.set("blocks", entries);
        writeFile(HEADER + data.saveToString(), snapshot, false);
    }

    /**
     * Writes text to the data file atomically.
     *
     * @param content  full file content
     * @param snapshot entries being written (only for debug output)
     * @param quiet    whether to hide the debug line
     */
    private void writeFile(String content, List<BlockCommand> snapshot, boolean quiet) {
        if (file == null) {
            return;
        }
        synchronized (writeLock) {
            try {
                File folder = file.getParentFile();
                if (folder != null && !folder.exists() && !folder.mkdirs()) {
                    plugin.getLogger().severe("Could not create the plugin data folder.");
                    return;
                }

                if (config.isBackupOnSave() && file.exists() && file.length() > 0) {
                    Path backup = new File(folder, file.getName() + ".bak").toPath();
                    Files.copy(file.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
                }

                Path target = file.toPath();
                Path temp = new File(folder, file.getName() + ".tmp").toPath();
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicFailure) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }

                if (!quiet) {
                    debug("Saved " + snapshot.size() + " block command(s) to " + file.getName() + ".");
                }
            } catch (IOException exception) {
                plugin.getLogger().log(Level.SEVERE,
                        "Could not save " + file.getName() + " - block commands may be lost on restart!", exception);
            }
        }
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

    private void debug(String message) {
        if (config.isDebug() || config.isStorageDebugLogging()) {
            plugin.getLogger().info("[storage] " + message);
        }
    }

    private String asString(Object value, String def) {
        return value == null ? def : String.valueOf(value).trim();
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private long asLong(Object value, long def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    private String normaliseUuid(String worldId) {
        if (worldId.startsWith(com.mamadta.tablocktrigger.model.BlockKey.NAME_PREFIX)) {
            return "";
        }
        return worldId;
    }

    /** Resolves a loaded world by UUID; returns {@code null} for name-based or unknown ids. */
    private org.bukkit.World resolveWorld(String worldId) {
        String raw = normaliseUuid(worldId);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            java.util.UUID uuid = java.util.UUID.fromString(raw);
            return org.bukkit.Bukkit.getWorld(uuid);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
