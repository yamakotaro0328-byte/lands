package dev.lands.gui;

import dev.lands.LandActions;
import dev.lands.model.Flag;
import dev.lands.model.Land;
import org.bukkit.entity.Player;

public class FlagsMenu extends Menu {
    public FlagsMenu(Player player) {
        super(player, 4, "§d土地の設定");
    }

    @Override
    protected void build() {
        Land land = LandActions.requireLand(player);
        if (land == null) { player.closeInventory(); return; }
        fillBorder();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        Flag[] flags = Flag.values();
        for (int i = 0; i < flags.length && i < slots.length; i++) {
            Flag f = flags[i];
            boolean on = land.getFlag(f);
            set(slots[i], Items.of(f.icon, (on ? "§a" : "§c") + f.display,
                    "§7状態: " + (on ? "§a許可" : "§c禁止"), "", "§eクリックで切り替え"),
                    e -> { LandActions.toggleFlag(player, land, f); refresh(); });
        }
        backButton(31, () -> new MainMenu(player).open());
    }
}
