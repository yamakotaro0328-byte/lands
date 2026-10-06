package dev.lands.gui;

import dev.lands.LandsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** GUIから文字入力が必要な時（土地名など）にチャットで受け付ける */
public class ChatInput implements Listener {
    private final LandsPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatInput(LandsPlugin plugin) { this.plugin = plugin; }

    public void ask(Player p, String prompt, Consumer<String> callback) {
        p.closeInventory();
        pending.put(p.getUniqueId(), callback);
        LandsPlugin.msg(p, prompt);
        LandsPlugin.msg(p, "§7チャットに入力してください（「cancel」でキャンセル）");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent e) {
        Consumer<String> cb = pending.remove(e.getPlayer().getUniqueId());
        if (cb == null) return;
        e.setCancelled(true);
        String text = e.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancel")) {
                LandsPlugin.msg(e.getPlayer(), "§cキャンセルしました");
                return;
            }
            cb.accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { pending.remove(e.getPlayer().getUniqueId()); }
}
