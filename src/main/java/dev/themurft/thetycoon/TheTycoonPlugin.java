package dev.themurft.thetycoon;

import dev.themurft.thetycoon.command.TycoonCommand;
import dev.themurft.thetycoon.command.TycoonLangCommand;
import dev.themurft.thetycoon.economy.TycoonEconomy;
import dev.themurft.thetycoon.listener.MinerProtectionListener;
import dev.themurft.thetycoon.listener.PlotProtectionListener;
import dev.themurft.thetycoon.listener.ProgressionListener;
import dev.themurft.thetycoon.service.EconomyService;
import dev.themurft.thetycoon.service.LanguageService;
import dev.themurft.thetycoon.service.MinerService;
import dev.themurft.thetycoon.service.MinionService;
import dev.themurft.thetycoon.service.MinionStore;
import dev.themurft.thetycoon.service.PlotService;
import dev.themurft.thetycoon.service.PlotStore;
import dev.themurft.thetycoon.service.ShopService;
import dev.themurft.thetycoon.service.ScoreboardService;
import dev.themurft.thetycoon.service.UpgradeStore;
import dev.themurft.thetycoon.service.ProgressionStore;
import dev.themurft.thetycoon.service.PlotSessionService;
import dev.themurft.thetycoon.service.TycoonStore;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import net.milkbowl.vault.economy.Economy;

public final class TheTycoonPlugin extends JavaPlugin {
    private NamespacedKey minerOwnerKey;
    private TycoonStore store;
    private MinerService miners;
    private LanguageService languages;
    private EconomyService economy;
    private TycoonEconomy economyProvider;
    private PlotStore plotStore;
    private PlotService plotService;
    private MinionStore minionStore;
    private MinionService minionService;
    private UpgradeStore upgradeStore;
    private ScoreboardService scoreboardService;
    private ProgressionStore progressionStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        minerOwnerKey = new NamespacedKey(this, "miner_owner");
        languages = new LanguageService(this);
        languages.load();
        economyProvider = new TycoonEconomy(this);
        getServer().getServicesManager().register(Economy.class, economyProvider, this, ServicePriority.Highest);
        economy = new EconomyService(this);
        if (!economy.connect()) throw new IllegalStateException("TheTycoon could not register its Vault economy provider.");
        store = new TycoonStore(this);
        store.load();
        plotStore = new PlotStore(this);
        plotStore.load();
        plotService = new PlotService(this, plotStore);
        miners = new MinerService(this, store);
        upgradeStore = new UpgradeStore(this);
        upgradeStore.load();
        minionStore = new MinionStore(this);
        minionStore.load();
        progressionStore = new ProgressionStore(this);
        progressionStore.load();
        minionService = new MinionService(this, minionStore, upgradeStore, languages, progressionStore, plotService);
        PlotSessionService plotSessions = new PlotSessionService(plotStore, plotService, store, miners, minionService);
        ShopService shopService = new ShopService(this, minionService, upgradeStore, economy, plotService, languages, progressionStore);
        scoreboardService = new ScoreboardService(this, economy, minionService, upgradeStore, plotService, languages, progressionStore);

        TycoonCommand executor = new TycoonCommand(this, store, miners, languages, economy, plotStore, plotService,
                minionService, shopService, upgradeStore, progressionStore, plotSessions);
        PluginCommand command = getCommand("tycoon");
        if (command == null) throw new IllegalStateException("Command tycoon is missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        TycoonLangCommand languageExecutor = new TycoonLangCommand(languages);
        PluginCommand languageCommand = getCommand("tycoonlang");
        if (languageCommand == null) throw new IllegalStateException("Command tycoonlang is missing from plugin.yml");
        languageCommand.setExecutor(languageExecutor);
        languageCommand.setTabCompleter(languageExecutor);
        getServer().getPluginManager().registerEvents(new MinerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlotProtectionListener(plotService, languages), this);
        getServer().getPluginManager().registerEvents(shopService, this);
        getServer().getPluginManager().registerEvents(new ProgressionListener(minionService, progressionStore, languages, plotSessions), this);

        for (var plot : plotStore.all()) if (plot.ownerId() != null && getServer().getPlayer(plot.ownerId()) == null) plotSessions.release(plot.ownerId());

        minionService.restore();
        minionService.start();
        scoreboardService.start();
        var worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
        String worldEditVersion = worldEdit == null ? "unavailable" : worldEdit.getPluginMeta().getVersion();
        getLogger().info("TheTycoon enabled with " + store.all().size() + " tycoons. WorldEdit " + worldEditVersion + " is available.");
    }

    @Override
    public void onDisable() {
        if (miners != null) miners.stopTasks();
        if (minionService != null) minionService.stop();
        if (scoreboardService != null) scoreboardService.stop();
        if (store != null) store.save();
        if (plotStore != null) plotStore.save();
        if (minionStore != null) minionStore.save();
        if (upgradeStore != null) upgradeStore.save();
        if (progressionStore != null) progressionStore.save();
        if (economyProvider != null) economyProvider.shutdown();
        getServer().getServicesManager().unregisterAll(this);
    }

    public NamespacedKey minerOwnerKey() { return minerOwnerKey; }
    public void restartScoreboard() { if (scoreboardService != null) scoreboardService.start(); }
}
