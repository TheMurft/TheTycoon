package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class EconomyService {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg"};
    private final TheTycoonPlugin plugin;
    private Economy economy;

    public EconomyService(TheTycoonPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = registration == null ? null : registration.getProvider();
        return economy != null;
    }

    public boolean available() { return economy != null; }
    public double balance(OfflinePlayer player) { return available() ? economy.getBalance(player) : 0.0; }
    public String format(double amount) {
        if (!plugin.getConfig().getBoolean("economy.compact-format", true))
            return available() ? economy.format(amount) : String.format(Locale.US, "%.2f", amount);
        String currency = available() ? (Math.abs(amount) == 1 ? economy.currencyNameSingular() : economy.currencyNamePlural()) : "Coins";
        return compactNumber(amount) + (currency == null || currency.isBlank() ? "" : " " + currency);
    }

    public String compactNumber(double amount) {
        if (!Double.isFinite(amount)) return "0";
        double absolute = Math.abs(amount);
        if (absolute < 1000) return decimal(amount, absolute < 10);
        int group = Math.min(SUFFIXES.length - 1, (int) (Math.log10(absolute) / 3));
        if (group == SUFFIXES.length - 1 && absolute >= Math.pow(1000, SUFFIXES.length))
            return new DecimalFormat("0.##E0", DecimalFormatSymbols.getInstance(Locale.US)).format(amount);
        return decimal(amount / Math.pow(1000, group), true) + SUFFIXES[group];
    }

    private String decimal(double value, boolean allowDecimals) {
        int places = Math.max(0, Math.min(3, plugin.getConfig().getInt("economy.compact-decimal-places", 2)));
        String pattern = allowDecimals && places > 0 ? "0." + "#".repeat(places) : "#,##0";
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!available()) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!available()) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean setBalance(OfflinePlayer player, double amount) {
        if (!available() || !Double.isFinite(amount) || amount < 0) return false;
        double current = balance(player);
        return amount >= current ? deposit(player, amount - current) : withdraw(player, current - amount);
    }
}
