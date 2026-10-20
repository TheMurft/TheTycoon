package dev.themurft.thetycoon.model;

import org.bukkit.Location;

import java.util.UUID;

public final class MinionData {
    private final UUID id;
    private final UUID ownerId;
    private final String type;
    private Location location;
    private UUID entityId;
    private int level;
    private int stored;
    private long nextProductionAt;

    public MinionData(UUID id, UUID ownerId, String type, Location location, UUID entityId, int level, int stored, long nextProductionAt) {
        this.id = id; this.ownerId = ownerId; this.type = type; this.location = location;
        this.entityId = entityId; this.level = level; this.stored = stored;
        this.nextProductionAt = nextProductionAt;
    }
    public UUID id() { return id; }
    public UUID ownerId() { return ownerId; }
    public String type() { return type; }
    public Location location() { return location; }
    public void location(Location value) { location = value; }
    public UUID entityId() { return entityId; }
    public void entityId(UUID value) { entityId = value; }
    public int level() { return level; }
    public void level(int value) { level = Math.max(1, value); }
    public int stored() { return stored; }
    public void stored(int value) { stored = Math.max(0, value); }
    public long nextProductionAt() { return nextProductionAt; }
    public void nextProductionAt(long value) { nextProductionAt = value; }
}
