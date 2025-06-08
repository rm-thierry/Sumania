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
 * Command to check balance
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
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
}
