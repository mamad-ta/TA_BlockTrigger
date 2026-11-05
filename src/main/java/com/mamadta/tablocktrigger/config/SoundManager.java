package com.mamadta.tablocktrigger.config;

import com.mamadta.tablocktrigger.TABlockTrigger;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads and plays every configurable sound.
 *
 * <p>Sound names are accepted in three formats and are validated safely, so a typo can
 * never crash the plugin:</p>
 * <ul>
 *   <li>Bukkit enum style: {@code ENTITY_EXPERIENCE_ORB_PICKUP}</li>
 *   <li>Minecraft key style: {@code entity.experience_orb.pickup}</li>
 *   <li>Namespaced key style: {@code minecraft:entity.experience_orb.pickup}</li>
 * </ul>
 *
 * <p>Playback uses the Adventure sound API (key based). This keeps the plugin working on
 * 1.20.1 and on newer Paper versions where {@code org.bukkit.Sound} is no longer an enum.</p>
 */
public final class SoundManager {

    /** Every sound the plugin can play. */
    public enum SoundKey {
        SET_COMMAND("set-command"),
        REMOVE_COMMAND("remove-command"),
        INVALID_COMMAND("invalid-command"),
        NO_PERMISSION("no-permission"),
        CLICK("click"),
        RELOAD("reload"),
        COOLDOWN("cooldown");

        private final String configKey;

        SoundKey(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    /** One validated sound entry. */
    private static final class SoundSetting {
        private boolean enabled;
        private String key = "";
        private float volume = 1.0F;
        private float pitch = 1.0F;
    }

    private final TABlockTrigger plugin;
    private final Map<SoundKey, SoundSetting> settings = new EnumMap<>(SoundKey.class);
    private boolean globallyEnabled = true;

    public SoundManager(TABlockTrigger plugin) {
        this.plugin = plugin;
        for (SoundKey key : SoundKey.values()) {
            SoundSetting setting = new SoundSetting();
            setting.enabled = false;
            settings.put(key, setting);
        }
    }

    // =======================================================================
    //  Loading
    // =======================================================================

    /** (Re)loads the {@code sounds} section of {@code config.yml}. */
    public void load(FileConfiguration config) {
        settings.replaceAll((key, value) -> new SoundSetting());
        globallyEnabled = config.getBoolean("sounds.enabled", true);

        for (SoundKey key : SoundKey.values()) {
            String base = "sounds." + key.getConfigKey();
            SoundSetting setting = new SoundSetting();
            setting.enabled = config.getBoolean(base + ".enabled", false);

            String rawName = config.getString(base + ".sound", "");
            setting.key = resolveKey(rawName);

            if (setting.key == null) {
                if (setting.enabled) {
                    plugin.getLogger().warning("[sounds] Unknown sound '" + rawName + "' for '"
                            + key.getConfigKey() + "' - this sound is disabled. "
                            + "Use a valid sound such as 'ENTITY_EXPERIENCE_ORB_PICKUP' "
                            + "or 'block.note_block.bell'.");
                }
                setting.enabled = false;
                setting.key = "";
            }

            setting.volume = (float) clamp(readDouble(config, base + ".volume", 1.0D), 0.0D, 10.0D);
            setting.pitch = (float) clamp(readDouble(config, base + ".pitch", 1.0D), 0.1D, 2.0D);

            settings.put(key, setting);
        }
    }

    private double readDouble(FileConfiguration config, String path, double def) {
        Object raw = config.get(path);
        if (raw == null) {
            return def;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("[sounds] Invalid number at '" + path + "' (" + raw + ") - using " + def + ".");
            return def;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Converts any supported sound notation into a namespaced Minecraft sound key.
     *
     * <p>Accepted notations:</p>
     * <ul>
     *   <li>Bukkit constant: {@code ENTITY_EXPERIENCE_ORB_PICKUP}</li>
     *   <li>Minecraft key: {@code entity.experience_orb.pickup}</li>
     *   <li>Namespaced key: {@code minecraft:entity.experience_orb.pickup}</li>
     * </ul>
     *
     * @param rawName configured value
     * @return the sound key, or {@code null} when the value is empty/invalid
     */
    private String resolveKey(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }
        String name = rawName.trim();

        // Explicit namespace: "minecraft:block.note_block.bell"
        int colon = name.indexOf(':');
        if (colon >= 0) {
            String namespace = name.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String path = name.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
            if (namespace.isEmpty() || path.isEmpty() || path.contains(" ")) {
                plugin.getLogger().warning("[sounds] Invalid sound key '" + name + "'.");
                return null;
            }
            return namespace + ":" + path;
        }

        // Dotted path: "entity.experience_orb.pickup" (underscores are part of the path!)
        if (name.contains(".")) {
            String path = name.toLowerCase(Locale.ROOT);
            if (path.contains(" ")) {
                plugin.getLogger().warning("[sounds] Invalid sound key '" + name + "'.");
                return null;
            }
            return "minecraft:" + path;
        }

        // Bukkit enum/constant style - resolve the real key through the server's Sound class.
        Resolution resolution = resolveConstant(name);
        if (resolution.invalid()) {
            return null;
        }
        return resolution.key();
    }

    /** Result of resolving a Bukkit sound constant. */
    private record Resolution(String key, boolean invalid) {
    }

    /**
     * Resolves a Bukkit sound constant to its Minecraft key via reflection.
     *
     * <p>Reflection is used on purpose: {@code org.bukkit.Sound} is an enum on 1.20.1 but an
     * interface with static constants on newer Paper versions. Looking up the constant field
     * and asking it for its key works on both, and correctly keeps underscores that are part
     * of the sound path (e.g. {@code entity.experience_orb.pickup}).</p>
     */
    private Resolution resolveConstant(String constant) {
        String upper = constant.toUpperCase(Locale.ROOT);
        try {
            Class<?> soundClass = Class.forName("org.bukkit.Sound");
            java.lang.reflect.Field field;
            try {
                field = soundClass.getField(upper);
            } catch (NoSuchFieldException notFound) {
                return new Resolution(null, true); // definitively an unknown sound name
            }
            Object sound = field.get(null);
            if (sound == null) {
                return new Resolution(null, true);
            }
            try {
                Object key = soundClass.getMethod("getKey").invoke(sound);
                if (key != null) {
                    String text = String.valueOf(key).toLowerCase(Locale.ROOT);
                    if (text.contains(":")) {
                        return new Resolution(text, false);
                    }
                }
            } catch (NoSuchMethodException ignored) {
                // Fall through to the heuristic below.
            }
            return new Resolution("minecraft:" + upper.toLowerCase(Locale.ROOT).replace('_', '.'), false);
        } catch (ClassNotFoundException exception) {
            plugin.getLogger().warning("[sounds] The Sound class is unavailable - using the configured name as-is.");
            return new Resolution("minecraft:" + upper.toLowerCase(Locale.ROOT).replace('_', '.'), false);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[sounds] Could not validate the sound '" + constant + "' ("
                    + throwable.getClass().getSimpleName() + ") - using it as-is.");
            return new Resolution("minecraft:" + upper.toLowerCase(Locale.ROOT).replace('_', '.'), false);
        }
    }

    // =======================================================================
    //  Playback
    // =======================================================================

    /**
     * Plays a configured sound for a player. Silently does nothing when the sound is
     * disabled, unknown or when sounds are globally disabled.
     *
     * @param player receiver
     * @param key    which configured sound to play
     */
    public void play(Player player, SoundKey key) {
        if (player == null || key == null || !globallyEnabled) {
            return;
        }
        SoundSetting setting = settings.get(key);
        if (setting == null || !setting.enabled || setting.key.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.sound(
                    net.kyori.adventure.key.Key.key(setting.key),
                    Sound.Source.MASTER,
                    setting.volume,
                    setting.pitch);
            player.playSound(sound);
        } catch (Throwable throwable) {
            // Never let a sound issue break a command or an interaction.
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("[sounds] Could not play '" + setting.key + "': " + throwable.getMessage());
            }
        }
    }

    /**
     * @param key sound entry
     * @return whether the sound is currently enabled and valid
     */
    public boolean isEnabled(SoundKey key) {
        SoundSetting setting = settings.get(key);
        return globallyEnabled && setting != null && setting.enabled && !setting.key.isEmpty();
    }

    /** Debug helper listing the resolved sound keys. */
    public Map<String, String> describe() {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        out.put("enabled", String.valueOf(globallyEnabled));
        for (Map.Entry<SoundKey, SoundSetting> entry : settings.entrySet()) {
            out.put(entry.getKey().getConfigKey(), entry.getValue().enabled
                    ? entry.getValue().key + " (v=" + entry.getValue().volume + ", p=" + entry.getValue().pitch + ")"
                    : "disabled");
        }
        return out;
    }

    /** Reads a raw sound section - used for diagnostics only. */
    public ConfigurationSection section() {
        return plugin.getConfig().getConfigurationSection("sounds");
    }
}
