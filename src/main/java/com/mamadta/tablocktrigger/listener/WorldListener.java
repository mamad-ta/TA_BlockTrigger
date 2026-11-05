package com.mamadta.tablocktrigger.listener;

import com.mamadta.tablocktrigger.TABlockTrigger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Rebinds stored block commands to real world UUIDs when a world becomes available.
 *
 * <p>This keeps stored data valid even when a world was temporarily missing at startup
 * (for example a Multiverse world that loads later).</p>
 */
public final class WorldListener implements Listener {

    private final TABlockTrigger plugin;

    public WorldListener(TABlockTrigger plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (!plugin.getConfigManager().isPluginEnabled()) {
            return;
        }
        if (plugin.getBlockCommandManager().repairWorld(event.getWorld())) {
            plugin.getLogger().info("[storage] Block commands re-bound to the newly loaded world '"
                    + event.getWorld().getName() + "'.");
        }
    }
}
