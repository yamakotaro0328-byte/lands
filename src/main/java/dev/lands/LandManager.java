package dev.lands;

import dev.lands.model.*;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LandManager {
    private final LandsPlugin plugin;
    private final File file;
    private final Map<String, Land> lands = new LinkedHashMap<>();
    private final Map<ChunkPos, Land> chunkIndex = new HashMap<>();
    private long nextTax;

    public LandManager(LandsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lands.yml");
    }

    public long getNextTax() { return nextTax; }
    public void setNextTax(long t) { nextTax = t; }

    public Collection<Land> getLands() { return lands.values(); }

    public Land getLand(String name) { return lands.get(name.toLowerCase(Locale.ROOT)); }

    public Land getLandAt(Location loc) { return chunkIndex.get(ChunkPos.of(loc)); }

    public Land getLandAt(ChunkPos pos) { return chunkIndex.get(pos); }

    /** プレイヤーが所属している土地（1人1つまで） */
    public Land getLandOf(UUID uuid) {
        for (Land l : lands.values()) if (l.isMember(uuid)) return l;
        return null;
    }

    public List<Land> getInvitesOf(UUID uuid) {
        List<Land> list = new ArrayList<>();
        for (Land l : lands.values()) if (l.getInvites().contains(uuid)) list.add(l);
        return list;
    }

    public Land create(String name, UUID owner) {
        Land land = new Land(name, owner);
        lands.put(name.toLowerCase(Locale.ROOT), land);
        save();
        return land;
    }

    public void delete(Land land) {
        lands.remove(land.getName().toLowerCase(Locale.ROOT));
        for (ChunkPos c : land.getChunks()) chunkIndex.remove(c);
        save();
    }

    public void rename(Land land, String newName) {
        lands.remove(land.getName().toLowerCase(Locale.ROOT));
        land.setName(newName);
        lands.put(newName.toLowerCase(Locale.ROOT), land);
        save();
    }

    public void claim(Land land, ChunkPos pos) {
        land.getChunks().add(pos);
        chunkIndex.put(pos, land);
        save();
    }

    public void unclaim(Land land, ChunkPos pos) {
        land.getChunks().remove(pos);
        chunkIndex.remove(pos);
        save();
    }

    public void load() {
        lands.clear();
        chunkIndex.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        nextTax = yml.getLong("next-tax", 0);
        ConfigurationSection root = yml.getConfigurationSection("lands");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            try {
                Land land = new Land(s.getString("name"), UUID.fromString(s.getString("owner")));
                ConfigurationSection m = s.getConfigurationSection("members");
                if (m != null) for (String u : m.getKeys(false))
                    land.getMembers().put(UUID.fromString(u), Role.valueOf(m.getString(u)));
                for (String c : s.getStringList("chunks")) {
                    ChunkPos pos = ChunkPos.parse(c);
                    land.getChunks().add(pos);
                    chunkIndex.put(pos, land);
                }
                ConfigurationSection f = s.getConfigurationSection("flags");
                if (f != null) for (String fl : f.getKeys(false)) {
                    try { land.setFlag(Flag.valueOf(fl), f.getBoolean(fl)); } catch (IllegalArgumentException ignored) {}
                }
                for (String u : s.getStringList("invites")) land.getInvites().add(UUID.fromString(u));
                for (String u : s.getStringList("banned")) land.getBanned().add(UUID.fromString(u));
                land.setSpawn(s.getLocation("spawn"));
                land.setBank(s.getDouble("bank"));
                lands.put(land.getName().toLowerCase(Locale.ROOT), land);
            } catch (Exception e) {
                plugin.getLogger().warning("土地 " + key + " の読み込みに失敗: " + e.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("next-tax", nextTax);
        int i = 0;
        for (Land land : lands.values()) {
            String p = "lands." + (i++) + ".";
            yml.set(p + "name", land.getName());
            yml.set(p + "owner", land.getOwner().toString());
            land.getMembers().forEach((u, r) -> yml.set(p + "members." + u, r.name()));
            yml.set(p + "chunks", land.getChunks().stream().map(ChunkPos::toString).toList());
            for (Flag f : Flag.values()) yml.set(p + "flags." + f.name(), land.getFlag(f));
            yml.set(p + "invites", land.getInvites().stream().map(UUID::toString).toList());
            yml.set(p + "banned", land.getBanned().stream().map(UUID::toString).toList());
            yml.set(p + "spawn", land.getSpawn());
            yml.set(p + "bank", land.getBank());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("lands.yml の保存に失敗: " + e.getMessage());
        }
    }
}
