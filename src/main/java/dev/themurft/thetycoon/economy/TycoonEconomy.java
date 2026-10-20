package dev.themurft.thetycoon.economy;

import dev.themurft.thetycoon.TheTycoonPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TycoonEconomy implements Economy {
    private final TheTycoonPlugin plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();
    private boolean enabled = true;

    public TycoonEconomy(TheTycoonPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "economy.yml");
        load();
    }

    private synchronized void load() {
        balances.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("balances");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), Math.max(0.0, section.getDouble(key)));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignored invalid economy account: " + key);
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : balances.entrySet()) yaml.set("balances." + entry.getKey(), entry.getValue());
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save economy.yml: " + exception.getMessage());
        }
    }

    public void shutdown() {
        enabled = false;
        save();
    }

    private double startingBalance() { return Math.max(0.0, plugin.getConfig().getDouble("economy.starting-balance", 0.0)); }
    private synchronized double balance(UUID playerId) { return balances.getOrDefault(playerId, startingBalance()); }
    private OfflinePlayer player(String name) { return Bukkit.getOfflinePlayer(name); }
    private EconomyResponse success(double amount, double balance) { return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, ""); }
    private EconomyResponse failure(double amount, double balance, String message) { return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, message); }
    private EconomyResponse unsupported() { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank accounts are not supported."); }

    @Override public boolean isEnabled() { return enabled && plugin.isEnabled(); }
    @Override public String getName() { return "TheTycoon"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return Math.max(0, Math.min(6, plugin.getConfig().getInt("economy.decimal-places", 2))); }
    @Override public String currencyNamePlural() { return plugin.getConfig().getString("economy.currency-plural", "Coins"); }
    @Override public String currencyNameSingular() { return plugin.getConfig().getString("economy.currency-singular", "Coin"); }

    @Override
    public String format(double amount) {
        int digits = fractionalDigits();
        String pattern = digits == 0 ? "#,##0" : "#,##0." + "0".repeat(digits);
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount) + " " + (Math.abs(amount) == 1.0 ? currencyNameSingular() : currencyNamePlural());
    }

    @Override public synchronized boolean hasAccount(OfflinePlayer player) { return balances.containsKey(player.getUniqueId()); }
    @Override public boolean hasAccount(String playerName) { return hasAccount(player(playerName)); }
    @Override public boolean hasAccount(String playerName, String worldName) { return hasAccount(playerName); }
    @Override public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }
    @Override public double getBalance(OfflinePlayer player) { return balance(player.getUniqueId()); }
    @Override public double getBalance(String playerName) { return getBalance(player(playerName)); }
    @Override public double getBalance(String playerName, String worldName) { return getBalance(playerName); }
    @Override public double getBalance(OfflinePlayer player, String worldName) { return getBalance(player); }
    @Override public boolean has(OfflinePlayer player, double amount) { return amount >= 0 && getBalance(player) >= amount; }
    @Override public boolean has(String playerName, double amount) { return has(player(playerName), amount); }
    @Override public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
    @Override public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    @Override
    public synchronized EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double current = getBalance(player);
        if (!Double.isFinite(amount) || amount < 0) return failure(amount, current, "Amount must be a finite, non-negative number.");
        if (current < amount) return failure(amount, current, "Insufficient funds.");
        double updated = current - amount;
        balances.put(player.getUniqueId(), updated);
        save();
        return success(amount, updated);
    }

    @Override
    public synchronized EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double current = getBalance(player);
        if (!Double.isFinite(amount) || amount < 0) return failure(amount, current, "Amount must be a finite, non-negative number.");
        double updated = current + amount;
        if (!Double.isFinite(updated)) return failure(amount, current, "Balance limit exceeded.");
        balances.put(player.getUniqueId(), updated);
        save();
        return success(amount, updated);
    }

    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return withdrawPlayer(player(playerName), amount); }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }
    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return depositPlayer(player(playerName), amount); }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }

    @Override public synchronized boolean createPlayerAccount(OfflinePlayer player) {
        if (balances.containsKey(player.getUniqueId())) return false;
        balances.put(player.getUniqueId(), startingBalance());
        save();
        return true;
    }
    @Override public boolean createPlayerAccount(String playerName) { return createPlayerAccount(player(playerName)); }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return createPlayerAccount(playerName); }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return createPlayerAccount(player); }

    @Override public EconomyResponse createBank(String name, String player) { return unsupported(); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return unsupported(); }
    @Override public EconomyResponse deleteBank(String name) { return unsupported(); }
    @Override public EconomyResponse bankBalance(String name) { return unsupported(); }
    @Override public EconomyResponse bankHas(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return unsupported(); }
    @Override public EconomyResponse isBankOwner(String name, String player) { return unsupported(); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return unsupported(); }
    @Override public EconomyResponse isBankMember(String name, String player) { return unsupported(); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return unsupported(); }
    @Override public List<String> getBanks() { return Collections.emptyList(); }
}
