package dev.lands.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PlayerPickerMenu extends PagedMenu<Player> {
    private final Predicate<Player> filter;
    private final Consumer<Player> onPick;
    private final Runnable back;

    public PlayerPickerMenu(Player player, String title, Predicate<Player> filter, Consumer<Player> onPick, Runnable back) {
        super(player, title);
        this.filter = filter;
        this.onPick = onPick;
        this.back = back;
    }

    @Override
    protected List<Player> entries() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(filter)
                .map(p -> (Player) p).toList();
    }

    @Override
    protected void entry(int slot, Player p) {
        set(slot, Items.head(p, "§f" + p.getName(), "§eクリックで選択"), e -> {
            if (p.isOnline()) onPick.accept(p);
        });
    }

    @Override
    protected void footer() {
        backButton(45, back);
    }
}
