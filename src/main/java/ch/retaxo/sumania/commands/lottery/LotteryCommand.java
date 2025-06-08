package ch.retaxo.sumania.commands.lottery;

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

/**
 * Command to manage the lottery system
 */
public class LotteryCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public LotteryCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if lottery is enabled
        if (!config.getBoolean("lottery.enabled", true)) {
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.feature-disabled",
                        null
                );
            } else {
                plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cDie Verlosung ist deaktiviert.");
            }
            
            return true;
        }
        
        if (args.length == 0) {
            // Show lottery info
            return showLotteryInfo(sender);
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("kaufen")) {
                // Buy a lottery ticket
                if (!(sender instanceof Player)) {
                    plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cDieser Befehl kann nur von Spielern verwendet werden.");
                    return true;
                }
                
                return buyLotteryTicket((Player) sender);
            } else if (args[0].equalsIgnoreCase("info")) {
                // Show lottery info
                return showLotteryInfo(sender);
            } else if (args[0].equalsIgnoreCase("draw") || args[0].equalsIgnoreCase("ziehen")) {
                // Draw the lottery
                if (!sender.hasPermission("sumania.lottery.admin")) {
                    if (sender instanceof Player) {
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                (Player) sender,
                                "general.no-permission",
                                null
                        );
                    } else {
                        plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cDu hast keine Berechtigung dafür.");
                    }
                    
                    return true;
                }
                
                return drawLottery(sender);
            }
        }
        
        // Show usage
        if (sender instanceof Player) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/verlosung [buy|info|draw]");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    (Player) sender,
                    "general.invalid-args",
                    replacements
            );
        } else {
            plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cVerwendung: /verlosung [buy|info|draw]");
        }
        
        return true;
    }
    
    /**
     * Show lottery info
     * @param sender The command sender
     * @return True if the command was handled successfully
     */
    private boolean showLotteryInfo(CommandSender sender) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        double ticketPrice = config.getDouble("lottery.ticket-price", 100.0);
        String currencySymbol = config.getString("economy.currency-symbol", "$");
        
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Get total tickets
            String countQuery = "SELECT COUNT(*) AS total FROM " + tablePrefix + "lottery_tickets WHERE draw_id = " +
                    "(SELECT MAX(draw_id) FROM " + tablePrefix + "lottery_tickets)";
            
            int totalTickets = 0;
            int playerTickets = 0;
            
            try (PreparedStatement stmt = conn.prepareStatement(countQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalTickets = rs.getInt("total");
                }
            }
            
            // Get player tickets if sender is a player
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerQuery = "SELECT COUNT(*) AS player_tickets FROM " + tablePrefix + "lottery_tickets " +
                        "WHERE uuid = ? AND draw_id = (SELECT MAX(draw_id) FROM " + tablePrefix + "lottery_tickets)";
                
                try (PreparedStatement stmt = conn.prepareStatement(playerQuery)) {
                    stmt.setString(1, player.getUniqueId().toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            playerTickets = rs.getInt("player_tickets");
                        }
                    }
                }
            }
            
            // Calculate pot size
            double basePrize = config.getDouble("lottery.base-prize", 500.0);
            int poolPercentage = config.getInt("lottery.pool-percentage", 80);
            double potSize = basePrize + (totalTickets * ticketPrice * poolPercentage / 100.0);
            
            // Send lottery info
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Map<String, String> replacements = new HashMap<>();
                replacements.put("price", String.format("%.0f", ticketPrice));
                replacements.put("currency", currencySymbol);
                replacements.put("time", "60"); // TODO: Calculate actual time left
                replacements.put("amount", String.format("%.0f", potSize));
                replacements.put("tickets", String.valueOf(playerTickets));
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "lottery.lottery-info",
                        replacements
                );
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "lottery.tickets-owned",
                        replacements
                );
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "lottery.current-pot",
                        replacements
                );
            } else {
                plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§aVerlosung endet in §660 §aMinuten. Lose kosten §6" + 
                        String.format("%.0f", ticketPrice) + " " + currencySymbol + "§a.");
                plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§aDer aktuelle Gewinnpool beträgt §6" + 
                        String.format("%.0f", potSize) + " " + currencySymbol + "§a.");
                plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§aAktuell gibt es §6" + totalTickets + " §averkaufte Lose.");
            }
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Anzeigen der Verlosungsinformationen: " + e.getMessage());
            e.printStackTrace();
            
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.database-error",
                        null
                );
            } else {
                plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cDatenbankfehler beim Anzeigen der Verlosungsinformationen.");
            }
            
            return false;
        }
    }
    
    /**
     * Buy a lottery ticket
     * @param player The player buying the ticket
     * @return True if the ticket was bought successfully
     */
    private boolean buyLotteryTicket(Player player) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        double ticketPrice = config.getDouble("lottery.ticket-price", 100.0);
        
        // Check if player has enough money
        if (plugin.getAPI().getEconomyAPI().getBalance(player) < ticketPrice) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "lottery.not-enough-money",
                    null
            );
            
            return false;
        }
        
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Get current draw ID
            int drawId = 1; // Default to 1 if no tickets exist
            String drawIdQuery = "SELECT MAX(draw_id) AS max_draw_id FROM " + tablePrefix + "lottery_tickets";
            
            try (PreparedStatement stmt = conn.prepareStatement(drawIdQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getObject("max_draw_id") != null) {
                    drawId = rs.getInt("max_draw_id");
                }
            }
            
            // Check if player already has a ticket for this draw
            String checkTicketQuery = "SELECT COUNT(*) AS ticket_count FROM " + tablePrefix + "lottery_tickets " +
                    "WHERE uuid = ? AND draw_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(checkTicketQuery)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setInt(2, drawId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("ticket_count") > 0) {
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "lottery.already-bought",
                                null
                        );
                        
                        return false;
                    }
                }
            }
            
            // Insert ticket
            String insertQuery = "INSERT INTO " + tablePrefix + "lottery_tickets (uuid, draw_id) VALUES (?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setInt(2, drawId);
                stmt.executeUpdate();
            }
            
            // Withdraw money
            plugin.getAPI().getEconomyAPI().withdrawMoney(player, ticketPrice);
            
            // Send confirmation
            Map<String, String> replacements = new HashMap<>();
            replacements.put("price", String.format("%.0f", ticketPrice));
            replacements.put("currency", config.getString("economy.currency-symbol", "$"));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "lottery.ticket-bought",
                    replacements
            );
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Kaufen eines Lotterielos: " + e.getMessage());
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
     * Draw the lottery
     * @param sender The command sender
     * @return True if the lottery was drawn successfully
     */
    private boolean drawLottery(CommandSender sender) {
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Get current draw ID
            int drawId = 1; // Default to 1 if no tickets exist
            String drawIdQuery = "SELECT MAX(draw_id) AS max_draw_id FROM " + tablePrefix + "lottery_tickets";
            
            try (PreparedStatement stmt = conn.prepareStatement(drawIdQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getObject("max_draw_id") != null) {
                    drawId = rs.getInt("max_draw_id");
                }
            }
            
            // Count tickets for this draw
            String countQuery = "SELECT COUNT(*) AS ticket_count FROM " + tablePrefix + "lottery_tickets " +
                    "WHERE draw_id = ?";
            
            int ticketCount = 0;
            try (PreparedStatement stmt = conn.prepareStatement(countQuery)) {
                stmt.setInt(1, drawId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ticketCount = rs.getInt("ticket_count");
                    }
                }
            }
            
            if (ticketCount == 0) {
                if (sender instanceof Player) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "lottery.no-participants",
                            null
                    );
                } else {
                    plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cDie Verlosung findet nicht statt, da es keine Teilnehmer gibt.");
                }
                
                return false;
            }
            
            // Select random winner
            String winnerQuery = "SELECT uuid FROM " + tablePrefix + "lottery_tickets " +
                    "WHERE draw_id = ? ORDER BY RANDOM() LIMIT 1";
            
            String winnerUuid = null;
            try (PreparedStatement stmt = conn.prepareStatement(winnerQuery)) {
                stmt.setInt(1, drawId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        winnerUuid = rs.getString("uuid");
                    }
                }
            }
            
            if (winnerUuid != null) {
                // Calculate prize
                FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
                double ticketPrice = config.getDouble("lottery.ticket-price", 100.0);
                double basePrize = config.getDouble("lottery.base-prize", 500.0);
                int poolPercentage = config.getInt("lottery.pool-percentage", 80);
                double prize = basePrize + (ticketCount * ticketPrice * poolPercentage / 100.0);
                
                // Award prize to winner
                plugin.getAPI().getEconomyAPI().depositMoney(winnerUuid, prize);
                
                // Announce winner
                String winnerName = plugin.getAPI().getPlayerAPI().getPlayerName(winnerUuid);
                
                Map<String, String> replacements = new HashMap<>();
                replacements.put("player", winnerName);
                replacements.put("prize", String.format("%.0f", prize));
                replacements.put("currency", config.getString("economy.currency-symbol", "$"));
                
                plugin.getServer().broadcastMessage(
                        plugin.getConfigManager().getPrefix() + 
                        plugin.getAPI().getPlayerAPI().formatMessage("lottery.winner-announcement", replacements)
                );
                
                // Create new draw
                String updateQuery = "UPDATE " + tablePrefix + "lottery_tickets SET draw_id = ? WHERE draw_id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                    stmt.setInt(1, drawId + 1);
                    stmt.setInt(2, drawId);
                    stmt.executeUpdate();
                }
                
                return true;
            } else {
                plugin.getLogger().severe("Fehler bei der Verlosungsziehung: Kein Gewinner gefunden");
                
                if (sender instanceof Player) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            (Player) sender,
                            "general.error",
                            null
                    );
                } else {
                    plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cFehler bei der Verlosungsziehung: Kein Gewinner gefunden");
                }
                
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler bei der Verlosungsziehung: " + e.getMessage());
            e.printStackTrace();
            
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.database-error",
                        null
                );
            } else {
                plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, "§cDatenbankfehler bei der Verlosungsziehung.");
            }
            
            return false;
        }
    }
}