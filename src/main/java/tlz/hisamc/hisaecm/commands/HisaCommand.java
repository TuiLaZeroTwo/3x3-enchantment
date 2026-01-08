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
        
        // Default /hisaecm -> Show Help
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("enchant")) {
            if (sender instanceof Player player) {
                EnchantMenu.open(player);
            } else {
                sender.sendMessage(Component.text("Console cannot open GUI.", NamedTextColor.RED));
            }
            return true;
        }

        if (sub.equals("shop")) {
            if (sender instanceof Player player) {
                ShopMenu.open(player);
            } else {
                sender.sendMessage(Component.text("Console cannot open GUI.", NamedTextColor.RED));
            }
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

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- Hisa-ECM Help ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hisaecm enchant", NamedTextColor.YELLOW).append(Component.text(" - Open Enchant GUI", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hisaecm shop", NamedTextColor.YELLOW).append(Component.text(" - Open Item Shop", NamedTextColor.GRAY)));
        if (sender.hasPermission("hisaecm.admin")) {
            sender.sendMessage(Component.text("/hisaecm reload", NamedTextColor.RED).append(Component.text(" - Reload Config", NamedTextColor.GRAY)));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("enchant");
            list.add("shop");
            if (sender.hasPermission("hisaecm.admin")) list.add("reload");
        }
        return list;
    }
}