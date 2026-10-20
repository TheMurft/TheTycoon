package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.TycoonData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TycoonStore {
    private final TheTycoonPlugin plugin;
    private final File file;
    private final Map<UUID, TycoonData> tycoons = new HashMap<>();

    public TycoonStore(TheTycoonPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        tycoons.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("tycoons");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) continue;
                World world = Bukkit.getWorld(section.getString("world", ""));
                if (world == null) continue;
                Location location = new Location(world, section.getDouble("x"), section.getDouble("y"),
                        section.getDouble("z"), (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
                String minerText = section.getString("miner");
                UUID miner = minerText == null || minerText.isBlank() ? null : UUID.fromString(minerText);
                tycoons.put(owner, new TycoonData(owner, location, miner, section.getInt("stored")));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignored invalid tycoon entry: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (TycoonData data : tycoons.values()) {
            String path = "tycoons." + data.ownerId();
            Location location = data.plotLocation();
            yaml.set(path + ".world", location.getWorld().getName());
            yaml.set(path + ".x", location.getX());
            yaml.set(path + ".y", location.getY());
            yaml.set(path + ".z", location.getZ());
            yaml.set(path + ".yaw", location.getYaw());
            yaml.set(path + ".pitch", location.getPitch());
            yaml.set(path + ".miner", data.minerId() == null ? null : data.minerId().toString());
            yaml.set(path + ".stored", data.stored());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save data.yml: " + exception.getMessage());
        }
    }

    public TycoonData get(UUID owner) { return tycoons.get(owner); }
    public void put(TycoonData data) { tycoons.put(data.ownerId(), data); save(); }
    public TycoonData remove(UUID owner) { TycoonData removed = tycoons.remove(owner); save(); return removed; }
    public Collection<TycoonData> all() { return tycoons.values(); }
}
