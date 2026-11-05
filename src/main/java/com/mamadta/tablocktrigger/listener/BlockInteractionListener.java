package com.mamadta.tablocktrigger.listener;

import com.mamadta.tablocktrigger.TABlockTrigger;
import com.mamadta.tablocktrigger.config.ConfigManager;
import com.mamadta.tablocktrigger.model.BlockCommand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Detects player interactions with configured blocks and triggers their commands.
 *
 * <p>Behaviour overview:</p>
 * <ul>
 *   <li>Both left and right clicks are supported; the configured click mode decides which
 *       one actually fires the command.</li>
 *   <li>Blocks without a configured command are never touched - normal gameplay is not
 *       affected at all.</li>
 *   <li>Off-hand duplicates, duplicate events in the same tick and accidental block
 *       breaking are handled according to {@code config.yml}.</li>
 * </ul>
 */
public final class BlockInteractionListener implements Listener {

    private final TABlockTrigger plugin;

    public BlockInteractionListener(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    // =======================================================================
    //  Click handling
    // =======================================================================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        ConfigManager config = plugin.getConfigManager();
        if (!config.isPluginEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        BlockCommand command = plugin.getBlockCommandManager().get(block);
        if (command == null) {
            return; // unconfigured block - never interfere
        }

        boolean leftClick = action == Action.LEFT_CLICK_BLOCK;

        // --- main-hand / off-hand duplicate ---------------------------------
        if (config.isPreventOffhandDuplicates() && event.getHand() == EquipmentSlot.OFF_HAND) {
            debug("Ignored off-hand duplicate on " + command.locationString());
            protect(event, leftClick);
            return;
        }

        // --- events cancelled by other plugins ------------------------------
        // useInteractedBlock() is the modern, non-deprecated equivalent of isCancelled().
        if (event.useInteractedBlock() == Event.Result.DENY && !config.isRunWhenEventCancelled()) {
            protect(event, leftClick);
            return;
        }

        // --- world filter ---------------------------------------------------
        if (!isWorldActive(block)) {
            protect(event, leftClick);
            plugin.getMessageManager().send(player, "execution.world-disabled",
                    "world", block.getWorld().getName());
            return;
        }

        // --- click mode -----------------------------------------------------
        boolean modeMatches = command.getClickMode().matches(leftClick);
        if (!modeMatches || !config.isClickEnabled(command.getClickMode(), leftClick)) {
            protect(event, leftClick);
            return;
        }

        // --- duplicate interaction guard (same tick, same block) ------------
        if (config.isPreventDuplicateInteractions()
                && plugin.getCooldownManager().isDuplicate(player, block, leftClick)) {
            debug("Ignored duplicate interaction from " + player.getName() + " on " + command.locationString());
            protect(event, leftClick);
            return;
        }

        protect(event, leftClick);

        plugin.getCommandDispatcher().execute(player, block, command, leftClick);
    }

    /**
     * Protects configured blocks: cancels block breaking (left click activation) and, when
     * configured, the normal interaction of the block.
     */
    private void protect(PlayerInteractEvent event, boolean leftClick) {
        ConfigManager config = plugin.getConfigManager();
        if (leftClick && config.isPreventBlockBreaking()) {
            // Prevents the client from starting to break the block.
            event.setCancelled(true);
        }
        if (!config.isAllowNormalInteraction()) {
            // Prevents opening chests, pressing buttons, using anvils, ...
            event.setCancelled(true);
        }
    }

    private boolean isWorldActive(Block block) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isRunInFilteredWorlds()) {
            return config.isWorldAllowed(block.getWorld().getName());
        }
        return true;
    }

    // =======================================================================
    //  Block protection
    // =======================================================================

    /** Stops a player from damaging a configured block in survival mode. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isPluginEnabled() || !config.isPreventBlockBreaking()) {
            return;
        }
        if (!plugin.getBlockCommandManager().has(event.getBlock())) {
            return;
        }
        event.setCancelled(true);
        event.setInstaBreak(false);
    }

    /** Stops a player from breaking a configured block (creative / instant break). */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isPluginEnabled() || !config.isPreventBlockBreaking()) {
            return;
        }
        BlockCommand command = plugin.getBlockCommandManager().get(event.getBlock());
        if (command == null) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageManager().send(event.getPlayer(), "general.block-protected");
        debug("Prevented " + event.getPlayer().getName() + " from breaking " + command.locationString());
    }

    private void debug(String message) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[interact] " + message);
        }
    }
}
