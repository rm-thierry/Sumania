package ch.retaxo.sumania.commands.chat;

import ch.retaxo.sumania.Sumania;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to manage chat
 */
public class ChatCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public ChatCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("mute")) {
            if (!sender.hasPermission("sumania.chat.mute")) {
                if (sender instanceof Player) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "general.no-permission",
                            null
                    );
                } else {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cDu hast keine Berechtigung dafür.");
                }
                
                return true;
            }
            
            plugin.getAPI().getChatAPI().setChatMuted(true);
            plugin.getAPI().getChatAPI().broadcastMessage("chat.chat-muted", null);
            
            return true;
        } else if (args[0].equalsIgnoreCase("unmute")) {
            if (!sender.hasPermission("sumania.chat.mute")) {
                if (sender instanceof Player) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "general.no-permission",
                            null
                    );
                } else {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cDu hast keine Berechtigung dafür.");
                }
                
                return true;
            }
            
            plugin.getAPI().getChatAPI().setChatMuted(false);
            plugin.getAPI().getChatAPI().broadcastMessage("chat.chat-unmuted", null);
            
            return true;
        } else if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("sumania.chat.clear")) {
                if (sender instanceof Player) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "general.no-permission",
                            null
                    );
                } else {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cDu hast keine Berechtigung dafür.");
                }
                
                return true;
            }
            
            plugin.getAPI().getChatAPI().clearChat();
            
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
        String prefix = plugin.getConfigManager().getPrefix();
        
        sender.sendMessage(prefix + "§8• §b§lSumania Chat §8•");
        
        if (sender.hasPermission("sumania.chat.mute")) {
            sender.sendMessage(prefix + "§b/chat mute §7- Chat stummschalten");
            sender.sendMessage(prefix + "§b/chat unmute §7- Chat aktivieren");
        }
        
        if (sender.hasPermission("sumania.chat.clear")) {
            sender.sendMessage(prefix + "§b/chat clear §7- Chat löschen");
        }
    }
}
