package dev.themurft.thetycoon.listener;

import dev.themurft.thetycoon.TheTycoonPlugin;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

public final class MinerProtectionListener implements Listener {
    private final TheTycoonPlugin plugin;

    public MinerProtectionListener(TheTycoonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof ArmorStand stand && isMiner(stand)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (isMiner(event.getRightClicked())) event.setCancelled(true);
    }

    private boolean isMiner(ArmorStand stand) {
        return stand.getPersistentDataContainer().has(plugin.minerOwnerKey(), PersistentDataType.STRING);
    }
}
