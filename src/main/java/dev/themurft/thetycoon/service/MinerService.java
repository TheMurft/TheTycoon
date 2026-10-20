package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.TycoonData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.UUID;
import java.util.function.Predicate;

public final class MinerService {
    private final TheTycoonPlugin plugin;
    private final TycoonStore store;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private BukkitTask productionTask;
    private BukkitTask animationTask;
    private boolean swing;

    public MinerService(TheTycoonPlugin plugin, TycoonStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public ArmorStand spawnOrMove(TycoonData data, Location location) {
        removeMiner(data);
        location.getChunk().load();
        removeOwnedMinersNear(data.ownerId(), location);
        OfflinePlayer owner = Bukkit.getOfflinePlayer(data.ownerId());
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setArms(true);
            armorStand.setBasePlate(true);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setPersistent(true);
            armorStand.setCanPickupItems(false);
            armorStand.setCustomNameVisible(true);
            armorStand.customName(minerName(owner));
            armorStand.getPersistentDataContainer().set(plugin.minerOwnerKey(), PersistentDataType.STRING, data.ownerId().toString());
            equip(armorStand, owner);
            armorStand.setRightArmPose(new EulerAngle(Math.toRadians(-65), 0, Math.toRadians(10)));
            armorStand.setLeftArmPose(new EulerAngle(Math.toRadians(-15), 0, Math.toRadians(-8)));
        });
        data.minerId(stand.getUniqueId());
        data.plotLocation(location.clone());
        store.save();
        return stand;
    }

    private Component minerName(OfflinePlayer owner) {
        String template = plugin.getConfig().getString("miner.display-name", "<gold>Miner <owner>");
        String name = owner.getName() == null ? owner.getUniqueId().toString().substring(0, 8) : owner.getName();
        return miniMessage.deserialize(template.replace("<owner>", name));
    }

    private void equip(ArmorStand stand, OfflinePlayer owner) {
        EntityEquipment equipment = stand.getEquipment();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(owner);
        head.setItemMeta(skull);
        equipment.setHelmet(head);

        Color color = Color.fromRGB(
                plugin.getConfig().getInt("miner.armor-color.red", 181),
                plugin.getConfig().getInt("miner.armor-color.green", 143),
                plugin.getConfig().getInt("miner.armor-color.blue", 78));
        equipment.setChestplate(leather(Material.LEATHER_CHESTPLATE, color));
        equipment.setLeggings(leather(Material.LEATHER_LEGGINGS, color));
        equipment.setBoots(leather(Material.LEATHER_BOOTS, color));
        equipment.setItemInMainHand(new ItemStack(Material.WOODEN_PICKAXE));
    }

    private ItemStack leather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    public void startTasks() {
        stopTasks();
        long interval = Math.max(1, plugin.getConfig().getLong("miner.interval-seconds", 5)) * 20L;
        productionTask = Bukkit.getScheduler().runTaskTimer(plugin, this::produce, interval, interval);
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animate, 10L, 10L);
    }

    private void produce() {
        int limit = Math.max(1, plugin.getConfig().getInt("miner.storage-limit", 2304));
        int amount = Math.max(1, plugin.getConfig().getInt("miner.amount-per-cycle", 1));
        boolean changed = false;
        for (TycoonData data : store.all()) {
            ArmorStand miner = findMiner(data);
            if (miner != null && miner.isValid() && data.stored() < limit) {
                data.stored(Math.min(limit, data.stored() + amount));
                changed = true;
            }
        }
        if (changed) store.save();
    }

    private void animate() {
        swing = !swing;
        double x = Math.toRadians(swing ? -105 : -45);
        for (TycoonData data : store.all()) {
            ArmorStand miner = findMiner(data);
            if (miner != null && miner.isValid()) miner.setRightArmPose(new EulerAngle(x, 0, Math.toRadians(10)));
        }
    }

    public ArmorStand findMiner(TycoonData data) {
        if (data.minerId() == null) return null;
        Entity entity = Bukkit.getEntity(data.minerId());
        return entity instanceof ArmorStand stand ? stand : null;
    }

    public void removeMiner(TycoonData data) {
        ArmorStand stand = findMiner(data);
        if (stand != null) stand.remove();
        Location location = data.plotLocation();
        if (location.getWorld() != null) {
            location.getChunk().load();
            removeOwnedMinersNear(data.ownerId(), location);
        }
        data.minerId(null);
    }

    private void removeOwnedMinersNear(UUID ownerId, Location center) {
        String expectedOwner = ownerId.toString();
        for (Entity entity : center.getWorld().getNearbyEntities(center, 16, 16, 16)) {
            if (!(entity instanceof ArmorStand stand)) continue;
            String storedOwner = stand.getPersistentDataContainer().get(plugin.minerOwnerKey(), PersistentDataType.STRING);
            if (expectedOwner.equals(storedOwner)) stand.remove();
        }
    }

    public void restoreMissingMiners(Predicate<TycoonData> shouldRestore) {
        for (TycoonData data : store.all()) {
            if (shouldRestore.test(data) && findMiner(data) == null) spawnOrMove(data, data.plotLocation());
        }
    }

    public void stopTasks() {
        if (productionTask != null) productionTask.cancel();
        if (animationTask != null) animationTask.cancel();
        productionTask = null;
        animationTask = null;
    }
}
