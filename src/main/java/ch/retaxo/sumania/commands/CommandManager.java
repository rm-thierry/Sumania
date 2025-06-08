package ch.retaxo.sumania.commands;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.commands.admin.ReloadCommand;
import ch.retaxo.sumania.commands.chat.ChatCommand;
import ch.retaxo.sumania.commands.economy.BalanceCommand;
import ch.retaxo.sumania.commands.economy.PayCommand;
import ch.retaxo.sumania.commands.teleport.HomeCommand;
import ch.retaxo.sumania.commands.teleport.TeleportCommand;
import ch.retaxo.sumania.commands.teleport.WarpCommand;
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
            registerCommand("balance", new BalanceCommand(plugin));
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
            registerCommand("warp", new WarpCommand(plugin));
            registerCommand("setwarp", new WarpCommand(plugin));
            registerCommand("delwarp", new WarpCommand(plugin));
            registerCommand("warps", new WarpCommand(plugin));
        }
        
        // Register chat commands
        registerCommand("chat", new ChatCommand(plugin));
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
}
