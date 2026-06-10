package org.alifmis.calitinaPvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

public final class CustomItems {

    public static final String KEY_ANCIENT_CORE = "ancient_core";
    public static final String KEY_RESONANT_DRAGON_EGG = "resonant_dragon_egg";
    public static final String KEY_RESONANT_NETHER_STAR = "resonant_nether_star";
    public static final String KEY_FATAL_CORE = "fatal_core";
    public static final String KEY_FATAL_WEAPON = "fatal_weapon";
    public static final String KEY_FATAL_HIT = "fatal_hit"; // temporary marker on victim

    public static final Set<Material> SPECIALTY_WEAPONS = Set.of(
        Material.MACE
    );

    public static final Set<Material> ALL_WEAPONS = Set.of(
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
        Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
        Material.BOW, Material.CROSSBOW,
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
        Material.MACE, Material.TRIDENT
    );

    private NamespacedKey ancientCoreKey;
    private NamespacedKey resonantDragonEggKey;
    private NamespacedKey resonantNetherStarKey;
    private NamespacedKey fatalCoreKey;
    private NamespacedKey fatalWeaponKey;
    private NamespacedKey fatalHitKey;
    private NamespacedKey recipeKey;

    private final JavaPlugin plugin;

    public CustomItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        ancientCoreKey = key(KEY_ANCIENT_CORE);
        resonantDragonEggKey = key(KEY_RESONANT_DRAGON_EGG);
        resonantNetherStarKey = key(KEY_RESONANT_NETHER_STAR);
        fatalCoreKey = key(KEY_FATAL_CORE);
        fatalWeaponKey = key(KEY_FATAL_WEAPON);
        fatalHitKey = key(KEY_FATAL_HIT);
        recipeKey = key("fatal_core_recipe");
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }

    public NamespacedKey ancientCoreKey() { return ancientCoreKey; }
    public NamespacedKey resonantDragonEggKey() { return resonantDragonEggKey; }
    public NamespacedKey resonantNetherStarKey() { return resonantNetherStarKey; }
    public NamespacedKey fatalCoreKey() { return fatalCoreKey; }
    public NamespacedKey fatalWeaponKey() { return fatalWeaponKey; }
    public NamespacedKey fatalHitKey() { return fatalHitKey; }
    public NamespacedKey recipeKey() { return recipeKey; }

    public boolean isAncientCore(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(ancientCoreKey, PersistentDataType.INTEGER);
    }

    public boolean isResonantDragonEgg(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(resonantDragonEggKey, PersistentDataType.BOOLEAN);
    }

    public boolean isResonantNetherStar(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(resonantNetherStarKey, PersistentDataType.BOOLEAN);
    }

    public boolean isFatalCore(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(fatalCoreKey, PersistentDataType.INTEGER);
    }

    public boolean isFatalWeapon(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(fatalWeaponKey, PersistentDataType.INTEGER);
    }

    public int getAncientCoreDurability(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer()
            .getOrDefault(ancientCoreKey, PersistentDataType.INTEGER, 0);
    }

    public void setAncientCoreDurability(ItemStack item, int value) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(ancientCoreKey, PersistentDataType.INTEGER, value));
    }

    public int getFatalCoreUses(ItemStack item) {
        if (isFatalCore(item)) {
            return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(fatalCoreKey, PersistentDataType.INTEGER, 0);
        }
        if (isFatalWeapon(item)) {
            return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(fatalWeaponKey, PersistentDataType.INTEGER, 0);
        }
        return 0;
    }

    public void setFatalWeaponUses(ItemStack item, int value) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(fatalWeaponKey, PersistentDataType.INTEGER, value));
    }

    public void decrementFatalWeaponUses(ItemStack item) {
        item.editMeta(meta -> {
            Integer current = meta.getPersistentDataContainer().get(fatalWeaponKey, PersistentDataType.INTEGER);
            if (current != null) {
                meta.getPersistentDataContainer().set(fatalWeaponKey, PersistentDataType.INTEGER, current - 1);
            }
        });
    }

    public ItemStack createAncientCore(int durability) {
        int damagePercent = plugin.getConfig().getInt("ancient-core-damage-percent", 60);
        ItemStack item = ItemStack.of(Material.ECHO_SHARD);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(ancientCoreKey, PersistentDataType.INTEGER, durability);
            meta.displayName(mm("<dark_gray><b>远古核心</b></dark_gray>"));
            meta.lore(List.of(
                mm("<gray>—— ✦ 来自幽匿的远古遗物 ✦ ——</gray>"),
                mm("<dark_gray>深埋于幽匿噬痕之下的异界残骸。</dark_gray>"),
                mm("<dark_gray>监守者以其身躯为狱，封印着这份</dark_gray>"),
                mm("<dark_gray>被遗忘的力量。其核心脉动着与</dark_gray>"),
                mm("<dark_gray>监守者同源的声波共振，每一次</dark_gray>"),
                mm("<dark_gray>挥击都是远古的回响。</dark_gray>"),
                Component.empty(),
                mm("<gray>剩余耐久: <aqua>" + durability + "</aqua></gray>"),
                mm("<dark_gray>右键发射远古声波</dark_gray>"),
                mm("<dark_gray>伤害: 监守者声波的 " + damagePercent + "%</dark_gray>")
            ));
        });
        return item;
    }

    public ItemStack createResonantDragonEgg() {
        ItemStack item = ItemStack.of(Material.DRAGON_EGG);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(resonantDragonEggKey, PersistentDataType.BOOLEAN, true);
            meta.displayName(mm("<#d400ff><b>受远古之力共鸣的龙蛋</b></#d400ff>"));
            meta.lore(List.of(
                mm("<gray>—— ✦ 终末之地的回响 ✦ ——</gray>"),
                mm("<dark_gray>末影龙陨落之际，远古核心的</dark_gray>"),
                mm("<dark_gray>声波频率与龙蛋产生了奇异的</dark_gray>"),
                mm("<dark_gray>共振。蛋壳表面流转着幽匿般的</dark_gray>"),
                mm("<dark_gray>暗色纹路，仿佛在低语着虚空的秘密。</dark_gray>")
            ));
        });
        return item;
    }

    public ItemStack createResonantNetherStar() {
        ItemStack item = ItemStack.of(Material.NETHER_STAR);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(resonantNetherStarKey, PersistentDataType.BOOLEAN, true);
            meta.displayName(mm("<#ff4500><b>受远古之力共鸣的下界之星</b></#ff4500>"));
            meta.lore(List.of(
                mm("<gray>—— ✦ 凋零的悲鸣 ✦ ——</gray>"),
                mm("<dark_gray>凋零崩解之时，远古核心的</dark_gray>"),
                mm("<dark_gray>声波穿透了它的灵魂核心。下界之星</dark_gray>"),
                mm("<dark_gray>被染上了幽匿般的暗色，内部</dark_gray>"),
                mm("<dark_gray>涌动着扭曲的毁灭之力。</dark_gray>")
            ));
        });
        return item;
    }

    public ItemStack createFatalCore(int uses) {
        ItemStack item = ItemStack.of(Material.NETHER_STAR);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(fatalCoreKey, PersistentDataType.INTEGER, uses);
            meta.displayName(mm("<obf>###</obf> <dark_red><b>必杀核心</b></dark_red> <obf>###</obf>"));
            meta.lore(List.of(
                mm("<gray>—— ✦ 终极湮灭 ✦ ——</gray>"),
                mm("<dark_red>远古核心与龙蛋、下界之星</dark_red>"),
                mm("<dark_red>在共振中融合而成的禁忌之物。</dark_red>"),
                mm("<dark_red>其内部坍缩着足以抹消一切</dark_red>"),
                mm("<dark_red>存在的绝对杀意。</dark_red>"),
                Component.empty(),
                mm("<red>右键发射远古声波</red>"),
                mm("<gray>可用铁砧打到武器上</gray>"),
                mm("<gray>剩余使用次数: <red>" + uses + "</red></gray>")
            ));
        });
        return item;
    }

    public ItemStack applyFatalCoreToWeapon(ItemStack weapon, int totalUses) {
        ItemStack result = weapon.clone();
        result.editMeta(meta -> {
            meta.getPersistentDataContainer().set(fatalWeaponKey, PersistentDataType.INTEGER, totalUses);
            // Preserve original name but add fatal marker
            List<Component> lore = meta.hasLore() ?
                new java.util.ArrayList<>(meta.lore()) :
                new java.util.ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm("<dark_red>⚔ 必杀附魔 ⚔</dark_red>"));
            lore.add(mm("<red>一击必杀 · 无视不死图腾</red>"));
            lore.add(mm("<gray>剩余: <red>" + totalUses + "</red> 次</gray>"));
            meta.lore(lore);
        });
        return result;
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component mm(String message) {
        return MM.deserialize(message);
    }
}
