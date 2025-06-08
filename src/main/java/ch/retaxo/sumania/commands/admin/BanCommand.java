package ch.retaxo.sumania.commands.admin;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ch.retaxo.sumania.models.Home;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Command to ban players
 */
public class BanCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public BanCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("sumania.ban")) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                sender instanceof Player ? (Player) sender : null,
                "general.no-permission",
                null
            );
            return true;
        }
        
        String adminName = sender instanceof Player ? sender.getName() : "Console";
        
        // Handle different ban commands
        if (label.equalsIgnoreCase("ban")) {
            return handleBanCommand(sender, args, adminName);
        } else if (label.equalsIgnoreCase("tempban")) {
            return handleTempBanCommand(sender, args, adminName);
        } else if (label.equalsIgnoreCase("unban")) {
            return handleUnbanCommand(sender, args, adminName);
        } else if (label.equalsIgnoreCase("lookup")) {
            return handleLookupCommand(sender, args);
        }
        
        return false;
    }
    
    /**
     * Handle the /ban command
     */
    private boolean handleBanCommand(CommandSender sender, String[] args, String adminName) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ban <player> <reason>");
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Check if player exists
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return true;
        }
        
        // Build ban reason
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();
        
        // Ban the player
        plugin.getAPI().getPlayerAPI().banPlayer(target, reason, adminName, -1);
        
        return true;
    }
    
    /**
     * Handle the /tempban command
     */
    private boolean handleTempBanCommand(CommandSender sender, String[] args, String adminName) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /tempban <player> <duration> <reason>");
            sender.sendMessage("§cDuration format: 1d2h3m (days, hours, minutes)");
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Check if player exists
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return true;
        }
        
        // Parse duration
        String durationStr = args[1];
        long duration = parseDuration(durationStr);
        
        if (duration <= 0) {
            sender.sendMessage("§cInvalid duration format: " + durationStr);
            sender.sendMessage("§cUse format like: 1d2h3m (days, hours, minutes)");
            return true;
        }
        
        // Build ban reason
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();
        
        // Ban the player
        plugin.getAPI().getPlayerAPI().banPlayer(target, reason, adminName, duration);
        
        return true;
    }
    
    /**
     * Handle the /unban command
     */
    private boolean handleUnbanCommand(CommandSender sender, String[] args, String adminName) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /unban <player>");
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Check if player exists
        if (!target.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return true;
        }
        
        // Check if player is banned
        if (!plugin.getAPI().getPlayerAPI().isBanned(target)) {
            sender.sendMessage("§cPlayer is not banned: " + targetName);
            return true;
        }
        
        // Unban the player
        plugin.getAPI().getPlayerAPI().unbanPlayer(target, adminName);
        
        return true;
    }
    
    /**
     * Handle the /lookup command
     */
    private boolean handleLookupCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /lookup <player>");
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Check if player exists
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return true;
        }
        
        // Send player info
        sender.sendMessage("§6=== Player Information for §e" + target.getName() + " §6===");
        sender.sendMessage("§7UUID: §f" + target.getUniqueId());
        sender.sendMessage("§7Online: §f" + (target.isOnline() ? "Yes" : "No"));
        
        // Check if player is banned
        if (plugin.getAPI().getPlayerAPI().isBanned(target)) {
            String reason = plugin.getAPI().getPlayerAPI().getBanReason(target);
            long expiration = plugin.getAPI().getPlayerAPI().getBanExpiration(target);
            
            sender.sendMessage("§7Ban Status: §c§lBANNED");
            sender.sendMessage("§7Reason: §f" + reason);
            
            String banPath = "players." + target.getUniqueId() + ".ban";
            String admin = plugin.getConfigManager().getConfig("data.yml")
                    .getString(banPath + ".admin", "Unknown");
            long banTime = plugin.getConfigManager().getConfig("data.yml")
                    .getLong(banPath + ".time", 0);
            
            sender.sendMessage("§7Banned by: §f" + admin);
            sender.sendMessage("§7Banned on: §f" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(banTime)));
            
            if (expiration > 0) {
                sender.sendMessage("§7Expires: §f" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(expiration)));
                sender.sendMessage("§7Time left: §f" + plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis()));
            } else {
                sender.sendMessage("§7Expires: §4NEVER");
            }
        } else {
            sender.sendMessage("§7Ban Status: §aNot Banned");
        }
        
        // Player stats
        sender.sendMessage("§7Kills: §f" + plugin.getAPI().getPlayerAPI().getKills(target));
        sender.sendMessage("§7Deaths: §f" + plugin.getAPI().getPlayerAPI().getDeaths(target));
        
        // Player economy
        double balance = plugin.getAPI().getEconomyAPI().getBalance(target);
        String formatted = plugin.getAPI().getEconomyAPI().format(balance);
        sender.sendMessage("§7Balance: §f" + formatted);
        
        // Player homes
        Map<String, Home> homes = plugin.getAPI().getPlayerAPI().getHomes(target);
        sender.sendMessage("§7Homes: §f" + homes.size());
        
        return true;
    }
    
    /**
     * Parse a duration string into milliseconds
     * @param durationStr The duration string (e.g. 1d2h3m)
     * @return The duration in milliseconds
     */
    private long parseDuration(String durationStr) {
        long duration = 0;
        
        StringBuilder numBuilder = new StringBuilder();
        for (int i = 0; i < durationStr.length(); i++) {
            char c = durationStr.charAt(i);
            
            if (Character.isDigit(c)) {
                numBuilder.append(c);
            } else {
                if (numBuilder.length() > 0) {
                    int num = Integer.parseInt(numBuilder.toString());
                    numBuilder = new StringBuilder();
                    
                    if (c == 'd' || c == 'D') {
                        duration += TimeUnit.DAYS.toMillis(num);
                    } else if (c == 'h' || c == 'H') {
                        duration += TimeUnit.HOURS.toMillis(num);
                    } else if (c == 'm' || c == 'M') {
                        duration += TimeUnit.MINUTES.toMillis(num);
                    } else if (c == 's' || c == 'S') {
                        duration += TimeUnit.SECONDS.toMillis(num);
                    }
                }
            }
        }
        
        // Handle case where the string ends with a number
        if (numBuilder.length() > 0) {
            int num = Integer.parseInt(numBuilder.toString());
            duration += TimeUnit.MINUTES.toMillis(num); // Default to minutes
        }
        
        return duration;
    }
}