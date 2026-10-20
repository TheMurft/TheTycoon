package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.PlotData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScoreboardService {
    private final TheTycoonPlugin plugin;private final EconomyService economy;private final MinionService minions;private final UpgradeStore upgrades;private final PlotService plots;private final LanguageService languages;private final ProgressionStore progression;
    private final Map<UUID,Scoreboard> boards=new HashMap<>();private BukkitTask task;
    public ScoreboardService(TheTycoonPlugin plugin,EconomyService economy,MinionService minions,UpgradeStore upgrades,PlotService plots,LanguageService languages,ProgressionStore progression){this.plugin=plugin;this.economy=economy;this.minions=minions;this.upgrades=upgrades;this.plots=plots;this.languages=languages;this.progression=progression;}
    public void start(){stop();if(!plugin.getConfig().getBoolean("scoreboard.enabled",true))return;long interval=Math.max(20,plugin.getConfig().getLong("scoreboard.update-interval-ticks",40));task=Bukkit.getScheduler().runTaskTimer(plugin,()->Bukkit.getOnlinePlayers().forEach(this::update),1,interval);}
    private void update(Player player){
        Scoreboard board=boards.computeIfAbsent(player.getUniqueId(),id->Bukkit.getScoreboardManager().getNewScoreboard());Objective objective=board.getObjective("thetycoon");
        String title=render(player,plugin.getConfig().getString("scoreboard.title","&6&lTHE TYCOON"));if(objective==null){objective=board.registerNewObjective("thetycoon",Criteria.DUMMY,title);objective.setDisplaySlot(DisplaySlot.SIDEBAR);}else objective.setDisplayName(title);
        for(String entry:board.getEntries())board.resetScores(entry);
        List<String> configured=plugin.getConfig().getStringList("scoreboard.lines");if(configured.isEmpty())configured=List.of("&e{player}","","&f{money_label}: &6{money}","&f{minions_label}: &6{minions}/{max_minions}","&e{footer}");
        int amount=Math.min(15,configured.size());for(int index=0;index<amount;index++){String line=render(player,configured.get(index));String unique=line+uniquifier(index);objective.getScore(unique).setScore(amount-index);}
        if(player.getScoreboard()!=board)player.setScoreboard(board);
    }
    private String render(Player player,String template){UUID id=player.getUniqueId();PlotData plot=plots.owned(id);ProgressionStore.Progress progress=progression.get(id);long minutes=player.getStatistic(Statistic.PLAY_ONE_MINUTE)/1200L;String time=languages.plainText(player,"scoreboard-hours","<hours>",String.valueOf(minutes/60),"<minutes>",String.valueOf(minutes%60));int profit=upgrades.profitPercent(id)+(int)Math.round((progression.prestigeMultiplier(id)-1)*100);Map<String,String> values=new HashMap<>();values.put("player",player.getName());values.put("plot",plot==null?languages.plainText(player,"scoreboard-value-none"):String.valueOf(plot.id()));values.put("money",compact(economy.balance(player)));values.put("offline_money",compact(progress.pendingOfflineMoney()));values.put("prestige",String.valueOf(progress.prestige()));values.put("minions",String.valueOf(minions.owned(id).size()));values.put("max_minions",String.valueOf(upgrades.limit(id)));values.put("stored",compact(minions.totalStored(id)));values.put("profit",String.valueOf(profit));values.put("speed",String.valueOf(upgrades.speedPercent(id)));values.put("playtime",time);values.put("lifetime_earnings",compact(progress.lifetimeEarnings()));values.put("items_sold",compact(progress.itemsSold()));values.put("online",String.valueOf(Bukkit.getOnlinePlayers().size()));values.put("max_online",String.valueOf(Bukkit.getMaxPlayers()));values.put("footer",plugin.getConfig().getString("scoreboard.footer","play.yourserver.net"));for(String label:List.of("plot","money","prestige","minions","stored","profit","speed","playtime","lifetime","sold","online"))values.put(label+"_label",languages.plainText(player,"scoreboard-label-"+label));String result=template==null?"":template;for(Map.Entry<String,String> value:values.entrySet())result=result.replace("{"+value.getKey()+"}",value.getValue());return color(result);}
    private String uniquifier(int index){ChatColor[] colors=ChatColor.values();return colors[index%colors.length].toString()+ChatColor.RESET;}
    private String compact(double value){return economy.compactNumber(value);}
    private String color(String value){return ChatColor.translateAlternateColorCodes('&',value==null?"":value);}
    public void stop(){if(task!=null)task.cancel();task=null;if(Bukkit.getScoreboardManager()!=null){Scoreboard main=Bukkit.getScoreboardManager().getMainScoreboard();for(Player player:Bukkit.getOnlinePlayers())if(boards.get(player.getUniqueId())==player.getScoreboard())player.setScoreboard(main);}boards.clear();}
}
