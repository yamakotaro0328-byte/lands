package dev.lands.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** 上5行にエントリ、最下段に操作ボタンを置くページ付きメニュー */
public abstract class PagedMenu<T> extends Menu {
    private int page = 0;

    protected PagedMenu(Player player, String title) {
        super(player, 6, title);
    }

    protected abstract List<T> entries();
    protected abstract void entry(int slot, T value);
    protected abstract void footer();

    @Override
    protected void build() {
        List<T> list = entries();
        int pages = Math.max(1, (list.size() + 44) / 45);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= list.size()) break;
            entry(i, list.get(idx));
        }
        for (int i = 45; i < 54; i++) set(i, Items.of(Material.BLACK_STAINED_GLASS_PANE, " "));
        if (page > 0) set(48, Items.of(Material.ARROW, "§f前のページ"), e -> { page--; refresh(); });
        if (page < pages - 1) set(50, Items.of(Material.ARROW, "§f次のページ"), e -> { page++; refresh(); });
        set(49, Items.of(Material.PAPER, "§7ページ " + (page + 1) + "/" + pages));
        footer();
    }
}
