package dev.lands.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** クリック処理付きのシンプルなGUI基底クラス */
public abstract class Menu implements InventoryHolder {
    protected final Player player;
    private final Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(Player player, int rows, String title) {
        this.player = player;
        this.inv = Bukkit.createInventory(this, rows * 9, title);
    }

    protected abstract void build();

    public void open() {
        inv.clear();
        actions.clear();
        build();
        player.openInventory(inv);
    }

    /** 画面を閉じずに再描画 */
    public void refresh() {
        inv.clear();
        actions.clear();
        build();
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
        else actions.remove(slot);
    }

    protected void set(int slot, ItemStack item) { set(slot, item, null); }

    protected void fillBorder() {
        ItemStack pane = Items.of(Material.GRAY_STAINED_GLASS_PANE, " ");
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, pane);
        }
    }

    protected void backButton(int slot, Runnable back) {
        set(slot, Items.of(Material.ARROW, "§e戻る"), e -> back.run());
    }

    protected int size() { return inv.getSize(); }

    void handle(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> a = actions.get(e.getRawSlot());
        if (a != null) a.accept(e);
    }

    @Override
    public Inventory getInventory() { return inv; }
}
