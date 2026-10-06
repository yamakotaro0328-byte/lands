package dev.lands.gui;

import dev.lands.Economy;
import dev.lands.LandActions;
import dev.lands.LandsPlugin;
import dev.lands.model.Land;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

public class BankMenu extends Menu {
    private static final double[] AMOUNTS = {100, 1000, 10000};

    public BankMenu(Player player) {
        super(player, 4, "§6土地の銀行");
    }

    @Override
    protected void build() {
        Land land = LandActions.requireLand(player);
        if (land == null) { player.closeInventory(); return; }
        Economy eco = LandsPlugin.get().economy();
        fillBorder();

        long remain = Math.max(0, LandsPlugin.get().lands().getNextTax() - System.currentTimeMillis());
        double tax = eco.taxOf(land);
        String taxLine = eco.taxEnabled()
                ? "§7次の税金: §c" + eco.format(tax) + " §7(あと " + (remain / 3600_000) + "時間" + (remain / 60_000 % 60) + "分)"
                : "§7税金: §aなし";
        int periods = tax > 0 ? (int) (land.getBank() / tax) : -1;
        set(4, Items.of(Material.GOLD_BLOCK, "§6§l銀行残高: " + eco.format(land.getBank()),
                taxLine,
                periods >= 0 ? "§7残高で払える回数: §f" + periods + "回" : "",
                "§7チャンク保護費用: §f" + eco.format(eco.claimCost()),
                "§7あなたの所持金: §f" + eco.format(eco.balance(player)),
                "", "§8税金が払えないとチャンクが没収されます"));

        amountRow(10, Material.LIME_DYE, "§a入金", (p, a) -> LandActions.deposit(p, land, a));
        amountRow(19, Material.ORANGE_DYE, "§6引き出し", (p, a) -> LandActions.withdraw(p, land, a));
        backButton(27, () -> new MainMenu(player).open());
    }

    private void amountRow(int start, Material mat, String label, BiConsumer<Player, Double> action) {
        Economy eco = LandsPlugin.get().economy();
        for (int i = 0; i < AMOUNTS.length; i++) {
            double a = AMOUNTS[i];
            set(start + 1 + i, Items.of(mat, label + " " + eco.format(a)), e -> { action.accept(player, a); refresh(); });
        }
        set(start + 5, Items.of(Material.OAK_SIGN, label + "（金額を入力）", "§eクリックしてチャットで入力"),
                e -> LandsPlugin.get().chatInput().ask(player, label + "§aする金額を入力してください", text -> {
                    try { action.accept(player, Double.parseDouble(text)); }
                    catch (NumberFormatException ex) { LandsPlugin.msg(player, "§c数字を入力してください"); }
                    new BankMenu(player).open();
                }));
    }
}
