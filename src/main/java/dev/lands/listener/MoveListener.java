package dev.lands.listener;

import dev.lands.LandsPlugin;
import dev.lands.model.Land;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MoveListener implements Listener {
    private final LandsPlugin plugin;

    public MoveListener(LandsPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if ((e.getFrom().getBlockX() >> 4) == (e.getTo().getBlockX() >> 4)
                && (e.getFrom().getBlockZ() >> 4) == (e.getTo().getBlockZ() >> 4)
                && e.getFrom().getWorld() == e.getTo().getWorld()) return;
        Land from = plugin.lands().getLandAt(e.getFrom());
        Land to = plugin.lands().getLandAt(e.getTo());
        if (from == to) return;
        Player p = e.getPlayer();
        if (to != null && to.getBanned().contains(p.getUniqueId()) && !p.hasPermission("lands.admin")) {
            e.setCancelled(true);
            p.sendMessage("§cあなたは「" + to.getName() + "」への立ち入りを禁止されています");
            return;
        }
        if (!plugin.getConfig().getBoolean("show-enter-title", true)) return;
        if (to != null) p.sendTitle("§a" + to.getName(), to.isMember(p.getUniqueId()) ? "§7おかえりなさい" : "§7に入りました", 5, 30, 10);
        else p.sendTitle("", "§7荒野に出ました", 5, 25, 10);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        onMove(e);
    }
}
