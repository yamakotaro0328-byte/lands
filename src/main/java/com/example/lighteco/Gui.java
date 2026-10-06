package com.example.lighteco;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** メニュー画面（メイン / 職業 / ギャンブル）。 */
public final class Gui {

    public static final class Holder implements InventoryHolder {
        public final String type;
        private Inventory inv;

        Holder(String type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private final LightEco plugin;

    public Gui(LightEco plugin) {
        this.plugin = plugin;
    }

    static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(LightEco.MM.deserialize("<!italic>" + name));
        List<net.kyori.adventure.text.Component> l = new ArrayList<>();
        for (String s : lore) l.add(LightEco.MM.deserialize("<!italic><gray>" + s));
        meta.lore(l);
        it.setItemMeta(meta);
        return it;
    }

    private Inventory create(Player p, String type, String title) {
        Holder h = new Holder(type);
        Inventory inv = Bukkit.createInventory(h, 27, LightEco.MM.deserialize(title));
        h.inv = inv;
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);
        return inv;
    }

    public void openMain(Player p) {
        Data d = plugin.data;
        UUID id = p.getUniqueId();
        Inventory inv = create(p, "main", "<gold>LightEco メニュー");

        inv.setItem(10, item(Material.GOLD_INGOT, "<yellow>所持金 / ログインボーナス",
                "所持金: <white>" + d.fmt(d.get(id)),
                "連続ログイン: <white>" + d.streak.getOrDefault(id, 0) + "日",
                "ボーナスは毎日ログイン時に自動で受け取れます"));
        inv.setItem(12, item(Material.DIAMOND, "<aqua>ギャンブル",
                "コインフリップ / スロット", "クリックで開く"));
        inv.setItem(14, item(Material.IRON_PICKAXE, "<green>職業",
                "ハンター・マイナーなどを選ぶ", "クリックで開く"));
        inv.setItem(16, item(Material.GRASS_BLOCK, "<dark_green>この土地を買う (/l claim)",
                "いまいるチャンクを購入します",
                "次の価格: <white>" + d.fmt(d.landPrice(d.landCount(id))),
                "所有数: <white>" + d.landCount(id)));
        inv.setItem(22, item(Material.WHITE_BANNER, "<light_purple>町・国",
                "クリックで自分の所属を表示", "作成: /nation create <名前>"));
        p.openInventory(inv);
    }

    public void openJobs(Player p) {
        Jobs jobs = plugin.jobs;
        UUID id = p.getUniqueId();
        Inventory inv = create(p, "jobs", "<green>職業 (クリックで就職/退職)");
        int slot = 10;
        for (String job : Jobs.IDS) {
            boolean on = jobs.active(id).contains(job);
            inv.setItem(slot++, item(Jobs.icon(job),
                    (on ? "<green>" : "<white>") + Jobs.label(job) + (on ? " <gray>(就職中)" : ""),
                    Jobs.hint(job),
                    "レベル: <white>" + jobs.level(p, job),
                    "クリックで" + (on ? "退職" : "就職")));
        }
        inv.setItem(22, item(Material.ARROW, "<white>戻る"));
        p.openInventory(inv);
    }

    public void openGamble(Player p) {
        Inventory inv = create(p, "gamble", "<aqua>ギャンブル");
        long[] bets = {100, 1000, 10000};
        for (int i = 0; i < bets.length; i++) {
            inv.setItem(10 + i, item(Material.GOLD_NUGGET, "<yellow>コインフリップ " + plugin.data.fmt(bets[i]),
                    "勝率47% / 勝てば2倍"));
            inv.setItem(14 + i, item(Material.AMETHYST_SHARD, "<light_purple>スロット " + plugin.data.fmt(bets[i]),
                    "最大10倍のジャックポット"));
        }
        inv.setItem(22, item(Material.ARROW, "<white>戻る"));
        p.openInventory(inv);
    }
}
