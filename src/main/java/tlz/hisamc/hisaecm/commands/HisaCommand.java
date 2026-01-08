package tlz.hisamc.hisaecm.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.BountyMenu;
import tlz.hisamc.hisaecm.gui.EnchantMenu;
import tlz.hisamc.hisaecm.gui.MainMenu;
import tlz.hisamc.hisaecm.gui.ShopMenu;

import java.util.ArrayList;
import java.util.List;

public class HisaCommand implements CommandExecutor, TabCompleter {

    private final HisaECM plugin;

    public HisaCommand(HisaECM plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        
        // --- HANDLE ALIAS: /bounty ---
        if (command.getName().equalsIgnoreCase("bounty")) {
            if (sender instanceof Player player) {
                BountyMenu.open(player, 0);
            } else {
                sender.sendMessage(Component.text("Console cannot open GUI.", NamedTextColor.RED));
            }
            return true;
        }

        // --- MAIN COMMAND: /hisaecm ---
        
        // 1. No Arguments -> Open Main Menu
        if (args.length == 0) {
            if (sender instanceof Player player) {
                MainMenu.open(player);
            } else {
                sender.sendMessage(Component.text("Console cannot open GUI.", NamedTextColor.RED));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // 2. Subcommands
        switch (sub) {
            case "enchant":
                if (sender instanceof Player player) EnchantMenu.open(player);
                return true;

            case "shop":
                if (sender instanceof Player player) ShopMenu.open(player);
                return true;

            case "bounty": // New Bounty Subcommand
                if (sender instanceof Player player) BountyMenu.open(player, 0);
                return true;
            
            case "show": // Explicit "Show" command
                if (sender instanceof Player player) MainMenu.open(player);
                return true;

            case "reload":
                if (sender.hasPermission("hisaecm.admin")) {
                    plugin.reloadConfig();
                    sender.sendMessage(Component.text("Reloaded configuration.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                }
                return true;
                
            default:
                sender.sendMessage(Component.text("Unknown command. Try /hisaecm", NamedTextColor.RED));
                return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        
        // Only provide completions for /hisaecm (not /bounty which has no args usually)
        if (cmd.getName().equalsIgnoreCase("hisaecm") && args.length == 1) {
            list.add("enchant");
            list.add("shop");
            list.add("bounty");
            list.add("show");
            if (sender.hasPermission("hisaecm.admin")) list.add("reload");
        }
        return list;
    }
}