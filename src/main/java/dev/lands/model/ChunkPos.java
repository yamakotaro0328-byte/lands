package dev.lands.model;

import org.bukkit.Chunk;
import org.bukkit.Location;

public record ChunkPos(String world, int x, int z) {
    public static ChunkPos of(Chunk c) {
        return new ChunkPos(c.getWorld().getName(), c.getX(), c.getZ());
    }

    public static ChunkPos of(Location l) {
        return new ChunkPos(l.getWorld().getName(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
    }

    public static ChunkPos parse(String s) {
        String[] p = s.split(";");
        return new ChunkPos(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }

    public ChunkPos offset(int dx, int dz) {
        return new ChunkPos(world, x + dx, z + dz);
    }

    @Override
    public String toString() {
        return world + ";" + x + ";" + z;
    }
}
