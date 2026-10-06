package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.Land;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class InvitesMenu extends PagedMenu<Land> {
    public InvitesMenu(Player player) {
        super(player, "§b届いている招待");
    }

    @Override
    protected List<Land> entries() {
        return LandsPlugin.get().lands().getInvitesOf(player.getUniqueId());
    }

    @Override
    protected void entry(int slot, Land land) {
        set(slot, Items.of(Material.WRITABLE_BOOK, "§a" + land.getName(),
                        "§7オーナー: §f" + Items.name(Bukkit.getOfflinePlayer(land.getOwner())),
                        "§7メンバー数: §f" + land.getMembers().size(),
                        "", "§a左クリックで参加", "§c右クリックで拒否"),
                e -> {
                    if (e.getClick() == ClickType.RIGHT) {
                        LandActions.decline(player, land);
                        refresh();
                    } else {
                        LandActions.accept(player, land);
                        new MainMenu(player).open();
                    }
                });
    }

    @Override
    protected void footer() {
        backButton(45, () -> new MainMenu(player).open());
    }
}
