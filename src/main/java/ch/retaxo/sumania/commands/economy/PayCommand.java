package ch.retaxo.sumania.commands.economy;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to pay another player
 */
public class PayCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public PayCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 2) {
            // Invalid arguments
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/pay <player> <amount>");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", targetName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.player-not-found",
                    replacements
            );
            
            return true;
        }
        
        double amount;
        
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/pay <player> <amount>");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        if (amount <= 0) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("usage", "/pay <player> <amount>");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.invalid-args",
                    replacements
            );
            
            return true;
        }
        
        // Check if player has enough money
        if (!plugin.getAPI().getEconomyAPI().has(player, amount)) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "economy.not-enough-money",
                    null
            );
            
            return true;
        }
        
        // Transfer money
        plugin.getAPI().getEconomyAPI().transfer(player, target, amount);
        
        // Send messages
        String formatted = plugin.getAPI().getEconomyAPI().format(amount);
        String currencyName = plugin.getAPI().getEconomyAPI().getCurrencyName();
        
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", target.getName());
        replacements.put("amount", formatted);
        replacements.put("currency", currencyName);
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "economy.pay-success",
                replacements
        );
        
        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            
            replacements = new HashMap<>();
            replacements.put("player", player.getName());
            replacements.put("amount", formatted);
            replacements.put("currency", currencyName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    targetPlayer,
                    "economy.pay-received",
                    replacements
            );
        }
        
        return true;
    }
}
