package org.alifmis.calitinaPvp.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// fatal weapon melee kills and Fatal Core sonic booms, both bypassing the Totem of Undying
public class FatalWeaponListener implements Listener {

    private final CalitinaPvp plugin;
    private final CustomItems items;

    // Entities marked for fatal death (to cancel resurrection)
    private final Set<UUID> fatalMarked = ConcurrentHashMap.newKeySet();

    public FatalWeaponListener(CalitinaPvp plugin, CustomItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    // fatal weapon melee instant kill
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalWeaponAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!items.isFatalWeapon(weapon)) return;

        // Mark for fatal death
        LivingEntity victim = (LivingEntity) event.getEntity();
        fatalMarked.add(victim.getUniqueId());

        // kill anything instantly
        victim.setHealth(0);

        // Visual + sound feedback
        Location loc = victim.getLocation().add(0, 1, 0);
        victim.getWorld().spawnParticle(
            Particle.DUST, loc, 30, 0.4, 0.8, 0.4, 0,
            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f)
        );
        victim.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);

        // Decrement weapon uses
        decrementFatalWeapon(player, weapon);
    }

    // cancel Totem of Undying resurrection for fatal-marked entities
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onResurrect(EntityResurrectEvent event) {
        if (fatalMarked.remove(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Fatal Core standalone right-click: instant-kill sonic boom
    @EventHandler
    public void onFatalCoreUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
            && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!items.isFatalCore(item)) return;

        event.setCancelled(true);

        int uses = items.getFatalCoreUses(item);
        if (uses <= 0) {
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }

        // Fire sonic boom (marks target for instant kill)
        fireFatalSonicBoom(player);

        // Decrement uses — handle stacking
        uses--;
        int amount = item.getAmount();

        if (amount > 1) {
            // Stacked: remove 1 from stack, give back item with decremented durability
            item.setAmount(amount - 1);
            if (uses > 0) {
                ItemStack newItem = items.createFatalCore(uses);
                newItem.setAmount(1);
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(newItem);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
            // if uses=0, the single item is just consumed (already removed from stack)
        } else {
            // Single item: decrement in place
            final int newUses = uses;
            if (newUses <= 0) {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                item.editMeta(meta -> {
                    meta.getPersistentDataContainer().set(
                        items.fatalCoreKey(), PersistentDataType.INTEGER, newUses
                    );
                    java.util.List<net.kyori.adventure.text.Component> lore = meta.hasLore()
                        ? new java.util.ArrayList<>(meta.lore())
                        : new java.util.ArrayList<>();
                    if (lore.size() >= 1) {
                        lore.set(lore.size() - 1, CustomItems.mm("<gray>剩余使用次数: <red>" + newUses + "</red></gray>"));
                    }
                    meta.lore(lore);
                });
            }
        }
    }

    private void fireFatalSonicBoom(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();

        double range = plugin.getConfig().getDouble("fatal-core-sonic-range", 15.0);

        // Ray trace to find target entity
        RayTraceResult result = world.rayTraceEntities(
            eye, direction, range, 0.5,
            entity -> entity instanceof LivingEntity && entity != player
        );

        // Spawn particles along the path
        double hitDistance = result != null && result.getHitPosition() != null
            ? result.getHitPosition().distance(eye.toVector())
            : range;

        Location particleLoc = eye.clone();
        Vector step = direction.clone().normalize().multiply(0.3);
        int particleCount = (int) (hitDistance / 0.3);

        for (int i = 0; i < particleCount; i++) {
            particleLoc.add(step);
            // Red dust particles
            world.spawnParticle(
                Particle.DUST, particleLoc, 2, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(Color.fromRGB(200, 20, 20), 1.2f)
            );
            if (i % 3 == 0) {
                world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        // Sound along the path
        world.playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);

        // INSTANT KILL the hit entity (bypasses totem)
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            // Mark for fatal death (bypasses totem)
            fatalMarked.add(target.getUniqueId());

            // Set health to 0 directly for instant kill
            target.setHealth(0);

            // Extra particles at hit location
            Location hitLoc = target.getLocation().add(0, 1, 0);
            world.spawnParticle(
                Particle.DUST, hitLoc, 50, 0.5, 1.0, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(200, 20, 20), 2.0f)
            );
            world.spawnParticle(Particle.SONIC_BOOM, hitLoc, 10, 0.3, 0.3, 0.3, 0);
            world.playSound(hitLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.4f);
        }
    }

    // helpers
    private void decrementFatalWeapon(Player player, ItemStack weapon) {
        items.decrementFatalWeaponUses(weapon);
        int remaining = items.getFatalCoreUses(weapon);

        if (remaining <= 0) {
            // Weapon shatters
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            player.sendActionBar(CustomItems.mm("<red>⚔ 必杀武器已破碎! ⚔</red>"));
        } else {
            // Update lore to show remaining uses
            weapon.editMeta(meta -> {
                java.util.List<net.kyori.adventure.text.Component> lore = meta.hasLore()
                    ? new java.util.ArrayList<>(meta.lore())
                    : new java.util.ArrayList<>();
                if (lore.size() >= 1) {
                    for (int i = lore.size() - 1; i >= 0; i--) {
                        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                            .plainText().serialize(lore.get(i));
                        if (plain.contains("剩余")) {
                            lore.set(i, CustomItems.mm("<gray>剩余: <red>" + remaining + "</red> 次</gray>"));
                            break;
                        }
                    }
                }
                meta.lore(lore);
            });
            player.sendActionBar(CustomItems.mm("<red>⚔ 必杀武器剩余 " + remaining + " 次 ⚔</red>"));
        }
    }
}
