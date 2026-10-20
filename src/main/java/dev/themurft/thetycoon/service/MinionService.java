package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.MinionData;
import dev.themurft.thetycoon.model.MinionDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

public final class MinionService {
    private final TheTycoonPlugin plugin;
    private final MinionStore store;
    private final UpgradeStore upgrades;
    private final LanguageService languages;
    private final ProgressionStore progression;
    private final PlotService plots;
    private final MinionCatalog catalog;
    private final NamespacedKey entityKey, itemTypeKey, itemLevelKey, itemStoredKey;
    private BukkitTask productionTask, animationTask, visibilityTask, saveTask;
    private boolean swing;
    private int animationCursor;

    public MinionService(TheTycoonPlugin plugin, MinionStore store, UpgradeStore upgrades, LanguageService languages, ProgressionStore progression, PlotService plots) {
        this.plugin=plugin; this.store=store; this.upgrades=upgrades; this.languages=languages; this.progression=progression; this.plots=plots;
        catalog=new MinionCatalog(plugin);
        entityKey=new NamespacedKey(plugin,"minion_id"); itemTypeKey=new NamespacedKey(plugin,"minion_type");
        itemLevelKey=new NamespacedKey(plugin,"minion_level"); itemStoredKey=new NamespacedKey(plugin,"minion_stored");
    }
    public NamespacedKey entityKey(){return entityKey;}
    public List<MinionData> owned(UUID owner){return store.owned(owner);}
    public MinionData get(UUID id){return store.get(id);}
    public MinionCatalog catalog(){return catalog;}
    public void reload(){catalog.reload();}

    public MinionData place(UUID owner,String type,int level,int stored,Location location){
        MinionData data=new MinionData(UUID.randomUUID(),owner,type.toUpperCase(Locale.ROOT),location.clone(),null,
                Math.min(maxLevel(),Math.max(1,level)),stored,0);
        data.nextProductionAt(System.currentTimeMillis()+intervalMillis(data));store.add(data);spawn(data);return data;
    }
    public void spawn(MinionData data){
        MinionDefinition definition=catalog.get(data.type());if(definition==null){plugin.getLogger().warning("Cannot spawn unknown minion type "+data.type());return;}
        removeEntity(data);Location location=data.location();if(location.getWorld()==null)return;OfflinePlayer owner=Bukkit.getOfflinePlayer(data.ownerId());
        ArmorStand stand=location.getWorld().spawn(location,ArmorStand.class,a->{
            a.setArms(true);a.setGravity(false);a.setInvulnerable(false);a.setPersistent(false);a.setCanPickupItems(false);a.setCustomNameVisible(true);
            a.customName(languages.plainComponent(data.ownerId(),"minion-entity-name","<type>",typeName(data.ownerId(),data.type()),"<level>",String.valueOf(data.level())));
            a.getPersistentDataContainer().set(entityKey,PersistentDataType.STRING,data.id().toString());
            ItemStack head=new ItemStack(Material.PLAYER_HEAD);SkullMeta skull=(SkullMeta)head.getItemMeta();skull.setOwningPlayer(owner);head.setItemMeta(skull);a.getEquipment().setHelmet(head);
            equipLeather(a,definition.armorColor());a.getEquipment().setItemInMainHand(new ItemStack(definition.tool()));
            a.setRightArmPose(new EulerAngle(Math.toRadians(-65),0,Math.toRadians(10)));
        });data.entityId(stand.getUniqueId());store.markDirty();
    }
    private void equipLeather(ArmorStand stand,org.bukkit.Color color){stand.getEquipment().setChestplate(leather(Material.LEATHER_CHESTPLATE,color));stand.getEquipment().setLeggings(leather(Material.LEATHER_LEGGINGS,color));stand.getEquipment().setBoots(leather(Material.LEATHER_BOOTS,color));}
    private ItemStack leather(Material material,org.bukkit.Color color){ItemStack item=new ItemStack(material);LeatherArmorMeta meta=(LeatherArmorMeta)item.getItemMeta();meta.setColor(color);item.setItemMeta(meta);return item;}
    public ArmorStand entity(MinionData data){Entity entity=data.entityId()==null?null:Bukkit.getEntity(data.entityId());return entity instanceof ArmorStand stand?stand:null;}
    private void removeEntity(MinionData data){ArmorStand stand=entity(data);if(stand!=null)stand.remove();if(data.entityId()!=null){data.entityId(null);store.markDirty();}}
    private void refreshVisual(MinionData data){boolean rendered=entity(data)!=null;removeEntity(data);if(rendered)spawn(data);else store.markDirty();}

    public ItemStack item(UUID owner,String type,int level,int stored){
        MinionDefinition definition=catalog.get(type);if(definition==null)return new ItemStack(Material.BARRIER);
        ItemStack item=new ItemStack(definition.block());ItemMeta meta=item.getItemMeta();
        meta.displayName(languages.plainComponent(owner,"minion-item-name","<type>",typeName(owner,type),"<level>",String.valueOf(level)));
        meta.lore(List.of(languages.plainComponent(owner,"minion-item-place"),languages.plainComponent(owner,"minion-item-stored","<stored>",String.valueOf(stored))));
        meta.getPersistentDataContainer().set(itemTypeKey,PersistentDataType.STRING,type);meta.getPersistentDataContainer().set(itemLevelKey,PersistentDataType.INTEGER,level);meta.getPersistentDataContainer().set(itemStoredKey,PersistentDataType.INTEGER,stored);item.setItemMeta(meta);return item;
    }
    public String itemType(ItemStack item){if(item==null||!item.hasItemMeta())return null;String value=item.getItemMeta().getPersistentDataContainer().get(itemTypeKey,PersistentDataType.STRING);return catalog.get(value)==null?null:value;}
    public int itemLevel(ItemStack item){return item.getItemMeta().getPersistentDataContainer().getOrDefault(itemLevelKey,PersistentDataType.INTEGER,1);}
    public int itemStored(ItemStack item){return item.getItemMeta().getPersistentDataContainer().getOrDefault(itemStoredKey,PersistentDataType.INTEGER,0);}
    public ItemStack collect(MinionData data){removeEntity(data);store.remove(data);return item(data.ownerId(),data.type(),data.level(),data.stored());}
    public List<ItemStack> collectAll(UUID owner){List<ItemStack> items=new java.util.ArrayList<>();for(MinionData data:List.copyOf(owned(owner)))items.add(collect(data));return items;}
    public List<ItemStack> collectAllForPrestige(UUID owner){List<ItemStack> items=new java.util.ArrayList<>();for(MinionData data:List.copyOf(owned(owner))){data.level(1);data.stored(0);items.add(collect(data));}return items;}
    public void upgrade(MinionData data){if(atMaximumSpeed(data))return;data.level(Math.min(maxLevel(),data.level()+1));data.nextProductionAt(System.currentTimeMillis()+intervalMillis(data));refreshVisual(data);store.markDirty();}
    public void upgradeLevels(MinionData data,int levels){if(levels<=0||atMaximumSpeed(data))return;data.level(Math.min(maxLevel(),data.level()+levels));data.nextProductionAt(System.currentTimeMillis()+intervalMillis(data));refreshVisual(data);store.markDirty();}
    public int maxLevel(){return Math.max(1,plugin.getConfig().getInt("minion-upgrades.max-level",15));}
    public double upgradePrice(MinionData data){return plugin.getConfig().getDouble("minion-upgrades.base-price",200)*data.level()*data.level();}
    public double upgradePrice(MinionData data,int currentLevel){return plugin.getConfig().getDouble("minion-upgrades.base-price",200)*currentLevel*currentLevel;}
    public int storageLimit(UUID owner){return plugin.getConfig().getInt("miner.storage-limit",2304)*upgrades.get(owner).storageLevel();}
    public double intervalSeconds(MinionData data){return intervalSeconds(data,data.level());}
    public double intervalSeconds(MinionData data,int level){MinionDefinition d=catalog.get(data.type());double base=d==null?10:d.baseInterval();double reduction=plugin.getConfig().getDouble("minion-upgrades.speed-reduction-per-level",.5);double minimum=Math.max(.1,plugin.getConfig().getDouble("minion-upgrades.minimum-interval-seconds",1));double global=(upgrades.get(data.ownerId()).speedLevel()-1)*plugin.getConfig().getDouble("shop.speed-per-level",.05);return Math.max(minimum,(base-(Math.min(level,maxLevel())-1)*reduction)*Math.max(.05,1-global));}
    public boolean atMaximumSpeed(MinionData data){return data.level()>=maxLevel();}
    private long intervalMillis(MinionData data){return Math.max(100L,Math.round(intervalSeconds(data)*1000));}
    public double unitSellPrice(MinionData data){return unitSellPrice(data,data.level());}
    public double unitSellPrice(MinionData data,int level){MinionDefinition definition=catalog.get(data.type());if(definition==null)return 0;double increase=Math.max(0,plugin.getConfig().getDouble("minion-upgrades.value-increase-per-level",.05));double levelValue=definition.sellPrice()*(1+(Math.max(1,Math.min(level,maxLevel()))-1)*increase);double profit=1+(upgrades.get(data.ownerId()).profitLevel()-1)*plugin.getConfig().getDouble("shop.profit-per-level",.10);return levelValue*profit*progression.prestigeMultiplier(data.ownerId());}
    public double storedMarketValue(MinionData data){return data.stored()*unitSellPrice(data);}
    public double totalValue(UUID owner){double total=0;for(MinionData m:owned(owner))total+=storedMarketValue(m);return total;}
    public int totalStored(UUID owner){return owned(owner).stream().mapToInt(MinionData::stored).sum();}
    public void clearStorage(UUID owner){owned(owner).forEach(m->m.stored(0));store.save();}
    public void parkAll(UUID owner){owned(owner).forEach(this::removeEntity);store.save();}
    public void relocateAll(UUID owner,Location oldCenter,Location newCenter){double dx=newCenter.getX()-oldCenter.getX(),dy=newCenter.getY()-oldCenter.getY(),dz=newCenter.getZ()-oldCenter.getZ();for(MinionData m:owned(owner)){removeEntity(m);Location moved=new Location(newCenter.getWorld(),m.location().getX()+dx,m.location().getY()+dy,m.location().getZ()+dz,m.location().getYaw(),m.location().getPitch());m.location(moved);m.nextProductionAt(Math.max(m.nextProductionAt(),System.currentTimeMillis()+intervalMillis(m)));}store.save();}
    public void resetLevels(UUID owner){owned(owner).forEach(m->{m.level(1);m.stored(0);m.nextProductionAt(System.currentTimeMillis()+intervalMillis(m));refreshVisual(m);});store.save();}
    public OfflineProduction applyOffline(UUID owner){if(!plugin.getConfig().getBoolean("progression.offline-production.enabled",true))return new OfflineProduction(0,0);long now=System.currentTimeMillis();long cap=Math.max(0,plugin.getConfig().getLong("progression.offline-production.max-hours",12))*3_600_000L;int perCycle=Math.max(1,plugin.getConfig().getInt("production.amount-per-cycle",1));long produced=0;int affected=0;for(MinionData m:owned(owner)){if(m.nextProductionAt()>now||catalog.get(m.type())==null)continue;long interval=intervalMillis(m);long start=Math.max(m.nextProductionAt(),now-cap);long cycles=1+(now-start)/interval;int before=m.stored();long wanted=(long)before+cycles*perCycle;m.stored((int)Math.min(storageLimit(owner),wanted));int added=m.stored()-before;if(added>0){produced+=added;affected++;}m.nextProductionAt(now+interval);}if(affected>0)store.save();return new OfflineProduction(produced,affected);}
    public double offlineEarnings(UUID owner,long elapsedMillis){if(!plugin.getConfig().getBoolean("progression.offline-production.enabled",true)||elapsedMillis<=0)return 0;double rate=Math.max(0,Math.min(1,plugin.getConfig().getDouble("progression.offline-production.money-rate",.75)));int amount=Math.max(1,plugin.getConfig().getInt("production.amount-per-cycle",1));double total=0;for(MinionData minion:owned(owner)){long interval=intervalMillis(minion);long cycles=elapsedMillis/interval;if(cycles>0)total+=cycles*amount*unitSellPrice(minion);}return total*rate;}
    public record OfflineProduction(long items,int minions){}
    public void removeAll(UUID owner){for(MinionData m:List.copyOf(owned(owner))){removeEntity(m);store.remove(m);}}
    public String typeName(UUID player,String type){String translated=languages.plainText(player,"materials."+type);return translated.startsWith("Missing language key:")?pretty(type):translated;}
    public String typeName(org.bukkit.command.CommandSender player,String type){String translated=languages.plainText(player,"materials."+type);return translated.startsWith("Missing language key:")?pretty(type):translated;}
    private String pretty(String id){String value=id.toLowerCase(Locale.ROOT).replace('_',' ');return value.isEmpty()?id:Character.toUpperCase(value.charAt(0))+value.substring(1);}
    public void start(){stop();long productionTicks=Math.max(1,plugin.getConfig().getLong("performance.production-check-ticks",20));productionTask=Bukkit.getScheduler().runTaskTimer(plugin,()->{long now=System.currentTimeMillis();int amount=Math.max(1,plugin.getConfig().getInt("production.amount-per-cycle",1));for(MinionData m:store.all()){if(catalog.get(m.type())==null||plots.owned(m.ownerId())==null)continue;if(m.nextProductionAt()<=0)m.nextProductionAt(now+intervalMillis(m));if(now>=m.nextProductionAt()){m.stored(Math.min(storageLimit(m.ownerId()),m.stored()+amount));m.nextProductionAt(now+intervalMillis(m));store.markDirty();}}},productionTicks,productionTicks);
        long visibilityTicks=Math.max(10,plugin.getConfig().getLong("performance.visibility-check-ticks",40));visibilityTask=Bukkit.getScheduler().runTaskTimer(plugin,this::updateVisibility,1,visibilityTicks);
        long animationTicks=Math.max(5,plugin.getConfig().getLong("performance.animation-interval-ticks",10));animationTask=Bukkit.getScheduler().runTaskTimer(plugin,()->{if(!plugin.getConfig().getBoolean("performance.animations-enabled",true))return;List<ArmorStand> rendered=new ArrayList<>();for(MinionData m:store.all()){ArmorStand stand=entity(m);if(stand!=null)rendered.add(stand);}if(rendered.isEmpty())return;swing=!swing;double x=Math.toRadians(swing?-105:-45);int batch=plugin.getConfig().getBoolean("performance.enabled",true)?Math.max(1,plugin.getConfig().getInt("performance.animation-batch-size",64)):rendered.size();for(int i=0;i<Math.min(batch,rendered.size());i++){animationCursor%=rendered.size();rendered.get(animationCursor++).setRightArmPose(new EulerAngle(x,0,Math.toRadians(10)));}},animationTicks,animationTicks);
        long saveTicks=Math.max(100,plugin.getConfig().getLong("performance.save-interval-ticks",600));saveTask=Bukkit.getScheduler().runTaskTimer(plugin,store::saveIfDirty,saveTicks,saveTicks);}
    private void updateVisibility(){boolean optimization=plugin.getConfig().getBoolean("performance.enabled",true),dynamic=plugin.getConfig().getBoolean("performance.dynamic-rendering",true);double distance=Math.max(8,plugin.getConfig().getDouble("performance.render-distance",32)),maximumDistance=distance*distance;int perPlot=Math.max(1,plugin.getConfig().getInt("performance.max-rendered-per-plot",32)),globalMaximum=Math.max(perPlot,plugin.getConfig().getInt("performance.max-rendered-global",256));List<MinionData> ordered=new ArrayList<>(store.all());ordered.sort(Comparator.comparingDouble(this::nearestViewerDistance));Map<UUID,Integer> renderedByOwner=new HashMap<>();int renderedTotal=0;for(MinionData data:ordered){double nearest=nearestViewerDistance(data);int used=renderedByOwner.getOrDefault(data.ownerId(),0);boolean desired=plots.owned(data.ownerId())!=null&&(!optimization||((!dynamic||nearest<=maximumDistance)&&used<perPlot&&renderedTotal<globalMaximum));if(desired){renderedByOwner.put(data.ownerId(),used+1);renderedTotal++;if(entity(data)==null)spawn(data);}else if(entity(data)!=null)removeEntity(data);}}
    private double nearestViewerDistance(MinionData data){Location location=data.location();if(location.getWorld()==null)return Double.MAX_VALUE;double nearest=Double.MAX_VALUE;for(var player:Bukkit.getOnlinePlayers())if(player.getWorld().equals(location.getWorld()))nearest=Math.min(nearest,player.getLocation().distanceSquared(location));return nearest;}
    public void restore(){for(MinionData m:store.all()){ArmorStand stand=entity(m);if(stand!=null)stand.remove();m.entityId(null);}store.markDirty();}
    public void stop(){if(productionTask!=null)productionTask.cancel();if(animationTask!=null)animationTask.cancel();if(visibilityTask!=null)visibilityTask.cancel();if(saveTask!=null)saveTask.cancel();productionTask=null;animationTask=null;visibilityTask=null;saveTask=null;store.saveIfDirty();}
}
