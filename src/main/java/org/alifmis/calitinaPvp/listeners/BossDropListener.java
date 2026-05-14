package org.alifmis.calitinaPvp.listeners;

import org.bukkit.Location;
import org.bukkit.World;
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

    // Dragon egg drop location in the End
    private static final double DRAGON_DROP_X = 0.5;
    private static final double DRAGON_DROP_Y = 65.0;
    private static final double DRAGON_DROP_Z = 0.5;

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
        if (!wardenListener.ancientCoreLastHitter.containsKey(event.getEntity().getUniqueId())) {
            return;
        }

        double chance = plugin.getConfig().getDouble("dragon-egg-drop-chance", 30.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

        ItemStack egg = items.createResonantDragonEgg();
        World endWorld = event.getEntity().getWorld();
        Location dropLoc = new Location(endWorld, DRAGON_DROP_X, DRAGON_DROP_Y, DRAGON_DROP_Z);

        // Drop as item entity at fixed coordinates
        endWorld.dropItem(dropLoc, egg);

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
