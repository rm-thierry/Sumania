package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menu for managing claims
 */
public class ClaimManagementMenu implements Listener {

    private final Sumania plugin;
    private final Claim claim;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    
    // Menu items
    private static final int INFO_SLOT = 4;
    private static final int ADD_TRUSTED_SLOT = 11;
    private static final int REMOVE_TRUSTED_SLOT = 15;
    private static final int EXPAND_SLOT = 20;
    private static final int TELEPORT_SLOT = 22;
    private static final int DELETE_SLOT = 24;
    
    // Add/Remove trusted player menus
    private static final int TRUSTED_PLAYERS_PER_PAGE = 36;
    private final Map<UUID, Integer> playerMenuPages = new HashMap<>();
    private final Map<UUID, List<OfflinePlayer>> playerMenuPlayers = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     * @param claim The claim to manage
     */
    public ClaimManagementMenu(Sumania plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the menu for a player
     * @param player The player to open the menu for
     */
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 27, "§8» §7Grundstücksverwaltung");
        
        // Create menu items
        createMainMenuItems(inventory);
        
        // Open inventory
        player.openInventory(inventory);
        
        // Store open inventory
        openInventories.put(player.getUniqueId(), inventory);
    }
    
    /**
     * Create menu items for the main menu
     * @param inventory The inventory to add items to
     */
    private void createMainMenuItems(Inventory inventory) {
        // Fill with glass panes
        ItemStack glassFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassFiller.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glassFiller.setItemMeta(glassMeta);
        }
        
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glassFiller);
        }
        
        // Claim info item
        ItemStack infoItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.getOwnerUUID());
            String ownerName = owner.getName() != null ? owner.getName() : "Unbekannt";
            
            infoMeta.setDisplayName("§a§lGrundstück: §r§7" + claim.getId().substring(0, 8));
            
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Besitzer: §f" + ownerName);
            infoLore.add("§7Größe: §f" + (claim.getMaxX() - claim.getMinX()) + "x" + (claim.getMaxZ() - claim.getMinZ()) + " Blöcke");
            infoLore.add("§7Position: §f" + claim.getMinX() + "," + claim.getMinZ() + " bis " + claim.getMaxX() + "," + claim.getMaxZ());
            
            // Add trusted players
            List<String> trustedPlayers = plugin.getAPI().getClaimAPI().getTrustedPlayerNames(claim);
            if (!trustedPlayers.isEmpty()) {
                infoLore.add("");
                infoLore.add("§7Vertraute Spieler:");
                for (String trustedPlayer : trustedPlayers) {
                    infoLore.add("§7- §f" + trustedPlayer);
                }
            }
            
            infoMeta.setLore(infoLore);
            infoItem.setItemMeta(infoMeta);
        }
        inventory.setItem(INFO_SLOT, infoItem);
        
        // Add trusted player item
        ItemStack addTrustedItem = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta addTrustedMeta = addTrustedItem.getItemMeta();
        if (addTrustedMeta != null) {
            addTrustedMeta.setDisplayName("§a§lVertrauten Spieler hinzufügen");
            addTrustedMeta.setLore(Arrays.asList(
                    "§7Füge einen Spieler hinzu, der",
                    "§7auf deinem Grundstück bauen darf."
            ));
            addTrustedItem.setItemMeta(addTrustedMeta);
        }
        inventory.setItem(ADD_TRUSTED_SLOT, addTrustedItem);
        
        // Remove trusted player item
        ItemStack removeTrustedItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta removeTrustedMeta = removeTrustedItem.getItemMeta();
        if (removeTrustedMeta != null) {
            removeTrustedMeta.setDisplayName("§c§lVertrauten Spieler entfernen");
            removeTrustedMeta.setLore(Arrays.asList(
                    "§7Entferne einen Spieler von",
                    "§7deinem Grundstück."
            ));
            removeTrustedItem.setItemMeta(removeTrustedMeta);
        }
        inventory.setItem(REMOVE_TRUSTED_SLOT, removeTrustedItem);
        
        // Expand claim item (placeholder for future implementation)
        ItemStack expandItem = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta expandMeta = expandItem.getItemMeta();
        if (expandMeta != null) {
            expandMeta.setDisplayName("§6§lGrundstück erweitern");
            expandMeta.setLore(Arrays.asList(
                    "§7Erweitere dein Grundstück.",
                    "§7Kosten: §f5000 Coins pro Block"
            ));
            expandItem.setItemMeta(expandMeta);
        }
        inventory.setItem(EXPAND_SLOT, expandItem);
        
        // Teleport to claim item
        ItemStack teleportItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta teleportMeta = teleportItem.getItemMeta();
        if (teleportMeta != null) {
            teleportMeta.setDisplayName("§5§lZum Grundstück teleportieren");
            teleportMeta.setLore(Arrays.asList(
                    "§7Teleportiere dich zum",
                    "§7Marker-Block deines Grundstücks."
            ));
            teleportItem.setItemMeta(teleportMeta);
        }
        inventory.setItem(TELEPORT_SLOT, teleportItem);
        
        // Delete claim item
        ItemStack deleteItem = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§c§lGrundstück löschen");
            deleteMeta.setLore(Arrays.asList(
                    "§7Löscht dein Grundstück.",
                    "§c§lACHTUNG: §r§7Diese Aktion kann nicht",
                    "§7rückgängig gemacht werden!"
            ));
            deleteItem.setItemMeta(deleteMeta);
        }
        inventory.setItem(DELETE_SLOT, deleteItem);
    }
    
    /**
     * Open the add trusted player menu
     * @param player The player to open the menu for
     */
    private void openAddTrustedMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, "§8» §7Vertrauten Spieler hinzufügen");
        
        // Get all online players
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        // Remove players that are already trusted
        onlinePlayers.removeIf(p -> p.getUniqueId().equals(claim.getOwnerUUID()) || claim.isTrusted(p.getUniqueId()));
        
        // Store players for menu
        playerMenuPlayers.put(player.getUniqueId(), new ArrayList<>(onlinePlayers));
        playerMenuPages.put(player.getUniqueId(), 0);
        
        // Fill menu
        updatePlayerSelectionMenu(player, inventory, 0);
        
        // Open inventory
        player.openInventory(inventory);
        
        // Store open inventory
        openInventories.put(player.getUniqueId(), inventory);
    }
    
    /**
     * Open the remove trusted player menu
     * @param player The player to open the menu for
     */
    private void openRemoveTrustedMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, "§8» §7Vertrauten Spieler entfernen");
        
        // Get trusted players
        List<OfflinePlayer> trustedPlayers = new ArrayList<>();
        for (UUID trustedUUID : claim.getTrustedPlayers()) {
            trustedPlayers.add(Bukkit.getOfflinePlayer(trustedUUID));
        }
        
        // Store players for menu
        playerMenuPlayers.put(player.getUniqueId(), trustedPlayers);
        playerMenuPages.put(player.getUniqueId(), 0);
        
        // Fill menu
        updatePlayerSelectionMenu(player, inventory, 0);
        
        // Open inventory
        player.openInventory(inventory);
        
        // Store open inventory
        openInventories.put(player.getUniqueId(), inventory);
    }
    
    /**
     * Update the player selection menu
     * @param player The player viewing the menu
     * @param inventory The inventory to update
     * @param page The page to display
     */
    private void updatePlayerSelectionMenu(Player player, Inventory inventory, int page) {
        // Clear inventory
        inventory.clear();
        
        // Get players for this menu
        List<OfflinePlayer> players = playerMenuPlayers.get(player.getUniqueId());
        
        if (players == null || players.isEmpty()) {
            // No players available
            ItemStack noPlayersItem = new ItemStack(Material.BARRIER);
            ItemMeta noPlayersMeta = noPlayersItem.getItemMeta();
            if (noPlayersMeta != null) {
                noPlayersMeta.setDisplayName("§c§lKeine Spieler verfügbar");
                noPlayersItem.setItemMeta(noPlayersMeta);
            }
            inventory.setItem(22, noPlayersItem);
            
            // Back button
            ItemStack backItem = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backItem.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName("§7§lZurück");
                backItem.setItemMeta(backMeta);
            }
            inventory.setItem(49, backItem);
            
            return;
        }
        
        // Calculate total pages
        int totalPages = (int) Math.ceil(players.size() / (double) TRUSTED_PLAYERS_PER_PAGE);
        
        // Ensure page is valid
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerMenuPages.put(player.getUniqueId(), page);
        
        // Add player heads
        int startIndex = page * TRUSTED_PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + TRUSTED_PLAYERS_PER_PAGE, players.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            OfflinePlayer offlinePlayer = players.get(i);
            int slot = i - startIndex;
            
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            
            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);
                meta.setDisplayName("§a§l" + (offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unbekannt"));
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Klicke, um diesen Spieler " + (player.getOpenInventory().getTitle().contains("hinzufügen") ? "hinzuzufügen" : "zu entfernen"));
                
                meta.setLore(lore);
                playerHead.setItemMeta(meta);
            }
            
            inventory.setItem(slot, playerHead);
        }
        
        // Navigation buttons
        // Previous page
        if (page > 0) {
            ItemStack previousItem = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previousItem.getItemMeta();
            if (previousMeta != null) {
                previousMeta.setDisplayName("§7§lVorherige Seite");
                previousItem.setItemMeta(previousMeta);
            }
            inventory.setItem(48, previousItem);
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§7§lZurück");
            backItem.setItemMeta(backMeta);
        }
        inventory.setItem(49, backItem);
        
        // Next page
        if (page < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§7§lNächste Seite");
                nextItem.setItemMeta(nextMeta);
            }
            inventory.setItem(50, nextItem);
        }
    }
    
    /**
     * Handle inventory click events
     * @param event The event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        
        // Check if this is one of our inventories
        if (!openInventories.containsKey(playerUUID)) {
            return;
        }
        
        // Cancel the event to prevent item moving
        event.setCancelled(true);
        
        // Get clicked inventory and slot
        Inventory clickedInventory = event.getClickedInventory();
        int slot = event.getSlot();
        
        // Check if the player clicked in their own inventory
        if (clickedInventory == null || clickedInventory.equals(player.getInventory())) {
            return;
        }
        
        // Get the title of the inventory
        String title = event.getView().getTitle();
        
        // Handle main menu clicks
        if (title.equals("§8» §7Grundstücksverwaltung")) {
            handleMainMenuClick(player, slot);
        }
        // Handle add trusted player menu clicks
        else if (title.equals("§8» §7Vertrauten Spieler hinzufügen")) {
            handleAddTrustedMenuClick(player, slot);
        }
        // Handle remove trusted player menu clicks
        else if (title.equals("§8» §7Vertrauten Spieler entfernen")) {
            handleRemoveTrustedMenuClick(player, slot);
        }
    }
    
    /**
     * Handle main menu clicks
     * @param player The player who clicked
     * @param slot The slot that was clicked
     */
    private void handleMainMenuClick(Player player, int slot) {
        // Only the owner can manage the claim
        if (!player.getUniqueId().equals(claim.getOwnerUUID())) {
            return;
        }
        
        switch (slot) {
            case ADD_TRUSTED_SLOT:
                openAddTrustedMenu(player);
                break;
                
            case REMOVE_TRUSTED_SLOT:
                openRemoveTrustedMenu(player);
                break;
                
            case EXPAND_SLOT:
                // TODO: Implement claim expansion
                player.closeInventory();
                plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.expand-not-implemented");
                break;
                
            case TELEPORT_SLOT:
                player.closeInventory();
                teleportToClaim(player);
                break;
                
            case DELETE_SLOT:
                player.closeInventory();
                deleteClaim(player);
                break;
        }
    }
    
    /**
     * Handle add trusted player menu clicks
     * @param player The player who clicked
     * @param slot The slot that was clicked
     */
    private void handleAddTrustedMenuClick(Player player, int slot) {
        // Check if this is a navigation button
        if (slot == 48) {
            // Previous page
            int page = playerMenuPages.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) {
                updatePlayerSelectionMenu(player, player.getOpenInventory().getTopInventory(), page - 1);
            }
        } else if (slot == 49) {
            // Back button
            open(player);
        } else if (slot == 50) {
            // Next page
            int page = playerMenuPages.getOrDefault(player.getUniqueId(), 0);
            List<OfflinePlayer> players = playerMenuPlayers.get(player.getUniqueId());
            int totalPages = (int) Math.ceil(players.size() / (double) TRUSTED_PLAYERS_PER_PAGE);
            if (page < totalPages - 1) {
                updatePlayerSelectionMenu(player, player.getOpenInventory().getTopInventory(), page + 1);
            }
        } else if (slot < TRUSTED_PLAYERS_PER_PAGE) {
            // Player selection
            List<OfflinePlayer> players = playerMenuPlayers.get(player.getUniqueId());
            int page = playerMenuPages.getOrDefault(player.getUniqueId(), 0);
            int index = page * TRUSTED_PLAYERS_PER_PAGE + slot;
            
            if (players != null && index < players.size()) {
                OfflinePlayer selectedPlayer = players.get(index);
                
                // Add trusted player
                if (plugin.getAPI().getClaimAPI().addTrustedPlayer(claim, selectedPlayer)) {
                    player.closeInventory();
                    
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("player", selectedPlayer.getName() != null ? selectedPlayer.getName() : "Unbekannt");
                    
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "claim.trusted-added",
                            replacements
                    );
                }
            }
        }
    }
    
    /**
     * Handle remove trusted player menu clicks
     * @param player The player who clicked
     * @param slot The slot that was clicked
     */
    private void handleRemoveTrustedMenuClick(Player player, int slot) {
        // Check if this is a navigation button
        if (slot == 48) {
            // Previous page
            int page = playerMenuPages.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) {
                updatePlayerSelectionMenu(player, player.getOpenInventory().getTopInventory(), page - 1);
            }
        } else if (slot == 49) {
            // Back button
            open(player);
        } else if (slot == 50) {
            // Next page
            int page = playerMenuPages.getOrDefault(player.getUniqueId(), 0);
            List<OfflinePlayer> players = playerMenuPlayers.get(player.getUniqueId());
            int totalPages = (int) Math.ceil(players.size() / (double) TRUSTED_PLAYERS_PER_PAGE);
            if (page < totalPages - 1) {
                updatePlayerSelectionMenu(player, player.getOpenInventory().getTopInventory(), page + 1);
            }
        } else if (slot < TRUSTED_PLAYERS_PER_PAGE) {
            // Player selection
            List<OfflinePlayer> players = playerMenuPlayers.get(player.getUniqueId());
            int page = playerMenuPages.getOrDefault(player.getUniqueId(), 0);
            int index = page * TRUSTED_PLAYERS_PER_PAGE + slot;
            
            if (players != null && index < players.size()) {
                OfflinePlayer selectedPlayer = players.get(index);
                
                // Remove trusted player
                if (plugin.getAPI().getClaimAPI().removeTrustedPlayer(claim, selectedPlayer)) {
                    player.closeInventory();
                    
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("player", selectedPlayer.getName() != null ? selectedPlayer.getName() : "Unbekannt");
                    
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "claim.trusted-removed",
                            replacements
                    );
                }
            }
        }
    }
    
    /**
     * Handle inventory close events
     * @param event The event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Remove open inventory
        openInventories.remove(player.getUniqueId());
        
        // Clear player menu data
        playerMenuPages.remove(player.getUniqueId());
        playerMenuPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Teleport a player to the claim
     * @param player The player to teleport
     */
    private void teleportToClaim(Player player) {
        // Get the world
        org.bukkit.World world = Bukkit.getWorld(claim.getWorldName());
        
        if (world == null) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.world-not-found");
            return;
        }
        
        // Create the teleport location (above the marker block)
        org.bukkit.Location location = new org.bukkit.Location(
                world,
                claim.getMarkerX() + 0.5,
                claim.getMarkerY() + 1,
                claim.getMarkerZ() + 0.5
        );
        
        // Teleport the player
        plugin.getAPI().getTeleportAPI().teleport(player, location);
        
        // Send success message
        plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.teleported");
    }
    
    /**
     * Delete a claim
     * @param player The player deleting the claim
     */
    private void deleteClaim(Player player) {
        // Check if player is the owner
        if (!player.getUniqueId().equals(claim.getOwnerUUID())) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.not-owner");
            return;
        }
        
        // Delete the claim
        if (plugin.getAPI().getClaimAPI().deleteClaim(claim)) {
            // Send success message
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.deleted");
        } else {
            // Send error message
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.delete-failed");
        }
    }
}