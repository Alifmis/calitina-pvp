package org.alifmis.calitinaPvp.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

// Warden Ancient Core drops, Ancient Core sonic boom, and last-core-hit tracking on bosses
public class WardenListener implements Listener {

    private final CalitinaPvp plugin;
    private final CustomItems items;

    // last player to hit this boss with an Ancient Core: entity UUID -> player UUID
    public final Map<UUID, UUID> ancientCoreLastHitter = new ConcurrentHashMap<>();

    public WardenListener(CalitinaPvp plugin, CustomItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    // Warden death: chance to drop an Ancient Core, player kills only, one per player ever
    @EventHandler
    public void onWardenDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WARDEN) return;

        // Must be killed by a player
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Player already caused an Ancient Core drop — no more
        if (plugin.isAncientCoreCooldown(killer.getName())) return;

        double chance = plugin.getConfig().getDouble("warden-drop-chance", 1.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

        int durability = plugin.getConfig().getInt("ancient-core-durability", 100);
        ItemStack core = items.createAncientCore(durability);

        event.getEntity().getWorld().dropItemNaturally(
            event.getEntity().getLocation(), core
        );

        // Record this player — they can never get another Ancient Core drop
        plugin.addAncientCoreCooldown(killer.getName());
    }

    // Ancient Core right-click: sonic boom, tracks boss hits, only 1 durability per use
    @EventHandler
    public void onAncientCoreUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
            && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!items.isAncientCore(item)) return;

        event.setCancelled(true);

        int durability = items.getAncientCoreDurability(item);
        if (durability <= 0) {
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }

        // Fire sonic boom
        fireAncientSonicBoom(player);

        // Decrement durability — handle stacking
        durability--;
        int amount = item.getAmount();

        if (amount > 1) {
            // Stacked: remove 1 from stack, give back item with decremented durability
            item.setAmount(amount - 1);
            if (durability > 0) {
                ItemStack newItem = items.createAncientCore(durability);
                newItem.setAmount(1);
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(newItem);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
            // durability=0: item consumed (already removed from stack)
        } else {
            // Single item: decrement in place
            if (durability <= 0) {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                final int finalDurability = durability;
                items.setAncientCoreDurability(item, durability);
                item.editMeta(meta -> {
                    java.util.List<net.kyori.adventure.text.Component> lore = meta.hasLore()
                        ? new java.util.ArrayList<>(meta.lore())
                        : new java.util.ArrayList<>();
                    // Durability line is at lore.size() - 3 (after header, 5 desc lines, empty)
                    if (lore.size() >= 3) {
                        lore.set(lore.size() - 3, CustomItems.mm("<gray>剩余耐久: <aqua>" + finalDurability
                            + "</aqua></gray>"));
                    }
                    meta.lore(lore);
                });
            }
        }
    }

    private void fireAncientSonicBoom(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();

        double range = 15.0; // same as warden
        double percent = plugin.getConfig().getDouble("ancient-core-damage-percent", 60.0);
        double damage = 10.0 * (percent / 100.0);

        // Ray trace to find target entity
        RayTraceResult result = world.rayTraceEntities(
            eye, direction, range, 0.5,
            entity -> entity instanceof LivingEntity && entity != player
        );

        double hitDistance = result != null && result.getHitPosition() != null
            ? result.getHitPosition().distance(eye.toVector())
            : range;

        // Spawn particles along the path (dark teal/cyan — warden themed)
        Location particleLoc = eye.clone();
        Vector step = direction.clone().normalize().multiply(0.3);
        int particleCount = (int) (hitDistance / 0.3);

        for (int i = 0; i < particleCount; i++) {
            particleLoc.add(step);
            world.spawnParticle(
                Particle.DUST, particleLoc, 2, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(Color.fromRGB(0, 200, 180), 1.2f)
            );
            if (i % 3 == 0) {
                world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        // Sound along the path
        world.playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);

        // Damage the hit entity
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            // Track boss hits
            EntityType type = target.getType();
            if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
                ancientCoreLastHitter.put(target.getUniqueId(), player.getUniqueId());
            }

            target.damage(damage, player);

            // Extra particles at hit location
            Location hitLoc = target.getLocation().add(0, 1, 0);
            world.spawnParticle(
                Particle.DUST, hitLoc, 40, 0.5, 1.0, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(0, 200, 180), 2.0f)
            );
            world.spawnParticle(Particle.SONIC_BOOM, hitLoc, 8, 0.3, 0.3, 0.3, 0);
            world.playSound(hitLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
        }
    }
}
