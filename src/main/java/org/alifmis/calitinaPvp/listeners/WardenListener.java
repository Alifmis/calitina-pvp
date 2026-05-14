package org.alifmis.calitinaPvp.listeners;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class WardenListener implements Listener {

    private final CalitinaPvp plugin;
    private final CustomItems items;

    public final Map<UUID, UUID> ancientCoreLastHitter = new ConcurrentHashMap<>();

    public WardenListener(CalitinaPvp plugin, CustomItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @EventHandler
    public void onWardenDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WARDEN) return;

        double chance = plugin.getConfig().getDouble("warden-drop-chance", 1.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

        int durability = plugin.getConfig().getInt("ancient-core-durability", 100);
        ItemStack core = items.createAncientCore(durability);

        event.getEntity().getWorld().dropItemNaturally(
            event.getEntity().getLocation(), core
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (items.isAncientCore(weapon)) {
            EntityType type = event.getEntityType();
            if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
                ancientCoreLastHitter.put(event.getEntity().getUniqueId(), player.getUniqueId());
            }

            double percent = plugin.getConfig().getDouble("ancient-core-damage-percent", 60.0);
            // Warden sonic boom base damage is 10.0
            double damage = 10.0 * (percent / 100.0);
            event.setDamage(damage);

            decrementAncientCoreDurability(player, weapon);
        }
    }

    private void decrementAncientCoreDurability(Player player, ItemStack weapon) {
        int durability = items.getAncientCoreDurability(weapon);
        durability--;

        if (durability <= 0) {
            // Break the weapon
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        } else {
            items.setAncientCoreDurability(weapon, durability);
            // Update lore to show remaining durability
            final int finalDurability = durability;
            final int maxDurability = plugin.getConfig().getInt("ancient-core-durability", 100);
            weapon.editMeta(meta -> {
                java.util.List<net.kyori.adventure.text.Component> lore = meta.hasLore()
                    ? new java.util.ArrayList<>(meta.lore())
                    : new java.util.ArrayList<>();
                // Update the durability line (second to last lore line)
                if (lore.size() >= 2) {
                    lore.set(lore.size() - 2, CustomItems.mm("<gray>耐久: <aqua>" + finalDurability
                        + "</aqua> / " + maxDurability + "</gray>"));
                }
                meta.lore(lore);
            });
        }
    }
}
