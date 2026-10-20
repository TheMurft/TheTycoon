package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.MinionDefinition;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MinionCatalog {
    private final TheTycoonPlugin plugin;
    private final Map<String, MinionDefinition> definitions = new LinkedHashMap<>();

    public MinionCatalog(TheTycoonPlugin plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        definitions.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("minions");
        if (root == null) return;
        for (String rawId : root.getKeys(false)) {
            String id = rawId.toUpperCase(Locale.ROOT);
            String path = "minions." + rawId + ".";
            Material output = material(path + "output", rawId);
            Material block = material(path + "display-material", rawId);
            Material tool = material(path + "tool", "WOODEN_PICKAXE");
            if (output == null || block == null || tool == null) {
                plugin.getLogger().warning("Ignored minion " + id + ": invalid material (output=" + plugin.getConfig().getString(path+"output") + ":"+(output!=null)+", display=" + plugin.getConfig().getString(path+"display-material") + ":"+(block!=null)+", tool=" + plugin.getConfig().getString(path+"tool") + ":"+(tool!=null)+")");
                continue;
            }
            int red = clamp(plugin.getConfig().getInt(path + "armor-color.red", 115));
            int green = clamp(plugin.getConfig().getInt(path + "armor-color.green", 115));
            int blue = clamp(plugin.getConfig().getInt(path + "armor-color.blue", 115));
            definitions.put(id, new MinionDefinition(id, output, block, tool,
                    Math.max(0, plugin.getConfig().getDouble(path + "buy-price", 100)),
                    Math.max(0, plugin.getConfig().getDouble(path + "sell-price", 1)),
                    Math.max(.1, plugin.getConfig().getDouble(path + "base-interval-seconds", 10)),
                    plugin.getConfig().getInt(path + "order", 0), Color.fromRGB(red, green, blue)));
        }
    }

    public MinionDefinition get(String id) { return id == null ? null : definitions.get(id.toUpperCase(Locale.ROOT)); }
    public List<MinionDefinition> sortedBestFirst() {
        List<MinionDefinition> result = new ArrayList<>(definitions.values());
        result.sort(Comparator.comparingDouble(MinionDefinition::buyPrice)
                .thenComparingDouble(MinionDefinition::sellPrice)
                .thenComparing(MinionDefinition::id));
        return result;
    }
    private Material material(String path, String fallback) { return Material.matchMaterial(plugin.getConfig().getString(path, fallback)); }
    private int clamp(int value) { return Math.max(0, Math.min(255, value)); }
}
