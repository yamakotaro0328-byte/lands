package com.example.lighteco;

import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;

import java.util.*;

/** 職業の定義と報酬計算。 */
public final class Jobs {

    public static final List<String> IDS = List.of("hunter", "miner", "farmer", "woodcutter", "fisher");

    private final LightEco plugin;

    public Jobs(LightEco plugin) {
        this.plugin = plugin;
    }

    public static String label(String id) {
        return switch (id) {
            case "hunter" -> "ハンター";
            case "miner" -> "マイナー";
            case "farmer" -> "農家";
            case "woodcutter" -> "木こり";
            case "fisher" -> "釣り人";
            default -> id;
        };
    }

    public static Material icon(String id) {
        return switch (id) {
            case "hunter" -> Material.IRON_SWORD;
            case "miner" -> Material.IRON_PICKAXE;
            case "farmer" -> Material.IRON_HOE;
            case "woodcutter" -> Material.IRON_AXE;
            default -> Material.FISHING_ROD;
        };
    }

    public static String hint(String id) {
        return switch (id) {
            case "hunter" -> "敵モブを倒すと報酬";
            case "miner" -> "鉱石を掘ると報酬";
            case "farmer" -> "育った作物を収穫すると報酬";
            case "woodcutter" -> "原木を切ると報酬";
            default -> "魚を釣ると報酬";
        };
    }

    // ---------- レベル ----------
    public int xp(Player p, String job) {
        return plugin.data.jobXp.getOrDefault(p.getUniqueId(), Map.of()).getOrDefault(job, 0);
    }

    public static int levelFromXp(int xp) {
        return Math.min(50, 1 + (int) Math.sqrt(xp / 25.0));
    }

    public int level(Player p, String job) {
        return levelFromXp(xp(p, job));
    }

    public Set<String> active(UUID id) {
        return plugin.data.activeJobs.computeIfAbsent(id, k -> new HashSet<>());
    }

    // ---------- 報酬 ----------
    public void reward(Player p, String job, double base) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        UUID id = p.getUniqueId();
        if (!active(id).contains(job)) return;

        Map<String, Integer> xpMap = plugin.data.jobXp.computeIfAbsent(id, k -> new HashMap<>());
        int before = xpMap.getOrDefault(job, 0);
        int oldLevel = levelFromXp(before);
        xpMap.put(job, before + 1);
        int level = levelFromXp(before + 1);

        double mult = plugin.getConfig().getDouble("jobs.reward-multiplier", 1.0);
        long pay = Math.max(1, Math.round(base * (1 + 0.05 * (level - 1)) * mult));
        plugin.data.add(id, pay);
        p.sendActionBar(Component.text("+" + plugin.data.fmt(pay) + " (" + label(job) + " Lv" + level + ")"));

        if (level > oldLevel) {
            plugin.msg(p, "<green>" + label(job) + " が <yellow>Lv" + level + "</yellow> に上がりました！");
        }
    }

    public static double mineBase(Material m) {
        if (m == Material.ANCIENT_DEBRIS) return 100;
        String n = m.name();
        if (!n.endsWith("_ORE")) return 0;
        if (n.contains("DIAMOND")) return 50;
        if (n.contains("EMERALD")) return 40;
        if (n.contains("GOLD")) return 10;
        if (n.contains("LAPIS")) return 8;
        if (n.contains("IRON") || n.contains("REDSTONE") || n.contains("QUARTZ")) return 6;
        return 3; // 石炭・銅など
    }

    public static boolean isCropType(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES
                || m == Material.BEETROOTS || m == Material.NETHER_WART;
    }

    public static boolean isCrop(Block b) {
        if (!isCropType(b.getType())) return false;
        return b.getBlockData() instanceof Ageable a && a.getAge() >= a.getMaximumAge();
    }
}
