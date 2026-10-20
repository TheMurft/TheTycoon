package dev.themurft.thetycoon.service;

import dev.themurft.thetycoon.TheTycoonPlugin;
import dev.themurft.thetycoon.model.MinionData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MinionStore {
    private final TheTycoonPlugin plugin;
    private final File file;
    private final List<MinionData> minions = new ArrayList<>();
    private boolean dirty;
    public MinionStore(TheTycoonPlugin plugin) { this.plugin = plugin; this.file = new File(plugin.getDataFolder(), "minions.yml"); }
    public void load() {
        if(file.exists())try{File backup=new File(plugin.getDataFolder(),"minions-backup.yml");Files.copy(file.toPath(),backup.toPath(),StandardCopyOption.REPLACE_EXISTING);}catch(IOException exception){plugin.getLogger().warning("Could not create minion backup: "+exception.getMessage());}
        minions.clear();dirty=false; YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var root = yaml.getConfigurationSection("minions"); if (root == null) return;
        for (String key : root.getKeys(false)) try {
            String p = "minions." + key; var world = Bukkit.getWorld(yaml.getString(p + ".world", "")); if (world == null) continue;
            String entity = yaml.getString(p + ".entity");
            minions.add(new MinionData(UUID.fromString(key), UUID.fromString(yaml.getString(p + ".owner")),
                    yaml.getString(p + ".type", "COBBLESTONE"),
                    new Location(world, yaml.getDouble(p + ".x"), yaml.getDouble(p + ".y"), yaml.getDouble(p + ".z"), (float) yaml.getDouble(p + ".yaw"), 0),
                    entity == null ? null : UUID.fromString(entity), yaml.getInt(p + ".level", 1), yaml.getInt(p + ".stored"),
                    yaml.getLong(p + ".next-production-at", System.currentTimeMillis())));
        } catch (Exception exception) { plugin.getLogger().warning("Ignored invalid minion: " + key); }
    }
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (MinionData m : minions) { String p="minions."+m.id(); Location l=m.location();
            yaml.set(p+".owner",m.ownerId().toString()); yaml.set(p+".type",m.type()); yaml.set(p+".world",l.getWorld().getName());
            yaml.set(p+".x",l.getX()); yaml.set(p+".y",l.getY()); yaml.set(p+".z",l.getZ()); yaml.set(p+".yaw",l.getYaw());
            yaml.set(p+".entity",m.entityId()==null?null:m.entityId().toString()); yaml.set(p+".level",m.level()); yaml.set(p+".stored",m.stored()); yaml.set(p+".next-production-at",m.nextProductionAt()); }
        try { yaml.save(file);dirty=false; } catch(IOException e){ plugin.getLogger().severe("Could not save minions.yml: "+e.getMessage()); }
    }
    public void markDirty(){dirty=true;}
    public void saveIfDirty(){if(dirty)save();}
    public List<MinionData> all(){ return minions; }
    public List<MinionData> owned(UUID owner){ return minions.stream().filter(m->m.ownerId().equals(owner)).toList(); }
    public MinionData get(UUID id){ return minions.stream().filter(m->m.id().equals(id)).findFirst().orElse(null); }
    public void add(MinionData m){ minions.add(m);markDirty(); }
    public void remove(MinionData m){ minions.remove(m);markDirty(); }
}
