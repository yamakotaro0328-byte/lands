package dev.lands;

import dev.lands.gui.ChatInput;
import dev.lands.gui.MenuListener;
import dev.lands.listener.MoveListener;
import dev.lands.listener.ProtectionListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class LandsPlugin extends JavaPlugin {
    private static LandsPlugin instance;
    private LandManager landManager;
    private ChatInput chatInput;
    private Economy economy;
    private TaxTask taxTask;

    public static final String PREFIX = ChatColor.DARK_GREEN + "[Lands] " + ChatColor.RESET;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        landManager = new LandManager(this);
        landManager.load();
        chatInput = new ChatInput(this);
        economy = new Economy();
        // 経済プラグインの登録を待ってから接続
        getServer().getScheduler().runTask(this, () -> {
            economy.setup();
            getLogger().info(economy.enabled() ? "Vault経済と連携しました" : "Vault経済が見つかりません。土地は無料・税金なしで動作します");
        });
        taxTask = new TaxTask(this);
        taxTask.runTaskTimer(this, 20 * 60, 20 * 60);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(chatInput, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);

        LandsCommand cmd = new LandsCommand(this);
        getCommand("lands").setExecutor(cmd);
        getCommand("lands").setTabCompleter(cmd);
        getLogger().info(landManager.getLands().size() + " 個の土地を読み込みました");
    }

    @Override
    public void onDisable() {
        if (landManager != null) landManager.save();
    }

    public static LandsPlugin get() { return instance; }
    public LandManager lands() { return landManager; }
    public ChatInput chatInput() { return chatInput; }
    public Economy economy() { return economy; }
    public TaxTask taxTask() { return taxTask; }

    public static void msg(org.bukkit.command.CommandSender s, String m) {
        s.sendMessage(PREFIX + m);
    }
}
