package com.example.lighteco;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.*;

public final class Listeners implements Listener {

    private final LightEco plugin;
    private final Set<UUID> giveBack = new HashSet<>();
    private final Map<UUID, Long> warned = new HashMap<>();
    // 置かれた鉱石・原木（置いて壊す不正対策。再起動で消える軽量版）
    private final Map<UUID, Set<Long>> placed = new HashMap<>();

    public Listeners(LightEco plugin) {
        this.plugin = plugin;
    }

    // ---------- 参加 / ログインボーナス ----------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        Data d = plugin.data;
        d.names.put(id, p.getName());

        if (!d.money.containsKey(id)) {
            d.set(id, plugin.getConfig().getLong("start-money", 1000));
            plugin.msg(p, "<yellow>ようこそ！ <white>/claim</white> でメニューアイテムを受け取れます。");
        }

        String today = LocalDate.now().toString();
        String last = d.lastLogin.get(id);
        if (!today.equals(last)) {
            int streak = LocalDate.now().minusDays(1).toString().equals(last) ? d.streak.getOrDefault(id, 0) + 1 : 1;
            var c = plugin.getConfig();
            long bonus = c.getLong("login-bonus.base", 500)
                    + c.getLong("login-bonus.streak-step", 100) * Math.min(streak - 1, c.getInt("login-bonus.max-streak-days", 6));
            d.lastLogin.put(id, today);
            d.streak.put(id, streak);
            d.add(id, bonus);
            plugin.msg(p, "<gold>ログインボーナス！ <green>+" + d.fmt(bonus) + " <gray>(連続 " + streak + " 日)");
        }
    }

    // ---------- メニューアイテム ----------
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (e.getHand() == EquipmentSlot.HAND
                && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                && plugin.isMenuItem(e.getItem())) {
            e.setCancelled(true);
            plugin.gui.openMain(p);
            return;
        }

        // 他人の土地のチェスト・ドアなどを守る
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b != null && b.getType().isInteractable() && !plugin.canBuild(p, b)) {
                e.setUseInteractedBlock(Event.Result.DENY);
                deny(p);
            }
        } else if (e.getAction() == Action.PHYSICAL) {
            Block b = e.getClickedBlock();
            if (b != null && b.getType() == Material.FARMLAND && !plugin.canBuild(p, b)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.isMenuItem(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        boolean had = e.getDrops().removeIf(plugin::isMenuItem);
        if (had) giveBack.add(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (giveBack.remove(p.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> p.getInventory().addItem(plugin.menuItem()));
        }
    }

    // ---------- メニュー操作 ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Gui.Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getSlot();

        switch (h.type) {
            case "main" -> {
                switch (slot) {
                    case 10 -> plugin.gui.openMain(p);
                    case 12 -> plugin.gui.openGamble(p);
                    case 14 -> plugin.gui.openJobs(p);
                    case 16 -> {
                        p.closeInventory();
                        plugin.claimHere(p);
                    }
                    case 22 -> {
                        p.closeInventory();
                        p.performCommand("nation info");
                    }
                    default -> {
                    }
                }
            }
            case "jobs" -> {
                if (slot == 22) {
                    plugin.gui.openMain(p);
                } else if (slot >= 10 && slot < 10 + Jobs.IDS.size()) {
                    String job = Jobs.IDS.get(slot - 10);
                    Set<String> act = plugin.jobs.active(p.getUniqueId());
                    if (act.remove(job)) {
                        plugin.msg(p, Jobs.label(job) + " を退職しました。");
                    } else if (act.size() >= plugin.getConfig().getInt("jobs.max-active", 2)) {
                        plugin.msg(p, "<red>同時に就ける職業は " + plugin.getConfig().getInt("jobs.max-active", 2) + " つまでです。");
                    } else {
                        act.add(job);
                        plugin.msg(p, "<green>" + Jobs.label(job) + " に就職しました！");
                    }
                    plugin.data.touch();
                    plugin.gui.openJobs(p);
                }
            }
            case "gamble" -> {
                long[] bets = {100, 1000, 10000};
                if (slot == 22) {
                    plugin.gui.openMain(p);
                } else if (slot >= 10 && slot <= 12) {
                    plugin.gamble.play(p, "coin", bets[slot - 10]);
                } else if (slot >= 14 && slot <= 16) {
                    plugin.gamble.play(p, "slot", bets[slot - 14]);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Gui.Holder) e.setCancelled(true);
    }

    // ---------- 土地の保護 ----------
    private void deny(Player p) {
        long now = System.currentTimeMillis();
        if (now - warned.getOrDefault(p.getUniqueId(), 0L) > 2000) {
            warned.put(p.getUniqueId(), now);
            plugin.msg(p, "<red>ここは他の人の土地です。");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakProtect(BlockBreakEvent e) {
        if (!plugin.canBuild(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceProtect(BlockPlaceEvent e) {
        if (!plugin.canBuild(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (!plugin.canBuild(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!plugin.canBuild(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.data.landOwner(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.data.landOwner(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4) != null);
    }

    // ---------- 職業の報酬 ----------
    private static long key(Block b) {
        return ((long) (b.getX() & 0x3FFFFFF) << 38) | ((long) (b.getZ() & 0x3FFFFFF) << 12) | (b.getY() & 0xFFF);
    }

    private static boolean tracked(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS || Tag.LOGS.isTagged(m);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceTrack(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!tracked(b.getType())) return;
        Set<Long> set = placed.computeIfAbsent(b.getWorld().getUID(), k -> new HashSet<>());
        if (set.size() > 20000) set.clear();
        set.add(key(b));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakReward(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        Material m = b.getType();

        if (Jobs.isCrop(b)) {
            plugin.jobs.reward(p, "farmer", 5);
            return;
        }
        if (!tracked(m)) return;

        Set<Long> set = placed.get(b.getWorld().getUID());
        if (set != null && set.remove(key(b))) return; // 置かれたブロックは対象外

        double ore = Jobs.mineBase(m);
        if (ore > 0) plugin.jobs.reward(p, "miner", ore);
        else if (Tag.LOGS.isTagged(m)) plugin.jobs.reward(p, "woodcutter", 3);
    }

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        LivingEntity ent = e.getEntity();
        Player killer = ent.getKiller();
        if (killer == null || !(ent instanceof Enemy)) return;
        double hp = ent.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null ? 20
                : ent.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        plugin.jobs.reward(killer, "hunter", hp >= 100 ? 500 : 15);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.jobs.reward(e.getPlayer(), "fisher", 20);
        }
    }
}
