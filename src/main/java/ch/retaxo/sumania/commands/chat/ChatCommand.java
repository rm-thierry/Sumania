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
                    sender.sendMessage("§cYou don't have permission to do that.");
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
                    sender.sendMessage("§cYou don't have permission to do that.");
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
                    sender.sendMessage("§cYou don't have permission to do that.");
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
        sender.sendMessage("§8===== §6Sumania Chat §8=====");
        
        if (sender.hasPermission("sumania.chat.mute")) {
            sender.sendMessage("§6/chat mute §7- Mute the chat");
            sender.sendMessage("§6/chat unmute §7- Unmute the chat");
        }
        
        if (sender.hasPermission("sumania.chat.clear")) {
            sender.sendMessage("§6/chat clear §7- Clear the chat");
        }
    }
}
