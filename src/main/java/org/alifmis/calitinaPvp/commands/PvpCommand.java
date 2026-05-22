package org.alifmis.calitinaPvp.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.alifmis.calitinaPvp.CustomItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PvpCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ITEM_NAMES = List.of(
        "ancient_core",
        "resonant_dragon_egg",
        "resonant_nether_star",
        "fatal_core"
    );

    private static final List<String> DURABILITY_REQUIRED = List.of("ancient_core", "fatal_core");

    private final CustomItems items;

    public PvpCommand(CustomItems items) {
        this.items = items;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // OP / console only
        if (!sender.isOp() && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(CustomItems.mm("<red>你没有权限使用此命令。</red>"));
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(CustomItems.mm("<red>用法: /pvp give <player_name> <item> [durability]</red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(CustomItems.mm("<red>未找到玩家: " + args[1] + "</red>"));
            return true;
        }

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

        ItemStack result = switch (itemArg) {
            case "ancient_core" -> {
                int maxDura = durability > 0 ? durability
                    : Bukkit.getPluginManager().getPlugin("calitina-pvp")
                        .getConfig().getInt("ancient-core-durability", 100);
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
                    : Bukkit.getPluginManager().getPlugin("calitina-pvp")
                        .getConfig().getInt("fatal-core-base-uses", 10);
                yield items.createFatalCore(uses);
            }
            default -> {
                sender.sendMessage(CustomItems.mm("<red>未知物品: " + itemArg
                    + "。可用: " + String.join(", ", ITEM_NAMES) + "</red>"));
                yield null;
            }
        };

        if (result == null) return true;

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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String label,
                                                 @NotNull String[] args) {
        if (!sender.isOp() && !(sender instanceof ConsoleCommandSender)) {
            return List.of();
        }

        if (args.length == 1) {
            // Subcommand: give
            if ("give".startsWith(args[0].toLowerCase())) {
                return List.of("give");
            }
            return List.of();
        }

        if (!args[0].equalsIgnoreCase("give")) {
            return List.of();
        }

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
}
