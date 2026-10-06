package com.example.lighteco;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** 全データの保持と data.yml への保存。 */
public final class Data {

    public static final class Nation {
        public String name;
        public UUID leader;
        public final Set<UUID> members = new LinkedHashSet<>();
        public long bank;
    }

    private final LightEco plugin;
    private final File file;

    public final Map<UUID, Long> money = new HashMap<>();
    public final Map<UUID, String> names = new HashMap<>();
    public final Map<UUID, Integer> streak = new HashMap<>();
    public final Map<UUID, String> lastLogin = new HashMap<>();
    public final Map<UUID, Set<UUID>> trusted = new HashMap<>();
    public final Map<UUID, Set<String>> activeJobs = new HashMap<>();
    public final Map<UUID, Map<String, Integer>> jobXp = new HashMap<>();
    public final Map<String, Nation> nations = new HashMap<>();   // キーは小文字の名前
    public final Map<UUID, String> nationOf = new HashMap<>();    // プレイヤー -> 小文字の国名

    // world -> (chunkKey -> owner)
    private final Map<String, Map<Long, UUID>> lands = new HashMap<>();
    private final Map<UUID, Integer> landCount = new HashMap<>();

    private boolean dirty;

    public Data(LightEco plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------- お金 ----------
    public long get(UUID id) {
        return money.getOrDefault(id, 0L);
    }

    public void add(UUID id, long amount) {
        money.merge(id, amount, Long::sum);
        dirty = true;
    }

    public void set(UUID id, long amount) {
        money.put(id, Math.max(0, amount));
        dirty = true;
    }

    public boolean take(UUID id, long amount) {
        long cur = get(id);
        if (cur < amount) return false;
        money.put(id, cur - amount);
        dirty = true;
        return true;
    }

    public String fmt(long v) {
        return String.format("%,d", v) + plugin.getConfig().getString("currency", "円");
    }

    public String nameOf(UUID id) {
        String n = names.get(id);
        if (n == null) n = Bukkit.getOfflinePlayer(id).getName();
        return n == null ? "不明" : n;
    }

    // ---------- 土地 ----------
    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public UUID landOwner(String world, int cx, int cz) {
        Map<Long, UUID> m = lands.get(world);
        return m == null ? null : m.get(chunkKey(cx, cz));
    }

    public int landCount(UUID id) {
        return landCount.getOrDefault(id, 0);
    }

    public void claimLand(UUID id, String world, int cx, int cz) {
        lands.computeIfAbsent(world, k -> new HashMap<>()).put(chunkKey(cx, cz), id);
        landCount.merge(id, 1, Integer::sum);
        dirty = true;
    }

    public void unclaimLand(UUID id, String world, int cx, int cz) {
        Map<Long, UUID> m = lands.get(world);
        if (m != null && m.remove(chunkKey(cx, cz)) != null) {
            landCount.merge(id, -1, Integer::sum);
            dirty = true;
        }
    }

    public long landPrice(int owned) {
        var c = plugin.getConfig();
        return c.getLong("land.base-price", 1000) + c.getLong("land.step", 500) * owned;
    }

    public void touch() {
        dirty = true;
    }

    // ---------- 国 ----------
    public Nation nationOfPlayer(UUID id) {
        String k = nationOf.get(id);
        return k == null ? null : nations.get(k);
    }

    public boolean sameNation(UUID a, UUID b) {
        String x = nationOf.get(a);
        return x != null && x.equals(nationOf.get(b));
    }

    // ---------- 保存 / 読み込み ----------
    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        read(y, "money", (k, s) -> money.put(k, s.getLong("money." + k)));
        read(y, "names", (k, s) -> names.put(k, y.getString("names." + k)));
        read(y, "streak", (k, s) -> streak.put(k, y.getInt("streak." + k)));
        read(y, "last-login", (k, s) -> lastLogin.put(k, y.getString("last-login." + k)));

        read(y, "trusted", (k, s) -> {
            Set<UUID> set = new HashSet<>();
            for (String t : y.getStringList("trusted." + k)) set.add(UUID.fromString(t));
            trusted.put(k, set);
        });

        read(y, "jobs.active", (k, s) -> activeJobs.put(k, new HashSet<>(y.getStringList("jobs.active." + k))));
        ConfigurationSection xp = y.getConfigurationSection("jobs.xp");
        if (xp != null) {
            for (String k : xp.getKeys(false)) {
                ConfigurationSection s = xp.getConfigurationSection(k);
                if (s == null) continue;
                Map<String, Integer> m = new HashMap<>();
                for (String job : s.getKeys(false)) m.put(job, s.getInt(job));
                jobXp.put(UUID.fromString(k), m);
            }
        }

        for (String line : y.getStringList("lands")) {
            String[] p = line.split("\\|");
            if (p.length != 4) continue;
            claimLand(UUID.fromString(p[0]), p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        }

        ConfigurationSection ns = y.getConfigurationSection("nations");
        if (ns != null) {
            for (String key : ns.getKeys(false)) {
                ConfigurationSection s = ns.getConfigurationSection(key);
                if (s == null) continue;
                Nation n = new Nation();
                n.name = s.getString("name", key);
                n.leader = UUID.fromString(Objects.requireNonNull(s.getString("leader")));
                n.bank = s.getLong("bank");
                for (String m : s.getStringList("members")) {
                    UUID id = UUID.fromString(m);
                    n.members.add(id);
                    nationOf.put(id, key);
                }
                nations.put(key, n);
            }
        }
        dirty = false;
    }

    private interface Reader {
        void accept(UUID id, YamlConfiguration y);
    }

    private void read(YamlConfiguration y, String path, Reader r) {
        ConfigurationSection s = y.getConfigurationSection(path);
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            try {
                r.accept(UUID.fromString(k), y);
            } catch (IllegalArgumentException ignored) {
                // 壊れた行は読み飛ばす
            }
        }
    }

    /** dirty のときだけ保存。sync=true は停止時用。 */
    public void save(boolean sync) {
        if (!dirty && !sync) return;
        dirty = false;

        YamlConfiguration y = new YamlConfiguration();
        money.forEach((k, v) -> y.set("money." + k, v));
        names.forEach((k, v) -> y.set("names." + k, v));
        streak.forEach((k, v) -> y.set("streak." + k, v));
        lastLogin.forEach((k, v) -> y.set("last-login." + k, v));
        trusted.forEach((k, v) -> {
            if (!v.isEmpty()) y.set("trusted." + k, v.stream().map(UUID::toString).toList());
        });
        activeJobs.forEach((k, v) -> y.set("jobs.active." + k, new ArrayList<>(v)));
        jobXp.forEach((k, m) -> m.forEach((job, xp) -> y.set("jobs.xp." + k + "." + job, xp)));

        List<String> landLines = new ArrayList<>();
        lands.forEach((world, m) -> m.forEach((key, owner) ->
                landLines.add(owner + "|" + world + "|" + (int) (key >> 32) + "|" + (int) (long) key)));
        y.set("lands", landLines);

        nations.forEach((key, n) -> {
            y.set("nations." + key + ".name", n.name);
            y.set("nations." + key + ".leader", n.leader.toString());
            y.set("nations." + key + ".bank", n.bank);
            y.set("nations." + key + ".members", n.members.stream().map(UUID::toString).toList());
        });

        String text = y.saveToString();
        Runnable write = () -> {
            try {
                plugin.getDataFolder().mkdirs();
                File tmp = new File(plugin.getDataFolder(), "data.yml.tmp");
                Files.writeString(tmp.toPath(), text, StandardCharsets.UTF_8);
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().severe("data.yml の保存に失敗: " + e.getMessage());
            }
        };
        if (sync) write.run();
        else Bukkit.getScheduler().runTaskAsynchronously(plugin, write);
    }
}
