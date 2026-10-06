package com.example.lighteco;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
    private final Map<UUID, Long> warned = new HashMap<>();
    // 置かれた鉱石・原木・骨粉作物の記録（チャンクに保存）
    private final Placed placed;
    private final NamespacedKey noRewardKey;
    private final Map<UUID, String> fishPos = new HashMap<>();
    private final Map<UUID, Integer> fishCount = new HashMap<>();

    public Listeners(LightEco plugin) {
        this.plugin = plugin;
        this.placed = new Placed(plugin);
        this.noRewardKey = new NamespacedKey(plugin, "noreward");
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
            plugin.msg(p, "<yellow>ようこそ！ <white>/menu</white> でメニューアイテムを受け取れます。");
        }
        loginBonus(p);
    }

    /** 今日まだ受け取っていなければログインボーナスを渡す（日付をまたいだオンライン中のプレイヤーにも定期的に呼ぶ）。 */
    public void loginBonus(Player p) {
        Data d = plugin.data;
        UUID id = p.getUniqueId();
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

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        fishPos.remove(e.getPlayer().getUniqueId());
        fishCount.remove(e.getPlayer().getUniqueId());
        plugin.gamble.forget(e.getPlayer().getUniqueId());
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

    /** メニューアイテムをチェスト等に入れられないようにする（複製防止） */
    @EventHandler(priority = EventPriority.LOW)
    public void onMenuItemMove(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Gui.Holder) return;
        var top = e.getView().getTopInventory().getType();
        if (top == org.bukkit.event.inventory.InventoryType.CRAFTING) return; // 自分のインベントリのみ
        boolean intoTop = e.getClickedInventory() == e.getView().getTopInventory();
        ItemStack hotbar = e.getHotbarButton() >= 0 ? e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) : null;
        if ((intoTop && (plugin.isMenuItem(e.getCursor()) || plugin.isMenuItem(hotbar)
                || (e.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND && plugin.isMenuItem(e.getWhoClicked().getInventory().getItemInOffHand()))))
                || (!intoTop && e.isShiftClick() && plugin.isMenuItem(e.getCurrentItem()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuItemDrag(InventoryDragEvent e) {
        if (!plugin.isMenuItem(e.getOldCursor())) return;
        int topSize = e.getView().getTopInventory().getSize();
        if (e.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING) return;
        for (int raw : e.getRawSlots()) {
            if (raw < topSize) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onFrame(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof org.bukkit.entity.ItemFrame
                && plugin.isMenuItem(e.getPlayer().getInventory().getItem(e.getHand()))) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.isMenuItem(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        boolean had = e.getDrops().removeIf(plugin::isMenuItem);
        // プレイヤーのデータに印を付ける（サーバー再起動をはさんでも返却できるように）
        if (had) e.getEntity().getPersistentDataContainer().set(plugin.menuKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (p.getPersistentDataContainer().has(plugin.menuKey, PersistentDataType.BYTE)) {
            p.getPersistentDataContainer().remove(plugin.menuKey);
            Bukkit.getScheduler().runTask(plugin, () -> {
                var left = p.getInventory().addItem(plugin.menuItem());
                left.values().forEach(it -> p.getWorld().dropItem(p.getLocation(), it));
            });
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
    private static boolean tracked(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS || Tag.LOGS.isTagged(m);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceTrack(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (tracked(b.getType())) placed.mark(b);
        else if (Jobs.isCropType(b.getType())) placed.remove(b); // 植え直したら記録をリセット
    }

    /** 骨粉で育てた作物は農家の報酬対象外にする */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent e) {
        for (var st : e.getBlocks()) {
            if (Jobs.isCropType(st.getType())) placed.mark(st.getBlock());
        }
    }

    /** ピストンで動かした鉱石・原木は「置かれたもの」扱いにする */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        pistonTrack(e.getBlock(), e.getBlocks(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        pistonTrack(e.getBlock(), e.getBlocks(), false);
    }

    private void pistonTrack(Block piston, List<Block> blocks, boolean extend) {
        if (!(piston.getBlockData() instanceof Directional d)) return;
        BlockFace move = extend ? d.getFacing() : d.getFacing().getOppositeFace();
        for (Block b : blocks) {
            if (tracked(b.getType())) {
                placed.remove(b);
                placed.mark(b.getRelative(move));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakReward(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        Material m = b.getType();

        if (Jobs.isCrop(b)) {
            if (!placed.remove(b)) plugin.jobs.reward(p, "farmer", 5);
            return;
        }
        if (!tracked(m)) return;
        if (placed.remove(b)) return; // 置かれたブロックは対象外

        double ore = Jobs.mineBase(m);
        if (ore > 0) plugin.jobs.reward(p, "miner", ore);
        else if (Tag.LOGS.isTagged(m)) plugin.jobs.reward(p, "woodcutter", 3);
    }

    /** スポナー・スポーンエッグ・スライム分裂で出たモブは報酬なし */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        switch (e.getSpawnReason()) {
            case SPAWNER, SPAWNER_EGG, SLIME_SPLIT, BUILD_WITHER, DISPENSE_EGG ->
                    e.getEntity().getPersistentDataContainer().set(noRewardKey, PersistentDataType.BYTE, (byte) 1);
            default -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        LivingEntity ent = e.getEntity();
        Player killer = ent.getKiller();
        if (killer == null || !(ent instanceof Enemy)) return;
        if (ent.getPersistentDataContainer().has(noRewardKey, PersistentDataType.BYTE)) return;
        boolean boss = ent instanceof Wither || ent instanceof EnderDragon || ent instanceof Warden || ent instanceof ElderGuardian;
        plugin.jobs.reward(killer, "hunter", boss ? 500 : 15);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player p = e.getPlayer();
        // 位置も視点もまったく変わらずに釣り続けている場合は放置釣り（AFK）とみなす
        var l = p.getLocation();
        String pos = l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," + Math.round(l.getYaw()) + "," + Math.round(l.getPitch());
        int n = pos.equals(fishPos.get(p.getUniqueId())) ? fishCount.merge(p.getUniqueId(), 1, Integer::sum) : 1;
        fishPos.put(p.getUniqueId(), pos);
        if (n == 1) fishCount.put(p.getUniqueId(), 1);
        if (n > 5) {
            if (n == 6) plugin.msg(p, "<yellow>同じ場所・同じ向きのままでは釣りの報酬が出ません。少し動いてください。");
            return;
        }
        plugin.jobs.reward(p, "fisher", 20);
    }
}
