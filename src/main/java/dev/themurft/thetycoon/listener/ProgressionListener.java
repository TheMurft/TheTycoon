package dev.themurft.thetycoon.listener;

import dev.themurft.thetycoon.service.LanguageService;
import dev.themurft.thetycoon.service.MinionService;
import dev.themurft.thetycoon.service.ProgressionStore;
import dev.themurft.thetycoon.service.PlotSessionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ProgressionListener implements Listener {
    private final MinionService minions;private final ProgressionStore progression;private final LanguageService languages;private final PlotSessionService sessions;
    public ProgressionListener(MinionService minions,ProgressionStore progression,LanguageService languages,PlotSessionService sessions){this.minions=minions;this.progression=progression;this.languages=languages;this.sessions=sessions;}
    @EventHandler public void onJoin(PlayerJoinEvent event){var id=event.getPlayer().getUniqueId();long elapsed=Math.max(0,System.currentTimeMillis()-progression.get(id).lastSeen());double earned=minions.offlineEarnings(id,elapsed);progression.addOfflineMoney(id,earned);if(progression.get(id).pendingOfflineMoney()>0)languages.send(event.getPlayer(),"offline-money-ready","<money>",String.format(java.util.Locale.US,"%.2f",progression.get(id).pendingOfflineMoney()));progression.updateLastSeen(id);}
    @EventHandler public void onQuit(PlayerQuitEvent event){sessions.release(event.getPlayer().getUniqueId());progression.updateLastSeen(event.getPlayer().getUniqueId());}
}
