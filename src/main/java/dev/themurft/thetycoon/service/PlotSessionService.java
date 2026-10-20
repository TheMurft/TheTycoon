package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.model.PlotData;
import dev.themurft.thetycoon.model.TycoonData;

import java.util.UUID;

public final class PlotSessionService {
    private final PlotStore plotStore;private final PlotService plots;private final TycoonStore tycoons;private final MinerService miners;private final MinionService minions;
    public PlotSessionService(PlotStore plotStore,PlotService plots,TycoonStore tycoons,MinerService miners,MinionService minions){this.plotStore=plotStore;this.plots=plots;this.tycoons=tycoons;this.miners=miners;this.minions=minions;}
    public PlotData release(UUID owner){PlotData plot=plots.owned(owner);if(plot==null)return null;TycoonData data=tycoons.get(owner);if(data!=null)miners.removeMiner(data);minions.parkAll(owner);plot.ownerId(null);plotStore.save();plots.reset(plot);return plot;}
}
