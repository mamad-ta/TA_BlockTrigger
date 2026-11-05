package com.mamadta.tablocktrigger.model;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * Immutable representation of one configured block command.
 *
 * <p>A block command is uniquely identified by {@link #getKey()} (world + coordinates).</p>
 */
public final class BlockCommand {

    private final String worldId;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final ExecutionType executionType;
    private final ClickMode clickMode;
    private final String command;
    private final String createdBy;
    private final long createdAt;

    public BlockCommand(String worldId,
                        String worldName,
                        int x, int y, int z,
                        ExecutionType executionType,
                        ClickMode clickMode,
                        String command,
                        String createdBy,
                        long createdAt) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
        this.executionType = Objects.requireNonNull(executionType, "executionType");
        this.clickMode = Objects.requireNonNull(clickMode, "clickMode");
        this.command = Objects.requireNonNull(command, "command");
        this.createdBy = createdBy == null ? "" : createdBy;
        this.createdAt = createdAt;
    }

    /** Creates a block command from the block an administrator is targeting. */
    public static BlockCommand fromBlock(Block block,
                                         ExecutionType executionType,
                                         ClickMode clickMode,
                                         String command,
                                         String createdBy) {
        World world = block.getWorld();
        return new BlockCommand(
                world.getUID().toString(),
                world.getName(),
                block.getX(), block.getY(), block.getZ(),
                executionType, clickMode, command, createdBy, System.currentTimeMillis());
    }

    public String getWorldId() {
        return worldId;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    public ClickMode getClickMode() {
        return clickMode;
    }

    /** The command <b>without</b> a leading slash. */
    public String getCommand() {
        return command;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /** Unique key of this block command. */
    public BlockKey getKey() {
        return new BlockKey(worldId, x, y, z);
    }

    /** @return {@code x, y, z} */
    public String coordinates() {
        return x + ", " + y + ", " + z;
    }

    /** @return {@code world [x, y, z]} */
    public String locationString() {
        return worldName + " [" + coordinates() + "]";
    }

    /** Copy of this block command bound to another world identifier (UUID repair). */
    public BlockCommand withWorldId(String newWorldId, String newWorldName) {
        return new BlockCommand(newWorldId, newWorldName, x, y, z,
                executionType, clickMode, command, createdBy, createdAt);
    }

    @Override
    public String toString() {
        return "BlockCommand{" + locationString()
                + ", execution=" + executionType.id()
                + ", click=" + clickMode.id()
                + ", command='" + command + "'}";
    }
}
