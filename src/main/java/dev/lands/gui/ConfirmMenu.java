package dev.lands.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ConfirmMenu extends Menu {
    private final Runnable yes, no;

    public ConfirmMenu(Player player, String title, Runnable yes, Runnable no) {
        super(player, 3, title);
        this.yes = yes;
        this.no = no;
    }

    @Override
    protected void build() {
        fillBorder();
        set(11, Items.of(Material.LIME_CONCRETE, "§a§lはい"), e -> yes.run());
        set(15, Items.of(Material.RED_CONCRETE, "§c§lいいえ"), e -> no.run());
    }
}
