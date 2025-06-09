package ch.retaxo.sumania.commands.smp;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.api.teleport.RandomTeleport;
import ch.retaxo.sumania.models.SMPWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command for SMP world management
 */
public class SMPCommand implements CommandExecutor, TabCompleter {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public SMPCommand(Sumania plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return showHelp(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                return showHelp(sender);
                
            case "join":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "general.player-only");
                    return true;
                }
                
                if (!sender.hasPermission("sumania.smp.join")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                return joinSMP((Player) sender);
                
            case "leave":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "general.player-only");
                    return true;
                }
                
                if (!sender.hasPermission("sumania.smp.leave")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                return leaveSMP((Player) sender);
                
            case "rtp":
            case "randomtp":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "general.player-only");
                    return true;
                }
                
                if (!sender.hasPermission("sumania.smp.rtp")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                return randomTeleport((Player) sender);
                
            case "setworld":
                if (!sender.hasPermission("sumania.smp.admin")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                if (args.length < 2) {
                    sendMessage(sender, "general.invalid-args", Map.of("usage", "/smp setworld <world>"));
                    return true;
                }
                
                return setWorld(sender, args[1]);
                
            case "setworldspawn":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, "general.player-only");
                    return true;
                }
                
                if (!sender.hasPermission("sumania.smp.admin")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                return setWorldSpawn((Player) sender);
                
            case "reset":
                if (!sender.hasPermission("sumania.smp.admin")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                    sendMessage(sender, "smp.reset-confirm");
                    return true;
                }
                
                return resetWorld(sender);
                
            case "info":
                if (!sender.hasPermission("sumania.smp.info")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                return showInfo(sender);
                
            case "enable":
                if (!sender.hasPermission("sumania.smp.admin")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                return toggleSMP(sender, true);
                
            case "disable":
                if (!sender.hasPermission("sumania.smp.admin")) {
                    sendMessage(sender, "general.no-permission");
                    return true;
                }
                
                return toggleSMP(sender, false);
                
            default:
                sendMessage(sender, "general.invalid-args", Map.of("usage", "/smp help"));
                return true;
        }
    }
    
    /**
     * Show the help message
     * @param sender The command sender
     * @return True if the command was executed successfully
     */
    private boolean showHelp(CommandSender sender) {
        boolean isAdmin = sender.hasPermission("sumania.smp.admin");
        
        sender.sendMessage(ChatColor.DARK_GRAY + "• " + ChatColor.AQUA + ChatColor.BOLD + "Sumania SMP Hilfe" + ChatColor.DARK_GRAY + " •");
        
        if (sender.hasPermission("sumania.smp.join")) {
            sender.sendMessage(ChatColor.AQUA + "/smp join" + ChatColor.GRAY + " - Betrete die SMP Welt");
        }
        
        if (sender.hasPermission("sumania.smp.leave")) {
            sender.sendMessage(ChatColor.AQUA + "/smp leave" + ChatColor.GRAY + " - Verlasse die SMP Welt");
        }
        
        if (sender.hasPermission("sumania.smp.rtp")) {
            sender.sendMessage(ChatColor.AQUA + "/smp rtp" + ChatColor.GRAY + " - Teleportiere an einen zufälligen Ort");
        }
        
        if (sender.hasPermission("sumania.smp.info")) {
            sender.sendMessage(ChatColor.AQUA + "/smp info" + ChatColor.GRAY + " - Zeige Informationen zur SMP Welt");
        }
        
        // Admin commands
        if (isAdmin) {
            sender.sendMessage(ChatColor.DARK_GRAY + "• " + ChatColor.AQUA + ChatColor.BOLD + "Admin Befehle" + ChatColor.DARK_GRAY + " •");
            sender.sendMessage(ChatColor.AQUA + "/smp setworld <world>" + ChatColor.GRAY + " - Setze die SMP Welt");
            sender.sendMessage(ChatColor.AQUA + "/smp setworldspawn" + ChatColor.GRAY + " - Setze den Spawn der SMP Welt");
            sender.sendMessage(ChatColor.AQUA + "/smp reset confirm" + ChatColor.GRAY + " - Setze die SMP Welt zurück");
            sender.sendMessage(ChatColor.AQUA + "/smp enable" + ChatColor.GRAY + " - Aktiviere das SMP System");
            sender.sendMessage(ChatColor.AQUA + "/smp disable" + ChatColor.GRAY + " - Deaktiviere das SMP System");
        }
        
        return true;
    }
    
    /**
     * Join the SMP world
     * @param player The player
     * @return True if the command was executed successfully
     */
    private boolean joinSMP(Player player) {
        // Check if SMP is enabled
        if (!plugin.getConfigManager().getConfig("config.yml").getBoolean("smp.enabled", true)) {
            sendMessage(player, "smp.disabled");
            return true;
        }
        
        SMPWorld smpWorld = plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
        
        if (smpWorld == null) {
            sendMessage(player, "smp.no-world");
            return true;
        }
        
        // Check if player is already in SMP world
        if (player.getWorld().getName().equals(smpWorld.getWorldName())) {
            sendMessage(player, "smp.already-in-world");
            return true;
        }
        
        // Teleport to SMP world
        boolean success = plugin.getAPI().getSMPWorldAPI().teleportToSMP(player);
        
        if (success) {
            sendMessage(player, "smp.joined");
        } else {
            sendMessage(player, "smp.join-failed");
        }
        
        return true;
    }
    
    /**
     * Leave the SMP world
     * @param player The player
     * @return True if the command was executed successfully
     */
    private boolean leaveSMP(Player player) {
        SMPWorld smpWorld = plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
        
        if (smpWorld == null) {
            sendMessage(player, "smp.no-world");
            return true;
        }
        
        // Check if player is in SMP world
        if (!player.getWorld().getName().equals(smpWorld.getWorldName())) {
            sendMessage(player, "smp.not-in-world");
            return true;
        }
        
        // Teleport out of SMP world
        boolean success = plugin.getAPI().getSMPWorldAPI().teleportFromSMP(player);
        
        if (success) {
            sendMessage(player, "smp.left");
        } else {
            sendMessage(player, "smp.leave-failed");
        }
        
        return true;
    }
    
    /**
     * Random teleport in the SMP world
     * @param player The player
     * @return True if the command was executed successfully
     */
    private boolean randomTeleport(Player player) {
        // Check if SMP is enabled
        if (!plugin.getConfigManager().getConfig("config.yml").getBoolean("smp.enabled", true)) {
            sendMessage(player, "smp.disabled");
            return true;
        }
        
        SMPWorld smpWorld = plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
        
        if (smpWorld == null) {
            sendMessage(player, "smp.no-world");
            return true;
        }
        
        // Check if player is in SMP world
        if (!player.getWorld().getName().equals(smpWorld.getWorldName())) {
            sendMessage(player, "smp.not-in-world");
            return true;
        }
        
        // Get the RandomTeleport instance
        RandomTeleport randomTp = plugin.getAPI().getRandomTeleport();
        
        // Check cooldown
        if (randomTp.isInCooldown(player)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(randomTp.getCooldownTimeLeft(player)));
            
            sendMessage(player, "smp.rtp-cooldown", replacements);
            return true;
        }
        
        // Perform random teleport
        randomTp.randomTeleport(player)
            .thenAccept(success -> {
                if (!success) {
                    sendMessage(player, "smp.rtp-failed");
                }
            });
        
        return true;
    }
    
    /**
     * Set the SMP world
     * @param sender The command sender
     * @param worldName The world name
     * @return True if the command was executed successfully
     */
    private boolean setWorld(CommandSender sender, String worldName) {
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            sendMessage(sender, "smp.world-not-found", Map.of("world", worldName));
            return true;
        }
        
        boolean success = plugin.getAPI().getSMPWorldAPI().setActiveSMPWorld(worldName);
        
        if (success) {
            sendMessage(sender, "smp.world-set", Map.of("world", worldName));
        } else {
            sendMessage(sender, "smp.world-set-failed", Map.of("world", worldName));
        }
        
        return true;
    }
    
    /**
     * Set the SMP world spawn
     * @param player The player
     * @return True if the command was executed successfully
     */
    private boolean setWorldSpawn(Player player) {
        SMPWorld smpWorld = plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
        
        if (smpWorld == null) {
            sendMessage(player, "smp.no-world");
            return true;
        }
        
        // Check if player is in SMP world
        if (!player.getWorld().getName().equals(smpWorld.getWorldName())) {
            sendMessage(player, "smp.not-in-world");
            return true;
        }
        
        // Set spawn point
        boolean success = plugin.getAPI().getSMPWorldAPI().setSpawnPoint(player.getLocation());
        
        if (success) {
            sendMessage(player, "smp.spawn-set");
        } else {
            sendMessage(player, "smp.spawn-set-failed");
        }
        
        return true;
    }
    
    /**
     * Reset the SMP world
     * @param sender The command sender
     * @return True if the command was executed successfully
     */
    private boolean resetWorld(CommandSender sender) {
        SMPWorld smpWorld = plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
        
        if (smpWorld == null) {
            sendMessage(sender, "smp.no-world");
            return true;
        }
        
        sendMessage(sender, "smp.resetting");
        
        // Reset world
        boolean success = plugin.getAPI().getSMPWorldAPI().resetSMPWorld();
        
        if (success) {
            sendMessage(sender, "smp.reset-success");
        } else {
            sendMessage(sender, "smp.reset-failed");
        }
        
        return true;
    }
    
    /**
     * Show information about the SMP world
     * @param sender The command sender
     * @return True if the command was executed successfully
     */
    private boolean showInfo(CommandSender sender) {
        SMPWorld smpWorld = plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
        
        if (smpWorld == null) {
            sendMessage(sender, "smp.no-world");
            return true;
        }
        
        sender.sendMessage(ChatColor.DARK_GRAY + "• " + ChatColor.AQUA + ChatColor.BOLD + "Sumania SMP Info" + ChatColor.DARK_GRAY + " •");
        sender.sendMessage(ChatColor.AQUA + "Welt: " + ChatColor.WHITE + smpWorld.getWorldName());
        
        if (smpWorld.exists()) {
            World world = smpWorld.getWorld();
            sender.sendMessage(ChatColor.AQUA + "Status: " + ChatColor.GREEN + "Geladen");
            sender.sendMessage(ChatColor.AQUA + "Spieler: " + ChatColor.WHITE + world.getPlayers().size());
            sender.sendMessage(ChatColor.AQUA + "Umgebung: " + ChatColor.WHITE + world.getEnvironment().name());
            sender.sendMessage(ChatColor.AQUA + "Border: " + ChatColor.WHITE + smpWorld.getBorderSize() + " Blöcke");
            
            if (smpWorld.getSpawnPoint() != null) {
                sender.sendMessage(ChatColor.AQUA + "Spawn: " + ChatColor.WHITE + 
                    String.format("%.1f, %.1f, %.1f", 
                        smpWorld.getSpawnPoint().getX(),
                        smpWorld.getSpawnPoint().getY(),
                        smpWorld.getSpawnPoint().getZ())
                );
            }
        } else {
            sender.sendMessage(ChatColor.AQUA + "Status: " + ChatColor.RED + "Nicht geladen");
        }
        
        return true;
    }
    
    /**
     * Toggle the SMP system
     * @param sender The command sender
     * @param enabled Whether to enable or disable the SMP system
     * @return True if the command was executed successfully
     */
    private boolean toggleSMP(CommandSender sender, boolean enabled) {
        plugin.getConfigManager().getConfig("config.yml").set("smp.enabled", enabled);
        plugin.getConfigManager().saveConfig("config.yml");
        
        if (enabled) {
            // Reload SMP world
            plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
            sendMessage(sender, "smp.enabled");
        } else {
            sendMessage(sender, "smp.disabled");
        }
        
        return true;
    }
    
    /**
     * Send a message to a command sender
     * @param sender The command sender
     * @param key The message key
     */
    private void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }
    
    /**
     * Send a message to a command sender with replacements
     * @param sender The command sender
     * @param key The message key
     * @param replacements The replacements
     */
    private void sendMessage(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender instanceof Player) {
            plugin.getAPI().getPlayerAPI().sendMessage((Player) sender, key, replacements);
        } else {
            String message = plugin.getAPI().getPlayerAPI().formatMessage(key, replacements);
            message = ChatColor.stripColor(message); // Strip color for console
            sender.sendMessage(message);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            // Basic commands for everyone
            completions.add("help");
            
            // Player commands
            if (sender instanceof Player) {
                if (sender.hasPermission("sumania.smp.join")) completions.add("join");
                if (sender.hasPermission("sumania.smp.leave")) completions.add("leave");
                if (sender.hasPermission("sumania.smp.rtp")) {
                    completions.add("rtp");
                    completions.add("randomtp");
                }
                if (sender.hasPermission("sumania.smp.info")) completions.add("info");
            }
            
            // Admin commands
            if (sender.hasPermission("sumania.smp.admin")) {
                completions.add("setworld");
                completions.add("setworldspawn");
                completions.add("reset");
                completions.add("enable");
                completions.add("disable");
            }
            
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setworld") && sender.hasPermission("sumania.smp.admin")) {
                return plugin.getAPI().getSMPWorldAPI().getAvailableWorlds().stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("reset") && sender.hasPermission("sumania.smp.admin")) {
                return Arrays.asList("confirm").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}