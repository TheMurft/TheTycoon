package dev.themurft.thetycoon.listener;

import dev.themurft.thetycoon.service.LanguageService;
import dev.themurft.thetycoon.service.PlotService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class PlotProtectionListener implements Listener {
    private final PlotService plots;
    private final LanguageService languages;

    public PlotProtectionListener(PlotService plots, LanguageService languages) {
        this.plots = plots;
        this.languages = languages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plots.isManagedWorld(event.getBlock().getWorld())) return;
        if (!event.getPlayer().hasPermission("thetycoon.build.bypass")) {
            event.setCancelled(true);
            languages.send(event.getPlayer(), "break-denied");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) deny(event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Location target = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        if (!canBuild(event.getPlayer(), target)) deny(event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlockClicked().getLocation())) deny(event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && plots.isManagedWorld(block.getWorld()) && !canBuild(event.getPlayer(), block.getLocation())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!plots.isManagedWorld(event.getBlock().getWorld())) return;
        if (event.getEntity() instanceof Player player && canBuild(player, event.getBlock().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plots.isManagedWorld(block.getWorld()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (plots.isManagedWorld(event.getBlock().getWorld()) && !plots.samePlot(event.getBlock().getLocation(), event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plots.isManagedWorld(event.getBlock().getWorld())) return;
        for (Block block : event.getBlocks()) {
            if (!plots.samePlot(block.getLocation(), block.getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!plots.isManagedWorld(event.getBlock().getWorld())) return;
        for (Block block : event.getBlocks()) {
            if (!plots.samePlot(block.getLocation(), block.getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean canBuild(Player player, Location location) {
        return player.hasPermission("thetycoon.build.bypass") || plots.canBuild(player.getUniqueId(), location);
    }

    private void deny(Player player, org.bukkit.event.Cancellable event) {
        event.setCancelled(true);
        languages.send(player, "build-denied");
    }
}
