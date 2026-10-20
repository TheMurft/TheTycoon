package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class LanguageService {
    private final TheTycoonPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, YamlConfiguration> languages = new LinkedHashMap<>();
    private final Map<UUID, String> playerLanguages = new LinkedHashMap<>();
    private final File preferencesFile;
    private String globalLanguage = "en";

    public LanguageService(TheTycoonPlugin plugin) {
        this.plugin = plugin;
        this.preferencesFile = new File(plugin.getDataFolder(), "languages.yml");
    }

    public void load() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();
        saveBundledLanguage("en.yml");
        saveBundledLanguage("es.yml");
        saveBundledLanguage("fr.yml");
        saveBundledLanguage("pt.yml");

        languages.clear();
        File[] files = langFolder.listFiles((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String code = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                YamlConfiguration language = YamlConfiguration.loadConfiguration(file);
                InputStream bundledStream = plugin.getResource("lang/" + file.getName());
                if (bundledStream != null) {
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(bundledStream, StandardCharsets.UTF_8));
                    language.setDefaults(defaults);
                    language.options().copyDefaults(true);
                    try {
                        language.save(file);
                    } catch (IOException exception) {
                        plugin.getLogger().warning("Could not update language file " + file.getName() + ": " + exception.getMessage());
                    }
                }
                languages.put(code, language);
            }
        }

        YamlConfiguration preferences = YamlConfiguration.loadConfiguration(preferencesFile);
        String configuredGlobal = preferences.getString("global", plugin.getConfig().getString("language.default", "en"));
        globalLanguage = normalizeAvailable(configuredGlobal, "en");
        playerLanguages.clear();
        var section = preferences.getConfigurationSection("players");
        if (section != null) {
            for (String uuidText : section.getKeys(false)) {
                try {
                    String language = normalize(section.getString(uuidText, ""));
                    if (languages.containsKey(language)) playerLanguages.put(UUID.fromString(uuidText), language);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignored invalid language preference for " + uuidText);
                }
            }
        }
        savePreferences();
    }

    private void saveBundledLanguage(String name) {
        File destination = new File(plugin.getDataFolder(), "lang/" + name);
        if (!destination.exists()) plugin.saveResource("lang/" + name, false);
    }

    public Set<String> availableLanguages() {
        return new TreeSet<>(languages.keySet());
    }

    public boolean isAvailable(String language) {
        return languages.containsKey(normalize(language));
    }

    public String personalLanguage(UUID playerId) {
        return playerLanguages.get(playerId);
    }

    public String effectiveLanguage(CommandSender sender) {
        if (sender instanceof Player player) return playerLanguages.getOrDefault(player.getUniqueId(), globalLanguage);
        return globalLanguage;
    }

    public String globalLanguage() {
        return globalLanguage;
    }

    public String effectiveLanguage(UUID playerId) {
        return playerLanguages.getOrDefault(playerId, globalLanguage);
    }

    public String displayName(String language) {
        String normalized = normalizeAvailable(language, "en");
        return languages.get(normalized).getString("language-name", normalized);
    }

    public void setPlayerLanguage(UUID playerId, String language) {
        playerLanguages.put(playerId, normalize(language));
        savePreferences();
    }

    public void resetPlayerLanguage(UUID playerId) {
        playerLanguages.remove(playerId);
        savePreferences();
    }

    public void setGlobalLanguage(String language) {
        globalLanguage = normalize(language);
        savePreferences();
    }

    public Component component(CommandSender sender, String key, String... replacements) {
        return component(effectiveLanguage(sender), key, replacements);
    }

    public Component component(String language, String key, String... replacements) {
        String value = raw(language, "prefix") + raw(language, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        return miniMessage.deserialize(value);
    }

    public Component plainComponent(CommandSender sender, String key, String... replacements) {
        String language = effectiveLanguage(sender);
        String value = raw(language, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        return miniMessage.deserialize(value);
    }

    public Component plainComponent(UUID playerId, String key, String... replacements) {
        String value = raw(effectiveLanguage(playerId), key);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        return miniMessage.deserialize(value);
    }

    public String plainText(UUID playerId, String key, String... replacements) {
        return PlainTextComponentSerializer.plainText().serialize(plainComponent(playerId, key, replacements));
    }

    public String plainText(CommandSender sender, String key, String... replacements) {
        return PlainTextComponentSerializer.plainText().serialize(plainComponent(sender, key, replacements));
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(component(sender, key, replacements));
    }

    private String raw(String language, String key) {
        YamlConfiguration selected = languages.get(normalizeAvailable(language, "en"));
        String value = selected == null ? null : selected.getString(key);
        if (value == null && languages.get("en") != null) value = languages.get("en").getString(key);
        return value == null ? "<red>Missing language key: " + key : value;
    }

    private String normalizeAvailable(String language, String fallback) {
        String normalized = normalize(language);
        if (languages.containsKey(normalized)) return normalized;
        return languages.containsKey(fallback) ? fallback : languages.keySet().stream().findFirst().orElse("en");
    }

    private String normalize(String language) {
        return language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
    }

    private void savePreferences() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("global", globalLanguage);
        for (var entry : playerLanguages.entrySet()) yaml.set("players." + entry.getKey(), entry.getValue());
        try {
            yaml.save(preferencesFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save languages.yml: " + exception.getMessage());
        }
    }
}
