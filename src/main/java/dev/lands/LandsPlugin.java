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

    public static final String PREFIX = ChatColor.DARK_GREEN + "[Lands] " + ChatColor.RESET;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        landManager = new LandManager(this);
        landManager.load();
        chatInput = new ChatInput(this);

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

    public static void msg(org.bukkit.command.CommandSender s, String m) {
        s.sendMessage(PREFIX + m);
    }
}
