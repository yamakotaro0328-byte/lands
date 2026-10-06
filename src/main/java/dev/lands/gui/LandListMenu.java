package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.Land;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Comparator;
import java.util.List;

public class LandListMenu extends PagedMenu<Land> {
    public LandListMenu(Player player) {
        super(player, "§d土地一覧");
    }

    @Override
    protected List<Land> entries() {
        return LandsPlugin.get().lands().getLands().stream()
                .sorted(Comparator.comparingInt((Land l) -> l.getChunks().size()).reversed()).toList();
    }

    @Override
    protected void entry(int slot, Land land) {
        boolean admin = player.hasPermission("lands.admin");
        boolean banned = land.getBanned().contains(player.getUniqueId());
        set(slot, Items.of(banned ? Material.BARRIER : Material.GRASS_BLOCK, "§a" + land.getName(),
                        "§7オーナー: §f" + Items.name(Bukkit.getOfflinePlayer(land.getOwner())),
                        "§7メンバー数: §f" + land.getMembers().size(),
                        "§7チャンク数: §f" + land.getChunks().size(),
                        "",
                        banned ? "§c訪問禁止されています" : "§eクリックでテレポート",
                        admin ? "§4Shift+右クリックで削除(管理者)" : ""),
                e -> {
                    if (admin && e.getClick() == ClickType.SHIFT_RIGHT) {
                        new ConfirmMenu(player, "§4「" + land.getName() + "」を削除？",
                                () -> { LandActions.delete(player, land); new LandListMenu(player).open(); },
                                () -> new LandListMenu(player).open()).open();
                    } else {
                        LandActions.teleport(player, land);
                    }
                });
    }

    @Override
    protected void footer() {
        backButton(45, () -> new MainMenu(player).open());
    }
}
