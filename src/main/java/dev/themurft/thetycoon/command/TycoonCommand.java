package dev.themurft.thetycoon.command;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.PlotData;
import dev.themurft.thetycoon.model.TycoonData;
import dev.themurft.thetycoon.service.EconomyService;
import dev.themurft.thetycoon.service.LanguageService;
import dev.themurft.thetycoon.service.MinerService;
import dev.themurft.thetycoon.service.MinionService;
import dev.themurft.thetycoon.service.PlotService;
import dev.themurft.thetycoon.service.PlotStore;
import dev.themurft.thetycoon.service.ShopService;
import dev.themurft.thetycoon.service.UpgradeStore;
import dev.themurft.thetycoon.service.TycoonStore;
import dev.themurft.thetycoon.service.ProgressionStore;
import dev.themurft.thetycoon.service.PlotSessionService;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TycoonCommand implements CommandExecutor, TabCompleter {
    private final TheTycoonPlugin plugin;
    private final TycoonStore tycoons;
    private final MinerService miners;
    private final LanguageService languages;
    private final EconomyService economy;
    private final PlotStore plots;
    private final PlotService plotService;
    private final MinionService minions;
    private final ShopService shops;
    private final UpgradeStore upgrades;
    private final ProgressionStore progression;
    private final PlotSessionService sessions;

    public TycoonCommand(TheTycoonPlugin plugin, TycoonStore tycoons, MinerService miners,
                         LanguageService languages, EconomyService economy, PlotStore plots, PlotService plotService,
                         MinionService minions, ShopService shops, UpgradeStore upgrades, ProgressionStore progression, PlotSessionService sessions) {
        this.plugin = plugin;
        this.tycoons = tycoons;
        this.miners = miners;
        this.languages = languages;
        this.economy = economy;
        this.plots = plots;
        this.plotService = plotService;
        this.minions = minions;
        this.shops = shops;
        this.upgrades = upgrades;
        this.progression = progression;
        this.sessions = sessions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("money")) {
            money(sender, args);
            return true;
        }
        if (!(sender instanceof Player player)) {
            languages.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("thetycoon.use")) {
            languages.send(player, "no-permission");
            return true;
        }
        String action = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "menu" -> shops.openDashboard(player);
            case "tp" -> visitPlot(player);
            case "plots" -> visitPlot(player);
            case "claim" -> claim(player);
            case "shop" -> shops.openMinions(player);
            case "upgrades" -> shops.openUpgrades(player);
            case "sell" -> sell(player);
            case "info" -> info(player);
            case "missions", "quests" -> missions(player,args);
            case "prestige" -> prestige(player,args);
            case "top", "leaderboard" -> top(player);
            case "offline" -> claimOffline(player);
            case "unclaim" -> unclaim(player);
            case "generate" -> generate(player, args);
            case "reload" -> reload(player);
            default -> languages.send(player, "usage-tycoon");
        }
        return true;
    }

    private void visitPlot(Player player) {
        PlotData plot = plotService.owned(player.getUniqueId());
        boolean owned = plot != null;
        if (plot == null) plot = plotService.available();
        if (plot == null) {
            languages.send(player, "no-available-plots");
            return;
        }
        player.teleportAsync(plot.center(plotService.world(plot)));
        languages.send(player, owned ? "plot-home" : "plot-teleported", "<plot>", String.valueOf(plot.id()));
    }

    private void claim(Player player) {
        if (plotService.owned(player.getUniqueId()) != null) {
            languages.send(player, "already-created");
            return;
        }
        PlotData plot = plotService.at(player.getLocation());
        if (plot == null) {
            languages.send(player, "not-inside-plot");
            return;
        }
        if (plot.ownerId() != null) {
            languages.send(player, "plot-already-claimed");
            return;
        }

        TycoonData legacy = tycoons.get(player.getUniqueId());
        plot.ownerId(player.getUniqueId());
        plots.save();
        var newCenter = plot.center(plotService.world(plot));
        if (legacy == null) {
            TycoonData data = new TycoonData(player.getUniqueId(), newCenter, null, 0);
            tycoons.put(data);
            String starter = plugin.getConfig().getString("starter-minion", "COBBLESTONE");
            if (minions.catalog().get(starter) != null) minions.place(player.getUniqueId(), starter, 1, 0, newCenter);
        } else {
            var oldCenter = legacy.plotLocation().clone();
            miners.removeMiner(legacy);
            legacy.plotLocation(newCenter);
            tycoons.put(legacy);
            if (minions.owned(player.getUniqueId()).isEmpty()) {
                String starter = plugin.getConfig().getString("starter-minion", "COBBLESTONE");
                if (minions.catalog().get(starter) != null) minions.place(player.getUniqueId(), starter, 1, 0, newCenter);
            } else minions.relocateAll(player.getUniqueId(), oldCenter, newCenter);
        }
        languages.send(player, "created", "<plot>", String.valueOf(plot.id()));
        languages.send(player, legacy == null ? "miner-created" : "tycoon-restored", "<amount>", String.valueOf(minions.owned(player.getUniqueId()).size()));
    }

    private void sell(Player player) {
        TycoonData data = requireTycoon(player);
        if (data == null) return;
        int amount = minions.totalStored(player.getUniqueId());
        if (amount <= 0) {
            languages.send(player, "storage-empty");
            return;
        }
        double total = minions.totalValue(player.getUniqueId());
        if (!economy.deposit(player, total)) {
            languages.send(player, "economy-unavailable");
            return;
        }
        minions.clearStorage(player.getUniqueId());
        progression.addSale(player.getUniqueId(), amount, total);
        languages.send(player, "sold", "<amount>", String.valueOf(amount), "<material>", languages.plainText(player,"resources-name"),
                "<money>", economy.format(total));
    }

    private void info(Player player) {
        TycoonData data = requireTycoon(player);
        if (data == null) return;
        PlotData plot = plotService.owned(player.getUniqueId());
        player.sendMessage(languages.plainComponent(player, "info-title"));
        if (plot != null) player.sendMessage(languages.plainComponent(player, "info-plot", "<plot>", String.valueOf(plot.id())));
        player.sendMessage(languages.plainComponent(player, "info-storage", "<amount>", String.valueOf(minions.totalStored(player.getUniqueId())), "<material>", languages.plainText(player,"resources-name")));
        player.sendMessage(languages.plainComponent(player, "info-market-value", "<money>", economy.format(minions.totalValue(player.getUniqueId()))));
        player.sendMessage(languages.plainComponent(player, "info-minions", "<amount>", String.valueOf(minions.owned(player.getUniqueId()).size()), "<limit>", String.valueOf(upgrades.limit(player.getUniqueId()))));
        player.sendMessage(languages.plainComponent(player, "info-balance", "<balance>", economy.format(economy.balance(player))));
        player.sendMessage(languages.plainComponent(player, "info-prestige", "<level>", String.valueOf(progression.get(player.getUniqueId()).prestige())));
        player.sendMessage(languages.plainComponent(player, "info-lifetime", "<money>", economy.format(progression.get(player.getUniqueId()).lifetimeEarnings())));
        player.sendMessage(languages.plainComponent(player, "info-hint"));
    }

    private void prestige(Player player,String[] args){
        if(requireTycoon(player)==null)return;double cost=progression.prestigeCost(player.getUniqueId());if(args.length<2||!args[1].equalsIgnoreCase("confirm")){languages.send(player,"prestige-confirm","<cost>",economy.format(cost),"<level>",String.valueOf(progression.get(player.getUniqueId()).prestige()+1));return;}if(economy.balance(player)<cost){languages.send(player,"not-enough-money","<money>",economy.format(cost));return;}
        var collected=minions.collectAllForPrestige(player.getUniqueId());int dropped=0;for(var item:collected){var leftovers=player.getInventory().addItem(item);for(var leftover:leftovers.values()){dropped+=leftover.getAmount();player.getWorld().dropItemNaturally(player.getLocation(),leftover);}}
        economy.setBalance(player,0);upgrades.reset(player.getUniqueId());progression.prestige(player.getUniqueId());languages.send(player,"prestige-success","<level>",String.valueOf(progression.get(player.getUniqueId()).prestige()),"<percent>",String.valueOf((int)Math.round((progression.prestigeMultiplier(player.getUniqueId())-1)*100)));languages.send(player,"prestige-minions-returned","<amount>",String.valueOf(collected.size()));if(dropped>0)languages.send(player,"prestige-minions-dropped","<amount>",String.valueOf(dropped));
    }

    private void missions(Player player,String[] args){ProgressionStore.Progress progress=progression.get(player.getUniqueId());if(args.length>=3&&args[1].equalsIgnoreCase("claim")){int id;try{id=Integer.parseInt(args[2]);}catch(NumberFormatException e){languages.send(player,"mission-usage");return;}long required=plugin.getConfig().getLong("progression.missions."+id+".required-items",-1);double reward=plugin.getConfig().getDouble("progression.missions."+id+".reward",0);if(required<0){languages.send(player,"mission-unknown");return;}if(progress.claimedMissions().contains(id)){languages.send(player,"mission-claimed");return;}if(progress.itemsSold()<required){languages.send(player,"mission-incomplete","<current>",String.valueOf(progress.itemsSold()),"<required>",String.valueOf(required));return;}progression.claim(player.getUniqueId(),id);progression.save();economy.deposit(player,reward);languages.send(player,"mission-reward","<money>",economy.format(reward));return;}player.sendMessage(languages.plainComponent(player,"missions-title"));var section=plugin.getConfig().getConfigurationSection("progression.missions");if(section==null)return;for(String key:section.getKeys(false)){long required=section.getLong(key+".required-items");double reward=section.getDouble(key+".reward");String state=progress.claimedMissions().contains(Integer.parseInt(key))?languages.plainText(player,"mission-state-claimed"):progress.itemsSold()>=required?languages.plainText(player,"mission-state-ready"):progress.itemsSold()+"/"+required;player.sendMessage(languages.plainComponent(player,"mission-line","<id>",key,"<state>",state,"<money>",economy.format(reward)));}player.sendMessage(languages.plainComponent(player,"mission-claim-hint"));}

    private void top(Player player){player.sendMessage(languages.plainComponent(player,"top-title"));int rank=1;for(ProgressionStore.Ranking row:progression.top(10)){OfflinePlayer target=Bukkit.getOfflinePlayer(row.playerId());String name=target.getName()==null?row.playerId().toString().substring(0,8):target.getName();player.sendMessage(languages.plainComponent(player,"top-line","<rank>",String.valueOf(rank++),"<player>",name,"<money>",economy.format(row.earnings()),"<prestige>",String.valueOf(row.prestige())));}}

    private void claimOffline(Player player){double amount=progression.get(player.getUniqueId()).pendingOfflineMoney();if(amount<=0){languages.send(player,"offline-money-empty");return;}if(!economy.deposit(player,amount)){languages.send(player,"economy-unavailable");return;}progression.claimOfflineMoney(player.getUniqueId());languages.send(player,"offline-money-claimed","<money>",economy.format(amount));}

    private void unclaim(Player player) {
        PlotData plot = plotService.owned(player.getUniqueId());
        if (plot == null || tycoons.get(player.getUniqueId()) == null) {
            languages.send(player, "no-plot");
            return;
        }
        sessions.release(player.getUniqueId());
        languages.send(player, "plot-released-progress");
    }

    private void generate(Player player, String[] args) {
        if (!player.hasPermission("thetycoon.admin")) {
            languages.send(player, "no-permission");
            return;
        }
        int amount = 1;
        if (args.length > 1) {
            try { amount = Math.max(1, Math.min(100, Integer.parseInt(args[1]))); }
            catch (NumberFormatException ignored) { amount = 1; }
        }
        int generated = plotService.generate(player.getLocation(), amount);
        languages.send(player, "plots-generated", "<amount>", String.valueOf(generated));
    }

    private void reload(Player player) {
        if (!player.hasPermission("thetycoon.admin")) {
            languages.send(player, "no-permission");
            return;
        }
        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
        languages.load();
        minions.reload();
        minions.start();
        plugin.restartScoreboard();
        languages.send(player, "reloaded");
    }

    private void money(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thetycoon.money.admin")) {
            languages.send(sender, "no-permission");
            return;
        }
        if (args.length != 4) {
            languages.send(sender, "money-usage");
            return;
        }
        double amount;
        try { amount = Double.parseDouble(args[3]); }
        catch (NumberFormatException exception) { languages.send(sender, "money-invalid"); return; }
        if (!Double.isFinite(amount) || amount < 0) { languages.send(sender, "money-invalid"); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        double updated;
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> updated = economy.balance(target) + amount;
            case "set" -> updated = amount;
            case "take", "remove" -> updated = Math.max(0, economy.balance(target) - amount);
            default -> { languages.send(sender, "money-usage"); return; }
        }
        if (!economy.setBalance(target, updated)) { languages.send(sender, "money-invalid"); return; }
        String name = target.getName() == null ? args[2] : target.getName();
        languages.send(sender, "money-updated", "<player>", name, "<money>", economy.format(updated));
        if (target.isOnline() && target.getPlayer() != null && !target.getPlayer().equals(sender)) {
            languages.send(target.getPlayer(), "money-received", "<money>", economy.format(updated));
        }
    }

    private TycoonData requireTycoon(Player player) {
        TycoonData data = tycoons.get(player.getUniqueId());
        if (data == null) {
            languages.send(player, "no-plot");
            return null;
        }
        return data;
    }

    private Material configuredMaterial() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("miner.material", "COBBLESTONE"));
        return material == null || !material.isItem() ? Material.COBBLESTONE : material;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("money") && sender.hasPermission("thetycoon.money.admin")) {
            return List.of("add", "set", "take");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("money") && sender.hasPermission("thetycoon.money.admin")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("generate") && sender.hasPermission("thetycoon.admin")) {
            return List.of("1", "9", "25");
        }
        if (args.length != 1) return List.of();
        List<String> options = new ArrayList<>(List.of("menu", "tp", "plots", "claim", "shop", "upgrades", "sell", "offline", "missions", "prestige", "top", "info", "unclaim"));
        if (sender.hasPermission("thetycoon.admin")) options.addAll(List.of("generate", "reload"));
        if (sender.hasPermission("thetycoon.money.admin")) options.add("money");
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
