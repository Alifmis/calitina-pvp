package org.alifmis.calitinaPvp.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.Map;

// Anvil: weapon + Fatal Core; non-mace keeps the core's remaining uses exactly, mace scales proportionally
public class AnvilListener implements Listener {

    private final CalitinaPvp plugin;
    private final CustomItems items;

    public AnvilListener(CalitinaPvp plugin, CustomItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    // show the result preview in the anvil output slot
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

        // Read Fatal Core's remaining uses
        int coreUses = items.getFatalCoreUses(right);
        int baseUses = plugin.getConfig().getInt("fatal-core-base-uses", 10);
        int totalUses;

        if (CustomItems.SPECIALTY_WEAPONS.contains(type)) {
            int bonus = plugin.getConfig().getInt("fatal-core-specialty-bonus", 3);
            int maxWeaponUses = baseUses + bonus;
            totalUses = Math.max(1, Math.round((float) coreUses / baseUses * maxWeaponUses));
        } else {
            totalUses = coreUses;
        }

        ItemStack result = items.applyFatalCoreToWeapon(left, totalUses);
        event.setResult(result);
        inv.setRepairCost(10);
    }

    // intercept the result-slot click so only 1 Fatal Core is taken from the stack
    @SuppressWarnings("removal")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;

        // Only handle result slot (rawSlot = 2 in anvil)
        if (event.getRawSlot() != 2) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = inv.getItem(2);
        if (result == null || !items.isFatalWeapon(result)) return;

        ItemStack right = inv.getSecondItem();
        if (right == null || !items.isFatalCore(right)) return;

        // Cancel default behavior — we'll handle consumption manually
        event.setCancelled(true);

        // Left slot: consume 1
        ItemStack left = inv.getFirstItem();
        if (left != null) {
            left.setAmount(left.getAmount() - 1);
            inv.setFirstItem(left.getAmount() > 0 ? left : null);
        }

        // Right slot: consume only 1, not the whole stack
        right.setAmount(right.getAmount() - 1);
        inv.setSecondItem(right.getAmount() > 0 ? right : null);

        // Give result to player
        ItemStack oneResult = result.clone();
        oneResult.setAmount(1);

        if (event.isShiftClick()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(oneResult);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        } else {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType() == Material.AIR) {
                event.setCursor(oneResult);
            } else if (cursor.isSimilar(oneResult) && cursor.getAmount() < cursor.getMaxStackSize()) {
                cursor.setAmount(cursor.getAmount() + 1);
            } else {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(oneResult);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }

        // Pay levels
        int cost = inv.getRepairCost();
        if (cost > 0) {
            player.setLevel(player.getLevel() - cost);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        player.updateInventory();

        // Clear result slot — PrepareAnvilEvent will re-fire and set new result if applicable
        inv.setItem(2, null);
    }
}
