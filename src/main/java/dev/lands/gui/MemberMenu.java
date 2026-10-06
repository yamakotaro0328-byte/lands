package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.Land;
import dev.lands.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MemberMenu extends Menu {
    private final UUID target;

    public MemberMenu(Player player, UUID target) {
        super(player, 3, "§bメンバー: " + Items.name(Bukkit.getOfflinePlayer(target)));
        this.target = target;
    }

    @Override
    protected void build() {
        Land land = LandsPlugin.get().lands().getLandOf(player.getUniqueId());
        if (land == null || !land.isMember(target)) { new MembersMenu(player).open(); return; }
        fillBorder();
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        Role role = land.getRole(target);
        set(4, Items.head(op, "§f" + Items.name(op), "§7役職: §f" + role.display));

        if (role != Role.OWNER) {
            Role next = role == Role.MEMBER ? Role.TRUSTED : Role.MEMBER;
            set(11, Items.of(next.icon, "§e役職を「" + next.display + "」に変更", "§7" + next.description, "§8オーナーのみ"),
                    e -> { LandActions.setRole(player, land, target, next); refresh(); });
            set(13, Items.of(Material.DIAMOND, "§6オーナー権限を譲渡", "§7あなたは信頼メンバーになります", "§8オーナーのみ"),
                    e -> new ConfirmMenu(player, "§6" + Items.name(op) + " に譲渡しますか？",
                            () -> { LandActions.setRole(player, land, target, Role.OWNER); new MembersMenu(player).open(); },
                            this::open).open());
            set(15, Items.of(Material.IRON_DOOR, "§c土地から追放"),
                    e -> new ConfirmMenu(player, "§c" + Items.name(op) + " を追放しますか？",
                            () -> { LandActions.kick(player, land, target); new MembersMenu(player).open(); },
                            this::open).open());
        }
        backButton(18, () -> new MembersMenu(player).open());
    }
}
