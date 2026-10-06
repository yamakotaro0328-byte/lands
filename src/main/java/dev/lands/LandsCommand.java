package dev.lands;

import dev.lands.gui.MainMenu;
import dev.lands.gui.MapMenu;
import dev.lands.model.ChunkPos;
import dev.lands.model.Land;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** 基本は /lands でGUIを開くだけ。よく使う操作はショートカットとして用意 */
public class LandsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = List.of("claim", "unclaim", "map", "spawn", "bank", "reload", "taxnow");
    private final LandsPlugin plugin;

    public LandsCommand(LandsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lands.admin")) { LandsPlugin.msg(sender, "§c権限がありません"); return true; }
            plugin.reloadConfig();
            plugin.lands().load();
            LandsPlugin.msg(sender, "§aリロードしました");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("taxnow")) {
            if (!sender.hasPermission("lands.admin")) { LandsPlugin.msg(sender, "§c権限がありません"); return true; }
            plugin.taxTask().collectAll();
            LandsPlugin.msg(sender, "§a全ての土地から税金を徴収しました");
            return true;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("プレイヤーのみ実行できます"); return true; }
        if (args.length == 0) { new MainMenu(p).open(); return true; }
        switch (args[0].toLowerCase()) {
            case "claim" -> LandActions.claim(p, ChunkPos.of(p.getLocation()));
            case "unclaim" -> LandActions.unclaim(p, ChunkPos.of(p.getLocation()));
            case "map" -> new MapMenu(p).open();
            case "bank" -> new dev.lands.gui.BankMenu(p).open();
            case "spawn" -> { Land l = LandActions.requireLand(p); if (l != null) LandActions.teleport(p, l); }
            default -> new MainMenu(p).open();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) return SUBS.stream().filter(x -> x.startsWith(args[0].toLowerCase())).toList();
        return List.of();
    }
}
