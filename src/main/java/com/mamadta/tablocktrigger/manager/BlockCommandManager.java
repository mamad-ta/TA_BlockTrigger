package com.mamadta.tablocktrigger.manager;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.model.BlockCommand;
import com.mamadta.tablocktrigger.model.BlockKey;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory index of every configured block command plus all persistence triggers.
 *
 * <p>Lookups are O(1): the map is keyed by {@link BlockKey} (world UUID + coordinates).
 * Every mutating operation immediately requests a save, so a server crash right after
 * {@code /bt ...} can never lose the new configuration.</p>
 */
public final class BlockCommandManager {

    private final TABlockTrigger plugin;
    private final Map<BlockKey, BlockCommand> commands = new ConcurrentHashMap<>();

    public BlockCommandManager(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    // =======================================================================
    //  Persistence
    // =======================================================================

    /**
     * Loads all entries from the storage file into memory.
     *
     * @return amount of loaded entries
     */
    public int loadFromStorage() {
        List<BlockCommand> loaded = plugin.getBlockStorage().loadAll();
        commands.clear();
        for (BlockCommand command : loaded) {
            commands.put(command.getKey(), command);
        }
        repairWorlds();
        debug("Loaded " + commands.size() + " block command(s) into memory.");
        return commands.size();
    }

    /**
     * Reloads the storage file. When block data reloading is disabled in the config the
     * in-memory entries are kept and written back to the (possibly new) data file.
     *
     * @return amount of active block commands after the reload
     */
    public int reloadStorage() {
        plugin.getBlockStorage().reopen();
        if (plugin.getConfigManager().isStorageReloadOnPluginReload()) {
            int loaded = loadFromStorage();
            // Persist the freshly read state through the (possibly renamed) file.
            saveAll();
            return loaded;
        }
        saveAll();
        return commands.size();
    }

    /** Persists every entry immediately (asynchronously). */
    public void saveAll() {
        plugin.getBlockStorage().requestSave(commands.values());
    }

    // =======================================================================
    //  Lookup
    // =======================================================================

    /**
     * @param block clicked block
     * @return the command bound to that block or {@code null}
     */
    public BlockCommand get(Block block) {
        if (block == null) {
            return null;
        }
        return commands.get(BlockKey.of(block));
    }

    /** @param key block key @return the bound command or {@code null} */
    public BlockCommand get(BlockKey key) {
        return key == null ? null : commands.get(key);
    }

    /** @param block block to check @return whether a command is bound to the block */
    public boolean has(Block block) {
        return get(block) != null;
    }

    /** @return amount of configured blocks */
    public int size() {
        return commands.size();
    }

    /** @return an immutable snapshot of every configured block command */
    public Collection<BlockCommand> all() {
        return Collections.unmodifiableList(new ArrayList<>(commands.values()));
    }

    /** @return every configured command in the given world (by name, case-insensitive) */
    public List<BlockCommand> byWorld(String worldName) {
        List<BlockCommand> result = new ArrayList<>();
        if (worldName == null) {
            return result;
        }
        for (BlockCommand command : commands.values()) {
            if (command.getWorldName().equalsIgnoreCase(worldName)) {
                result.add(command);
            }
        }
        return result;
    }

    // =======================================================================
    //  Mutation
    // =======================================================================

    /**
     * Stores (or replaces) the command bound to a block and saves immediately.
     *
     * @param command new block command
     * @return the previously stored command or {@code null} when the block was free
     */
    public BlockCommand set(BlockCommand command) {
        BlockCommand previous = commands.put(command.getKey(), command);
        plugin.getBlockStorage().requestSave(commands.values());
        debug("Set command on " + command.locationString() + " -> " + command);
        return previous;
    }

    /**
     * Removes the command bound to a block and saves immediately.
     *
     * @param block target block
     * @return the removed command or {@code null} when the block had no command
     */
    public BlockCommand remove(Block block) {
        if (block == null) {
            return null;
        }
        BlockCommand removed = commands.remove(BlockKey.of(block));
        if (removed != null) {
            plugin.getBlockStorage().requestSave(commands.values());
            debug("Removed command from " + removed.locationString());
        }
        return removed;
    }

    // =======================================================================
    //  World repair
    // =======================================================================

    /**
     * Repairs entries that were stored with a name-based world identifier (for example
     * because the world was not loaded when the file was read).
     */
    public void repairWorlds() {
        boolean changed = false;
        for (World world : plugin.getServer().getWorlds()) {
            changed |= repairWorld(world, false);
        }
        if (changed) {
            plugin.getBlockStorage().requestSave(commands.values());
        }
    }

    /**
     * Rebinds entries of one world from a name-based identifier to the real world UUID.
     *
     * @param world the world that (re)loaded
     * @return {@code true} when at least one entry changed
     */
    public boolean repairWorld(World world) {
        return repairWorld(world, true);
    }

    private boolean repairWorld(World world, boolean save) {
        if (world == null) {
            return false;
        }
        String uuid = world.getUID().toString();
        String worldName = world.getName().toLowerCase(Locale.ROOT);

        List<BlockKey> staleKeys = new ArrayList<>();
        for (Map.Entry<BlockKey, BlockCommand> entry : commands.entrySet()) {
            BlockKey key = entry.getKey();
            if (key.worldId().equals(uuid)) {
                continue; // already correct
            }
            boolean nameBased = key.isNameBased() && worldName.equals(key.worldNameFromKey());
            boolean staleUuid = !key.isNameBased()
                    && entry.getValue().getWorldName().equalsIgnoreCase(world.getName())
                    && !isWorldLoaded(key.worldId());
            if (nameBased || staleUuid) {
                staleKeys.add(key);
            }
        }

        boolean changed = false;
        for (BlockKey stale : staleKeys) {
            BlockCommand command = commands.remove(stale);
            if (command == null) {
                continue;
            }
            BlockCommand repaired = command.withWorldId(uuid, world.getName());
            // Never overwrite an entry that already exists with the correct key.
            if (commands.putIfAbsent(repaired.getKey(), repaired) == null) {
                changed = true;
                debug("Rebound " + command.locationString() + " to world UUID " + uuid);
            }
        }
        if (changed && save) {
            plugin.getBlockStorage().requestSave(commands.values());
        }
        return changed;
    }

    /** @return whether a world with that UUID is currently loaded on the server. */
    private boolean isWorldLoaded(String worldId) {
        if (worldId == null || worldId.startsWith(BlockKey.NAME_PREFIX)) {
            return false;
        }
        try {
            return plugin.getServer().getWorld(java.util.UUID.fromString(worldId)) != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void debug(String message) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[blocks] " + message);
        }
    }
}
