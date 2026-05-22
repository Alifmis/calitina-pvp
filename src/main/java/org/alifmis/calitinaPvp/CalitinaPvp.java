package org.alifmis.calitinaPvp;

import org.bukkit.plugin.java.JavaPlugin;
import org.alifmis.calitinaPvp.commands.PvpCommand;
import org.alifmis.calitinaPvp.listeners.*;

public final class CalitinaPvp extends JavaPlugin {

    private CustomItems customItems;

    @Override
    public void onEnable() {
        // Save default config if not exists
        saveDefaultConfig();
        reloadConfig();

        // Initialize custom items registry
        customItems = new CustomItems(this);
        customItems.init();

        // Create all listeners
        WardenListener wardenListener = new WardenListener(this, customItems);

        // Register events
        getServer().getPluginManager().registerEvents(new InteractListener(customItems), this);
        getServer().getPluginManager().registerEvents(wardenListener, this);
        getServer().getPluginManager().registerEvents(
            new BossDropListener(this, customItems, wardenListener), this
        );
        getServer().getPluginManager().registerEvents(new CraftingListener(this, customItems), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this, customItems), this);
        getServer().getPluginManager().registerEvents(new FatalWeaponListener(this, customItems), this);

        // Register commands
        PvpCommand pvpCommand = new PvpCommand(customItems);
        var pvpCmd = getCommand("pvp");
        if (pvpCmd != null) {
            pvpCmd.setExecutor(pvpCommand);
            pvpCmd.setTabCompleter(pvpCommand);
        }

        getLogger().info("Calitina PVP enabled — Ancient Core & Fatal Core system active.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Calitina PVP disabled.");
    }

    public CustomItems getCustomItems() {
        return customItems;
    }
}
