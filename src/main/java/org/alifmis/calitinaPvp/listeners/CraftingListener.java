package org.alifmis.calitinaPvp.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.*;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.Map;

public class CraftingListener implements Listener {

    private final CalitinaPvp plugin;
    private final CustomItems items;

    public CraftingListener(CalitinaPvp plugin, CustomItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        InventoryView view = event.getView();
        InventoryType type = view.getType();
        if (type != InventoryType.WORKBENCH) return;

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        if (matchesFatalCorePattern(matrix)) {
            int uses = plugin.getConfig().getInt("fatal-core-base-uses", 10);
            inv.setResult(items.createFatalCore(uses));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        InventoryView view = event.getView();
        InventoryType type = view.getType();
        if (type != InventoryType.WORKBENCH) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot != 0) return; // Only result slot

        // CRAFTER uses CraftInventoryCrafter, not CraftingInventory — skip manual click handling
        if (!(view.getTopInventory() instanceof CraftingInventory inv)) return;
        ItemStack result = inv.getResult();
        if (result == null || !items.isFatalCore(result)) return;

        // Prevent default crafting behavior
        event.setCancelled(true);

        ItemStack[] matrix = inv.getMatrix();
        if (!matchesFatalCorePattern(matrix)) return;

        // Handle cursor
        ItemStack cursor = event.getCursor();
        ItemStack oneCraft = result.clone();
        oneCraft.setAmount(1);

        if (event.isShiftClick()) {
            // Shift-click: give to inventory
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(oneCraft);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        } else {
            // Left or right click: give 1 to cursor
            if (cursor == null || cursor.getType() == Material.AIR) {
                event.setCursor(oneCraft);
            } else if (cursor.isSimilar(oneCraft) && cursor.getAmount() < cursor.getMaxStackSize()) {
                cursor.setAmount(cursor.getAmount() + 1);
            } else {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(oneCraft);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }

        // Consume ingredients (1 from each slot with PDC tag)
        consumeIngredients(inv, matrix);
        player.updateInventory();
    }

    private boolean matchesFatalCorePattern(ItemStack[] matrix) {
        // Pattern indices: 0=S 1=E 2=S  3=E 4=A 5=E  6=S 7=E 8=S
        int[] starSlots  = {0, 2, 6, 8};
        int[] eggSlots   = {1, 3, 5, 7};
        int   coreSlot   = 4;

        for (int i : starSlots) {
            if (!items.isResonantNetherStar(matrix[i])) return false;
        }
        for (int i : eggSlots) {
            if (!items.isResonantDragonEgg(matrix[i])) return false;
        }
        if (!items.isAncientCore(matrix[coreSlot])) return false;

        return true;
    }

    private void consumeIngredients(CraftingInventory inv, ItemStack[] matrix) {
        // Consume 1 from each slot
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inv.getItem(i + 1); // matrix is 0-based, inventory slot is 1-based
            if (slot != null && slot.getAmount() > 0) {
                slot.setAmount(slot.getAmount() - 1);
                inv.setItem(i + 1, slot.getAmount() > 0 ? slot : null);
            }
        }
    }
}
