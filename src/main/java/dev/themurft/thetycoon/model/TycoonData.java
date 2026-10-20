package dev.themurft.thetycoon.model;

import org.bukkit.Location;

import java.util.UUID;

public final class TycoonData {
    private final UUID ownerId;
    private Location plotLocation;
    private UUID minerId;
    private int stored;

    public TycoonData(UUID ownerId, Location plotLocation, UUID minerId, int stored) {
        this.ownerId = ownerId;
        this.plotLocation = plotLocation;
        this.minerId = minerId;
        this.stored = stored;
    }

    public UUID ownerId() { return ownerId; }
    public Location plotLocation() { return plotLocation; }
    public void plotLocation(Location plotLocation) { this.plotLocation = plotLocation; }
    public UUID minerId() { return minerId; }
    public void minerId(UUID minerId) { this.minerId = minerId; }
    public int stored() { return stored; }
    public void stored(int stored) { this.stored = Math.max(0, stored); }
}
