package org.alifmis.calitinaPvp.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.concurrent.ThreadLocalRandom;

public class BossDropListener implements Listener {

    private final CalitinaPvp plugin;
    private final CustomItems items;
    private final WardenListener wardenListener;

    public BossDropListener(CalitinaPvp plugin, CustomItems items, WardenListener wardenListener) {
        this.plugin = plugin;
        this.items = items;
        this.wardenListener = wardenListener;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDeath(EntityDeathEvent event) {
        EntityType type = event.getEntityType();

        if (type == EntityType.ENDER_DRAGON) {
            handleDragonDrop(event);
        } else if (type == EntityType.WITHER) {
            handleWitherDrop(event);
        }
    }

    private void handleDragonDrop(EntityDeathEvent event) {
        // Check if last hit was from Ancient Core
        if (!wardenListener.ancientCoreLastHitter.containsKey(event.getEntity().getUniqueId())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("dragon-egg-drop-chance", 30.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

        ItemStack egg = items.createResonantDragonEgg();
        event.getEntity().getWorld().dropItemNaturally(
            event.getEntity().getLocation(), egg
        );

        // Clean up tracking
        wardenListener.ancientCoreLastHitter.remove(event.getEntity().getUniqueId());
    }

    private void handleWitherDrop(EntityDeathEvent event) {
        if (!wardenListener.ancientCoreLastHitter.containsKey(event.getEntity().getUniqueId())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("nether-star-drop-chance", 30.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

        ItemStack star = items.createResonantNetherStar();
        event.getEntity().getWorld().dropItemNaturally(
            event.getEntity().getLocation(), star
        );

        wardenListener.ancientCoreLastHitter.remove(event.getEntity().getUniqueId());
    }
}
