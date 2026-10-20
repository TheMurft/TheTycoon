package dev.themurft.thetycoon.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class PlotData {
    private final int id;
    private final String worldName;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final int groundY;
    private UUID ownerId;

    public PlotData(int id, String worldName, int minX, int maxX, int minZ, int maxZ, int groundY, UUID ownerId) {
        this.id = id;
        this.worldName = worldName;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.groundY = groundY;
        this.ownerId = ownerId;
    }

    public int id() { return id; }
    public String worldName() { return worldName; }
    public int minX() { return minX; }
    public int maxX() { return maxX; }
    public int minZ() { return minZ; }
    public int maxZ() { return maxZ; }
    public int groundY() { return groundY; }
    public UUID ownerId() { return ownerId; }
    public void ownerId(UUID ownerId) { this.ownerId = ownerId; }

    public boolean contains(Location location) {
        return location.getWorld() != null && location.getWorld().getName().equals(worldName)
                && location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public Location center(World world) {
        return new Location(world, (minX + maxX) / 2.0 + 0.5, groundY + 1.0, (minZ + maxZ) / 2.0 + 0.5);
    }
}
