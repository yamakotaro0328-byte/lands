package dev.lands.listener;

import dev.lands.LandsPlugin;
import dev.lands.model.Flag;
import dev.lands.model.Land;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.projectiles.ProjectileSource;

public class ProtectionListener implements Listener {
    private final LandsPlugin plugin;

    public ProtectionListener(LandsPlugin plugin) { this.plugin = plugin; }

    private Land at(Location l) { return plugin.lands().getLandAt(l); }

    /** プレイヤーがその場所で flag に関わる行動をできるか */
    private boolean allowed(Player p, Location loc, Flag flag) {
        Land land = at(loc);
        if (land == null || land.isMember(p.getUniqueId()) || p.hasPermission("lands.admin")) return true;
        if (land.getFlag(flag)) return true;
        p.sendMessage("§c" + land.getName() + " の土地では許可されていません");
        return false;
    }

    private static Player playerOf(Entity e) {
        if (e instanceof Player p) return p;
        if (e instanceof Projectile pr) {
            ProjectileSource s = pr.getShooter();
            if (s instanceof Player p) return p;
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent e) {
        if (!allowed(e.getPlayer(), e.getBlock().getLocation(), Flag.VISITOR_BUILD)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent e) {
        if (!allowed(e.getPlayer(), e.getBlock().getLocation(), Flag.VISITOR_BUILD)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (!allowed(e.getPlayer(), e.getBlock().getLocation(), Flag.VISITOR_BUILD)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!allowed(e.getPlayer(), e.getBlock().getLocation(), Flag.VISITOR_BUILD)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        if (b == null) return;
        if (e.getAction() == Action.PHYSICAL) {
            // 作物踏み荒らし
            if (b.getType().name().equals("FARMLAND") && !allowed(e.getPlayer(), b.getLocation(), Flag.VISITOR_BUILD))
                e.setCancelled(true);
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Flag flag;
        if (b.getState() instanceof Container) flag = Flag.VISITOR_CONTAINER;
        else if (b.getType().isInteractable()) flag = Flag.VISITOR_INTERACT;
        else return;
        if (!allowed(e.getPlayer(), b.getLocation(), flag)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Entity en = e.getRightClicked();
        if (en instanceof ItemFrame || en instanceof ArmorStand || en instanceof Vehicle && en instanceof org.bukkit.inventory.InventoryHolder) {
            if (!allowed(e.getPlayer(), en.getLocation(), Flag.VISITOR_CONTAINER)) e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player attacker = playerOf(e.getDamager());
        if (attacker == null) return;
        Entity victim = e.getEntity();
        Land land = at(victim.getLocation());
        if (land == null) return;
        if (victim instanceof Player v) {
            if (!v.equals(attacker) && !land.getFlag(Flag.PVP)) {
                attacker.sendMessage("§cこの土地ではPvPが無効です");
                e.setCancelled(true);
            }
        } else if (victim instanceof Monster) {
            // モンスターは誰でも倒せる
        } else if (victim instanceof Animals || victim instanceof Villager || victim instanceof Golem) {
            if (!allowed(attacker, victim.getLocation(), Flag.ANIMAL_DAMAGE)) e.setCancelled(true);
        } else if (victim instanceof Hanging || victim instanceof ArmorStand) {
            if (!allowed(attacker, victim.getLocation(), Flag.VISITOR_BUILD)) e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        Player p = e.getRemover() == null ? null : playerOf(e.getRemover());
        if (p != null && !allowed(p, e.getEntity().getLocation(), Flag.VISITOR_BUILD)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Monster)) return;
        CreatureSpawnEvent.SpawnReason r = e.getSpawnReason();
        if (r != CreatureSpawnEvent.SpawnReason.NATURAL && r != CreatureSpawnEvent.SpawnReason.REINFORCEMENTS
                && r != CreatureSpawnEvent.SpawnReason.PATROL) return;
        Land land = at(e.getLocation());
        if (land != null && !land.getFlag(Flag.MONSTER_SPAWN)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> { Land l = at(b.getLocation()); return l != null && !l.getFlag(Flag.EXPLOSIONS); });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> { Land l = at(b.getLocation()); return l != null && !l.getFlag(Flag.EXPLOSIONS); });
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (e.getSource().getType().name().contains("FIRE")) {
            Land l = at(e.getBlock().getLocation());
            if (l != null && !l.getFlag(Flag.FIRE_SPREAD)) e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        Land l = at(e.getBlock().getLocation());
        if (l != null && !l.getFlag(Flag.FIRE_SPREAD)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (e.getCause() != BlockIgniteEvent.IgniteCause.SPREAD && e.getCause() != BlockIgniteEvent.IgniteCause.LAVA) return;
        Land l = at(e.getBlock().getLocation());
        if (l != null && !l.getFlag(Flag.FIRE_SPREAD)) e.setCancelled(true);
    }

    /** 土地外からのピストン・液体流入を防ぐ */
    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        Land to = at(e.getToBlock().getLocation());
        if (to != null && to != at(e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        Land src = at(e.getBlock().getLocation());
        for (Block b : e.getBlocks()) {
            Land to = at(b.getRelative(e.getDirection()).getLocation());
            if (to != null && to != src) { e.setCancelled(true); return; }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        Land src = at(e.getBlock().getLocation());
        for (Block b : e.getBlocks()) {
            Land from = at(b.getLocation());
            if (from != null && from != src) { e.setCancelled(true); return; }
        }
    }
}
