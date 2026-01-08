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
        
        // --- NO ARGS: Open Main Menu ---
        if (args.length == 0) {
            if (sender instanceof Player player) {
                MainMenu.open(player);
            } else {
                sender.sendMessage(Component.text("Console cannot open GUI.", NamedTextColor.RED));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // Subcommands (Shortcuts)
        if (sub.equals("enchant")) {
            if (sender instanceof Player player) EnchantMenu.open(player);
            return true;
        }

        if (sub.equals("shop")) {
            if (sender instanceof Player player) ShopMenu.open(player);
            return true;
        }
        
        if (sub.equals("show")) { // Explicit "Show" command if user wants it
             if (sender instanceof Player player) MainMenu.open(player);
             return true;
        }

        if (sub.equals("reload")) {
            if (sender.hasPermission("hisaecm.admin")) {
                plugin.reloadConfig();
                sender.sendMessage(Component.text("Reloaded configuration.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            }
            return true;
        }

        sender.sendMessage(Component.text("Unknown command. Try /hisaecm", NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("enchant");
            list.add("shop");
            list.add("show");
            if (sender.hasPermission("hisaecm.admin")) list.add("reload");
        }
        return list;
    }
}