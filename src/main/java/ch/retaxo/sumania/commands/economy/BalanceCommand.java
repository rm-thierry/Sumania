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
 * Command to check balance and manage player economy
 */
public class BalanceCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public BalanceCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Console can use admin commands
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;
        
        // Handle different commands based on label
        if (label.equalsIgnoreCase("balance") || label.equalsIgnoreCase("bal")) {
            return handleBalanceCommand(sender, args);
        } else if (label.equalsIgnoreCase("eco")) {
            // Admin economy commands require permission
            if (isPlayer && !player.hasPermission("sumania.economy.admin")) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.no-permission",
                        null
                );
                return true;
            }
            return handleEcoCommand(sender, args);
        }
        
        return false;
    }
    
    /**
     * Handle /balance command
     */
    private boolean handleBalanceCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cConsole must use /eco get <player> to check balances.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Check own balance
            double balance = plugin.getAPI().getEconomyAPI().getBalance(player);
            String formatted = plugin.getAPI().getEconomyAPI().format(balance);
            String currencyName = plugin.getAPI().getEconomyAPI().getCurrencyName();
            
            Map<String, String> replacements = new HashMap<>();
            replacements.put("balance", formatted);
            replacements.put("currency", currencyName);
            replacements.put("player_obj", "true");
            replacements.put("player_name", player.getName());
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "economy.balance",
                    replacements
            );
            
            return true;
        } else if (args.length == 1) {
            // Check other player's balance
            if (!player.hasPermission("sumania.economy.balance.others")) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "general.no-permission",
                        null
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
            
            double balance = plugin.getAPI().getEconomyAPI().getBalance(target);
            String formatted = plugin.getAPI().getEconomyAPI().format(balance);
            String currencyName = plugin.getAPI().getEconomyAPI().getCurrencyName();
            
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", target.getName());
            replacements.put("balance", formatted);
            replacements.put("currency", currencyName);
            
            if (target.isOnline()) {
                replacements.put("player_obj", "true");
                replacements.put("player_name", target.getName());
            }
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "economy.balance-other",
                    replacements
            );
            
            return true;
        }
        
        // Invalid arguments
        Map<String, String> replacements = new HashMap<>();
        replacements.put("usage", "/balance [player]");
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "general.invalid-args",
                replacements
        );
        
        return true;
    }
    
    /**
     * Handle /eco admin commands
     */
    private boolean handleEcoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showEcoUsage(sender);
            return true;
        }
        
        String action = args[0].toLowerCase();
        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendPrefixedMessage(sender, "§cPlayer not found: " + targetName);
            return true;
        }
        
        if (action.equals("get")) {
            // Get player's balance
            double balance = plugin.getAPI().getEconomyAPI().getBalance(target);
            String formatted = plugin.getAPI().getEconomyAPI().format(balance);
            sendPrefixedMessage(sender, "§aBalance of §e" + target.getName() + "§a: " + formatted);
            return true;
        }
        
        // Actions requiring amount
        if (args.length < 3) {
            showEcoUsage(sender);
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount < 0) {
                sendPrefixedMessage(sender, "§cAmount must be positive.");
                return true;
            }
        } catch (NumberFormatException e) {
            sendPrefixedMessage(sender, "§cInvalid amount: " + args[2]);
            return true;
        }
        
        String formatted = plugin.getAPI().getEconomyAPI().format(amount);
        double newBalance;
        
        switch (action) {
            case "set":
                plugin.getAPI().getEconomyAPI().setBalance(target, amount);
                sendPrefixedMessage(sender, "§aSet balance of §e" + target.getName() + "§a to " + formatted);
                break;
                
            case "add":
            case "give":
                newBalance = plugin.getAPI().getEconomyAPI().deposit(target, amount);
                sendPrefixedMessage(sender, "§aAdded " + formatted + " to §e" + target.getName() + 
                        "§a's balance. New balance: §e" + plugin.getAPI().getEconomyAPI().format(newBalance));
                break;
                
            case "remove":
            case "take":
                newBalance = plugin.getAPI().getEconomyAPI().withdraw(target, amount);
                sendPrefixedMessage(sender, "§aRemoved " + formatted + " from §e" + target.getName() + 
                        "§a's balance. New balance: §e" + plugin.getAPI().getEconomyAPI().format(newBalance));
                break;
                
            default:
                showEcoUsage(sender);
        }
        
        return true;
    }
    
    /**
     * Show usage for eco command
     */
    private void showEcoUsage(CommandSender sender) {
        sendPrefixedMessage(sender, "§eEconomy Admin Commands:");
        sendPrefixedMessage(sender, "§f/eco get <player> §7- Check a player's balance");
        sendPrefixedMessage(sender, "§f/eco set <player> <amount> §7- Set a player's balance");
        sendPrefixedMessage(sender, "§f/eco add <player> <amount> §7- Add to a player's balance");
        sendPrefixedMessage(sender, "§f/eco remove <player> <amount> §7- Remove from a player's balance");
    }
    
    /**
     * Send a prefixed message to a command sender
     */
    private void sendPrefixedMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            plugin.getAPI().getPlayerAPI().sendPrefixedMessage(sender, message);
        } else {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + message);
        }
    }
}
