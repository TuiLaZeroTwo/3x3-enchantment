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
import tlz.hisamc.hisaecm.gui.EnchantMenu;

import java.util.ArrayList;
import java.util.List;

public class HisaCommand implements CommandExecutor, TabCompleter {

    private final HisaECM plugin;

    public HisaCommand(HisaECM plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        
        // Default to opening GUI if no args provided (Optional convenience)
        if (args.length == 0) {
            if (sender instanceof Player player) {
                EnchantMenu.open(player);
            } else {
                sender.sendMessage(Component.text("Console cannot open GUI. Use: /hisaecm reload", NamedTextColor.RED));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // --- SUBCOMMAND: /hisaecm gui ---
        if (subCommand.equals("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                return true;
            }
            EnchantMenu.open(player);
            return true;
        }

        // --- SUBCOMMAND: /hisaecm reload ---
        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("hisaecm.admin")) {
                sender.sendMessage(Component.text("You do not have permission to reload.", NamedTextColor.RED));
                return true;
            }

            plugin.reloadConfig();
            sender.sendMessage(Component.text("Hisa-ECM configuration reloaded successfully!", NamedTextColor.GREEN));
            return true;
        }

        // Unknown command
        sender.sendMessage(Component.text("Usage: /hisaecm [gui|reload]", NamedTextColor.RED));
        return true;
    }

    // --- TAB COMPLETION ---
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("gui");
            if (sender.hasPermission("hisaecm.admin")) {
                completions.add("reload");
            }
        }
        return completions;
    }
}