package org.alifmis.calitinaPvp;

import org.bukkit.plugin.java.JavaPlugin;
import org.alifmis.calitinaPvp.commands.PvpCommand;
import org.alifmis.calitinaPvp.listeners.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CalitinaPvp extends JavaPlugin {

    private CustomItems customItems;

    private final Set<String> ancientCoreCooldown = ConcurrentHashMap.newKeySet();
    private File cooldownFile;

    @Override
    public void onEnable() {
        // Save default config if not exists
        saveDefaultConfig();
        reloadConfig();

        // Initialize custom items registry
        customItems = new CustomItems(this);
        customItems.init();

        // Load ancient-core player cooldown file
        cooldownFile = new File(getDataFolder(), "ancient-core-players.txt");
        loadCooldownFile();

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
        PvpCommand pvpCommand = new PvpCommand(customItems, this);
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

    public boolean isAncientCoreCooldown(String playerName) {
        return ancientCoreCooldown.contains(playerName);
    }

    public void addAncientCoreCooldown(String playerName) {
        ancientCoreCooldown.add(playerName);
        saveCooldownFile();
    }

    public void removeAncientCoreCooldown(String playerName) {
        ancientCoreCooldown.remove(playerName);
        saveCooldownFile();
    }

    public void removeAllAncientCoreCooldowns() {
        ancientCoreCooldown.clear();
        saveCooldownFile();
    }

    public Set<String> getAncientCoreCooldownNames() {
        return Collections.unmodifiableSet(ancientCoreCooldown);
    }

    private void loadCooldownFile() {
        if (!cooldownFile.exists()) return;
        try {
            List<String> lines = Files.readAllLines(cooldownFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                String name = line.trim();
                if (!name.isEmpty()) ancientCoreCooldown.add(name);
            }
        } catch (IOException e) {
            getLogger().warning("Failed to load ancient-core-players.txt: " + e.getMessage());
        }
    }

    private void saveCooldownFile() {
        try {
            getDataFolder().mkdirs();
            List<String> lines = new ArrayList<>(ancientCoreCooldown);
            Collections.sort(lines);
            Files.write(cooldownFile.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().warning("Failed to save ancient-core-players.txt: " + e.getMessage());
        }
    }
}
