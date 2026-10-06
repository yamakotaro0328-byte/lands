package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.ChunkPos;
import dev.lands.model.Land;
import dev.lands.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class MainMenu extends Menu {
    public MainMenu(Player player) {
        super(player, 5, "§2§l土地メニュー");
    }

    @Override
    protected void build() {
        fillBorder();
        Land land = LandsPlugin.get().lands().getLandOf(player.getUniqueId());
        Land here = LandsPlugin.get().lands().getLandAt(player.getLocation());
        String hereName = here == null ? "§7未保護" : "§f" + here.getName();

        if (land == null) {
            set(20, Items.of(Material.GRASS_BLOCK, "§a§l土地を作成",
                    "§7今いるチャンクを最初の土地として", "§7新しい土地を作成します", "", "§eクリックして名前を入力"),
                    e -> LandsPlugin.get().chatInput().ask(player, "§a作成する土地の名前を入力してください",
                            name -> { if (LandActions.create(player, name)) new MainMenu(player).open(); }));
            List<Land> invites = LandsPlugin.get().lands().getInvitesOf(player.getUniqueId());
            set(22, Items.of(Material.WRITABLE_BOOK, "§b§l届いている招待 §7(" + invites.size() + ")",
                    "§7他の土地からの招待を確認します", "", "§eクリックで開く"),
                    e -> new InvitesMenu(player).open());
            set(24, Items.of(Material.COMPASS, "§d§l土地一覧・訪問", "§7サーバーの土地を見て回ります", "", "§eクリックで開く"),
                    e -> new LandListMenu(player).open());
            set(4, Items.of(Material.MAP, "§f現在地: " + hereName));
            return;
        }

        Role role = land.getRole(player.getUniqueId());
        int max = LandsPlugin.get().getConfig().getInt("max-chunks", 64);
        set(4, Items.of(Material.BEACON, "§6§l" + land.getName(),
                "§7オーナー: §f" + Items.name(Bukkit.getOfflinePlayer(land.getOwner())),
                "§7あなたの役職: §f" + role.display,
                "§7メンバー数: §f" + land.getMembers().size(),
                "§7チャンク: §f" + land.getChunks().size() + "/" + max,
                "§7銀行残高: §6" + LandsPlugin.get().economy().format(land.getBank()),
                "§7現在地: " + hereName));

        ChunkPos pos = ChunkPos.of(player.getLocation());
        set(10, Items.of(Material.LIME_BANNER, "§a§lこのチャンクを保護", "§7現在のチャンク (" + pos.x() + ", " + pos.z() + ")",
                        "§7費用: §f" + LandsPlugin.get().economy().format(LandsPlugin.get().economy().claimCost()) + " §7(銀行から)"),
                e -> { LandActions.claim(player, ChunkPos.of(player.getLocation())); refresh(); });
        set(11, Items.of(Material.RED_BANNER, "§c§lこのチャンクの保護を解除", "§7現在のチャンク (" + pos.x() + ", " + pos.z() + ")"),
                e -> { LandActions.unclaim(player, ChunkPos.of(player.getLocation())); refresh(); });
        set(12, Items.of(Material.FILLED_MAP, "§e§l土地マップ", "§7周辺のチャンクを表示し", "§7クリックで保護・解除できます"),
                e -> new MapMenu(player).open());
        set(14, Items.of(Material.PLAYER_HEAD, "§b§lメンバー管理", "§7招待・役職変更・追放"),
                e -> new MembersMenu(player).open());
        set(15, Items.of(Material.COMPARATOR, "§d§l土地の設定", "§7PvP・訪問者の権限などを設定"),
                e -> new FlagsMenu(player).open());
        set(16, Items.of(Material.BARRIER, "§c§l訪問禁止リスト", "§7特定のプレイヤーの立ち入りを禁止"),
                e -> new BanMenu(player).open());

        set(19, Items.of(Material.ENDER_PEARL, "§a§l土地へテレポート", "§7土地のスポーン地点へ移動"),
                e -> LandActions.teleport(player, land));
        set(20, Items.of(Material.RED_BED, "§e§lスポーン地点を設定", "§7現在地を土地のスポーンに設定"),
                e -> LandActions.setSpawn(player));
        set(21, Items.of(Material.NAME_TAG, "§f§l土地名を変更", "§7オーナーのみ"),
                e -> LandsPlugin.get().chatInput().ask(player, "§a新しい土地名を入力してください",
                        name -> { LandActions.rename(player, name); new MainMenu(player).open(); }));
        set(22, Items.of(Material.GOLD_INGOT, "§6§l土地の銀行", "§7残高: §f" + LandsPlugin.get().economy().format(land.getBank()),
                        "§7入金・引き出し・税金の確認"),
                e -> new BankMenu(player).open());
        set(23, Items.of(Material.COMPASS, "§d§l土地一覧・訪問", "§7サーバーの土地を見て回ります"),
                e -> new LandListMenu(player).open());

        if (role == Role.OWNER) {
            set(25, Items.of(Material.LAVA_BUCKET, "§4§l土地を削除", "§7全てのチャンクの保護が解除されます", "§c元に戻せません！"),
                    e -> new ConfirmMenu(player, "§4本当に「" + land.getName() + "」を削除？",
                            () -> { LandActions.delete(player, land); player.closeInventory(); },
                            () -> new MainMenu(player).open()).open());
        } else {
            set(25, Items.of(Material.OAK_DOOR, "§c§l土地から脱退"),
                    e -> new ConfirmMenu(player, "§c本当に脱退しますか？",
                            () -> { LandActions.leave(player); player.closeInventory(); },
                            () -> new MainMenu(player).open()).open());
        }
        set(40, Items.of(Material.OAK_SIGN, "§7閉じる"), e -> player.closeInventory());
    }
}
