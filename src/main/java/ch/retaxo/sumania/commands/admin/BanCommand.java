package ch.retaxo.sumania.commands.admin;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

import ch.retaxo.sumania.models.Home;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Command to ban players
 */
public class BanCommand implements CommandExecutor, TabCompleter {

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
        } else if (label.equalsIgnoreCase("banhistory")) {
            return handleBanHistoryCommand(sender, args);
        } else if (label.equalsIgnoreCase("mutehistory")) {
            return handleMuteHistoryCommand(sender, args);
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
        
        // Send player info header
        sender.sendMessage("§6=== Player Information for §e" + target.getName() + " §6===");
        
        // Only use fancy components if sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            sendFancyLookup(player, target);
        } else {
            sendPlainLookup(sender, target);
        }
        
        return true;
    }
    
    /**
     * Send fancy lookup information with hover and click effects
     */
    private void sendFancyLookup(Player player, OfflinePlayer target) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        
        // Basic info section
        TextComponent uuidComponent = new TextComponent("§7UUID: §f" + target.getUniqueId());
        uuidComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new Text("§7Click to copy UUID to clipboard")));
        uuidComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, 
            target.getUniqueId().toString()));
        player.spigot().sendMessage(uuidComponent);
        
        // Online status with last seen if offline
        TextComponent onlineComponent = new TextComponent("§7Online: §f" + (target.isOnline() ? "§aYes" : "§cNo"));
        if (!target.isOnline() && target.getLastPlayed() > 0) {
            onlineComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text("§7Last seen: §f" + dateFormat.format(new Date(target.getLastPlayed())))));
        }
        player.spigot().sendMessage(onlineComponent);
        
        // Ban status section with hover details
        boolean isBanned = plugin.getAPI().getPlayerAPI().isBanned(target);
        TextComponent banComponent = new TextComponent(isBanned ? 
            "§7Ban Status: §c§lBANNED" : "§7Ban Status: §aNot Banned");
        
        if (isBanned) {
            String reason = plugin.getAPI().getPlayerAPI().getBanReason(target);
            long expiration = plugin.getAPI().getPlayerAPI().getBanExpiration(target);
            
            // Try to get admin and ban time from database (more reliable than before)
            String admin = "Unknown";
            long banTime = 0;
            List<Map<String, Object>> banHistory = plugin.getAPI().getPlayerAPI().getBanHistory(target);
            if (!banHistory.isEmpty()) {
                Map<String, Object> latestBan = banHistory.get(0);
                admin = (String) latestBan.get("admin");
                banTime = (long) latestBan.get("time");
            }
            
            StringBuilder hoverText = new StringBuilder();
            hoverText.append("§cReason: §f").append(reason).append("\n");
            hoverText.append("§cBanned by: §f").append(admin).append("\n");
            hoverText.append("§cBanned on: §f").append(dateFormat.format(new Date(banTime))).append("\n");
            
            if (expiration > 0) {
                hoverText.append("§cExpires: §f").append(dateFormat.format(new Date(expiration))).append("\n");
                hoverText.append("§cTime left: §f").append(plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis()));
            } else {
                hoverText.append("§cExpires: §4NEVER");
            }
            
            banComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText.toString())));
            banComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/banhistory " + target.getName()));
            player.spigot().sendMessage(banComponent);
            
            // Basic ban info without hover
            player.sendMessage("§7Reason: §f" + reason);
            
            if (expiration > 0) {
                player.sendMessage("§7Expires: §f" + dateFormat.format(new Date(expiration)));
                player.sendMessage("§7Time left: §f" + plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis()));
            } else {
                player.sendMessage("§7Expires: §4NEVER");
            }
        } else {
            banComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text("§7Click to view ban history")));
            banComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/banhistory " + target.getName()));
            player.spigot().sendMessage(banComponent);
        }
        
        // Stats section with hover details
        int kills = plugin.getAPI().getPlayerAPI().getKills(target);
        int deaths = plugin.getAPI().getPlayerAPI().getDeaths(target);
        float kdr = deaths > 0 ? (float) kills / deaths : kills;
        
        TextComponent statsComponent = new TextComponent("§7Stats: §fKills: " + kills + " §7/ §fDeaths: " + deaths);
        statsComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new Text("§7K/D Ratio: §f" + String.format("%.2f", kdr))));
        player.spigot().sendMessage(statsComponent);
        
        // Economy section with hover
        double balance = plugin.getAPI().getEconomyAPI().getBalance(target);
        String formatted = plugin.getAPI().getEconomyAPI().format(balance);
        
        TextComponent economyComponent = new TextComponent("§7Balance: §f" + formatted);
        if (player.hasPermission("sumania.economy.admin")) {
            economyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text("§7Click to manage balance")));
            economyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                "/eco give " + target.getName() + " "));
        }
        player.spigot().sendMessage(economyComponent);
        
        // Homes section with hover to list homes
        Map<String, Home> homes = plugin.getAPI().getPlayerAPI().getHomes(target);
        TextComponent homesComponent = new TextComponent("§7Homes: §f" + homes.size());
        
        if (!homes.isEmpty()) {
            StringBuilder homesList = new StringBuilder("§7Homes: \n");
            for (String homeName : homes.keySet()) {
                Home home = homes.get(homeName);
                homesList.append("§f- ").append(homeName).append(" §7(")
                        .append(home.getLocation().getWorld().getName()).append(", ")
                        .append((int) home.getLocation().getX()).append(", ")
                        .append((int) home.getLocation().getY()).append(", ")
                        .append((int) home.getLocation().getZ()).append(")\n");
            }
            homesComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(homesList.toString())));
        }
        player.spigot().sendMessage(homesComponent);
    }
    
    /**
     * Send plain lookup information (for console)
     */
    private void sendPlainLookup(CommandSender sender, OfflinePlayer target) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        
        // Basic info
        sender.sendMessage("§7UUID: §f" + target.getUniqueId());
        sender.sendMessage("§7Online: §f" + (target.isOnline() ? "Yes" : "No"));
        if (!target.isOnline() && target.getLastPlayed() > 0) {
            sender.sendMessage("§7Last seen: §f" + dateFormat.format(new Date(target.getLastPlayed())));
        }
        
        // Ban status
        if (plugin.getAPI().getPlayerAPI().isBanned(target)) {
            String reason = plugin.getAPI().getPlayerAPI().getBanReason(target);
            long expiration = plugin.getAPI().getPlayerAPI().getBanExpiration(target);
            
            sender.sendMessage("§7Ban Status: §c§lBANNED");
            sender.sendMessage("§7Reason: §f" + reason);
            
            // Try to get admin and ban time from database
            String admin = "Unknown";
            long banTime = 0;
            List<Map<String, Object>> banHistory = plugin.getAPI().getPlayerAPI().getBanHistory(target);
            if (!banHistory.isEmpty()) {
                Map<String, Object> latestBan = banHistory.get(0);
                admin = (String) latestBan.get("admin");
                banTime = (long) latestBan.get("time");
            }
            
            sender.sendMessage("§7Banned by: §f" + admin);
            sender.sendMessage("§7Banned on: §f" + dateFormat.format(new Date(banTime)));
            
            if (expiration > 0) {
                sender.sendMessage("§7Expires: §f" + dateFormat.format(new Date(expiration)));
                sender.sendMessage("§7Time left: §f" + plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis()));
            } else {
                sender.sendMessage("§7Expires: §4NEVER");
            }
        } else {
            sender.sendMessage("§7Ban Status: §aNot Banned");
        }
        
        // Player stats
        int kills = plugin.getAPI().getPlayerAPI().getKills(target);
        int deaths = plugin.getAPI().getPlayerAPI().getDeaths(target);
        float kdr = deaths > 0 ? (float) kills / deaths : kills;
        
        sender.sendMessage("§7Kills: §f" + kills);
        sender.sendMessage("§7Deaths: §f" + deaths);
        sender.sendMessage("§7K/D Ratio: §f" + String.format("%.2f", kdr));
        
        // Player economy
        double balance = plugin.getAPI().getEconomyAPI().getBalance(target);
        String formatted = plugin.getAPI().getEconomyAPI().format(balance);
        sender.sendMessage("§7Balance: §f" + formatted);
        
        // Player homes
        Map<String, Home> homes = plugin.getAPI().getPlayerAPI().getHomes(target);
        sender.sendMessage("§7Homes: §f" + homes.size());
        
        if (!homes.isEmpty()) {
            sender.sendMessage("§7Home Locations:");
            for (String homeName : homes.keySet()) {
                Home home = homes.get(homeName);
                sender.sendMessage("  §f- " + homeName + " §7(" + 
                        home.getLocation().getWorld().getName() + ", " +
                        (int) home.getLocation().getX() + ", " +
                        (int) home.getLocation().getY() + ", " +
                        (int) home.getLocation().getZ() + ")");
            }
        }
    }
    
    /**
     * Handle the /banhistory command
     */
    private boolean handleBanHistoryCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /banhistory <player>");
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Check if player exists
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return true;
        }
        
        // Get ban history
        List<Map<String, Object>> banHistory = plugin.getAPI().getPlayerAPI().getBanHistory(target);
        
        // Send ban history
        sender.sendMessage("§6=== Ban History for §e" + target.getName() + " §6===");
        
        if (banHistory.isEmpty()) {
            sender.sendMessage("§7No ban history found.");
            return true;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        
        // Only use fancy components if sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            sendFancyBanHistory(player, target, banHistory, dateFormat);
        } else {
            sendPlainBanHistory(sender, target, banHistory, dateFormat);
        }
        
        return true;
    }
    
    /**
     * Handle the /mutehistory command
     */
    private boolean handleMuteHistoryCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /mutehistory <player>");
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Check if player exists
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return true;
        }
        
        // Get mute history
        List<Map<String, Object>> muteHistory = plugin.getAPI().getPlayerAPI().getMuteHistory(target);
        
        // Send mute history
        sender.sendMessage("§6=== Mute History for §e" + target.getName() + " §6===");
        
        if (muteHistory.isEmpty()) {
            sender.sendMessage("§7No mute history found.");
            return true;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        
        // Only use fancy components if sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            sendFancyMuteHistory(player, target, muteHistory, dateFormat);
        } else {
            sendPlainMuteHistory(sender, target, muteHistory, dateFormat);
        }
        
        return true;
    }
    
    /**
     * Send fancy ban history with hover and click effects
     */
    private void sendFancyBanHistory(Player player, OfflinePlayer target, List<Map<String, Object>> banHistory, SimpleDateFormat dateFormat) {
        int count = 1;
        for (Map<String, Object> ban : banHistory) {
            String reason = (String) ban.get("reason");
            String admin = (String) ban.get("admin");
            long banTime = (long) ban.get("time");
            long expiration = (long) ban.get("until");
            boolean active = (boolean) ban.get("active");
            boolean expired = (boolean) ban.get("expired");
            String unbannedBy = (String) ban.get("unbanned_by");
            long unbannedTime = (long) ban.get("unbanned_time");
            
            // Create the base component with ban number and reason
            TextComponent banComponent = new TextComponent("§7#" + count + " - " + 
                    (active ? "§c§lACTIVE" : (expired ? "§e§lEXPIRED" : "§a§lUNBANNED")) + 
                    " §7- §f" + reason);
            
            // Build hover text with details
            StringBuilder hoverText = new StringBuilder();
            hoverText.append("§7Ban #").append(count).append("\n");
            hoverText.append("§7Status: ").append(active ? "§cActive" : (expired ? "§eExpired" : "§aUnbanned")).append("\n");
            hoverText.append("§7Reason: §f").append(reason).append("\n");
            hoverText.append("§7Admin: §f").append(admin).append("\n");
            hoverText.append("§7Banned on: §f").append(dateFormat.format(new Date(banTime))).append("\n");
            
            if (expiration > 0) {
                hoverText.append("§7Expires: §f").append(dateFormat.format(new Date(expiration))).append("\n");
                
                if (active) {
                    hoverText.append("§7Time left: §f").append(plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis())).append("\n");
                }
            } else if (expiration == -1) {
                hoverText.append("§7Expires: §4NEVER\n");
            }
            
            if (unbannedBy != null && !active) {
                hoverText.append("§7Unbanned by: §f").append(unbannedBy).append("\n");
                if (unbannedTime > 0) {
                    hoverText.append("§7Unbanned on: §f").append(dateFormat.format(new Date(unbannedTime))).append("\n");
                }
            }
            
            banComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText.toString())));
            
            // Send the component to the player
            player.spigot().sendMessage(banComponent);
            
            count++;
        }
        
        // Add a footer
        player.sendMessage("§7Total bans: §f" + banHistory.size());
    }
    
    /**
     * Send plain ban history (for console)
     */
    private void sendPlainBanHistory(CommandSender sender, OfflinePlayer target, List<Map<String, Object>> banHistory, SimpleDateFormat dateFormat) {
        int count = 1;
        for (Map<String, Object> ban : banHistory) {
            String reason = (String) ban.get("reason");
            String admin = (String) ban.get("admin");
            long banTime = (long) ban.get("time");
            long expiration = (long) ban.get("until");
            boolean active = (boolean) ban.get("active");
            boolean expired = (boolean) ban.get("expired");
            String unbannedBy = (String) ban.get("unbanned_by");
            long unbannedTime = (long) ban.get("unbanned_time");
            
            // Send ban info
            sender.sendMessage("§7Ban #" + count + ":");
            sender.sendMessage("  §7Status: " + (active ? "§cActive" : (expired ? "§eExpired" : "§aUnbanned")));
            sender.sendMessage("  §7Reason: §f" + reason);
            sender.sendMessage("  §7Admin: §f" + admin);
            sender.sendMessage("  §7Banned on: §f" + dateFormat.format(new Date(banTime)));
            
            if (expiration > 0) {
                sender.sendMessage("  §7Expires: §f" + dateFormat.format(new Date(expiration)));
                
                if (active) {
                    sender.sendMessage("  §7Time left: §f" + plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis()));
                }
            } else if (expiration == -1) {
                sender.sendMessage("  §7Expires: §4NEVER");
            }
            
            if (unbannedBy != null && !active) {
                sender.sendMessage("  §7Unbanned by: §f" + unbannedBy);
                if (unbannedTime > 0) {
                    sender.sendMessage("  §7Unbanned on: §f" + dateFormat.format(new Date(unbannedTime)));
                }
            }
            
            count++;
        }
        
        // Add a footer
        sender.sendMessage("§7Total bans: §f" + banHistory.size());
    }
    
    /**
     * Send fancy mute history with hover and click effects
     */
    private void sendFancyMuteHistory(Player player, OfflinePlayer target, List<Map<String, Object>> muteHistory, SimpleDateFormat dateFormat) {
        int count = 1;
        for (Map<String, Object> mute : muteHistory) {
            String reason = (String) mute.get("reason");
            String admin = (String) mute.get("admin");
            long muteTime = (long) mute.get("time");
            long expiration = (long) mute.get("until");
            boolean active = (boolean) mute.get("active");
            boolean expired = (boolean) mute.get("expired");
            String unmutedBy = (String) mute.get("unmuted_by");
            long unmutedTime = (long) mute.get("unmuted_time");
            
            // Create the base component with mute number and reason
            TextComponent muteComponent = new TextComponent("§7#" + count + " - " + 
                    (active ? "§c§lACTIVE" : (expired ? "§e§lEXPIRED" : "§a§lUNMUTED")) + 
                    " §7- §f" + reason);
            
            // Build hover text with details
            StringBuilder hoverText = new StringBuilder();
            hoverText.append("§7Mute #").append(count).append("\n");
            hoverText.append("§7Status: ").append(active ? "§cActive" : (expired ? "§eExpired" : "§aUnmuted")).append("\n");
            hoverText.append("§7Reason: §f").append(reason).append("\n");
            hoverText.append("§7Admin: §f").append(admin).append("\n");
            hoverText.append("§7Muted on: §f").append(dateFormat.format(new Date(muteTime))).append("\n");
            
            if (expiration > 0) {
                hoverText.append("§7Expires: §f").append(dateFormat.format(new Date(expiration))).append("\n");
                
                if (active) {
                    hoverText.append("§7Time left: §f").append(plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis())).append("\n");
                }
            } else if (expiration == -1) {
                hoverText.append("§7Expires: §4NEVER\n");
            }
            
            if (unmutedBy != null && !active) {
                hoverText.append("§7Unmuted by: §f").append(unmutedBy).append("\n");
                if (unmutedTime > 0) {
                    hoverText.append("§7Unmuted on: §f").append(dateFormat.format(new Date(unmutedTime))).append("\n");
                }
            }
            
            muteComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText.toString())));
            
            // Send the component to the player
            player.spigot().sendMessage(muteComponent);
            
            count++;
        }
        
        // Add a footer
        player.sendMessage("§7Total mutes: §f" + muteHistory.size());
    }
    
    /**
     * Send plain mute history (for console)
     */
    private void sendPlainMuteHistory(CommandSender sender, OfflinePlayer target, List<Map<String, Object>> muteHistory, SimpleDateFormat dateFormat) {
        int count = 1;
        for (Map<String, Object> mute : muteHistory) {
            String reason = (String) mute.get("reason");
            String admin = (String) mute.get("admin");
            long muteTime = (long) mute.get("time");
            long expiration = (long) mute.get("until");
            boolean active = (boolean) mute.get("active");
            boolean expired = (boolean) mute.get("expired");
            String unmutedBy = (String) mute.get("unmuted_by");
            long unmutedTime = (long) mute.get("unmuted_time");
            
            // Send mute info
            sender.sendMessage("§7Mute #" + count + ":");
            sender.sendMessage("  §7Status: " + (active ? "§cActive" : (expired ? "§eExpired" : "§aUnmuted")));
            sender.sendMessage("  §7Reason: §f" + reason);
            sender.sendMessage("  §7Admin: §f" + admin);
            sender.sendMessage("  §7Muted on: §f" + dateFormat.format(new Date(muteTime)));
            
            if (expiration > 0) {
                sender.sendMessage("  §7Expires: §f" + dateFormat.format(new Date(expiration)));
                
                if (active) {
                    sender.sendMessage("  §7Time left: §f" + plugin.getAPI().getPlayerAPI().formatDuration(expiration - System.currentTimeMillis()));
                }
            } else if (expiration == -1) {
                sender.sendMessage("  §7Expires: §4NEVER");
            }
            
            if (unmutedBy != null && !active) {
                sender.sendMessage("  §7Unmuted by: §f" + unmutedBy);
                if (unmutedTime > 0) {
                    sender.sendMessage("  §7Unmuted on: §f" + dateFormat.format(new Date(unmutedTime)));
                }
            }
            
            count++;
        }
        
        // Add a footer
        sender.sendMessage("§7Total mutes: §f" + muteHistory.size());
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
    
    /**
     * Tab complete for all ban-related commands
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest online players
            String partialName = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // For tempban, suggest durations
            if (alias.equalsIgnoreCase("tempban")) {
                String partialDuration = args[1].toLowerCase();
                for (String duration : new String[]{"1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d"}) {
                    if (duration.startsWith(partialDuration)) {
                        completions.add(duration);
                    }
                }
            }
            // For ban, suggest common reasons
            else if (alias.equalsIgnoreCase("ban")) {
                String partialReason = args[1].toLowerCase();
                for (String reason : new String[]{"Hacking", "Cheating", "Griefing", "Spamming", "Advertising", "Inappropriate behavior"}) {
                    if (reason.toLowerCase().startsWith(partialReason)) {
                        completions.add(reason);
                    }
                }
            }
        }
        
        return completions;
    }
}