package ch.retaxo.sumania.commands.teleport;

import ch.retaxo.sumania.Sumania;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the warp menu
 */
public class WarpMenuCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public WarpMenuCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("sumania.warp.menu")) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.no-permission",
                    null
            );
            
            return true;
        }
        
        // Open warp menu
        plugin.getAPI().getTeleportAPI().openWarpMenu(player);
        
        return true;
    }
}