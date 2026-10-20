package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.MinionData;
import dev.themurft.thetycoon.model.MinionDefinition;
import dev.themurft.thetycoon.model.PlotData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService implements Listener {
    private final TheTycoonPlugin plugin;private final MinionService minions;private final UpgradeStore upgrades;
    private final EconomyService economy;private final PlotService plots;private final LanguageService languages;private final ProgressionStore progression;private final NamespacedKey actionKey;
    private final Map<UUID,Long> pendingUpgradeAll=new ConcurrentHashMap<>();
    public ShopService(TheTycoonPlugin plugin,MinionService minions,UpgradeStore upgrades,EconomyService economy,PlotService plots,LanguageService languages,ProgressionStore progression){this.plugin=plugin;this.minions=minions;this.upgrades=upgrades;this.economy=economy;this.plots=plots;this.languages=languages;this.progression=progression;actionKey=new NamespacedKey(plugin,"shop_action");}

    public void openMinions(Player player){openMinions(player,0);}
    public void openDashboard(Player player){
        MenuHolder holder=new MenuHolder("dashboard",null,0);Inventory inv=Bukkit.createInventory(holder,45,languages.plainComponent(player,"dashboard-title"));holder.inventory=inv;UUID id=player.getUniqueId();
        ItemStack filler=button(Material.BLACK_STAINED_GLASS_PANE,Component.text(" "),List.of(),"none");for(int slot=0;slot<45;slot++)inv.setItem(slot,filler);
        inv.setItem(4,button(Material.BEACON,languages.plainComponent(player,"dashboard-summary"),List.of(
                languages.plainComponent(player,"dashboard-summary-money","<money>",economy.format(economy.balance(player))),
                languages.plainComponent(player,"dashboard-summary-minions","<amount>",String.valueOf(minions.owned(id).size()),"<limit>",String.valueOf(upgrades.limit(id))),
                languages.plainComponent(player,"dashboard-summary-storage","<items>",String.valueOf(minions.totalStored(id)),"<money>",economy.format(minions.totalValue(id)))),"none"));
        inv.setItem(10,dashboardButton(player,Material.ARMOR_STAND,"dashboard-minions","dashboard-minions-lore","dashboard-shop","<amount>",String.valueOf(minions.owned(id).size()),"<limit>",String.valueOf(upgrades.limit(id))));
        inv.setItem(12,dashboardButton(player,Material.ANVIL,"dashboard-upgrades","dashboard-upgrades-lore","dashboard-upgrades"));
        inv.setItem(14,dashboardButton(player,Material.EMERALD_BLOCK,"dashboard-sell","dashboard-sell-lore","dashboard-sell","<items>",String.valueOf(minions.totalStored(id)),"<money>",economy.format(minions.totalValue(id))));
        inv.setItem(16,dashboardButton(player,Material.NETHER_STAR,"dashboard-prestige","dashboard-prestige-lore","dashboard-prestige","<level>",String.valueOf(progression.get(id).prestige()),"<cost>",economy.format(progression.prestigeCost(id))));
        inv.setItem(28,dashboardButton(player,Material.GOLD_INGOT,"dashboard-offline","dashboard-offline-lore","dashboard-offline","<money>",economy.format(progression.get(id).pendingOfflineMoney())));
        inv.setItem(30,dashboardButton(player,Material.WRITABLE_BOOK,"dashboard-missions","dashboard-missions-lore","dashboard-missions"));
        UpgradePlan plan=upgradePlan(player);inv.setItem(32,button(plan.levels()==0?Material.BARRIER:Material.EXPERIENCE_BOTTLE,languages.plainComponent(player,"dashboard-upgrade-all"),List.of(
                languages.plainComponent(player,"dashboard-upgrade-all-lore","<amount>",String.valueOf(plan.minions()),"<levels>",String.valueOf(plan.levels()),"<cost>",economy.format(plan.cost())),
                languages.plainComponent(player,"dashboard-click-confirm")),plan.count()==0?"none":"dashboard-upgrade-all"));
        inv.setItem(40,button(Material.BARRIER,languages.plainComponent(player,"dashboard-close"),List.of(languages.plainComponent(player,"dashboard-close-lore")),"dashboard-close"));player.openInventory(inv);
    }
    private ItemStack dashboardButton(Player player,Material material,String name,String lore,String action,String... replacements){return button(material,languages.plainComponent(player,name),List.of(languages.plainComponent(player,lore,replacements),languages.plainComponent(player,"dashboard-click")),action);}
    private void openMinions(Player player,int requestedPage){
        List<MinionDefinition> catalog=minions.catalog().sortedBestFirst();int pages=Math.max(1,(catalog.size()+44)/45);int page=Math.max(0,Math.min(pages-1,requestedPage));
        MenuHolder holder=new MenuHolder("minions",null,page);Inventory inv=Bukkit.createInventory(holder,54,languages.plainComponent(player,"shop-title"));holder.inventory=inv;
        int from=page*45,to=Math.min(catalog.size(),from+45);for(int index=from;index<to;index++){MinionDefinition type=catalog.get(index);List<Component> lore=new ArrayList<>();lore.add(languages.plainComponent(player,"shop-buy-price","<money>",economy.format(type.buyPrice())));lore.add(languages.plainComponent(player,"shop-sell-price","<money>",economy.format(type.sellPrice())));lore.add(languages.plainComponent(player,"shop-base-speed","<seconds>",seconds(type.baseInterval())));inv.setItem(index-from,button(type.block(),languages.plainComponent(player,"shop-minion-name","<type>",minions.typeName(player,type.id())),lore,"minion:"+type.id()));}
        if(page>0)inv.setItem(45,button(Material.ARROW,languages.plainComponent(player,"shop-previous"),List.of(),"page:"+(page-1)));
        inv.setItem(49,button(Material.NETHER_STAR,languages.plainComponent(player,"shop-page","<page>",String.valueOf(page+1),"<pages>",String.valueOf(pages)),List.of(),"none"));
        if(page+1<pages)inv.setItem(53,button(Material.ARROW,languages.plainComponent(player,"shop-next"),List.of(),"page:"+(page+1)));
        player.openInventory(inv);languages.send(player,"shop-opened");
    }
    private void openPurchase(Player player,String type){MinionDefinition definition=minions.catalog().get(type);if(definition==null)return;MenuHolder holder=new MenuHolder("purchase",null,0);Inventory inv=Bukkit.createInventory(holder,27,languages.plainComponent(player,"purchase-title","<type>",minions.typeName(player,type)));holder.inventory=inv;ItemStack filler=button(Material.GRAY_STAINED_GLASS_PANE,Component.text(" "),List.of(),"none");for(int slot=0;slot<27;slot++)inv.setItem(slot,filler);int[] amounts={1,2,3,4,8,16,32};int[] slots={10,11,12,13,14,15,16};for(int i=0;i<amounts.length;i++){int amount=amounts[i];ItemStack choice=button(definition.block(),languages.plainComponent(player,"purchase-option","<amount>",String.valueOf(amount)),List.of(languages.plainComponent(player,"purchase-unit-price","<money>",economy.format(definition.buyPrice())),languages.plainComponent(player,"purchase-total-price","<money>",economy.format(definition.buyPrice()*amount)),languages.plainComponent(player,"purchase-click")),"buy:"+type+":"+amount);choice.setAmount(amount);inv.setItem(slots[i],choice);}inv.setItem(22,button(Material.ARROW,languages.plainComponent(player,"purchase-back"),List.of(),"purchase-back"));player.openInventory(inv);}
    private void purchase(Player player,String type,int amount){MinionDefinition definition=minions.catalog().get(type);if(definition==null||amount<1)return;ItemStack item=minions.item(player.getUniqueId(),type,1,0);int capacity=0;for(ItemStack content:player.getInventory().getStorageContents()){if(content==null||content.getType().isAir())capacity+=item.getMaxStackSize();else if(content.isSimilar(item))capacity+=Math.max(0,content.getMaxStackSize()-content.getAmount());}if(capacity<amount){languages.send(player,"purchase-no-space","<amount>",String.valueOf(amount));return;}double total=definition.buyPrice()*amount;if(!economy.withdraw(player,total)){languages.send(player,"not-enough-money","<money>",economy.format(total));return;}item.setAmount(amount);player.getInventory().addItem(item);languages.send(player,"purchase-success","<amount>",String.valueOf(amount),"<type>",minions.typeName(player,type),"<money>",economy.format(total));openPurchase(player,type);}
    public void openUpgrades(Player player){
        MenuHolder holder=new MenuHolder("upgrades",null,0);Inventory inv=Bukkit.createInventory(holder,27,languages.plainComponent(player,"upgrades-title"));holder.inventory=inv;UUID id=player.getUniqueId();UpgradeStore.Upgrades current=upgrades.get(id);int storageBase=plugin.getConfig().getInt("miner.storage-limit",2304);
        inv.setItem(10,upgradeButton(player,upgrades.slotMaxed(id),Material.ARMOR_STAND,"shop-slot-name",List.of(
                languages.plainComponent(player,"shop-slot-current","<limit>",String.valueOf(upgrades.limit(id)),"<max>",String.valueOf(upgrades.maxMinionLimit())),
                languages.plainComponent(player,"shop-slot-next","<limit>",String.valueOf(upgrades.nextMinionLimit(id))),
                languages.plainComponent(player,"shop-price","<money>",economy.format(upgrades.slotPrice(id))),languages.plainComponent(player,"shop-click-upgrade")),"slot"));
        inv.setItem(12,upgradeButton(player,upgrades.storageMaxed(id),Material.BARREL,"shop-storage-name",List.of(
                languages.plainComponent(player,"shop-upgrade-level","<level>",String.valueOf(current.storageLevel()),"<max>",String.valueOf(plugin.getConfig().getInt("shop.max-storage-level",25))),
                languages.plainComponent(player,"shop-storage-current","<amount>",String.valueOf(storageBase*current.storageLevel())),
                languages.plainComponent(player,"shop-storage-next","<amount>",String.valueOf(storageBase*(current.storageLevel()+1))),
                languages.plainComponent(player,"shop-price","<money>",economy.format(upgrades.storagePrice(id))),languages.plainComponent(player,"shop-click-upgrade")),"storage"));
        int profitStep=(int)Math.round(plugin.getConfig().getDouble("shop.profit-per-level",.10)*100),speedStep=(int)Math.round(plugin.getConfig().getDouble("shop.speed-per-level",.05)*100);
        inv.setItem(14,upgradeButton(player,upgrades.profitMaxed(id),Material.EMERALD,"shop-profit-name",List.of(
                languages.plainComponent(player,"shop-upgrade-level","<level>",String.valueOf(current.profitLevel()),"<max>",String.valueOf(plugin.getConfig().getInt("shop.max-profit-level",20))),
                languages.plainComponent(player,"shop-profit-current","<percent>",String.valueOf(upgrades.profitPercent(id))),
                languages.plainComponent(player,"shop-profit-next","<percent>",String.valueOf(upgrades.profitPercent(id)+profitStep)),
                languages.plainComponent(player,"shop-price","<money>",economy.format(upgrades.profitPrice(id))),languages.plainComponent(player,"shop-click-upgrade")),"profit"));
        inv.setItem(16,upgradeButton(player,upgrades.speedMaxed(id),Material.SUGAR,"shop-speed-name",List.of(
                languages.plainComponent(player,"shop-upgrade-level","<level>",String.valueOf(current.speedLevel()),"<max>",String.valueOf(plugin.getConfig().getInt("shop.max-speed-level",20))),
                languages.plainComponent(player,"shop-speed-current","<percent>",String.valueOf(upgrades.speedPercent(id))),
                languages.plainComponent(player,"shop-speed-next","<percent>",String.valueOf(upgrades.speedPercent(id)+speedStep)),
                languages.plainComponent(player,"shop-price","<money>",economy.format(upgrades.speedPrice(id))),languages.plainComponent(player,"shop-click-upgrade")),"speed"));
        player.openInventory(inv);languages.send(player,"upgrades-opened");
    }
    private ItemStack upgradeButton(Player player,boolean maxed,Material material,String name,List<Component> lore,String action){if(!maxed)return button(material,languages.plainComponent(player,name),lore,action);return button(Material.BARRIER,languages.plainComponent(player,name),List.of(lore.getFirst(),languages.plainComponent(player,"shop-max-reached")),"none");}
    public void openMinion(Player player,MinionData data){
        MinionDefinition definition=minions.catalog().get(data.type());if(definition==null){player.closeInventory();return;}
        MenuHolder holder=new MenuHolder("minion",data.id(),0);Inventory inv=Bukkit.createInventory(holder,27,languages.plainComponent(player,"minion-menu-title","<type>",minions.typeName(player,data.type())));holder.inventory=inv;
        ItemStack status=new ItemStack(definition.block());ItemMeta statusMeta=status.getItemMeta();statusMeta.displayName(languages.plainComponent(player,"minion-menu-status"));
        statusMeta.lore(List.of(
                languages.plainComponent(player,"minion-menu-level","<level>",String.valueOf(data.level()),"<max>",String.valueOf(minions.maxLevel())),
                languages.plainComponent(player,"minion-menu-material","<material>",minions.typeName(player,data.type())),
                languages.plainComponent(player,"minion-menu-storage","<stored>",String.valueOf(data.stored()),"<capacity>",String.valueOf(minions.storageLimit(data.ownerId()))),
                languages.plainComponent(player,"minion-menu-current-speed","<seconds>",seconds(minions.intervalSeconds(data))),
                languages.plainComponent(player,"minion-menu-unit-value","<money>",economy.format(minions.unitSellPrice(data))),
                languages.plainComponent(player,"minion-menu-stored-value","<money>",economy.format(minions.storedMarketValue(data)))));
        status.setItemMeta(statusMeta);inv.setItem(11,status);boolean maximum=minions.atMaximumSpeed(data);ItemStack upgrade=new ItemStack(maximum?Material.BARRIER:Material.EMERALD);ItemMeta upgradeMeta=upgrade.getItemMeta();
        if(maximum)upgradeMeta.displayName(languages.plainComponent(player,"minion-menu-max","<level>",String.valueOf(minions.maxLevel())));
        else{upgradeMeta.displayName(languages.plainComponent(player,"minion-menu-upgrade"));upgradeMeta.lore(List.of(
                languages.plainComponent(player,"minion-menu-upgrade-price","<money>",economy.format(minions.upgradePrice(data))),
                languages.plainComponent(player,"minion-menu-upgrade-result","<level>",String.valueOf(data.level()+1)),
                languages.plainComponent(player,"minion-menu-new-speed","<seconds>",seconds(minions.intervalSeconds(data,data.level()+1))),
                languages.plainComponent(player,"minion-menu-speed-improvement","<seconds>",seconds(minions.intervalSeconds(data)-minions.intervalSeconds(data,data.level()+1))),
                languages.plainComponent(player,"minion-menu-new-value","<money>",economy.format(minions.unitSellPrice(data,data.level()+1))),
                languages.plainComponent(player,"minion-menu-value-improvement","<money>",economy.format(minions.unitSellPrice(data,data.level()+1)-minions.unitSellPrice(data)))));upgradeMeta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"upgrade-minion");}
        upgrade.setItemMeta(upgradeMeta);inv.setItem(15,upgrade);player.openInventory(inv);
    }
    private ItemStack button(Material material,Component name,List<Component> lore,String action){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.displayName(name);meta.lore(lore);meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);item.setItemMeta(meta);return item;}
    private String seconds(double value){return String.format(Locale.US,"%.1f",value);}

    private UpgradePlan upgradePlan(Player player){
        PriorityQueue<UpgradeCandidate> queue=new PriorityQueue<>(Comparator.comparingDouble(candidate->minions.upgradePrice(candidate.data(),candidate.level())));for(MinionData data:minions.owned(player.getUniqueId()))if(!minions.atMaximumSpeed(data))queue.add(new UpgradeCandidate(data,data.level()));double available=economy.balance(player),cost=0;int levels=0;Map<UUID,Integer> upgradesByMinion=new LinkedHashMap<>();while(!queue.isEmpty()){UpgradeCandidate candidate=queue.poll();double price=minions.upgradePrice(candidate.data(),candidate.level());if(cost+price>available)break;cost+=price;levels++;upgradesByMinion.merge(candidate.data().id(),1,Integer::sum);int nextLevel=candidate.level()+1;if(nextLevel<minions.maxLevel())queue.add(new UpgradeCandidate(candidate.data(),nextLevel));}return new UpgradePlan(upgradesByMinion.size(),levels,cost,upgradesByMinion);
    }
    private void requestUpgradeAll(Player player){UpgradePlan plan=upgradePlan(player);player.closeInventory();if(plan.levels()==0){languages.send(player,minions.owned(player.getUniqueId()).stream().allMatch(minions::atMaximumSpeed)?"upgrade-all-none":"upgrade-all-no-money");return;}pendingUpgradeAll.put(player.getUniqueId(),System.currentTimeMillis()+30_000L);languages.send(player,"upgrade-all-confirm","<amount>",String.valueOf(plan.minions()),"<levels>",String.valueOf(plan.levels()),"<cost>",economy.format(plan.cost()));}
    private void confirmUpgradeAll(Player player){
        UpgradePlan plan=upgradePlan(player);if(plan.levels()==0||!economy.withdraw(player,plan.cost())){languages.send(player,"upgrade-all-no-longer-available");return;}for(var entry:plan.upgrades().entrySet()){MinionData data=minions.get(entry.getKey());if(data!=null)minions.upgradeLevels(data,entry.getValue());}languages.send(player,"upgrade-all-success","<amount>",String.valueOf(plan.minions()),"<levels>",String.valueOf(plan.levels()),"<cost>",economy.format(plan.cost()));
    }
    private record UpgradeCandidate(MinionData data,int level){}
    private record UpgradePlan(int minions,int levels,double cost,Map<UUID,Integer> upgrades){int count(){return levels;}}

    @EventHandler public void onMenu(InventoryClickEvent event){if(!(event.getInventory().getHolder() instanceof MenuHolder holder)||!(event.getWhoClicked() instanceof Player player))return;event.setCancelled(true);ItemStack item=event.getCurrentItem();if(item==null||!item.hasItemMeta())return;String action=item.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(action==null||action.equals("none"))return;if(action.equals("purchase-back")){openMinions(player);return;}if(action.startsWith("buy:")){String[] parts=action.split(":");if(parts.length==3)try{purchase(player,parts[1],Integer.parseInt(parts[2]));}catch(NumberFormatException ignored){}return;}if(action.equals("dashboard-close")){player.closeInventory();return;}if(action.equals("dashboard-upgrade-all")){requestUpgradeAll(player);return;}if(action.equals("dashboard-shop")){openMinions(player);return;}if(action.equals("dashboard-upgrades")){openUpgrades(player);return;}if(action.equals("dashboard-sell")){player.closeInventory();player.performCommand("tycoon sell");return;}if(action.equals("dashboard-prestige")){player.closeInventory();player.performCommand("tycoon prestige");return;}if(action.equals("dashboard-missions")){player.closeInventory();player.performCommand("tycoon missions");return;}if(action.startsWith("page:")){openMinions(player,Integer.parseInt(action.substring(5)));return;}if(action.equals("upgrade-minion")){MinionData data=holder.targetId==null?null:minions.get(holder.targetId);if(data==null){player.closeInventory();return;}if(!data.ownerId().equals(player.getUniqueId())){languages.send(player,"not-your-minion");player.closeInventory();return;}if(minions.atMaximumSpeed(data)){languages.send(player,"minion-max-level","<level>",String.valueOf(minions.maxLevel()));openMinion(player,data);return;}double price=minions.upgradePrice(data);if(!economy.withdraw(player,price)){languages.send(player,"not-enough-money","<money>",economy.format(price));return;}minions.upgrade(data);languages.send(player,"minion-upgraded","<level>",String.valueOf(data.level()));openMinion(player,data);return;}if(action.startsWith("minion:")){openPurchase(player,action.substring(7));return;}UUID id=player.getUniqueId();if(action.equals("slot")){double price=upgrades.slotPrice(id);if(!economy.withdraw(player,price)){languages.send(player,"not-enough-money","<money>",economy.format(price));return;}upgrades.addSlot(id);languages.send(player,"upgrade-limit-bought","<limit>",String.valueOf(upgrades.limit(id)));openUpgrades(player);return;}if(action.equals("storage")){double price=upgrades.storagePrice(id);if(!economy.withdraw(player,price)){languages.send(player,"not-enough-money","<money>",economy.format(price));return;}upgrades.addStorage(id);languages.send(player,"upgrade-storage-bought","<level>",String.valueOf(upgrades.get(id).storageLevel()));openUpgrades(player);return;}if(action.equals("profit")){double price=upgrades.profitPrice(id);if(!economy.withdraw(player,price)){languages.send(player,"not-enough-money","<money>",economy.format(price));return;}upgrades.addProfit(id);languages.send(player,"upgrade-profit-bought","<percent>",String.valueOf(upgrades.profitPercent(id)));openUpgrades(player);return;}if(action.equals("speed")){double price=upgrades.speedPrice(id);if(!economy.withdraw(player,price)){languages.send(player,"not-enough-money","<money>",economy.format(price));return;}upgrades.addSpeed(id);languages.send(player,"upgrade-speed-bought","<percent>",String.valueOf(upgrades.speedPercent(id)));openUpgrades(player);}}
    @EventHandler public void onUpgradeAllChat(AsyncChatEvent event){Player player=event.getPlayer();Long expires=pendingUpgradeAll.get(player.getUniqueId());if(expires==null)return;event.setCancelled(true);String answer=PlainTextComponentSerializer.plainText().serialize(event.message()).trim().toLowerCase(Locale.ROOT);if(System.currentTimeMillis()>expires){pendingUpgradeAll.remove(player.getUniqueId());languages.send(player,"upgrade-all-expired");return;}if(List.of("yes","y","confirm","si","sí","confirmar","oui","o","sim","s").contains(answer)){pendingUpgradeAll.remove(player.getUniqueId());Bukkit.getScheduler().runTask(plugin,()->confirmUpgradeAll(player));return;}if(List.of("no","n","non","não","nao","cancel","cancelar","annuler").contains(answer)){pendingUpgradeAll.remove(player.getUniqueId());languages.send(player,"upgrade-all-cancelled");return;}languages.send(player,"upgrade-all-invalid");}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=false)public void onOfflineDashboard(InventoryClickEvent event){if(!(event.getInventory().getHolder() instanceof MenuHolder)||!(event.getWhoClicked() instanceof Player player))return;ItemStack item=event.getCurrentItem();if(item==null||!item.hasItemMeta())return;String action=item.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(!"dashboard-offline".equals(action))return;event.setCancelled(true);player.closeInventory();player.performCommand("tycoon offline");}
    @EventHandler public void onJoinRepair(PlayerJoinEvent event){Player player=event.getPlayer();Bukkit.getScheduler().runTask(plugin,()->{int owned=minions.owned(player.getUniqueId()).size();if(owned<=upgrades.limit(player.getUniqueId()))return;List<ItemStack> returned=minions.collectAll(player.getUniqueId());int dropped=0;for(ItemStack item:returned)for(ItemStack leftover:player.getInventory().addItem(item).values()){dropped+=leftover.getAmount();player.getWorld().dropItemNaturally(player.getLocation(),leftover);}languages.send(player,"minion-limit-repaired","<amount>",String.valueOf(returned.size()));if(dropped>0)languages.send(player,"prestige-minions-dropped","<amount>",String.valueOf(dropped));});}
    @EventHandler public void onPrepareCraft(PrepareItemCraftEvent event){if(containsMinion(event.getInventory().getMatrix()))event.getInventory().setResult(null);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void onCraft(CraftItemEvent event){if(!containsMinion(event.getInventory().getMatrix()))return;event.setCancelled(true);event.setCurrentItem(null);if(event.getWhoClicked() instanceof Player player)languages.send(player,"minion-crafting-blocked");}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void onUpgrade(EntityDamageByEntityEvent event){String raw=event.getEntity().getPersistentDataContainer().get(minions.entityKey(),PersistentDataType.STRING);if(raw==null)return;event.setCancelled(true);if(!(event.getDamager() instanceof Player player))return;MinionData data=minions.get(UUID.fromString(raw));if(data==null)return;if(!data.ownerId().equals(player.getUniqueId())){languages.send(player,"not-your-minion");return;}openMinion(player,data);}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=false)public void onMinionDamage(EntityDamageEvent event){if(event.getEntity().getPersistentDataContainer().has(minions.entityKey(),PersistentDataType.STRING))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void onCollect(PlayerArmorStandManipulateEvent event){String raw=event.getRightClicked().getPersistentDataContainer().get(minions.entityKey(),PersistentDataType.STRING);if(raw==null)return;event.setCancelled(true);MinionData data=minions.get(UUID.fromString(raw));if(data==null)return;if(!data.ownerId().equals(event.getPlayer().getUniqueId())){languages.send(event.getPlayer(),"not-your-minion");return;}if(!event.getPlayer().isSneaking()){openMinion(event.getPlayer(),data);return;}event.getPlayer().getInventory().addItem(minions.collect(data));languages.send(event.getPlayer(),"minion-collected","<type>",minions.typeName(event.getPlayer(),data.type()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void onBulkPlace(PlayerInteractEvent event){if(event.getHand()!=EquipmentSlot.HAND||event.getAction()!=Action.RIGHT_CLICK_BLOCK||!event.getPlayer().isSneaking()||event.getClickedBlock()==null||minions.itemType(event.getItem())==null)return;event.setCancelled(true);Player player=event.getPlayer();PlotData plot=plots.owned(player.getUniqueId());if(plot==null||!plot.contains(event.getClickedBlock().getLocation())||!plots.isFloorY(plot,event.getClickedBlock().getY())){languages.send(player,"bulk-place-floor");return;}int available=upgrades.limit(player.getUniqueId())-minions.owned(player.getUniqueId()).size();if(available<=0){languages.send(player,"minion-limit","<limit>",String.valueOf(upgrades.limit(player.getUniqueId())));return;}List<org.bukkit.Location> positions=new ArrayList<>();Set<String> occupied=new HashSet<>();for(MinionData data:minions.owned(player.getUniqueId()))occupied.add(data.location().getBlockX()+":"+data.location().getBlockY()+":"+data.location().getBlockZ());int clickedX=event.getClickedBlock().getX(),clickedY=event.getClickedBlock().getY(),clickedZ=event.getClickedBlock().getZ();addBulkPosition(positions,occupied,plot,clickedX,clickedY,clickedZ);for(int floor=0;floor<plots.floorCount();floor++){int y=plots.floorY(plot,floor);for(int x=plot.minX();x<=plot.maxX();x++)for(int z=plot.minZ();z<=plot.maxZ();z++)addBulkPosition(positions,occupied,plot,x,y,z);}int placed=0;int storageSize=player.getInventory().getStorageContents().length;for(int slot=0;slot<storageSize&&placed<available&&!positions.isEmpty();slot++){ItemStack item=player.getInventory().getItem(slot);String type=minions.itemType(item);if(type==null)continue;int amount=item.getAmount(),used=0,level=minions.itemLevel(item),stored=minions.itemStored(item);while(used<amount&&placed<available&&!positions.isEmpty()){minions.place(player.getUniqueId(),type,level,stored,positions.removeFirst());used++;placed++;}if(used>=amount)player.getInventory().setItem(slot,null);else item.setAmount(amount-used);}if(placed==0)languages.send(player,"bulk-place-no-space");else languages.send(player,"bulk-place-success","<amount>",String.valueOf(placed),"<limit>",String.valueOf(upgrades.limit(player.getUniqueId())));}
    private void addBulkPosition(List<org.bukkit.Location> positions,Set<String> occupied,PlotData plot,int x,int floorY,int z){String key=x+":"+(floorY+1)+":"+z;if(x<plot.minX()||x>plot.maxX()||z<plot.minZ()||z>plot.maxZ()||occupied.contains(key))return;var world=plots.world(plot);if(world==null||!world.getBlockAt(x,floorY+1,z).isPassable()||!world.getBlockAt(x,floorY+2,z).isPassable())return;occupied.add(key);positions.add(new org.bukkit.Location(world,x+.5,floorY+1,z+.5));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void onPlace(BlockPlaceEvent event){String type=minions.itemType(event.getItemInHand());if(type==null)return;event.setCancelled(true);Player player=event.getPlayer();if(!plots.canBuild(player.getUniqueId(),event.getBlock().getLocation())){languages.send(player,"build-denied");return;}if(minions.owned(player.getUniqueId()).size()>=upgrades.limit(player.getUniqueId())){languages.send(player,"minion-limit","<limit>",String.valueOf(upgrades.limit(player.getUniqueId())));return;}ItemStack hand=event.getItemInHand();int level=minions.itemLevel(hand),stored=minions.itemStored(hand);if(hand.getAmount()<=1)player.getInventory().setItem(event.getHand(),null);else hand.setAmount(hand.getAmount()-1);minions.place(player.getUniqueId(),type,level,stored,event.getBlock().getLocation().add(.5,0,.5));languages.send(player,"minion-placed","<type>",minions.typeName(player,type),"<level>",String.valueOf(level));}
    private boolean containsMinion(ItemStack[] items){for(ItemStack item:items)if(item!=null&&minions.itemType(item)!=null)return true;return false;}
    private static final class MenuHolder implements InventoryHolder{private final String type;private final UUID targetId;private final int page;private Inventory inventory;private MenuHolder(String type,UUID targetId,int page){this.type=type;this.targetId=targetId;this.page=page;}public Inventory getInventory(){return inventory;}}
}
