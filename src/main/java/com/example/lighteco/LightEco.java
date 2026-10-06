package com.example.lighteco;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LightEco extends JavaPlugin {

    public static final MiniMessage MM = MiniMessage.miniMessage();

    public Data data;
    public Jobs jobs;
    public Gui gui;
    public Gamble gamble;
    public Listeners listeners;
    public NamespacedKey menuKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        menuKey = new NamespacedKey(this, "menu");
        data = new Data(this);
        data.load();
        jobs = new Jobs(this);
        gui = new Gui(this);
        gamble = new Gamble(this);

        listeners = new Listeners(this);
        getServer().getPluginManager().registerEvents(listeners, this);
        getServer().getPluginManager().registerEvents(new Protect(this), this);

        Commands cmds = new Commands(this);
        for (String name : List.of("money", "pay", "baltop", "menu", "gamble", "l", "jobs", "nation", "leco")) {
            PluginCommand pc = getCommand(name);
            if (pc != null) {
                pc.setExecutor(cmds);
                pc.setTabCompleter(cmds);
            }
        }

        // 5分ごとに、変更があったときだけ非同期保存
        getServer().getScheduler().runTaskTimer(this, () -> data.save(false), 6000L, 6000L);
        // 日付をまたいでログインし続けている人にもログインボーナスを渡す
        getServer().getScheduler().runTaskTimer(this, () -> getServer().getOnlinePlayers().forEach(listeners::loginBonus), 1200L, 1200L);
    }

    @Override
    public void onDisable() {
        if (data != null) data.save(true);
    }

    public void msg(CommandSender to, String mini) {
        to.sendMessage(MM.deserialize("<gray>[<gold>LightEco</gold>]</gray> " + mini));
    }

    /** ユーザー入力を MiniMessage に埋め込むときのエスケープ。 */
    public static String esc(String s) {
        return MM.escapeTags(s);
    }

    public ItemStack menuItem() {
        ItemStack it = new ItemStack(Material.COMPASS);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MM.deserialize("<!italic><gold>LightEco メニュー"));
        meta.lore(List.of(MM.deserialize("<!italic><gray>右クリックでメニューを開く")));
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    public boolean isMenuItem(ItemStack it) {
        return it != null && it.getType() == Material.COMPASS && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }

    public void reload() {
        reloadConfig();
    }

    // ---------- 土地 ----------

    /** いまいるチャンクを購入する（/l claim とメニュー共通）。 */
    public void claimHere(Player p) {
        var loc = p.getLocation();
        String world = loc.getWorld().getName();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        UUID id = p.getUniqueId();

        UUID owner = data.landOwner(world, cx, cz);
        if (owner != null) {
            msg(p, owner.equals(id) ? "<yellow>ここはすでにあなたの土地です。"
                    : "<red>ここは " + esc(data.nameOf(owner)) + " の土地です。");
            return;
        }
        int owned = data.landCount(id);
        if (owned >= getConfig().getInt("land.max-per-player", 100)) {
            msg(p, "<red>これ以上土地を持てません。");
            return;
        }
        long price = data.landPrice(owned);
        if (!data.take(id, price)) {
            msg(p, "<red>お金が足りません。 価格: " + data.fmt(price) + " / 所持金: " + data.fmt(data.get(id)));
            return;
        }
        data.claimLand(id, world, cx, cz);
        msg(p, "<green>土地を購入しました！ <gray>(-" + data.fmt(price) + ") 次の価格: " + data.fmt(data.landPrice(owned + 1)));
    }

    /** 建築・破壊などが許可されているか。 */
    public boolean canBuild(Player p, org.bukkit.block.Block b) {
        return canBuild(p, b.getLocation());
    }

    public boolean canBuild(Player p, org.bukkit.Location l) {
        UUID owner = data.landOwner(l.getWorld().getName(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
        if (owner == null || owner.equals(p.getUniqueId())) return true;
        if (p.hasPermission("lighteco.admin")) return true;
        Set<UUID> t = data.trusted.get(owner);
        if (t != null && t.contains(p.getUniqueId())) return true;
        return getConfig().getBoolean("nation.member-land-share", true) && data.sameNation(owner, p.getUniqueId());
    }
}
