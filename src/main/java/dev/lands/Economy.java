package dev.lands;

import dev.lands.model.Land;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault経済のラッパー。Vaultが無い場合は無効（全て無料） */
public class Economy {
    private net.milkbowl.vault.economy.Economy eco;

    public void setup() {
        eco = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp != null) eco = rsp.getProvider();
    }

    public boolean enabled() { return eco != null; }

    private FileConfiguration cfg() { return LandsPlugin.get().getConfig(); }

    public double createCost() { return enabled() ? cfg().getDouble("economy.create-cost") : 0; }
    public double claimCost() { return enabled() ? cfg().getDouble("economy.claim-cost") : 0; }
    public double unclaimRefund() { return enabled() ? cfg().getDouble("economy.unclaim-refund") : 0; }

    public boolean taxEnabled() { return enabled() && cfg().getBoolean("tax.enabled"); }

    public double taxOf(Land land) {
        if (!taxEnabled()) return 0;
        return land.getChunks().size() * cfg().getDouble("tax.per-chunk")
                + land.getMembers().size() * cfg().getDouble("tax.per-member");
    }

    public String format(double v) {
        return enabled() ? eco.format(v) : String.format("%.0f", v);
    }

    public double balance(OfflinePlayer p) { return enabled() ? eco.getBalance(p) : 0; }

    public boolean has(OfflinePlayer p, double amount) { return !enabled() || amount <= 0 || eco.has(p, amount); }

    public boolean withdraw(OfflinePlayer p, double amount) {
        if (!enabled() || amount <= 0) return true;
        return eco.withdrawPlayer(p, amount).transactionSuccess();
    }

    public void deposit(OfflinePlayer p, double amount) {
        if (enabled() && amount > 0) eco.depositPlayer(p, amount);
    }
}
