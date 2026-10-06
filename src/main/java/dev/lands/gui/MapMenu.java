package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.ChunkPos;
import dev.lands.model.Land;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** 周辺9x5チャンクを表示。上が北。 */
public class MapMenu extends Menu {
    private ChunkPos center;

    public MapMenu(Player player) {
        super(player, 6, "§2土地マップ §7(上が北)");
        center = ChunkPos.of(player.getLocation());
    }

    @Override
    protected void build() {
        Land mine = LandsPlugin.get().lands().getLandOf(player.getUniqueId());
        ChunkPos playerPos = ChunkPos.of(player.getLocation());
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                ChunkPos pos = center.offset(col - 4, row - 2);
                Land at = LandsPlugin.get().lands().getLandAt(pos);
                Material mat;
                String name;
                String action;
                if (at == null) {
                    mat = Material.LIGHT_GRAY_STAINED_GLASS_PANE; name = "§7未保護"; action = mine != null ? "§eクリックで保護" : "";
                } else if (at == mine) {
                    mat = Material.LIME_STAINED_GLASS_PANE; name = "§a" + at.getName() + " §7(あなたの土地)"; action = "§eクリックで保護解除";
                } else {
                    mat = Material.RED_STAINED_GLASS_PANE; name = "§c" + at.getName(); action = "";
                }
                if (pos.equals(playerPos)) mat = at == mine && at != null ? Material.EMERALD_BLOCK : Material.PLAYER_HEAD;
                String title = pos.equals(playerPos) ? name + " §f§l(現在地)" : name;
                set(row * 9 + col, Items.of(mat, title, "§7チャンク: " + pos.x() + ", " + pos.z(), action), e -> {
                    if (mine == null) return;
                    if (at == mine) LandActions.unclaim(player, pos);
                    else if (at == null) LandActions.claim(player, pos);
                    refresh();
                });
            }
        }
        backButton(45, () -> new MainMenu(player).open());
        set(48, Items.of(Material.SPECTRAL_ARROW, "§f← 西へ"), e -> { center = center.offset(-3, 0); refresh(); });
        set(49, Items.of(Material.SPECTRAL_ARROW, "§f↑ 北へ"), e -> { center = center.offset(0, -2); refresh(); });
        set(50, Items.of(Material.SPECTRAL_ARROW, "§f↓ 南へ"), e -> { center = center.offset(0, 2); refresh(); });
        set(51, Items.of(Material.SPECTRAL_ARROW, "§f東へ →"), e -> { center = center.offset(3, 0); refresh(); });
        set(53, Items.of(Material.COMPASS, "§e現在地に戻る"), e -> { center = ChunkPos.of(player.getLocation()); refresh(); });
    }
}
