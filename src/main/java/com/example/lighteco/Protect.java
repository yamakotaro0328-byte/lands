package com.example.lighteco;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 土地保護の追加分（ホッパー・ピストン・液体・火・エンティティ・モブによる破壊）。 */
public final class Protect implements Listener {

    private final LightEco plugin;
    private final Map<UUID, Long> warned = new HashMap<>();

    public Protect(LightEco plugin) {
        this.plugin = plugin;
    }

    private UUID owner(Location l) {
        return l == null || l.getWorld() == null ? null
                : plugin.data.landOwner(l.getWorld().getName(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
    }

    private UUID owner(Block b) {
        return plugin.data.landOwner(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4);
    }

    /** to が誰かの土地で、from と所有者が違うなら true（境界越え）。 */
    private boolean crosses(UUID from, UUID to) {
        return to != null && !to.equals(from);
    }

    private void deny(Player p) {
        long now = System.currentTimeMillis();
        if (now - warned.getOrDefault(p.getUniqueId(), 0L) > 2000) {
            warned.put(p.getUniqueId(), now);
            plugin.msg(p, "<red>ここは他の人の土地です。");
        }
    }

    private static Player playerOf(Entity e) {
        if (e instanceof Player p) return p;
        if (e instanceof Projectile pr) {
            ProjectileSource s = pr.getShooter();
            if (s instanceof Player p) return p;
        }
        return null;
    }

    // ---------- ホッパー等によるアイテム移動 ----------
    @EventHandler(ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent e) {
        UUID src = owner(e.getSource().getLocation());
        if (src == null) return;
        if (!src.equals(owner(e.getDestination().getLocation()))) e.setCancelled(true);
    }

    // ---------- ピストン ----------
    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (pistonCrosses(e.getBlock(), e.getBlocks(), true)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (pistonCrosses(e.getBlock(), e.getBlocks(), false)) e.setCancelled(true);
    }

    private boolean pistonCrosses(Block piston, List<Block> blocks, boolean extend) {
        UUID src = owner(piston);
        BlockFace face = piston.getBlockData() instanceof Directional d ? d.getFacing() : BlockFace.SELF;
        BlockFace move = extend ? face : face.getOppositeFace();
        if (extend && crosses(src, owner(piston.getRelative(face)))) return true; // ピストンヘッド
        for (Block b : blocks) {
            if (crosses(src, owner(b)) || crosses(src, owner(b.getRelative(move)))) return true;
        }
        return false;
    }

    // ---------- 液体・火 ----------
    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (crosses(owner(e.getBlock()), owner(e.getToBlock()))) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (owner(e.getBlock()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        var c = e.getCause();
        if ((c == BlockIgniteEvent.IgniteCause.SPREAD || c == BlockIgniteEvent.IgniteCause.LAVA
                || c == BlockIgniteEvent.IgniteCause.LIGHTNING) && owner(e.getBlock()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (e.getSource().getType().name().endsWith("FIRE") && owner(e.getBlock()) != null) e.setCancelled(true);
    }

    // ---------- モブによるブロック変更（エンダーマン・ウィザー・ラヴェジャー・ドア破壊など） ----------
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Entity en = e.getEntity();
        if ((en instanceof Enemy || en instanceof WitherSkull) && owner(e.getBlock()) != null) e.setCancelled(true);
    }

    // ---------- エンティティ ----------
    private static boolean protectedEntity(Entity en) {
        return !(en instanceof Player) && !(en instanceof Enemy);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player p = playerOf(e.getDamager());
        if (p == null || !protectedEntity(e.getEntity())) return;
        if (!plugin.canBuild(p, e.getEntity().getLocation())) {
            e.setCancelled(true);
            deny(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        Player p = e.getRemover() == null ? null : playerOf(e.getRemover());
        if (p == null) {
            // プレイヤー以外（爆発・モブなど）による額縁・絵画の破壊
            if (owner(e.getEntity().getLocation()) != null) e.setCancelled(true);
        } else if (!plugin.canBuild(p, e.getEntity().getLocation())) {
            e.setCancelled(true);
            deny(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent e) {
        Player p = e.getAttacker() == null ? null : playerOf(e.getAttacker());
        if (p != null && !plugin.canBuild(p, e.getVehicle().getLocation())) {
            e.setCancelled(true);
            deny(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Entity en = e.getRightClicked();
        boolean guarded = en instanceof Hanging || en instanceof ArmorStand
                || (en instanceof InventoryHolder && !(en instanceof Villager) && !(en instanceof WanderingTrader))
                || en instanceof Animals;
        if (guarded && !plugin.canBuild(e.getPlayer(), en.getLocation())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (!plugin.canBuild(e.getPlayer(), e.getRightClicked().getLocation())) {
            e.setCancelled(true);
            deny(e.getPlayer());
        }
    }

}
