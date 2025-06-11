package ch.retaxo.sumania.commands;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.commands.admin.BanCommand;
import ch.retaxo.sumania.commands.admin.ReloadCommand;
import ch.retaxo.sumania.commands.auction.AuctionCommand;
import ch.retaxo.sumania.commands.chat.ChatCommand;
import ch.retaxo.sumania.commands.claim.ClaimCommand;
import ch.retaxo.sumania.commands.discord.DiscordCommand;
import ch.retaxo.sumania.commands.economy.BalanceCommand;
import ch.retaxo.sumania.commands.economy.PayCommand;
import ch.retaxo.sumania.commands.rewards.RewardsCommand;
import ch.retaxo.sumania.commands.shop.ShopCommand;
import ch.retaxo.sumania.commands.smp.SMPCommand;
import ch.retaxo.sumania.commands.teleport.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages all commands for the plugin
 */
public class CommandManager {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public CommandManager(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Register all commands
     */
    public void registerCommands() {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Register admin commands
        registerCommand("sumania", new ReloadCommand(plugin));
        
        // Register economy commands if enabled
        if (config.getBoolean("commands.economy", true)) {
            BalanceCommand balanceCommand = new BalanceCommand(plugin);
            registerCommand("balance", balanceCommand);
            registerCommand("bal", balanceCommand);
            registerCommand("eco", balanceCommand);
            registerCommand("pay", new PayCommand(plugin));
        }
        
        // Register teleport commands if enabled
        if (config.getBoolean("commands.teleport", true)) {
            registerCommand("tp", new TeleportCommand(plugin));
            registerCommand("tpa", new TeleportCommand(plugin));
            registerCommand("tpaccept", new TeleportCommand(plugin));
            registerCommand("tpdeny", new TeleportCommand(plugin));
        }
        
        // Register home commands if enabled
        if (config.getBoolean("commands.home", true)) {
            registerCommand("home", new HomeCommand(plugin));
            registerCommand("sethome", new HomeCommand(plugin));
            registerCommand("delhome", new HomeCommand(plugin));
            registerCommand("homes", new HomeCommand(plugin));
        }
        
        // Register warp commands if enabled
        if (config.getBoolean("commands.warp", true)) {
            registerCommand("warp", new WarpMenuCommand(plugin));
        }
        
        // Register chat commands
        registerCommand("chat", new ChatCommand(plugin));
        
        // Register discord commands if enabled
        if (config.getBoolean("commands.discord", true)) {
            registerCommand("discord", new DiscordCommand(plugin));
        }
        
        // Register shop commands if enabled
        if (config.getBoolean("commands.shop", true)) {
            registerCommand("shop", new ShopCommand(plugin));
        }
        
        // Register auction commands if enabled
        if (config.getBoolean("commands.auction", true)) {
            AuctionCommand auctionCommand = new AuctionCommand(plugin);
            registerCommand("auction", auctionCommand);
            registerCommand("ah", auctionCommand);
            
            // Set tab completer for auction commands
            if (plugin.getCommand("auction") != null) {
                plugin.getCommand("auction").setTabCompleter(auctionCommand);
            }
            if (plugin.getCommand("ah") != null) {
                plugin.getCommand("ah").setTabCompleter(auctionCommand);
            }
        }
        
        // Register rewards commands if enabled
        if (config.getBoolean("commands.rewards", true)) {
            registerCommand("rewards", new RewardsCommand(plugin));
        }
        
        // Register SMP commands if SMP is enabled
        if (config.getBoolean("smp.enabled", true)) {
            // Register SMP command
            SMPCommand smpCommand = new SMPCommand(plugin);
            registerCommand("smp", smpCommand);
            if (plugin.getCommand("smp") != null) {
                plugin.getCommand("smp").setTabCompleter(smpCommand);
            }
        }
        
        // Register claim commands if claims are enabled
        if (config.getBoolean("protection.claims-enabled", true)) {
            ClaimCommand claimCommand = new ClaimCommand(plugin);
            registerCommand("claim", claimCommand);
            if (plugin.getCommand("claim") != null) {
                plugin.getCommand("claim").setTabCompleter(claimCommand);
            }
        }
        
        // Register ban commands if enabled
        if (config.getBoolean("commands.ban", true)) {
            BanCommand banCommand = new BanCommand(plugin);
            registerCommand("ban", banCommand);
            registerCommand("tempban", banCommand);
            registerCommand("unban", banCommand);
            registerCommand("lookup", banCommand);
            registerCommand("banhistory", banCommand);
            registerCommand("mutehistory", banCommand);
            
            // Set tab completers for commands
            if (plugin.getCommand("ban") != null) plugin.getCommand("ban").setTabCompleter(banCommand);
            if (plugin.getCommand("tempban") != null) plugin.getCommand("tempban").setTabCompleter(banCommand);
            if (plugin.getCommand("unban") != null) plugin.getCommand("unban").setTabCompleter(banCommand);
            if (plugin.getCommand("lookup") != null) plugin.getCommand("lookup").setTabCompleter(banCommand);
            if (plugin.getCommand("banhistory") != null) plugin.getCommand("banhistory").setTabCompleter(banCommand);
            if (plugin.getCommand("mutehistory") != null) plugin.getCommand("mutehistory").setTabCompleter(banCommand);
        }
    }
    
    /**
     * Register a command with the plugin
     * @param name The name of the command
     * @param executor The command executor
     */
    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        
        if (command != null) {
            command.setExecutor(executor);
        } else {
            plugin.getLogger().warning("Failed to register command: " + name);
        }
    }
    
    /**
     * Get a command executor by name
     * @param name The name of the command
     * @return The command executor, or null if not found
     */
    public CommandExecutor getCommand(String name) {
        PluginCommand command = plugin.getCommand(name);
        
        if (command != null) {
            return command.getExecutor();
        }
        
        return null;
    }
}
