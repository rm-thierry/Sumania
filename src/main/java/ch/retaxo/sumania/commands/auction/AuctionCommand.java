package ch.retaxo.sumania.commands.auction;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.api.auction.AuctionAPI;
import ch.retaxo.sumania.models.Auction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Command handler for the auction house
 */
public class AuctionCommand implements CommandExecutor, TabCompleter, Listener {

    private final Sumania plugin;
    private final AuctionAPI auctionAPI;
    private final AuctionMenuHandler menuHandler;
    
    private final Map<UUID, ItemStack> creatingAuction = new HashMap<>();
    private final Map<UUID, Double> settingPrice = new HashMap<>();
    private final Map<UUID, Integer> settingDuration = new HashMap<>();
    private final Map<UUID, String> settingCategory = new HashMap<>();
    private final Map<UUID, Integer> viewingPage = new HashMap<>();
    private final Map<UUID, String> viewingCategory = new HashMap<>();
    
    // NamespacedKeys for persistent data
    private final NamespacedKey auctionIdKey;
    private final NamespacedKey menuActionKey;
    private final NamespacedKey valueKey;
    private final NamespacedKey pageKey;

    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public AuctionCommand(Sumania plugin) {
        this.plugin = plugin;
        this.auctionAPI = plugin.getAPI().getAuctionAPI();
        this.menuHandler = new AuctionMenuHandler(plugin);
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(menuHandler, plugin);
        
        // Create NamespacedKeys
        auctionIdKey = new NamespacedKey(plugin, "auction_id");
        menuActionKey = new NamespacedKey(plugin, "menu_action");
        valueKey = new NamespacedKey(plugin, "value");
        pageKey = new NamespacedKey(plugin, "page");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if auction house is enabled
        if (!plugin.getConfigManager().getConfig("config.yml").getBoolean("auction.enabled", true)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDas Auktionshaus ist derzeit deaktiviert.");
            return true;
        }
        
        // Check if command is enabled
        if (!plugin.getConfigManager().getConfig("config.yml").getBoolean("commands.auction", true)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDieser Befehl ist deaktiviert.");
            return true;
        }
        
        // Check permission
        if (!player.hasPermission("sumania.auction")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }
        
        // Handle subcommands
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "help":
                    sendHelpMessage(player);
                    return true;
                case "sell":
                    handleSellCommand(player, args);
                    return true;
                case "cancel":
                    handleCancelCommand(player, args);
                    return true;
                case "my":
                case "view":
                    menuHandler.openPlayerAuctionsMenu(player);
                    return true;
                case "category":
                    if (args.length > 1) {
                        menuHandler.openCategoryMenu(player, args[1]);
                    } else {
                        menuHandler.openCategoriesMenu(player);
                    }
                    return true;
                case "search":
                    // Not implemented yet - planned for future
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§eDiese Funktion ist noch nicht verfügbar.");
                    return true;
                default:
                    // Open main menu
                    menuHandler.openMainMenu(player);
                    return true;
            }
        } else {
            // No arguments, open main menu
            menuHandler.openMainMenu(player);
            return true;
        }
    }
    
    /**
     * Send help message to player
     * @param player The player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage("§8§m---------------------§r §b§lAuktionshaus §8§m---------------------");
        player.sendMessage("§b/ah §8- §7Öffnet das Auktionshaus");
        player.sendMessage("§b/ah sell §8- §7Verkaufe das Item in deiner Hand");
        player.sendMessage("§b/ah cancel <id> §8- §7Bricht eine deiner Auktionen ab");
        player.sendMessage("§b/ah my §8- §7Zeigt deine aktiven Auktionen");
        player.sendMessage("§b/ah category §8- §7Zeigt alle Kategorien");
        player.sendMessage("§b/ah help §8- §7Zeigt diese Hilfe an");
        player.sendMessage("§8§m--------------------------------------------------------");
    }
    
    /**
     * Handle the sell command
     * @param player The player
     * @param args The command arguments
     */
    private void handleSellCommand(Player player, String[] args) {
        // Check if player is already in the process of creating an auction
        if (creatingAuction.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDu bist bereits dabei, eine Auktion zu erstellen.");
            return;
        }
        
        // Check if player has item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDu musst ein Item in der Hand halten, um es zu verkaufen.");
            return;
        }
        
        // Store item for auction creation
        creatingAuction.put(player.getUniqueId(), item.clone());
        
        // Open price selection menu
        menuHandler.openCreateAuctionMenu(player, item);
    }
    
    /**
     * Handle the cancel command
     * @param player The player
     * @param args The command arguments
     */
    private void handleCancelCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cVerwendung: /ah cancel <id>");
            return;
        }
        
        try {
            int auctionId = Integer.parseInt(args[1]);
            
            // Get the auction
            Auction auction = auctionAPI.getAuction(auctionId);
            
            if (auction == null) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cAuktion mit ID " + auctionId + " nicht gefunden.");
                return;
            }
            
            // Check if player is the seller
            if (!auction.getSellerUuid().equals(player.getUniqueId())) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDu kannst nur deine eigenen Auktionen abbrechen.");
                return;
            }
            
            // Check if auction is active
            if (!auction.isActive()) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cDiese Auktion ist nicht mehr aktiv.");
                return;
            }
            
            // Cancel the auction
            if (auctionAPI.cancelAuction(auction, player)) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§aAuktion erfolgreich abgebrochen.");
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cFehler beim Abbrechen der Auktion.");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cBitte gib eine gültige Auktions-ID an.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("help", "sell", "cancel", "my", "view", "category", "search");
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("category")) {
            // Second argument for category - list available categories
            for (String category : auctionAPI.getCategories().keySet()) {
                if (category.startsWith(args[1].toLowerCase())) {
                    completions.add(category);
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Create an item for the menu
     * @param material The material
     * @param name The name
     * @param lore The lore
     * @return The created item
     */
    public ItemStack createMenuItem(Material material, String name, List<String> lore) {
        return menuHandler.createMenuItem(material, name, lore);
    }
    
    /**
     * Create an item with custom model data
     * @param material The material
     * @param name The name
     * @param lore The lore
     * @param modelData The custom model data
     * @return The created item
     */
    public ItemStack createMenuItemWithModelData(Material material, String name, List<String> lore, int modelData) {
        return menuHandler.createMenuItemWithModelData(material, name, lore, modelData);
    }
    
    /**
     * Set a menu action for an item
     * @param item The item
     * @param action The action
     * @return The modified item
     */
    public ItemStack setMenuAction(ItemStack item, String action) {
        return menuHandler.setMenuAction(item, action);
    }
    
    /**
     * Set a value for an item
     * @param item The item
     * @param value The value
     * @return The modified item
     */
    public ItemStack setValue(ItemStack item, String value) {
        return menuHandler.setValue(item, value);
    }
    
    /**
     * Set an auction ID for an item
     * @param item The item
     * @param auctionId The auction ID
     * @return The modified item
     */
    public ItemStack setAuctionId(ItemStack item, int auctionId) {
        return menuHandler.setAuctionId(item, auctionId);
    }
    
    /**
     * Get the auction ID from an item
     * @param item The item
     * @return The auction ID, or -1 if not set
     */
    public int getAuctionId(ItemStack item) {
        return menuHandler.getAuctionId(item);
    }
    
    /**
     * Get the menu action from an item
     * @param item The item
     * @return The menu action, or null if not set
     */
    public String getMenuAction(ItemStack item) {
        return menuHandler.getMenuAction(item);
    }
    
    /**
     * Get the value from an item
     * @param item The item
     * @return The value, or null if not set
     */
    public String getValue(ItemStack item) {
        return menuHandler.getValue(item);
    }
    
    /**
     * Check if a player is creating an auction
     * @param player The player
     * @return True if the player is creating an auction
     */
    public boolean isCreatingAuction(Player player) {
        return creatingAuction.containsKey(player.getUniqueId());
    }
    
    /**
     * Get the item a player is creating an auction for
     * @param player The player
     * @return The item, or null if not creating an auction
     */
    public ItemStack getCreatingAuctionItem(Player player) {
        return creatingAuction.get(player.getUniqueId());
    }
    
    /**
     * Set the price for an auction being created
     * @param player The player
     * @param price The price
     */
    public void setAuctionPrice(Player player, double price) {
        settingPrice.put(player.getUniqueId(), price);
    }
    
    /**
     * Get the price for an auction being created
     * @param player The player
     * @return The price, or -1 if not set
     */
    public double getAuctionPrice(Player player) {
        return settingPrice.getOrDefault(player.getUniqueId(), -1.0);
    }
    
    /**
     * Set the duration for an auction being created
     * @param player The player
     * @param duration The duration in hours
     */
    public void setAuctionDuration(Player player, int duration) {
        settingDuration.put(player.getUniqueId(), duration);
    }
    
    /**
     * Get the duration for an auction being created
     * @param player The player
     * @return The duration in hours, or -1 if not set
     */
    public int getAuctionDuration(Player player) {
        return settingDuration.getOrDefault(player.getUniqueId(), -1);
    }
    
    /**
     * Set the category for an auction being created
     * @param player The player
     * @param category The category
     */
    public void setAuctionCategory(Player player, String category) {
        settingCategory.put(player.getUniqueId(), category);
    }
    
    /**
     * Get the category for an auction being created
     * @param player The player
     * @return The category, or null if not set
     */
    public String getAuctionCategory(Player player) {
        return settingCategory.get(player.getUniqueId());
    }
    
    /**
     * Complete the auction creation process
     * @param player The player
     */
    public void completeAuctionCreation(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!creatingAuction.containsKey(playerId) || 
            !settingPrice.containsKey(playerId) || 
            !settingDuration.containsKey(playerId)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cFehler beim Erstellen der Auktion.");
            return;
        }
        
        ItemStack item = creatingAuction.get(playerId);
        double price = settingPrice.get(playerId);
        int duration = settingDuration.get(playerId);
        String category = settingCategory.get(playerId);
        
        // Create the auction
        int auctionId = auctionAPI.createAuction(player, item, price, duration, category);
        
        if (auctionId != -1) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aAuktion erfolgreich erstellt! ID: " + auctionId);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cFehler beim Erstellen der Auktion.");
            // Return the item to the player if auction creation failed
            player.getInventory().addItem(item);
        }
        
        // Clear creation data
        clearAuctionCreationData(playerId);
    }
    
    /**
     * Cancel the auction creation process
     * @param player The player
     */
    public void cancelAuctionCreation(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (creatingAuction.containsKey(playerId)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cAuktion abgebrochen.");
        }
        
        // Clear creation data
        clearAuctionCreationData(playerId);
    }
    
    /**
     * Clear auction creation data for a player
     * @param playerId The player UUID
     */
    private void clearAuctionCreationData(UUID playerId) {
        creatingAuction.remove(playerId);
        settingPrice.remove(playerId);
        settingDuration.remove(playerId);
        settingCategory.remove(playerId);
    }
    
    /**
     * Set the page a player is viewing
     * @param player The player
     * @param page The page
     */
    public void setViewingPage(Player player, int page) {
        viewingPage.put(player.getUniqueId(), page);
    }
    
    /**
     * Get the page a player is viewing
     * @param player The player
     * @return The page, or 0 if not set
     */
    public int getViewingPage(Player player) {
        return viewingPage.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Set the category a player is viewing
     * @param player The player
     * @param category The category
     */
    public void setViewingCategory(Player player, String category) {
        viewingCategory.put(player.getUniqueId(), category);
    }
    
    /**
     * Get the category a player is viewing
     * @param player The player
     * @return The category, or null if not set
     */
    public String getViewingCategory(Player player) {
        return viewingCategory.get(player.getUniqueId());
    }
    
    /**
     * Clear viewing data for a player
     * @param player The player
     */
    public void clearViewingData(Player player) {
        UUID playerId = player.getUniqueId();
        viewingPage.remove(playerId);
        viewingCategory.remove(playerId);
    }
}