package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.PlotData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PlotStore {
    private final TheTycoonPlugin plugin;
    private final File file;
    private final List<PlotData> plots = new ArrayList<>();

    public PlotStore(TheTycoonPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
    }

    public void load() {
        plots.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("plots");
        if (root == null) return;
        for (String idText : root.getKeys(false)) {
            try {
                int id = Integer.parseInt(idText);
                String path = "plots." + idText;
                String ownerText = yaml.getString(path + ".owner");
                UUID owner = ownerText == null || ownerText.isBlank() ? null : UUID.fromString(ownerText);
                plots.add(new PlotData(id, yaml.getString(path + ".world", "tycoon"),
                        yaml.getInt(path + ".min-x"), yaml.getInt(path + ".max-x"),
                        yaml.getInt(path + ".min-z"), yaml.getInt(path + ".max-z"),
                        yaml.getInt(path + ".ground-y", 64), owner));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignored invalid plot entry: " + idText);
            }
        }
        plots.sort(Comparator.comparingInt(PlotData::id));
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlotData plot : plots) {
            String path = "plots." + plot.id();
            yaml.set(path + ".world", plot.worldName());
            yaml.set(path + ".min-x", plot.minX());
            yaml.set(path + ".max-x", plot.maxX());
            yaml.set(path + ".min-z", plot.minZ());
            yaml.set(path + ".max-z", plot.maxZ());
            yaml.set(path + ".ground-y", plot.groundY());
            yaml.set(path + ".owner", plot.ownerId() == null ? null : plot.ownerId().toString());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save plots.yml: " + exception.getMessage());
        }
    }

    public List<PlotData> all() { return plots; }
    public void add(PlotData plot) { plots.add(plot); plots.sort(Comparator.comparingInt(PlotData::id)); }
    public PlotData byOwner(UUID owner) { return plots.stream().filter(plot -> owner.equals(plot.ownerId())).findFirst().orElse(null); }
    public PlotData available() { return plots.stream().filter(plot -> plot.ownerId() == null).findFirst().orElse(null); }
    public int nextId() { return plots.stream().mapToInt(PlotData::id).max().orElse(0) + 1; }
}
