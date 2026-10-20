package dev.themurft.thetycoon.service;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.PlotData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;
import java.util.UUID;

public final class PlotService {
    private final TheTycoonPlugin plugin;
    private final PlotStore store;

    public PlotService(TheTycoonPlugin plugin, PlotStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public int generate(Location anchor, int amount) {
        World world = anchor.getWorld();
        if (world == null) return 0;
        int size = Math.max(8, plugin.getConfig().getInt("plots.size", 8));
        int road = Math.max(3, plugin.getConfig().getInt("plots.road-width", 5));
        boolean squareLayout = plugin.getConfig().getBoolean("plots.square-layout", true);
        int columns = squareLayout ? Math.max(1, (int) Math.ceil(Math.sqrt(Math.max(1, amount))))
                : Math.max(1, plugin.getConfig().getInt("plots.columns", 3));
        int rows = Math.max(1, (int) Math.ceil((double) amount / columns));
        int groundY = anchor.getBlockY() - 1;
        int stride = size + road;
        int firstMinX = anchor.getBlockX() - ((columns - 1) * stride) / 2 - size / 2;
        int firstMinZ = anchor.getBlockZ() - ((rows - 1) * stride) / 2 - size / 2;
        int generated = 0;
        for (int i = 0; i < amount; i++) {
            int minX = firstMinX + (i % columns) * stride;
            int minZ = firstMinZ + (i / columns) * stride;
            PlotData plot = new PlotData(store.nextId(), world.getName(), minX, minX + size - 1,
                    minZ, minZ + size - 1, groundY, null);
            if (overlaps(plot)) continue;
            buildPlot(plot);
            store.add(plot);
            generated++;
        }
        store.save();
        return generated;
    }

    private boolean overlaps(PlotData candidate) {
        return store.all().stream().anyMatch(plot -> plot.worldName().equals(candidate.worldName())
                && candidate.minX() - 1 <= plot.maxX() + 1 && candidate.maxX() + 1 >= plot.minX() - 1
                && candidate.minZ() - 1 <= plot.maxZ() + 1 && candidate.maxZ() + 1 >= plot.minZ() - 1);
    }

    private void buildPlot(PlotData plot) {
        World world = world(plot);
        if (world == null) throw new IllegalStateException("Plot world is not loaded: " + plot.worldName());
        BlockType foundation = blockType("plots.foundation-material", Material.DIRT);
        BlockType ground = blockType("plots.ground-material", Material.GRASS_BLOCK);
        BlockType border = blockType("plots.border-material", Material.POLISHED_ANDESITE);
        try (EditSession edit = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            edit.setBlocks(region(world, plot.minX(), plot.groundY() - 3, plot.minZ(), plot.maxX(), plot.groundY() - 1, plot.maxZ()), foundation.getDefaultState());
            edit.setBlocks(region(world, plot.minX(), plot.groundY(), plot.minZ(), plot.maxX(), plot.groundY(), plot.maxZ()), ground.getDefaultState());
            edit.setBlocks(region(world, plot.minX(), plot.groundY() + 1, plot.minZ(), plot.maxX(), plot.groundY() + 20, plot.maxZ()), BlockTypes.AIR.getDefaultState());
            edit.setBlocks(region(world, plot.minX() - 1, plot.groundY(), plot.minZ() - 1, plot.maxX() + 1, plot.groundY(), plot.minZ() - 1), border.getDefaultState());
            edit.setBlocks(region(world, plot.minX() - 1, plot.groundY(), plot.maxZ() + 1, plot.maxX() + 1, plot.groundY(), plot.maxZ() + 1), border.getDefaultState());
            edit.setBlocks(region(world, plot.minX() - 1, plot.groundY(), plot.minZ(), plot.minX() - 1, plot.groundY(), plot.maxZ()), border.getDefaultState());
            edit.setBlocks(region(world, plot.maxX() + 1, plot.groundY(), plot.minZ(), plot.maxX() + 1, plot.groundY(), plot.maxZ()), border.getDefaultState());
            int floors=floorCount(),height=floorHeight();BlockType upperFloor=blockType("plots.floor-material",Material.SMOOTH_STONE);for(int floor=1;floor<floors;floor++){int y=plot.groundY()+floor*height;edit.setBlocks(region(world,plot.minX(),y,plot.minZ(),plot.maxX(),y,plot.maxZ()),upperFloor.getDefaultState());}
        } catch (MaxChangedBlocksException exception) {
            throw new IllegalStateException("WorldEdit could not generate plot #" + plot.id(), exception);
        }
    }

    public void reset(PlotData plot) { buildPlot(plot); }

    private CuboidRegion region(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new CuboidRegion(BukkitAdapter.adapt(world), BlockVector3.at(minX, minY, minZ), BlockVector3.at(maxX, maxY, maxZ));
    }

    private BlockType blockType(String path, Material fallback) {
        Material material = Material.matchMaterial(plugin.getConfig().getString(path, fallback.name()));
        BlockType type = BlockTypes.get((material == null ? fallback : material).getKey().toString());
        return type == null ? BlockTypes.STONE : type;
    }

    public PlotData at(Location location) { return store.all().stream().filter(plot -> plot.contains(location)).findFirst().orElse(null); }
    public PlotData owned(UUID owner) { return store.byOwner(owner); }
    public PlotData available() { return store.available(); }
    public World world(PlotData plot) { return Bukkit.getWorld(plot.worldName()); }
    public int floorCount(){return Math.max(1,plugin.getConfig().getInt("plots.floors",1));}
    public int floorHeight(){return Math.max(4,plugin.getConfig().getInt("plots.floor-height",5));}
    public int floorY(PlotData plot,int floor){return plot.groundY()+Math.max(0,floor)*floorHeight();}
    public boolean isFloorY(PlotData plot,int y){for(int floor=0;floor<floorCount();floor++)if(floorY(plot,floor)==y)return true;return false;}

    public boolean isManagedWorld(World world) {
        return world != null && store.all().stream().anyMatch(plot -> plot.worldName().equals(world.getName()));
    }

    public boolean canBuild(UUID playerId, Location location) {
        if (location.getWorld() == null || !isManagedWorld(location.getWorld())) return true;
        PlotData plot = at(location);
        return plot != null && playerId.equals(plot.ownerId());
    }

    public boolean samePlot(Location first, Location second) {
        PlotData a = at(first);
        PlotData b = at(second);
        return a != null && b != null && a.id() == b.id() && a.worldName().equals(b.worldName());
    }
}
