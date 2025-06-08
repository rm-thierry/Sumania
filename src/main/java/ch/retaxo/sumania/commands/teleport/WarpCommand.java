package ch.retaxo.sumania.commands.teleport;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to manage warps
 */
public class WarpCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public WarpCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("warp")) {
            // Warp to a location
            return handleWarp(sender, args);
        } else if (label.equalsIgnoreCase("setwarp")) {
            // Set a warp
            return handleSetWarp(sender, args);
        } else if (label.equalsIgnoreCase("delwarp")) {
            // Delete a warp
            return handleDeleteWarp(sender, args);
        } else if (label.equalsIgnoreCase("warps")) {
            // List warps
            return handleListWarps(sender, args);
        }
        
        return false;
    }
    
    /**
     * Handle warp command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 1) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/warp <name>");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        String warpName = args[0];
        Location warpLocation = plugin.getAPI().getTeleportAPI().getWarp(warpName);
        
        if (warpLocation == null) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("warp", warpName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "warp.warp-not-found",
                    replacements
            );
            
            return true;
        }
        
        // Check if player has permission to use this warp
        if (!player.hasPermission("sumania.warp.use." + warpName.toLowerCase()) &&
                !player.hasPermission("sumania.warp.use.*")) {
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.no-permission",
                    null
            );
            
            return true;
        }
        
        // Send teleport message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("warp", warpName);
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "warp.warp-teleport",
                replacements
        );
        
        // Teleport player
        plugin.getAPI().getTeleportAPI().teleport(player, warpLocation);
        
        return true;
    }
    
    /**
     * Handle set warp command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleSetWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("sumania.warp.set")) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.no-permission",
                    null
            );
            
            return true;
        }
        
        if (args.length != 1) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/setwarp <name>");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        String warpName = args[0];
        
        // Set warp
        plugin.getAPI().getTeleportAPI().setWarp(warpName, player.getLocation());
        
        // Send message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("warp", warpName);
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "warp.warp-created",
                replacements
        );
        
        return true;
    }
    
    /**
     * Handle delete warp command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleDeleteWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sumania.warp.delete")) {
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
        
        if (args.length != 1) {
            if (sender instanceof Player) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("usage", "/delwarp <name>");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.invalid-args",
                        replacements
                );
            } else {
                sender.sendMessage("§cUsage: /delwarp <name>");
            }
            
            return true;
        }
        
        String warpName = args[0];
        
        // Delete warp
        if (plugin.getAPI().getTeleportAPI().deleteWarp(warpName)) {
            // Send message
            if (sender instanceof Player) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("warp", warpName);
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "warp.warp-deleted",
                        replacements
                );
            } else {
                sender.sendMessage("§aWarp " + warpName + " has been deleted.");
            }
        } else {
            // Warp not found
            if (sender instanceof Player) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("warp", warpName);
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "warp.warp-not-found",
                        replacements
                );
            } else {
                sender.sendMessage("§cWarp " + warpName + " not found.");
            }
        }
        
        return true;
    }
    
    /**
     * Handle list warps command
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleListWarps(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sumania.warp.list")) {
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
        
        if (args.length != 0) {
            if (sender instanceof Player) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("usage", "/warps");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.invalid-args",
                        replacements
                );
            } else {
                sender.sendMessage("§cUsage: /warps");
            }
            
            return true;
        }
        
        Map<String, Location> warps = plugin.getAPI().getTeleportAPI().getWarps();
        
        if (warps.isEmpty()) {
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "warp.warp-list",
                        Map.of("warps", "None")
                );
            } else {
                sender.sendMessage("§aAvailable warps: None");
            }
            
            return true;
        }
        
        StringBuilder warpsList = new StringBuilder();
        
        for (String warpName : warps.keySet()) {
            if (warpsList.length() > 0) {
                warpsList.append(", ");
            }
            
            warpsList.append(warpName);
        }
        
        if (sender instanceof Player) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("warps", warpsList.toString());
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    (Player) sender,
                    "warp.warp-list",
                    replacements
            );
        } else {
            sender.sendMessage("§aAvailable warps: " + warpsList.toString());
        }
        
        return true;
    }
}
