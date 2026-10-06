package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.Land;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanMenu extends PagedMenu<UUID> {
    public BanMenu(Player player) {
        super(player, "§c訪問禁止リスト");
    }

    private Land land() { return LandsPlugin.get().lands().getLandOf(player.getUniqueId()); }

    @Override
    protected List<UUID> entries() {
        Land land = land();
        return land == null ? List.of() : new ArrayList<>(land.getBanned());
    }

    @Override
    protected void entry(int slot, UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        set(slot, Items.head(op, "§c" + Items.name(op), "§eクリックで禁止を解除"),
                e -> { LandActions.toggleBan(player, land(), uuid); refresh(); });
    }

    @Override
    protected void footer() {
        backButton(45, () -> new MainMenu(player).open());
        Land land = land();
        if (land == null) return;
        set(53, Items.of(Material.BARRIER, "§c§lプレイヤーを訪問禁止にする", "§7オンラインのプレイヤーから選択"),
                e -> new PlayerPickerMenu(player, "§c訪問禁止にするプレイヤー",
                        p -> !land.isMember(p.getUniqueId()) && !land.getBanned().contains(p.getUniqueId()),
                        target -> { LandActions.toggleBan(player, land, target.getUniqueId()); new BanMenu(player).open(); },
                        () -> new BanMenu(player).open()).open());
    }
}
