package ch.retaxo.sumania.commands.teleport;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Home;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to manage homes
 */
public class HomeCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public HomeCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (label.equalsIgnoreCase("home")) {
            // Teleport to home
            if (args.length == 0) {
                // Default home
                teleportToHome(player, "home");
            } else if (args.length == 1) {
                // Named home
                teleportToHome(player, args[0]);
            } else {
                // Invalid arguments
                Map<String, String> replacements = new HashMap<>();
                replacements.put("usage", "/home [name]");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.invalid-args",
                        replacements
                );
            }
            
            return true;
        } else if (label.equalsIgnoreCase("sethome")) {
            // Set home
            if (args.length == 0) {
                // Default home
                setHome(player, "home");
            } else if (args.length == 1) {
                // Named home
                setHome(player, args[0]);
            } else {
                // Invalid arguments
                Map<String, String> replacements = new HashMap<>();
                replacements.put("usage", "/sethome [name]");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.invalid-args",
                        replacements
                );
            }
            
            return true;
        } else if (label.equalsIgnoreCase("delhome")) {
            // Delete home
            if (args.length == 0) {
                // Default home
                deleteHome(player, "home");
            } else if (args.length == 1) {
                // Named home
                deleteHome(player, args[0]);
            } else {
                // Invalid arguments
                Map<String, String> replacements = new HashMap<>();
                replacements.put("usage", "/delhome [name]");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.invalid-args",
                        replacements
                );
            }
            
            return true;
        } else if (label.equalsIgnoreCase("homes")) {
            // List homes
            if (args.length == 0) {
                // Own homes
                listHomes(player);
            } else {
                // Invalid arguments
                Map<String, String> replacements = new HashMap<>();
                replacements.put("usage", "/homes");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.invalid-args",
                        replacements
                );
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Teleport a player to a home
     * @param player The player
     * @param homeName The name of the home
     */
    private void teleportToHome(Player player, String homeName) {
        Home home = plugin.getAPI().getPlayerAPI().getHome(player, homeName);
        
        if (home == null) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("home", homeName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "home.home-not-found",
                    replacements
            );
            
            return;
        }
        
        // Send teleport message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("home", homeName);
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "home.home-teleport",
                replacements
        );
        
        // Teleport player
        plugin.getAPI().getTeleportAPI().teleport(player, home.getLocation());
    }
    
    /**
     * Set a home for a player
     * @param player The player
     * @param homeName The name of the home
     */
    private void setHome(Player player, String homeName) {
        // Check if player has reached home limit
        if (!player.hasPermission("sumania.home.bypass.limit") &&
                plugin.getAPI().getPlayerAPI().hasReachedHomeLimit(player) &&
                plugin.getAPI().getPlayerAPI().getHome(player, homeName) == null) {
            
            Map<String, String> replacements = new HashMap<>();
            replacements.put("limit", String.valueOf(plugin.getAPI().getPlayerAPI().getHomeLimit(player)));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "home.home-limit-reached",
                    replacements
            );
            
            return;
        }
        
        // Set home
        plugin.getAPI().getPlayerAPI().setHome(player, homeName, player.getLocation());
        
        // Send message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("home", homeName);
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "home.home-set",
                replacements
        );
    }
    
    /**
     * Delete a home for a player
     * @param player The player
     * @param homeName The name of the home
     */
    private void deleteHome(Player player, String homeName) {
        if (plugin.getAPI().getPlayerAPI().deleteHome(player, homeName)) {
            // Send message
            Map<String, String> replacements = new HashMap<>();
            replacements.put("home", homeName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "home.home-deleted",
                    replacements
            );
        } else {
            // Home not found
            Map<String, String> replacements = new HashMap<>();
            replacements.put("home", homeName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "home.home-not-found",
                    replacements
            );
        }
    }
    
    /**
     * List all homes for a player
     * @param player The player
     */
    private void listHomes(Player player) {
        Map<String, Home> homes = plugin.getAPI().getPlayerAPI().getHomes(player);
        
        if (homes.isEmpty()) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "home.home-list",
                    Map.of("homes", "None")
            );
            
            return;
        }
        
        StringBuilder homesList = new StringBuilder();
        
        for (String homeName : homes.keySet()) {
            if (homesList.length() > 0) {
                homesList.append(", ");
            }
            
            homesList.append(homeName);
        }
        
        Map<String, String> replacements = new HashMap<>();
        replacements.put("homes", homesList.toString());
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "home.home-list",
                replacements
        );
    }
}
