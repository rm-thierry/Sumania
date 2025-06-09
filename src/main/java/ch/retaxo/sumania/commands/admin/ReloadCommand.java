package ch.retaxo.sumania.commands.admin;

import ch.retaxo.sumania.Sumania;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

/**
 * Command to reload the plugin
 */
public class ReloadCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public ReloadCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sumania.admin.reload")) {
                if (sender instanceof Player) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "general.no-permission",
                            null
                    );
                } else {
                    sender.sendMessage("§cYou don't have permission to do that.");
                }
                
                return true;
            }
            
            // Reload configuration
            plugin.getConfigManager().reloadAllConfigs();
            
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.reload-success",
                        null
                );
            } else {
                sender.sendMessage("§aAll configuration files have been reloaded.");
            }
            
            return true;
        }
        
        sendHelp(sender);
        return true;
    }
    
    /**
     * Send help information to a player
     * @param sender The command sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8• §b§lSumania §8•");
        sender.sendMessage("§b/sumania reload §7- Reload the plugin");
    }
}
