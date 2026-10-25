package org.alifmis.calitinaPvp.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.alifmis.calitinaPvp.CalitinaPvp;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// /pvp give <player> <item> [durability] | /pvp allow [player] | /pvp list | /pvp reload — OP or console
public class PvpCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ITEM_NAMES = List.of(
        "ancient_core",
        "resonant_dragon_egg",
        "resonant_nether_star",
        "fatal_core"
    );

    private static final List<String> DURABILITY_REQUIRED = List.of("ancient_core", "fatal_core");

    private static final List<String> SUBCOMMANDS = List.of("give", "allow", "list", "reload");

    private final CustomItems items;
    private final CalitinaPvp plugin;

    public PvpCommand(CustomItems items, CalitinaPvp plugin) {
        this.items = items;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // OP / console only
        if (!sender.isOp() && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(CustomItems.mm("<red>你没有权限使用此命令。</red>"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(CustomItems.mm("<red>用法: /pvp give|allow|list</red>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "give" -> handleGive(sender, args);
            case "allow" -> handleAllow(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage(CustomItems.mm("<red>未知子命令: " + sub
                    + "。可用: give, allow, list, reload</red>"));
                yield true;
            }
        };
    }

    // /pvp give
    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CustomItems.mm("<red>用法: /pvp give <player_name> <item> [durability]</red>"));
            return true;
        }

        // parse player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(CustomItems.mm("<red>未找到玩家: " + args[1] + "</red>"));
            return true;
        }

        // parse item
        String itemArg = args[2].toLowerCase();
        int durability = -1; // sentinel: not provided

        if (args.length >= 4) {
            try {
                durability = Integer.parseInt(args[3]);
                if (durability < 1) {
                    sender.sendMessage(CustomItems.mm("<red>耐久/次数必须大于 0。</red>"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(CustomItems.mm("<red>耐久/次数必须是个数字。</red>"));
                return true;
            }
        }

        // create item
        ItemStack result = switch (itemArg) {
            case "ancient_core" -> {
                int maxDura = durability > 0 ? durability
                    : plugin.getConfig().getInt("ancient-core-durability", 100);
                yield items.createAncientCore(maxDura);
            }
            case "resonant_dragon_egg" -> {
                if (durability > 0) {
                    sender.sendMessage(CustomItems.mm("<yellow>共振龙蛋无需耐久参数，已忽略。</yellow>"));
                }
                yield items.createResonantDragonEgg();
            }
            case "resonant_nether_star" -> {
                if (durability > 0) {
                    sender.sendMessage(CustomItems.mm("<yellow>共振下界之星无需耐久参数，已忽略。</yellow>"));
                }
                yield items.createResonantNetherStar();
            }
            case "fatal_core" -> {
                int uses = durability > 0 ? durability
                    : plugin.getConfig().getInt("fatal-core-base-uses", 10);
                yield items.createFatalCore(uses);
            }
            default -> {
                sender.sendMessage(CustomItems.mm("<red>未知物品: " + itemArg
                    + "。可用: " + String.join(", ", ITEM_NAMES) + "</red>"));
                yield null;
            }
        };

        if (result == null) return true;

        // give to player, dropping anything that does not fit
        var leftover = target.getInventory().addItem(result);
        for (var drop : leftover.values()) {
            target.getWorld().dropItem(target.getLocation(), drop);
        }

        String displayName = result.getItemMeta() != null && result.getItemMeta().hasDisplayName()
            ? CustomItems.mm("").append(result.getItemMeta().displayName()).toString()
            : itemArg;
        sender.sendMessage(CustomItems.mm("<green>已给予 " + target.getName() + " " + displayName + "</green>"));
        return true;
    }

    // /pvp allow
    private boolean handleAllow(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // /pvp allow — remove ALL players
            int count = plugin.getAncientCoreCooldownNames().size();
            plugin.removeAllAncientCoreCooldowns();
            sender.sendMessage(CustomItems.mm("<green>已移除所有 " + count
                + " 名玩家的远古核心掉落限制。</green>"));
            return true;
        }

        // /pvp allow <player_name>
        String name = args[1];
        if (!plugin.isAncientCoreCooldown(name)) {
            sender.sendMessage(CustomItems.mm("<yellow>玩家 " + name + " 不在限制列表中。</yellow>"));
            return true;
        }

        plugin.removeAncientCoreCooldown(name);
        sender.sendMessage(CustomItems.mm("<green>已移除 " + name + " 的远古核心掉落限制。</green>"));
        return true;
    }

    // /pvp list
    private boolean handleList(CommandSender sender) {
        Set<String> names = plugin.getAncientCoreCooldownNames();
        if (names.isEmpty()) {
            sender.sendMessage(CustomItems.mm("<gray>当前无玩家被限制远古核心掉落。</gray>"));
            return true;
        }

        sender.sendMessage(CustomItems.mm("<gold>已获得过远古核心的玩家 (" + names.size() + "):</gold>"));
        for (String name : names) {
            sender.sendMessage(CustomItems.mm("<gray>  - " + name + "</gray>"));
        }
        return true;
    }

    // /pvp reload
    private boolean handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(CustomItems.mm("<green>配置已重新加载。</green>"));
        return true;
    }

    // tab complete
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String label,
                                                 @NotNull String[] args) {
        if (!sender.isOp() && !(sender instanceof ConsoleCommandSender)) {
            return List.of();
        }

        if (args.length == 1) {
            // Subcommands: give, allow, list
            return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }

        String sub = args[0].toLowerCase();

        // give subcommand
        if (sub.equals("give")) {
            if (args.length == 2) {
                // Player names
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }
            if (args.length == 3) {
                // Item names
                return ITEM_NAMES.stream()
                    .filter(name -> name.startsWith(args[2].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }
            if (args.length == 4) {
                // Durability hint — only show for items that accept it
                if (DURABILITY_REQUIRED.contains(args[2].toLowerCase())) {
                    return List.of("<耐久>");
                }
                return List.of();
            }
            return List.of();
        }

        // allow subcommand
        if (sub.equals("allow")) {
            if (args.length == 2) {
                // Players currently in cooldown file + online players
                List<String> suggestions = new ArrayList<>(plugin.getAncientCoreCooldownNames());
                Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !suggestions.contains(name))
                    .forEach(suggestions::add);
                return suggestions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
            }
            return List.of();
        }

        // list subcommand takes no arguments
        return List.of();
    }
}
