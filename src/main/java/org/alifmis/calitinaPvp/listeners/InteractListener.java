package org.alifmis.calitinaPvp.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.alifmis.calitinaPvp.CustomItems;

// prevents placing/using block-type model items such as the Dragon Egg
public class InteractListener implements Listener {

    private final CustomItems items;

    public InteractListener(CustomItems items) {
        this.items = items;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
            && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        var item = event.getItem();
        if (item == null) return;

        // Prevent placing Resonant Dragon Egg (modeled after DRAGON_EGG which is placeable)
        if (items.isResonantDragonEgg(item)) {
            event.setCancelled(true);
            return;
        }

        // Prevent placing the Fatal Core if we ever model it after a block
        if (items.isFatalCore(item) && item.getType().isBlock()) {
            event.setCancelled(true);
        }
    }
}
