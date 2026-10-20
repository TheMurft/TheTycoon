package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProgressionStore {
    public static final class Progress {
        private double lifetimeEarnings; private long itemsSold; private int prestige; private long lastSeen; private double pendingOfflineMoney;
        private final Set<Integer> claimedMissions;
        private Progress(double earnings,long sold,int prestige,long lastSeen,double pending,Set<Integer> claimed){this.lifetimeEarnings=earnings;this.itemsSold=sold;this.prestige=prestige;this.lastSeen=lastSeen;this.pendingOfflineMoney=pending;this.claimedMissions=claimed;}
        public double lifetimeEarnings(){return lifetimeEarnings;} public long itemsSold(){return itemsSold;} public int prestige(){return prestige;} public long lastSeen(){return lastSeen;} public double pendingOfflineMoney(){return pendingOfflineMoney;} public Set<Integer> claimedMissions(){return claimedMissions;}
    }
    public record Ranking(UUID playerId,double earnings,int prestige){}
    private final TheTycoonPlugin plugin;private final File file;private final Map<UUID,Progress> data=new HashMap<>();
    public ProgressionStore(TheTycoonPlugin plugin){this.plugin=plugin;file=new File(plugin.getDataFolder(),"progression.yml");}
    public void load(){data.clear();YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);var root=yaml.getConfigurationSection("players");if(root==null)return;for(String key:root.getKeys(false))try{UUID id=UUID.fromString(key);String p=key+".";data.put(id,new Progress(root.getDouble(p+"lifetime-earnings"),root.getLong(p+"items-sold"),root.getInt(p+"prestige"),root.getLong(p+"last-seen"),root.getDouble(p+"pending-offline-money"),new HashSet<>(root.getIntegerList(p+"claimed-missions"))));}catch(IllegalArgumentException e){plugin.getLogger().warning("Ignored invalid progression owner: "+key);}}
    public Progress get(UUID id){return data.computeIfAbsent(id,key->new Progress(0,0,0,System.currentTimeMillis(),0,new HashSet<>()));}
    public void addSale(UUID id,long items,double money){Progress p=get(id);p.itemsSold+=Math.max(0,items);p.lifetimeEarnings+=Math.max(0,money);save();}
    public void prestige(UUID id){Progress p=get(id);p.prestige++;save();}
    public boolean claim(UUID id,int mission){return get(id).claimedMissions.add(mission);}
    public void updateLastSeen(UUID id){get(id).lastSeen=System.currentTimeMillis();save();}
    public void addOfflineMoney(UUID id,double amount){if(!Double.isFinite(amount)||amount<=0)return;get(id).pendingOfflineMoney+=amount;save();}
    public double claimOfflineMoney(UUID id){Progress progress=get(id);double amount=progress.pendingOfflineMoney;progress.pendingOfflineMoney=0;progress.lifetimeEarnings+=amount;save();return amount;}
    public double prestigeMultiplier(UUID id){return 1+get(id).prestige*plugin.getConfig().getDouble("progression.prestige.profit-bonus-per-level",.10);}
    public double prestigeCost(UUID id){double base=plugin.getConfig().getDouble("progression.prestige.base-cost",100000);double growth=plugin.getConfig().getDouble("progression.prestige.cost-growth",2);return base*Math.pow(growth,get(id).prestige);}
    public List<Ranking> top(int limit){List<Ranking> list=new ArrayList<>();data.forEach((id,p)->list.add(new Ranking(id,p.lifetimeEarnings,p.prestige)));list.sort(Comparator.comparingDouble(Ranking::earnings).reversed());return list.subList(0,Math.min(limit,list.size()));}
    public void save(){YamlConfiguration yaml=new YamlConfiguration();data.forEach((id,p)->{String root="players."+id;yaml.set(root+".lifetime-earnings",p.lifetimeEarnings);yaml.set(root+".items-sold",p.itemsSold);yaml.set(root+".prestige",p.prestige);yaml.set(root+".last-seen",p.lastSeen);yaml.set(root+".pending-offline-money",p.pendingOfflineMoney);yaml.set(root+".claimed-missions",new ArrayList<>(p.claimedMissions));});try{yaml.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save progression.yml: "+e.getMessage());}}
}
