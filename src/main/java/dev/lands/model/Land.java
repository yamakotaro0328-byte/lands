package dev.lands.model;

import org.bukkit.Location;

import java.util.*;

public class Land {
    private String name;
    private UUID owner;
    private final Map<UUID, Role> members = new LinkedHashMap<>();
    private final Set<ChunkPos> chunks = new HashSet<>();
    private final EnumMap<Flag, Boolean> flags = new EnumMap<>(Flag.class);
    private final Set<UUID> invites = new HashSet<>();
    private final Set<UUID> banned = new HashSet<>();
    private Location spawn;

    public Land(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        members.put(owner, Role.OWNER);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwner() { return owner; }
    public Map<UUID, Role> getMembers() { return members; }
    public Set<ChunkPos> getChunks() { return chunks; }
    public Set<UUID> getInvites() { return invites; }
    public Set<UUID> getBanned() { return banned; }
    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }

    public Role getRole(UUID uuid) { return members.get(uuid); }
    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }

    public boolean getFlag(Flag f) { return flags.getOrDefault(f, f.def); }
    public void setFlag(Flag f, boolean v) { flags.put(f, v); }

    public void transferOwnership(UUID newOwner) {
        members.put(owner, Role.TRUSTED);
        members.put(newOwner, Role.OWNER);
        owner = newOwner;
    }
}
