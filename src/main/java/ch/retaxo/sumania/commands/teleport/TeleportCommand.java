package ch.retaxo.sumania.commands.teleport;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.api.teleport.TeleportRequestType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to teleport players
 */
public class TeleportCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public TeleportCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("tp")) {
            return handleTeleport(sender, args);
        } else if (label.equalsIgnoreCase("tpa")) {
            return handleTeleportRequest(sender, args);
        } else if (label.equalsIgnoreCase("tpaccept")) {
            return handleTeleportAccept(sender, args);
        } else if (label.equalsIgnoreCase("tpdeny")) {
            return handleTeleportDeny(sender, args);
        }
        
        return false;
    }
    
    /**
     * Handle teleport command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length != 2) {
            sender.sendMessage("§cUsage: /tp <player> [target]");
            return true;
        }
        
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                
                Map<String, String> replacements = new HashMap<>();
                replacements.put("usage", "/tp <player> [target]");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.invalid-args",
                        replacements
                );
            } else {
                sender.sendMessage("§cUsage: /tp <player> <target>");
            }
            
            return true;
        }
        
        if (args.length == 1) {
            // Teleport sender to player
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou must specify both players when using this command from console.");
                return true;
            }
            
            Player player = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);
            
            if (target == null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("player", args[0]);
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.player-not-found",
                        replacements
                );
                
                return true;
            }
            
            if (!player.hasPermission("sumania.teleport.tp")) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.no-permission",
                        null
                );
                
                return true;
            }
            
            // Teleport player
            plugin.getAPI().getTeleportAPI().teleport(player, target.getLocation());
            
            return true;
        } else if (args.length == 2) {
            // Teleport player1 to player2
            Player player1 = Bukkit.getPlayer(args[0]);
            Player player2 = Bukkit.getPlayer(args[1]);
            
            if (player1 == null) {
                if (sender instanceof Player) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("player", args[0]);
                    
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "general.player-not-found",
                            replacements
                    );
                } else {
                    sender.sendMessage("§cPlayer " + args[0] + " not found.");
                }
                
                return true;
            }
            
            if (player2 == null) {
                if (sender instanceof Player) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("player", args[1]);
                    
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "general.player-not-found",
                            replacements
                    );
                } else {
                    sender.sendMessage("§cPlayer " + args[1] + " not found.");
                }
                
                return true;
            }
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                
                if (!player.hasPermission("sumania.teleport.tp.others")) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "general.no-permission",
                            null
                    );
                    
                    return true;
                }
            }
            
            // Teleport player
            plugin.getAPI().getTeleportAPI().teleport(player1, player2.getLocation());
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle teleport request command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleTeleportRequest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 1) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/tpa <player>");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", args[0]);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.player-not-found",
                    replacements
            );
            
            return true;
        }
        
        if (target == player) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/tpa <player>");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        // Create teleport request
        plugin.getAPI().getTeleportAPI().createTeleportRequest(player, target, TeleportRequestType.TO_PLAYER);
        
        return true;
    }
    
    /**
     * Handle teleport accept command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleTeleportAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 0) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/tpaccept");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        // Accept teleport request
        plugin.getAPI().getTeleportAPI().acceptTeleportRequest(player);
        
        return true;
    }
    
    /**
     * Handle teleport deny command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleTeleportDeny(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 0) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/tpdeny");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        // Deny teleport request
        plugin.getAPI().getTeleportAPI().denyTeleportRequest(player);
        
        return true;
    }
}
