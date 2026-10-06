package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.Land;
import dev.lands.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class MembersMenu extends PagedMenu<Map.Entry<UUID, Role>> {
    public MembersMenu(Player player) {
        super(player, "§bメンバー管理");
    }

    private Land land() { return LandsPlugin.get().lands().getLandOf(player.getUniqueId()); }

    @Override
    protected List<Map.Entry<UUID, Role>> entries() {
        Land land = land();
        if (land == null) return List.of();
        List<Map.Entry<UUID, Role>> list = new ArrayList<>(land.getMembers().entrySet());
        list.sort(Comparator.comparing((Map.Entry<UUID, Role> e) -> e.getValue()).reversed());
        return list;
    }

    @Override
    protected void entry(int slot, Map.Entry<UUID, Role> en) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(en.getKey());
        set(slot, Items.head(op, "§f" + Items.name(op), "§7役職: §f" + en.getValue().display,
                        op.isOnline() ? "§aオンライン" : "§7オフライン", "", "§eクリックで管理"),
                e -> new MemberMenu(player, en.getKey()).open());
    }

    @Override
    protected void footer() {
        backButton(45, () -> new MainMenu(player).open());
        Land land = land();
        if (land == null) return;
        set(53, Items.of(Material.EMERALD, "§a§lプレイヤーを招待", "§7オンラインのプレイヤーから選択",
                        "§7招待中: §f" + land.getInvites().size() + "人"),
                e -> new PlayerPickerMenu(player, "§a招待するプレイヤー",
                        p -> !land.isMember(p.getUniqueId()) && !land.getInvites().contains(p.getUniqueId()),
                        target -> { LandActions.invite(player, target); new MembersMenu(player).open(); },
                        () -> new MembersMenu(player).open()).open());
    }
}
