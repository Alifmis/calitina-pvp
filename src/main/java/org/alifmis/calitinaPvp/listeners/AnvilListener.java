package org.alifmis.calitinaPvp.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

public class AnvilListener implements Listener {

    private final CalitinaPvp plugin;
    private final CustomItems items;

    public AnvilListener(CalitinaPvp plugin, CustomItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @SuppressWarnings("removal")
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getFirstItem();
        ItemStack right = inv.getSecondItem();

        if (left == null || right == null) return;

        // Right slot must be Fatal Core
        if (!items.isFatalCore(right)) return;

        // Left slot must be a weapon
        Material type = left.getType();
        if (!CustomItems.ALL_WEAPONS.contains(type)) return;

        // Don't allow applying Fatal Core to an already-fatal weapon
        if (items.isFatalWeapon(left)) return;

        // Calculate uses
        int baseUses = plugin.getConfig().getInt("fatal-core-base-uses", 10);
        int bonus = CustomItems.SPECIALTY_WEAPONS.contains(type)
            ? plugin.getConfig().getInt("fatal-core-specialty-bonus", 3)
            : 0;
        int totalUses = baseUses + bonus;

        // Create result
        ItemStack result = items.applyFatalCoreToWeapon(left, totalUses);

        event.setResult(result);
        inv.setRepairCost(10);
    }
}
