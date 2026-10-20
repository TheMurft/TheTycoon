package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UpgradeStore {
    public record Upgrades(int extraSlots, int storageLevel, int profitLevel, int speedLevel) {}
    private final TheTycoonPlugin plugin;
    private final File file;
    private final Map<UUID, Upgrades> data = new HashMap<>();

    public UpgradeStore(TheTycoonPlugin plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "upgrades.yml"); }
    public void load() {
        data.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var root = yaml.getConfigurationSection("players");
        if (root == null) return;
        for (String key : root.getKeys(false)) try {
            UUID id = UUID.fromString(key);
            data.put(id, new Upgrades(root.getInt(key + ".extra-slots"), Math.max(1, root.getInt(key + ".storage-level", 1)),
                    Math.max(1, root.getInt(key + ".profit-level", 1)), Math.max(1, root.getInt(key + ".speed-level", 1))));
        } catch (IllegalArgumentException ignored) { plugin.getLogger().warning("Ignored invalid upgrade owner: " + key); }
    }
    public Upgrades get(UUID owner) { return data.getOrDefault(owner, new Upgrades(0, 1, 1, 1)); }
    public int limit(UUID owner) { return Math.min(maxMinionLimit(), plugin.getConfig().getInt("shop.minion-limit", 3) + get(owner).extraSlots()); }
    public int maxMinionLimit(){return Math.max(plugin.getConfig().getInt("shop.minion-limit",3),plugin.getConfig().getInt("shop.max-minion-limit",256));}
    public int nextMinionLimit(UUID owner){int current=limit(owner);if(current>=maxMinionLimit())return maxMinionLimit();int next=current<4?4:Integer.highestOneBit(current-1)<<1;return Math.min(maxMinionLimit(),Math.max(current+1,next));}
    public void addSlot(UUID owner) { Upgrades old=get(owner);int base=plugin.getConfig().getInt("shop.minion-limit",3);int extra=Math.max(0,nextMinionLimit(owner)-base);data.put(owner,new Upgrades(extra,old.storageLevel(),old.profitLevel(),old.speedLevel()));save(); }
    public void addStorage(UUID owner) { Upgrades old=get(owner); data.put(owner,new Upgrades(old.extraSlots(),old.storageLevel()+1,old.profitLevel(),old.speedLevel()));save(); }
    public void addProfit(UUID owner) { Upgrades old=get(owner); data.put(owner,new Upgrades(old.extraSlots(),old.storageLevel(),old.profitLevel()+1,old.speedLevel()));save(); }
    public void addSpeed(UUID owner) { Upgrades old=get(owner); data.put(owner,new Upgrades(old.extraSlots(),old.storageLevel(),old.profitLevel(),old.speedLevel()+1));save(); }
    public void reset(UUID owner) { data.put(owner,new Upgrades(0,1,1,1));save(); }
    public int profitPercent(UUID owner){return (int)Math.round((get(owner).profitLevel()-1)*plugin.getConfig().getDouble("shop.profit-per-level",0.10)*100);}
    public int speedPercent(UUID owner){return (int)Math.round((get(owner).speedLevel()-1)*plugin.getConfig().getDouble("shop.speed-per-level",0.05)*100);}
    public boolean slotMaxed(UUID owner){return limit(owner)>=maxMinionLimit();}
    public boolean storageMaxed(UUID owner){return get(owner).storageLevel()>=plugin.getConfig().getInt("shop.max-storage-level",10);}
    public boolean profitMaxed(UUID owner){return get(owner).profitLevel()>=plugin.getConfig().getInt("shop.max-profit-level",10);}
    public boolean speedMaxed(UUID owner){return get(owner).speedLevel()>=plugin.getConfig().getInt("shop.max-speed-level",10);}
    public double slotPrice(UUID owner){int target=nextMinionLimit(owner),tier=0,value=4;while(value<target){value*=2;tier++;}return price("shop.minion-limit-upgrade-price","shop.minion-limit-price-growth",1000,2.25,tier);}
    public double storagePrice(UUID owner){return price("shop.storage-upgrade-price","shop.storage-price-growth",750,1.55,get(owner).storageLevel()-1);}
    public double profitPrice(UUID owner){return price("shop.profit-upgrade-price","shop.profit-price-growth",1500,1.7,get(owner).profitLevel()-1);}
    public double speedPrice(UUID owner){return price("shop.speed-upgrade-price","shop.speed-price-growth",2000,1.85,get(owner).speedLevel()-1);}
    private double price(String basePath,String growthPath,double fallback,double growthFallback,int exponent){return plugin.getConfig().getDouble(basePath,fallback)*Math.pow(Math.max(1,plugin.getConfig().getDouble(growthPath,growthFallback)),Math.max(0,exponent));}
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        data.forEach((id,value)->{String p="players."+id;yaml.set(p+".extra-slots",value.extraSlots());yaml.set(p+".storage-level",value.storageLevel());yaml.set(p+".profit-level",value.profitLevel());yaml.set(p+".speed-level",value.speedLevel());});
        try { yaml.save(file); } catch (IOException exception) { plugin.getLogger().severe("Could not save upgrades.yml: " + exception.getMessage()); }
    }
}
