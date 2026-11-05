package com.mamadta.tablocktrigger.model;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Locale;
import java.util.Objects;

/**
 * Unique identifier of a configured block: world identifier + coordinates.
 *
 * <p>The world is identified by its UUID, because world names can be reused or
 * renamed on a server. When an entry has no known UUID (for example because the world
 * is not loaded yet), a stable {@code name:<lower-case world name>} identifier is used
 * instead and repaired to the real UUID as soon as the world becomes available.</p>
 */
public record BlockKey(String worldId, int x, int y, int z) {

    /** Prefix used for name-based (unresolved) world identifiers. */
    public static final String NAME_PREFIX = "name:";

    public BlockKey {
        Objects.requireNonNull(worldId, "worldId");
    }

    /** Builds the key of a world by its UUID. */
    public static BlockKey of(World world, int x, int y, int z) {
        return new BlockKey(world.getUID().toString(), x, y, z);
    }

    /** Builds the key of a placed block. */
    public static BlockKey of(Block block) {
        return of(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    /** Builds a name-based identifier used as a fallback for unloaded worlds. */
    public static String nameId(String worldName) {
        return NAME_PREFIX + worldName.toLowerCase(Locale.ROOT);
    }

    /** @return whether this key is based on a world name instead of a world UUID. */
    public boolean isNameBased() {
        return worldId.startsWith(NAME_PREFIX);
    }

    /** @return the world name stored in a name-based key, otherwise {@code null}. */
    public String worldNameFromKey() {
        return isNameBased() ? worldId.substring(NAME_PREFIX.length()) : null;
    }

    /** Copy of this key pointing at another world identifier. */
    public BlockKey withWorldId(String newWorldId) {
        return new BlockKey(newWorldId, x, y, z);
    }

    /** Human readable coordinates, e.g. {@code 12, 64, -5}. */
    public String coordinates() {
        return x + ", " + y + ", " + z;
    }
}
