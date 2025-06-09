package ch.retaxo.sumania.commands.teleport;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.api.teleport.RandomTeleport;
import ch.retaxo.sumania.models.SMPWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Command for random teleportation
 */
public class RTPCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public RTPCommand(Sumania plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("sumania.rtp")) {
            sendMessage(player, "general.no-permission");
            return true;
        }
        
        // Check if SMP is enabled
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        if (!config.getBoolean("smp.enabled", true)) {
            sendMessage(player, "smp.disabled");
            return true;
        }
        
        // Get SMP world
        SMPWorld smpWorld = plugin.getAPI().getSMPWorldAPI().getActiveSMPWorld();
        
        if (smpWorld == null) {
            sendMessage(player, "smp.no-world");
            return true;
        }
        
        // Get SMP world name from config
        String worldName = config.getString("smp.world-name");
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            sendMessage(player, "smp.no-world");
            return true;
        }
        
        // If player is not in the SMP world, teleport them first
        if (!player.getWorld().getName().equals(worldName)) {
            // Check if spawn is configured
            double spawnX = config.getDouble("smp.spawn.x", 0.0);
            double spawnY = config.getDouble("smp.spawn.y", 64.0);
            double spawnZ = config.getDouble("smp.spawn.z", 0.0);
            float spawnYaw = (float) config.getDouble("smp.spawn.yaw", 0.0);
            float spawnPitch = (float) config.getDouble("smp.spawn.pitch", 0.0);
            
            Location spawnLoc = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
            boolean success = plugin.getAPI().getTeleportAPI().teleport(player, spawnLoc);
            
            if (!success) {
                sendMessage(player, "smp.teleport-failed");
                return true;
            }
            
            sendMessage(player, "smp.teleported-to-smp");
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
            sender.sendMessage(message);
        }
    }
}