package ch.retaxo.sumania.commands.discord;

import ch.retaxo.sumania.Sumania;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Command to manage Discord integration
 */
public class DiscordCommand implements CommandExecutor {

    private final Sumania plugin;
    private final Random random = new Random();
    private final Map<String, String> linkCodes = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public DiscordCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if Discord integration is enabled
        if (!config.getBoolean("discord.enabled", true)) {
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.feature-disabled",
                        null
                );
            } else {
                sender.sendMessage("§cDiscord-Integration ist deaktiviert.");
            }
            
            return true;
        }
        
        // Check if command is run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Show Discord info
            return showDiscordInfo(player);
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("link") || args[0].equalsIgnoreCase("verknüpfen")) {
                // Generate link code
                return generateLinkCode(player);
            } else if (args[0].equalsIgnoreCase("unlink") || args[0].equalsIgnoreCase("trennen")) {
                // Unlink Discord account
                return unlinkDiscord(player);
            } else if (args[0].equalsIgnoreCase("info")) {
                // Show Discord info
                return showDiscordInfo(player);
            }
        }
        
        // Show usage
        Map<String, String> replacements = new HashMap<>();
        replacements.put("usage", "/discord [link|unlink|info]");
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "general.invalid-args",
                replacements
        );
        
        return true;
    }
    
    /**
     * Show Discord info
     * @param player The player to show info to
     * @return True if the info was shown successfully
     */
    private boolean showDiscordInfo(Player player) {
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Check if player is linked
            String query = "SELECT discord_id FROM " + tablePrefix + "discord_links WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Player is linked
                        String discordId = rs.getString("discord_id");
                        
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "discord.linked",
                                null
                        );
                        
                        return true;
                    } else {
                        // Player is not linked
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "discord.not-linked",
                                null
                        );
                        
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Abrufen von Discord-Informationen: " + e.getMessage());
            e.printStackTrace();
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.database-error",
                    null
            );
            
            return false;
        }
    }
    
    /**
     * Generate a link code for Discord integration
     * @param player The player to generate a code for
     * @return True if the code was generated successfully
     */
    private boolean generateLinkCode(Player player) {
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Check if player is already linked
            String query = "SELECT discord_id FROM " + tablePrefix + "discord_links WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Player is already linked
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "discord.already-linked",
                                null
                        );
                        
                        return false;
                    }
                }
            }
            
            // Generate a new link code
            String code = generateCode();
            linkCodes.put(player.getUniqueId().toString(), code);
            
            // Send link code to player
            Map<String, String> replacements = new HashMap<>();
            replacements.put("code", code);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "discord.link-code",
                    replacements
            );
            
            // Expire code after 5 minutes
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                linkCodes.remove(player.getUniqueId().toString());
            }, 6000L); // 5 minutes * 60 seconds * 20 ticks
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Generieren eines Link-Codes: " + e.getMessage());
            e.printStackTrace();
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.database-error",
                    null
            );
            
            return false;
        }
    }
    
    /**
     * Unlink a player's Discord account
     * @param player The player to unlink
     * @return True if the account was unlinked successfully
     */
    private boolean unlinkDiscord(Player player) {
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Check if player is linked
            String query = "SELECT discord_id FROM " + tablePrefix + "discord_links WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        // Player is not linked
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "discord.not-linked",
                                null
                        );
                        
                        return false;
                    }
                }
            }
            
            // Delete link
            String deleteQuery = "DELETE FROM " + tablePrefix + "discord_links WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.executeUpdate();
            }
            
            // Send unlink message
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "discord.unlinked",
                    null
            );
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Trennen eines Discord-Accounts: " + e.getMessage());
            e.printStackTrace();
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.database-error",
                    null
            );
            
            return false;
        }
    }
    
    /**
     * Generate a random 6-digit code
     * @return A random 6-digit code
     */
    private String generateCode() {
        int code = 100000 + random.nextInt(900000); // Generate a random 6-digit number
        return String.valueOf(code);
    }
    
    /**
     * Link a Discord account using a code
     * @param code The link code
     * @param discordId The Discord ID
     * @return True if the account was linked successfully
     */
    public boolean linkDiscordAccount(String code, String discordId) {
        for (Map.Entry<String, String> entry : linkCodes.entrySet()) {
            if (entry.getValue().equals(code)) {
                String uuid = entry.getKey();
                
                try {
                    Connection conn = plugin.getConfigManager().getDbConnection();
                    String tablePrefix = plugin.getConfigManager().getTablePrefix();
                    
                    // Insert or update link
                    String upsertQuery = "INSERT OR REPLACE INTO " + tablePrefix + "discord_links (uuid, discord_id) VALUES (?, ?)";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(upsertQuery)) {
                        stmt.setString(1, uuid);
                        stmt.setString(2, discordId);
                        stmt.executeUpdate();
                    }
                    
                    // Remove code
                    linkCodes.remove(uuid);
                    
                    return true;
                } catch (SQLException e) {
                    plugin.getLogger().severe("Fehler beim Verknüpfen eines Discord-Accounts: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        
        return false;
    }
}