package ch.retaxo.sumania.commands.claim;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Claim;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command for claim-related operations
 */
public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public ClaimCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Show help
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "buy":
                buyClaim(player);
                break;
                
            case "list":
                listClaims(player);
                break;
                
            case "info":
                if (args.length >= 2) {
                    showClaimInfo(player, args[1]);
                } else {
                    showClaimInfo(player, null);
                }
                break;
                
            case "trust":
                if (args.length >= 2) {
                    trustPlayer(player, args[1]);
                } else {
                    plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.trust-usage");
                }
                break;
                
            case "untrust":
                if (args.length >= 2) {
                    untrustPlayer(player, args[1]);
                } else {
                    plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.untrust-usage");
                }
                break;
                
            case "delete":
                if (args.length >= 2) {
                    deleteClaim(player, args[1]);
                } else {
                    plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.delete-usage");
                }
                break;
                
            default:
                // Unknown subcommand, show help
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    /**
     * Show help message
     * @param player The player to show help to
     */
    private void showHelp(Player player) {
        String[] helpLines = {
                "§8§l----- §7Grundstücks-Befehle §8§l-----",
                "§a/claim buy §8- §7Kaufe einen Grundstücks-Marker",
                "§a/claim list §8- §7Zeige eine Liste deiner Grundstücke",
                "§a/claim info [ID] §8- §7Zeige Informationen über ein Grundstück",
                "§a/claim trust <Spieler> §8- §7Füge einen vertrauten Spieler zu deinem Grundstück hinzu",
                "§a/claim untrust <Spieler> §8- §7Entferne einen vertrauten Spieler von deinem Grundstück",
                "§a/claim delete <ID> §8- §7Lösche ein Grundstück"
        };
        
        for (String line : helpLines) {
            player.sendMessage(line);
        }
    }
    
    /**
     * Buy a claim marker
     * @param player The player buying the marker
     */
    private void buyClaim(Player player) {
        plugin.getAPI().getClaimAPI().buyClaimMarker(player);
    }
    
    /**
     * List a player's claims
     * @param player The player
     */
    private void listClaims(Player player) {
        List<Claim> claims = plugin.getAPI().getClaimAPI().getClaimsByPlayer(player);
        
        if (claims.isEmpty()) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.no-claims");
            return;
        }
        
        player.sendMessage("§8§l----- §7Deine Grundstücke §8§l-----");
        
        for (Claim claim : claims) {
            int sizeX = claim.getMaxX() - claim.getMinX();
            int sizeZ = claim.getMaxZ() - claim.getMinZ();
            
            player.sendMessage(String.format(
                    "§a%s §8- §7%s, %s (%dx%d)",
                    claim.getId().substring(0, 8),
                    claim.getWorldName(),
                    claim.getMarkerX() + "," + claim.getMarkerY() + "," + claim.getMarkerZ(),
                    sizeX,
                    sizeZ
            ));
        }
    }
    
    /**
     * Show information about a claim
     * @param player The player
     * @param claimId The ID of the claim, or null to use the claim at the player's location
     */
    private void showClaimInfo(Player player, String claimId) {
        Claim claim;
        
        if (claimId != null) {
            // Find by ID
            claim = findClaimById(claimId);
            
            if (claim == null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("id", claimId);
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "claim.claim-not-found",
                        replacements
                );
                return;
            }
        } else {
            // Find by location
            claim = plugin.getAPI().getClaimAPI().getClaimAt(player.getLocation());
            
            if (claim == null) {
                plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.no-claim-here");
                return;
            }
        }
        
        // Get owner name
        OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.getOwnerUUID());
        String ownerName = owner.getName() != null ? owner.getName() : "Unbekannt";
        
        // Display claim info
        player.sendMessage("§8§l----- §7Grundstück: §a" + claim.getId().substring(0, 8) + " §8§l-----");
        player.sendMessage("§7Besitzer: §f" + ownerName);
        player.sendMessage("§7Welt: §f" + claim.getWorldName());
        player.sendMessage("§7Position: §f" + claim.getMinX() + "," + claim.getMinZ() + " bis " + claim.getMaxX() + "," + claim.getMaxZ());
        player.sendMessage("§7Größe: §f" + (claim.getMaxX() - claim.getMinX()) + "x" + (claim.getMaxZ() - claim.getMinZ()) + " Blöcke");
        player.sendMessage("§7Marker-Block: §f" + claim.getMarkerX() + "," + claim.getMarkerY() + "," + claim.getMarkerZ());
        
        // Display trusted players
        List<String> trustedPlayers = plugin.getAPI().getClaimAPI().getTrustedPlayerNames(claim);
        
        if (trustedPlayers.isEmpty()) {
            player.sendMessage("§7Vertraute Spieler: §fKeine");
        } else {
            player.sendMessage("§7Vertraute Spieler: §f" + String.join(", ", trustedPlayers));
        }
    }
    
    /**
     * Trust a player on a claim
     * @param player The player trusting another player
     * @param targetName The name of the player to trust
     */
    private void trustPlayer(Player player, String targetName) {
        // Get claim at player's location
        Claim claim = plugin.getAPI().getClaimAPI().getClaimAt(player.getLocation());
        
        if (claim == null) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.no-claim-here");
            return;
        }
        
        // Check if player is the owner
        if (!claim.getOwnerUUID().equals(player.getUniqueId())) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.not-owner");
            return;
        }
        
        // Find target player
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.cannot-trust-self");
            return;
        }
        
        // Add trusted player
        if (plugin.getAPI().getClaimAPI().addTrustedPlayer(claim, targetPlayer)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", targetName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.trusted-added",
                    replacements
            );
        } else {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", targetName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.already-trusted",
                    replacements
            );
        }
    }
    
    /**
     * Untrust a player on a claim
     * @param player The player untrusting another player
     * @param targetName The name of the player to untrust
     */
    private void untrustPlayer(Player player, String targetName) {
        // Get claim at player's location
        Claim claim = plugin.getAPI().getClaimAPI().getClaimAt(player.getLocation());
        
        if (claim == null) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.no-claim-here");
            return;
        }
        
        // Check if player is the owner
        if (!claim.getOwnerUUID().equals(player.getUniqueId())) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.not-owner");
            return;
        }
        
        // Find target player
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        // Remove trusted player
        if (plugin.getAPI().getClaimAPI().removeTrustedPlayer(claim, targetPlayer)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", targetName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.trusted-removed",
                    replacements
            );
        } else {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", targetName);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.not-trusted",
                    replacements
            );
        }
    }
    
    /**
     * Delete a claim
     * @param player The player deleting the claim
     * @param claimId The ID of the claim to delete
     */
    private void deleteClaim(Player player, String claimId) {
        // Find claim by ID
        Claim claim = findClaimById(claimId);
        
        if (claim == null) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("id", claimId);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.claim-not-found",
                    replacements
            );
            return;
        }
        
        // Check if player is the owner
        if (!claim.getOwnerUUID().equals(player.getUniqueId())) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.not-owner");
            return;
        }
        
        // Delete the claim
        if (plugin.getAPI().getClaimAPI().deleteClaim(claim)) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.deleted");
        } else {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.delete-failed");
        }
    }
    
    /**
     * Find a claim by partial ID
     * @param partialId The partial ID of the claim
     * @return The claim, or null if not found
     */
    private Claim findClaimById(String partialId) {
        for (Claim claim : plugin.getAPI().getClaimAPI().getClaimsByPlayer(Bukkit.getOfflinePlayer(UUID.randomUUID()))) {
            if (claim.getId().startsWith(partialId)) {
                return claim;
            }
        }
        
        return null;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            return Arrays.asList("buy", "list", "info", "trust", "untrust", "delete");
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("info") || subCommand.equals("delete")) {
                // Return list of claim IDs
                return plugin.getAPI().getClaimAPI().getClaimsByPlayer(player).stream()
                        .map(claim -> claim.getId().substring(0, 8))
                        .collect(Collectors.toList());
            } else if (subCommand.equals("trust") || subCommand.equals("untrust")) {
                // Return list of online players
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}