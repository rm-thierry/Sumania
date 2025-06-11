package ch.retaxo.sumania.commands.auction;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.api.auction.AuctionAPI;
import ch.retaxo.sumania.models.Auction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handler for auction house menus
 */
public class AuctionMenuHandler implements Listener {

    private final Sumania plugin;
    private final AuctionAPI auctionAPI;
    
    // NamespacedKeys for persistent data
    private final NamespacedKey auctionIdKey;
    private final NamespacedKey menuActionKey;
    private final NamespacedKey valueKey;
    private final NamespacedKey pageKey;
    
    // Price format
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    
    // Color codes from config
    private String primaryColor;
    private String secondaryColor;
    private String highlightColor;
    private String warningColor;
    private String priceColor;
    private String sellerColor;
    private String timeColor;
    
    // Menu titles from config
    private String mainMenuTitle;
    private String createAuctionTitle;
    private String playerAuctionsTitle;
    private String categoriesTitle;
    private String searchTitle;
    
    // Item materials from config
    private Material borderItem;
    private Material createAuctionItem;
    private Material playerAuctionsItem;
    private Material categoriesItem;
    private Material searchItem;
    private Material nextPageItem;
    private Material previousPageItem;
    private Material backItem;
    private Material confirmItem;
    private Material cancelItem;
    
    // Menu settings
    private int mainMenuRows;
    private int itemsPerPage;

    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public AuctionMenuHandler(Sumania plugin) {
        this.plugin = plugin;
        this.auctionAPI = plugin.getAPI().getAuctionAPI();
        
        // Create NamespacedKeys
        auctionIdKey = new NamespacedKey(plugin, "auction_id");
        menuActionKey = new NamespacedKey(plugin, "menu_action");
        valueKey = new NamespacedKey(plugin, "value");
        pageKey = new NamespacedKey(plugin, "page");
        
        // Load settings from config
        loadSettings();
    }
    
    /**
     * Load settings from config
     */
    private void loadSettings() {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Load color codes
        primaryColor = config.getString("auction.style.primary-color", "§7");
        secondaryColor = config.getString("auction.style.secondary-color", "§8");
        highlightColor = config.getString("auction.style.highlight-color", "§b");
        warningColor = config.getString("auction.style.warning-color", "§e");
        priceColor = config.getString("auction.style.price-color", "§a");
        sellerColor = config.getString("auction.style.seller-color", "§d");
        timeColor = config.getString("auction.style.time-color", "§f");
        
        // Load menu titles
        mainMenuTitle = config.getString("auction.style.menu-title", "§8» §7Auktionshaus");
        createAuctionTitle = config.getString("auction.style.create-auction-title", "§8» §7Auktion erstellen");
        playerAuctionsTitle = config.getString("auction.style.player-auctions-title", "§8» §7Meine Auktionen");
        categoriesTitle = config.getString("auction.style.categories-title", "§8» §7Kategorien");
        searchTitle = config.getString("auction.style.search-title", "§8» §7Suchergebnisse");
        
        // Load item materials
        borderItem = Material.getMaterial(config.getString("auction.style.border-item", "BLACK_STAINED_GLASS_PANE"));
        createAuctionItem = Material.getMaterial(config.getString("auction.style.create-auction-item", "GOLD_INGOT"));
        playerAuctionsItem = Material.getMaterial(config.getString("auction.style.player-auctions-item", "PLAYER_HEAD"));
        categoriesItem = Material.getMaterial(config.getString("auction.style.categories-item", "CHEST"));
        searchItem = Material.getMaterial(config.getString("auction.style.search-item", "COMPASS"));
        nextPageItem = Material.getMaterial(config.getString("auction.style.next-page-item", "ARROW"));
        previousPageItem = Material.getMaterial(config.getString("auction.style.previous-page-item", "ARROW"));
        backItem = Material.getMaterial(config.getString("auction.style.back-item", "BARRIER"));
        confirmItem = Material.getMaterial(config.getString("auction.style.confirm-item", "LIME_STAINED_GLASS_PANE"));
        cancelItem = Material.getMaterial(config.getString("auction.style.cancel-item", "RED_STAINED_GLASS_PANE"));
        
        // Set fallbacks if materials couldn't be loaded
        if (borderItem == null) borderItem = Material.BLACK_STAINED_GLASS_PANE;
        if (createAuctionItem == null) createAuctionItem = Material.GOLD_INGOT;
        if (playerAuctionsItem == null) playerAuctionsItem = Material.PLAYER_HEAD;
        if (categoriesItem == null) categoriesItem = Material.CHEST;
        if (searchItem == null) searchItem = Material.COMPASS;
        if (nextPageItem == null) nextPageItem = Material.ARROW;
        if (previousPageItem == null) previousPageItem = Material.ARROW;
        if (backItem == null) backItem = Material.BARRIER;
        if (confirmItem == null) confirmItem = Material.LIME_STAINED_GLASS_PANE;
        if (cancelItem == null) cancelItem = Material.RED_STAINED_GLASS_PANE;
        
        // Load menu settings
        mainMenuRows = config.getInt("auction.main-menu-rows", 6);
        if (mainMenuRows < 1) mainMenuRows = 1;
        if (mainMenuRows > 6) mainMenuRows = 6;
        
        itemsPerPage = config.getInt("auction.items-per-page", 36);
        if (itemsPerPage < 1) itemsPerPage = 1;
    }
    
    /**
     * Open the main auction house menu
     * @param player The player
     */
    public void openMainMenu(Player player) {
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, mainMenuRows * 9, mainMenuTitle);
        
        // Add border items - full border around the entire GUI
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = (mainMenuRows - 1) * 9; i < mainMenuRows * 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 0; i < mainMenuRows; i++) {
            menu.setItem(i * 9, createMenuItem(borderItem, " ", null));
            menu.setItem(i * 9 + 8, createMenuItem(borderItem, " ", null));
        }
        
        // Add menu items
        ItemStack createAuction = createMenuItem(createAuctionItem, 
                highlightColor + "Auktion erstellen", 
                Arrays.asList(
                    primaryColor + "Verkaufe ein Item im Auktionshaus",
                    primaryColor + "und lasse andere Spieler darauf bieten.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um ein Item zu verkaufen"
                ));
        createAuction = setMenuAction(createAuction, "create_auction");
        menu.setItem(2, createAuction);
        
        ItemStack playerAuctions = createMenuItem(playerAuctionsItem, 
                highlightColor + "Meine Auktionen", 
                Arrays.asList(
                    primaryColor + "Zeigt deine aktiven Auktionen",
                    primaryColor + "und abgelaufenen Auktionen an.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um deine Auktionen zu sehen"
                ));
        playerAuctions = setMenuAction(playerAuctions, "my_auctions");
        if (playerAuctionsItem == Material.PLAYER_HEAD) {
            playerAuctions = createPlayerHead(player, highlightColor + "Meine Auktionen", 
                    Arrays.asList(
                        primaryColor + "Zeigt deine aktiven Auktionen",
                        primaryColor + "und abgelaufenen Auktionen an.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um deine Auktionen zu sehen"
                    ));
            playerAuctions = setMenuAction(playerAuctions, "my_auctions");
        }
        menu.setItem(4, playerAuctions);
        
        ItemStack categories = createMenuItem(categoriesItem, 
                highlightColor + "Kategorien", 
                Arrays.asList(
                    primaryColor + "Durchsuche das Auktionshaus",
                    primaryColor + "nach verschiedenen Kategorien.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um Kategorien anzuzeigen"
                ));
        categories = setMenuAction(categories, "categories");
        menu.setItem(6, categories);
        
        // Add recent auctions - but leave the first slot (10) empty and maintain border
        List<Auction> recentAuctions = auctionAPI.getActiveAuctions();
        int maxAuctions = Math.min(recentAuctions.size(), (mainMenuRows - 2) * 7 - 1);
        
        // Start from slot 11 (skip slot 10) and maintain proper spacing
        for (int i = 0; i < maxAuctions; i++) {
            Auction auction = recentAuctions.get(i);
            ItemStack auctionItem = createAuctionItem(auction);
            
            // Calculate position - skip first slot (10) in the first row
            int position;
            if (i < 6) {
                position = 11 + i; // slots 11-16 in first row
            } else {
                // For subsequent rows, use normal calculation but adjust for the skipped slot
                int row = (i + 1) / 7; // +1 because we skipped one
                int col = (i + 1) % 7; // +1 because we skipped one
                position = 9 + col + (row * 9);
            }
            
            menu.setItem(position, auctionItem);
        }
        
        // Open the menu
        player.openInventory(menu);
    }
    
    /**
     * Open the create auction menu
     * @param player The player
     * @param item The item to auction
     */
    public void openCreateAuctionMenu(Player player, ItemStack item) {
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 3 * 9, createAuctionTitle);
        
        // Add border items
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 18; i < 27; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        menu.setItem(9, createMenuItem(borderItem, " ", null));
        menu.setItem(17, createMenuItem(borderItem, " ", null));
        
        // Add item to auction
        menu.setItem(13, item.clone());
        
        // Add price options
        double minPrice = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.min-price", 10.0);
        
        // Predefined price options
        ItemStack price1 = createMenuItem(Material.GOLD_NUGGET, 
                highlightColor + "Preis: " + priceColor + priceFormat.format(minPrice) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName(), 
                Arrays.asList(
                    primaryColor + "Setze den Preis für deine Auktion",
                    primaryColor + "auf " + priceColor + priceFormat.format(minPrice) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName() + primaryColor + ".",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um diesen Preis zu wählen"
                ));
        price1 = setMenuAction(price1, "set_price");
        price1 = setValue(price1, String.valueOf(minPrice));
        menu.setItem(11, price1);
        
        ItemStack price2 = createMenuItem(Material.GOLD_INGOT, 
                highlightColor + "Preis: " + priceColor + priceFormat.format(minPrice * 10) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName(), 
                Arrays.asList(
                    primaryColor + "Setze den Preis für deine Auktion",
                    primaryColor + "auf " + priceColor + priceFormat.format(minPrice * 10) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName() + primaryColor + ".",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um diesen Preis zu wählen"
                ));
        price2 = setMenuAction(price2, "set_price");
        price2 = setValue(price2, String.valueOf(minPrice * 10));
        menu.setItem(12, price2);
        
        ItemStack price3 = createMenuItem(Material.GOLD_BLOCK, 
                highlightColor + "Preis: " + priceColor + priceFormat.format(minPrice * 100) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName(), 
                Arrays.asList(
                    primaryColor + "Setze den Preis für deine Auktion",
                    primaryColor + "auf " + priceColor + priceFormat.format(minPrice * 100) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName() + primaryColor + ".",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um diesen Preis zu wählen"
                ));
        price3 = setMenuAction(price3, "set_price");
        price3 = setValue(price3, String.valueOf(minPrice * 100));
        menu.setItem(14, price3);
        
        ItemStack price4 = createMenuItem(Material.EMERALD, 
                highlightColor + "Preis: " + priceColor + priceFormat.format(minPrice * 1000) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName(), 
                Arrays.asList(
                    primaryColor + "Setze den Preis für deine Auktion",
                    primaryColor + "auf " + priceColor + priceFormat.format(minPrice * 1000) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName() + primaryColor + ".",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um diesen Preis zu wählen"
                ));
        price4 = setMenuAction(price4, "set_price");
        price4 = setValue(price4, String.valueOf(minPrice * 1000));
        menu.setItem(15, price4);
        
        // Custom price option
        ItemStack customPrice = createMenuItem(Material.NAME_TAG, 
                highlightColor + "Eigenen Preis festlegen", 
                Arrays.asList(
                    primaryColor + "Lege einen eigenen Preis für deine Auktion fest.",
                    primaryColor + "Min: " + priceColor + priceFormat.format(minPrice) + primaryColor + ", Max: " + priceColor + priceFormat.format(plugin.getConfigManager().getConfig("config.yml").getDouble("auction.max-price", 1000000.0)),
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um einen eigenen Preis festzulegen"
                ));
        customPrice = setMenuAction(customPrice, "custom_price");
        menu.setItem(16, customPrice);
        
        // Add cancel button
        ItemStack cancel = createMenuItem(cancelItem, 
                warningColor + "Abbrechen", 
                Arrays.asList(
                    primaryColor + "Bricht die Erstellung der Auktion ab.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um abzubrechen"
                ));
        cancel = setMenuAction(cancel, "cancel_auction");
        menu.setItem(22, cancel);
        
        // Open the menu
        player.openInventory(menu);
    }
    
    /**
     * Open the duration selection menu
     * @param player The player
     * @param item The item to auction
     * @param price The price of the auction
     */
    public void openDurationMenu(Player player, ItemStack item, double price) {
        try {
            // Validate inputs
            if (player == null || item == null || price <= 0) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cFehler beim Öffnen des Auktionsmenüs. Bitte versuche es erneut.");
                return;
            }
            
            // Create inventory
            Inventory menu = Bukkit.createInventory(null, 3 * 9, createAuctionTitle);
            
            // Add border items
            for (int i = 0; i < 9; i++) {
                menu.setItem(i, createMenuItem(borderItem, " ", null));
            }
            for (int i = 18; i < 27; i++) {
                menu.setItem(i, createMenuItem(borderItem, " ", null));
            }
            menu.setItem(9, createMenuItem(borderItem, " ", null));
            menu.setItem(17, createMenuItem(borderItem, " ", null));
            
            // Add item to auction with price
            ItemStack displayItem = item.clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add(priceColor + "Preis: " + priceFormat.format(price) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName());
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
            menu.setItem(13, displayItem);
            
            // Add duration options
            int minDuration = plugin.getConfigManager().getConfig("config.yml").getInt("auction.min-duration", 1);
            int maxDuration = plugin.getConfigManager().getConfig("config.yml").getInt("auction.max-duration", 72);
            int defaultDuration = plugin.getConfigManager().getConfig("config.yml").getInt("auction.default-duration", 24);
            
            ItemStack duration1 = createMenuItem(Material.CLOCK, 
                    highlightColor + "Dauer: " + timeColor + "1 Stunde", 
                    Arrays.asList(
                        primaryColor + "Setze die Dauer für deine Auktion",
                        primaryColor + "auf " + timeColor + "1 Stunde" + primaryColor + ".",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um diese Dauer zu wählen"
                    ));
            duration1 = setMenuAction(duration1, "set_duration");
            duration1 = setValue(duration1, "1");
            menu.setItem(11, duration1);
            
            ItemStack duration2 = createMenuItem(Material.CLOCK, 
                    highlightColor + "Dauer: " + timeColor + "6 Stunden", 
                    Arrays.asList(
                        primaryColor + "Setze die Dauer für deine Auktion",
                        primaryColor + "auf " + timeColor + "6 Stunden" + primaryColor + ".",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um diese Dauer zu wählen"
                    ));
            duration2 = setMenuAction(duration2, "set_duration");
            duration2 = setValue(duration2, "6");
            menu.setItem(12, duration2);
            
            ItemStack duration3 = createMenuItem(Material.CLOCK, 
                    highlightColor + "Dauer: " + timeColor + "12 Stunden", 
                    Arrays.asList(
                        primaryColor + "Setze die Dauer für deine Auktion",
                        primaryColor + "auf " + timeColor + "12 Stunden" + primaryColor + ".",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um diese Dauer zu wählen"
                    ));
            duration3 = setMenuAction(duration3, "set_duration");
            duration3 = setValue(duration3, "12");
            menu.setItem(14, duration3);
            
            ItemStack duration4 = createMenuItem(Material.CLOCK, 
                    highlightColor + "Dauer: " + timeColor + "24 Stunden", 
                    Arrays.asList(
                        primaryColor + "Setze die Dauer für deine Auktion",
                        primaryColor + "auf " + timeColor + "24 Stunden" + primaryColor + ".",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um diese Dauer zu wählen"
                    ));
            duration4 = setMenuAction(duration4, "set_duration");
            duration4 = setValue(duration4, "24");
            menu.setItem(15, duration4);
            
            // Add back button
            ItemStack back = createMenuItem(backItem, 
                    warningColor + "Zurück", 
                    Arrays.asList(
                        primaryColor + "Zurück zur Preisauswahl.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um zurückzugehen"
                    ));
            back = setMenuAction(back, "back_to_price");
            menu.setItem(22, back);
            
            // Open the menu
            player.openInventory(menu);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening duration menu: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEs ist ein Fehler aufgetreten. Bitte versuche es erneut.");
        }
    }
    
    /**
     * Open the category selection menu
     * @param player The player
     * @param item The item to auction
     * @param price The price of the auction
     * @param duration The duration of the auction in hours
     */
    public void openCategoryMenu(Player player, ItemStack item, double price, int duration) {
        try {
            // Validate inputs
            if (player == null || item == null || price <= 0 || duration <= 0) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cFehler beim Öffnen des Kategoriemenüs. Bitte versuche es erneut.");
                return;
            }
            
            // Create inventory
            Inventory menu = Bukkit.createInventory(null, 3 * 9, createAuctionTitle);
            
            // Add border items
            for (int i = 0; i < 9; i++) {
                menu.setItem(i, createMenuItem(borderItem, " ", null));
            }
            for (int i = 18; i < 27; i++) {
                menu.setItem(i, createMenuItem(borderItem, " ", null));
            }
            menu.setItem(9, createMenuItem(borderItem, " ", null));
            menu.setItem(17, createMenuItem(borderItem, " ", null));
            
            // Add item to auction with price and duration
            ItemStack displayItem = item.clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add(priceColor + "Preis: " + priceFormat.format(price) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName());
            lore.add(timeColor + "Dauer: " + duration + " Stunden");
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
            menu.setItem(4, displayItem);
            
            // Add category options
            Map<String, Material> categories = auctionAPI.getCategories();
            int slot = 10;
            
            for (Map.Entry<String, Material> entry : categories.entrySet()) {
                if (slot >= 17) break; // Only show max 7 categories
                
                String categoryName = entry.getKey();
                Material categoryIcon = entry.getValue();
                
                ItemStack categoryItem = createMenuItem(categoryIcon, 
                        highlightColor + "Kategorie: " + ChatColor.WHITE + categoryName, 
                        Arrays.asList(
                            primaryColor + "Wähle die Kategorie " + ChatColor.WHITE + categoryName,
                            primaryColor + "für deine Auktion.",
                            "",
                            secondaryColor + "» " + primaryColor + "Klicke, um diese Kategorie zu wählen"
                        ));
                categoryItem = setMenuAction(categoryItem, "set_category");
                categoryItem = setValue(categoryItem, categoryName);
                menu.setItem(slot, categoryItem);
                
                slot++;
            }
            
            // Add no category option
            ItemStack noCategory = createMenuItem(Material.BARRIER, 
                    highlightColor + "Keine Kategorie", 
                    Arrays.asList(
                        primaryColor + "Erstelle die Auktion ohne Kategorie.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um keine Kategorie zu wählen"
                    ));
            noCategory = setMenuAction(noCategory, "set_category");
            noCategory = setValue(noCategory, "null");
            menu.setItem(16, noCategory);
            
            // Add back button
            ItemStack back = createMenuItem(backItem, 
                    warningColor + "Zurück", 
                    Arrays.asList(
                        primaryColor + "Zurück zur Dauerauswahl.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um zurückzugehen"
                    ));
            back = setMenuAction(back, "back_to_duration");
            menu.setItem(22, back);
            
            // Open the menu
            player.openInventory(menu);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening category menu: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEs ist ein Fehler aufgetreten. Bitte versuche es erneut.");
        }
    }
    
    /**
     * Open the confirmation menu
     * @param player The player
     * @param item The item to auction
     * @param price The price of the auction
     * @param duration The duration of the auction in hours
     * @param category The category of the auction
     */
    public void openConfirmMenu(Player player, ItemStack item, double price, int duration, String category) {
        try {
            // Validate inputs
            if (player == null || item == null || price <= 0 || duration <= 0) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§cFehler beim Öffnen des Bestätigungsmenüs. Bitte versuche es erneut.");
                return;
            }
            
            // Create inventory
            Inventory menu = Bukkit.createInventory(null, 3 * 9, createAuctionTitle);
            
            // Add border items
            for (int i = 0; i < 9; i++) {
                menu.setItem(i, createMenuItem(borderItem, " ", null));
            }
            for (int i = 18; i < 27; i++) {
                menu.setItem(i, createMenuItem(borderItem, " ", null));
            }
            menu.setItem(9, createMenuItem(borderItem, " ", null));
            menu.setItem(17, createMenuItem(borderItem, " ", null));
            
            // Add item to auction with all details
            ItemStack displayItem = item.clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add(priceColor + "Preis: " + priceFormat.format(price) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName());
            lore.add(timeColor + "Dauer: " + duration + " Stunden");
            if (category != null && !category.equals("null")) {
                lore.add(secondaryColor + "Kategorie: " + ChatColor.WHITE + category);
            } else {
                lore.add(secondaryColor + "Kategorie: " + ChatColor.WHITE + "Keine");
            }
            
            // Calculate and add listing fee
            double listingFeePercent = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.listing-fee-percent", 5.0);
            double minListingFee = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.min-listing-fee", 10.0);
            double maxListingFee = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.max-listing-fee", 1000.0);
            
            double listingFee = price * (listingFeePercent / 100.0);
            listingFee = Math.max(minListingFee, Math.min(maxListingFee, listingFee));
            
            lore.add("");
            lore.add(warningColor + "Gebühr: " + priceFormat.format(listingFee) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName());
            
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
            menu.setItem(13, displayItem);
            
            // Add confirm button
            ItemStack confirm = createMenuItem(confirmItem, 
                    highlightColor + "Bestätigen", 
                    Arrays.asList(
                        primaryColor + "Bestätige die Erstellung deiner Auktion",
                        primaryColor + "mit den gewählten Einstellungen.",
                        "",
                        warningColor + "Achtung: Das Item wird aus deinem",
                        warningColor + "Inventar entfernt und eine Gebühr wird abgezogen!",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um zu bestätigen"
                    ));
            confirm = setMenuAction(confirm, "confirm_auction");
            menu.setItem(11, confirm);
            
            // Add back to category button
            ItemStack backToCategory = createMenuItem(Material.ARROW, 
                    highlightColor + "Kategorie ändern", 
                    Arrays.asList(
                        primaryColor + "Zurück zur Kategorieauswahl.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um die Kategorie zu ändern"
                    ));
            backToCategory = setMenuAction(backToCategory, "back_to_category");
            menu.setItem(15, backToCategory);
            
            // Add cancel button
            ItemStack cancel = createMenuItem(cancelItem, 
                    warningColor + "Abbrechen", 
                    Arrays.asList(
                        primaryColor + "Bricht die Erstellung der Auktion ab.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um abzubrechen"
                    ));
            cancel = setMenuAction(cancel, "cancel_auction");
            menu.setItem(22, cancel);
            
            // Open the menu
            player.openInventory(menu);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening confirmation menu: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cEs ist ein Fehler aufgetreten. Bitte versuche es erneut.");
        }
    }
    
    /**
     * Open the player auctions menu
     * @param player The player
     */
    public void openPlayerAuctionsMenu(Player player) {
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 6 * 9, playerAuctionsTitle);
        
        // Add border items - full border around the entire GUI
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 45; i < 54; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 0; i < 6; i++) {
            menu.setItem(i * 9, createMenuItem(borderItem, " ", null));
            menu.setItem(i * 9 + 8, createMenuItem(borderItem, " ", null));
        }
        
        // Get player auctions
        List<Auction> auctions = auctionAPI.getAuctionsBySeller(player.getUniqueId());
        
        if (auctions.isEmpty()) {
            // No auctions
            ItemStack noAuctions = createMenuItem(Material.BARRIER, 
                    warningColor + "Keine Auktionen", 
                    Arrays.asList(
                        primaryColor + "Du hast noch keine Auktionen erstellt.",
                        primaryColor + "Erstelle eine Auktion mit " + secondaryColor + "/ah sell",
                        primaryColor + "oder " + secondaryColor + "/ah"
                    ));
            menu.setItem(22, noAuctions);
        } else {
            // Add auctions - maintain border and skip first slot in each row
            int slot = 10; // Start at the second slot of the second row
            int itemsAdded = 0;
            
            for (Auction auction : auctions) {
                if (itemsAdded >= 28) break; // Limit due to border and navigation
                
                // Skip border slots
                if (slot % 9 == 0) {
                    slot++; // Skip left border
                }
                if (slot % 9 == 8) {
                    slot += 2; // Skip right border and move to next row
                }
                if (slot >= 45) break; // Stop at bottom border
                
                ItemStack auctionItem = createPlayerAuctionItem(auction);
                menu.setItem(slot, auctionItem);
                
                slot++;
                itemsAdded++;
            }
        }
        
        // Add back button
        ItemStack back = createMenuItem(backItem, 
                warningColor + "Zurück", 
                Arrays.asList(
                    primaryColor + "Zurück zum Hauptmenü.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um zurückzugehen"
                ));
        back = setMenuAction(back, "back_to_main");
        menu.setItem(49, back);
        
        // Open the menu
        player.openInventory(menu);
    }
    
    /**
     * Open the categories menu
     * @param player The player
     */
    public void openCategoriesMenu(Player player) {
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 3 * 9, categoriesTitle);
        
        // Add border items
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 18; i < 27; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        
        // Add categories
        Map<String, Material> categories = auctionAPI.getCategories();
        int slot = 9;
        
        for (Map.Entry<String, Material> entry : categories.entrySet()) {
            if (slot >= 18) break; // Only show max 9 categories
            
            String categoryName = entry.getKey();
            Material categoryIcon = entry.getValue();
            
            ItemStack categoryItem = createMenuItem(categoryIcon, 
                    highlightColor + "Kategorie: " + ChatColor.WHITE + categoryName, 
                    Arrays.asList(
                        primaryColor + "Zeigt alle Auktionen in der",
                        primaryColor + "Kategorie " + ChatColor.WHITE + categoryName + primaryColor + ".",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um diese Kategorie anzuzeigen"
                    ));
            categoryItem = setMenuAction(categoryItem, "view_category");
            categoryItem = setValue(categoryItem, categoryName);
            menu.setItem(slot, categoryItem);
            
            slot++;
        }
        
        // Add back button
        ItemStack back = createMenuItem(backItem, 
                warningColor + "Zurück", 
                Arrays.asList(
                    primaryColor + "Zurück zum Hauptmenü.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um zurückzugehen"
                ));
        back = setMenuAction(back, "back_to_main");
        menu.setItem(22, back);
        
        // Open the menu
        player.openInventory(menu);
    }
    
    /**
     * Open a category menu
     * @param player The player
     * @param category The category
     */
    public void openCategoryMenu(Player player, String category) {
        // Get category auctions
        List<Auction> auctions = auctionAPI.getAuctionsByCategory(category);
        
        // Store current page and category
        AuctionCommand auctionCommand = (AuctionCommand) plugin.getCommandManager().getCommand("ah");
        auctionCommand.setViewingPage(player, 0);
        auctionCommand.setViewingCategory(player, category);
        
        // Open the category menu page
        openCategoryMenuPage(player, category, auctions, 0);
    }
    
    /**
     * Open a category menu page
     * @param player The player
     * @param category The category
     * @param auctions The auctions in the category
     * @param page The page number
     */
    public void openCategoryMenuPage(Player player, String category, List<Auction> auctions, int page) {
        // Calculate pages
        int totalPages = (int) Math.ceil((double) auctions.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 6 * 9, categoriesTitle + " - " + category);
        
        // Add border items
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 45; i < 54; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        
        if (auctions.isEmpty()) {
            // No auctions in this category
            ItemStack noAuctions = createMenuItem(Material.BARRIER, 
                    warningColor + "Keine Auktionen", 
                    Arrays.asList(
                        primaryColor + "Es gibt keine aktiven Auktionen",
                        primaryColor + "in dieser Kategorie."
                    ));
            menu.setItem(22, noAuctions);
        } else {
            // Calculate start and end indices for this page
            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, auctions.size());
            
            // Add auctions
            int slot = 9;
            
            for (int i = start; i < end; i++) {
                Auction auction = auctions.get(i);
                ItemStack auctionItem = createAuctionItem(auction);
                menu.setItem(slot, auctionItem);
                
                slot++;
            }
        }
        
        // Add navigation items
        // Previous page button
        if (page > 0) {
            ItemStack prevPage = createMenuItem(previousPageItem, 
                    highlightColor + "Vorherige Seite", 
                    Arrays.asList(
                        primaryColor + "Zeigt die vorherige Seite an.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um zur vorherigen Seite zu gehen"
                    ));
            prevPage = setMenuAction(prevPage, "prev_page");
            prevPage = setValue(prevPage, String.valueOf(page - 1));
            menu.setItem(45, prevPage);
        }
        
        // Page indicator
        ItemStack pageIndicator = createMenuItem(Material.PAPER, 
                highlightColor + "Seite " + (page + 1) + "/" + totalPages, 
                Arrays.asList(
                    primaryColor + "Du befindest dich auf Seite " + (page + 1),
                    primaryColor + "von " + totalPages + " Seiten."
                ));
        menu.setItem(49, pageIndicator);
        
        // Next page button
        if (page < totalPages - 1) {
            ItemStack nextPage = createMenuItem(nextPageItem, 
                    highlightColor + "Nächste Seite", 
                    Arrays.asList(
                        primaryColor + "Zeigt die nächste Seite an.",
                        "",
                        secondaryColor + "» " + primaryColor + "Klicke, um zur nächsten Seite zu gehen"
                    ));
            nextPage = setMenuAction(nextPage, "next_page");
            nextPage = setValue(nextPage, String.valueOf(page + 1));
            menu.setItem(53, nextPage);
        }
        
        // Add back button
        ItemStack back = createMenuItem(backItem, 
                warningColor + "Zurück", 
                Arrays.asList(
                    primaryColor + "Zurück zur Kategorieauswahl.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um zurückzugehen"
                ));
        back = setMenuAction(back, "back_to_categories");
        menu.setItem(48, back);
        
        // Open the menu
        player.openInventory(menu);
    }
    
    /**
     * Create an item for the menu
     * @param material The material
     * @param name The name
     * @param lore The lore
     * @return The created item
     */
    public ItemStack createMenuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
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
        ItemStack item = createMenuItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(modelData);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a player head item
     * @param player The player
     * @param name The name
     * @param lore The lore
     * @return The created player head
     */
    public ItemStack createPlayerHead(Player player, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Set a menu action for an item
     * @param item The item
     * @param action The action
     * @return The modified item
     */
    public ItemStack setMenuAction(ItemStack item, String action) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(menuActionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Set a value for an item
     * @param item The item
     * @param value The value
     * @return The modified item
     */
    public ItemStack setValue(ItemStack item, String value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Set an auction ID for an item
     * @param item The item
     * @param auctionId The auction ID
     * @return The modified item
     */
    public ItemStack setAuctionId(ItemStack item, int auctionId) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(auctionIdKey, PersistentDataType.INTEGER, auctionId);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get the auction ID from an item
     * @param item The item
     * @return The auction ID, or -1 if not set
     */
    public int getAuctionId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return -1;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return -1;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(auctionIdKey, PersistentDataType.INTEGER)) {
            return -1;
        }
        
        return container.get(auctionIdKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Get the menu action from an item
     * @param item The item
     * @return The menu action, or null if not set
     */
    public String getMenuAction(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(menuActionKey, PersistentDataType.STRING)) {
            return null;
        }
        
        return container.get(menuActionKey, PersistentDataType.STRING);
    }
    
    /**
     * Get the value from an item
     * @param item The item
     * @return The value, or null if not set
     */
    public String getValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(valueKey, PersistentDataType.STRING)) {
            return null;
        }
        
        return container.get(valueKey, PersistentDataType.STRING);
    }
    
    /**
     * Create an item for an auction
     * @param auction The auction
     * @return The created item
     */
    public ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Add auction info to lore
        if (!lore.isEmpty()) {
            lore.add("");
        }
        
        lore.add(priceColor + "Preis: " + priceFormat.format(auction.getPrice()) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName());
        lore.add(sellerColor + "Verkäufer: " + auction.getSellerName());
        
        if (auction.isActive()) {
            lore.add(timeColor + "Verbleibend: " + auction.getFormattedRemainingTime());
        } else if (auction.getStatus() == Auction.Status.SOLD) {
            lore.add(highlightColor + "Status: " + ChatColor.GREEN + "Verkauft");
            lore.add(sellerColor + "Käufer: " + auction.getBuyerName());
        } else if (auction.getStatus() == Auction.Status.EXPIRED) {
            lore.add(highlightColor + "Status: " + ChatColor.RED + "Abgelaufen");
        } else if (auction.getStatus() == Auction.Status.CANCELLED) {
            lore.add(highlightColor + "Status: " + ChatColor.RED + "Abgebrochen");
        }
        
        lore.add("");
        lore.add(highlightColor + "Auktions-ID: " + auction.getId());
        
        if (auction.isActive()) {
            lore.add(secondaryColor + "» " + primaryColor + "Klicke, um diese Auktion zu kaufen");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        // Add auction ID to item
        item = setAuctionId(item, auction.getId());
        
        if (auction.isActive()) {
            // Add menu action for active auctions
            item = setMenuAction(item, "buy_auction");
        }
        
        return item;
    }
    
    /**
     * Create an item for a player's auction
     * @param auction The auction
     * @return The created item
     */
    public ItemStack createPlayerAuctionItem(Auction auction) {
        ItemStack item = auction.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Add auction info to lore
        if (!lore.isEmpty()) {
            lore.add("");
        }
        
        lore.add(priceColor + "Preis: " + priceFormat.format(auction.getPrice()) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName());
        
        if (auction.isActive()) {
            lore.add(timeColor + "Verbleibend: " + auction.getFormattedRemainingTime());
            lore.add("");
            lore.add(secondaryColor + "» " + primaryColor + "Klicke, um diese Auktion abzubrechen");
        } else if (auction.getStatus() == Auction.Status.SOLD) {
            lore.add(highlightColor + "Status: " + ChatColor.GREEN + "Verkauft");
            lore.add(sellerColor + "Käufer: " + auction.getBuyerName());
        } else if (auction.getStatus() == Auction.Status.EXPIRED) {
            lore.add(highlightColor + "Status: " + ChatColor.RED + "Abgelaufen");
            lore.add("");
            lore.add(secondaryColor + "» " + primaryColor + "Klicke, um das Item zurückzufordern");
        } else if (auction.getStatus() == Auction.Status.CANCELLED) {
            lore.add(highlightColor + "Status: " + ChatColor.RED + "Abgebrochen");
        }
        
        lore.add("");
        lore.add(highlightColor + "Auktions-ID: " + auction.getId());
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        // Add auction ID to item
        item = setAuctionId(item, auction.getId());
        
        if (auction.isActive()) {
            // Add menu action for active auctions
            item = setMenuAction(item, "cancel_player_auction");
        } else if (auction.getStatus() == Auction.Status.EXPIRED) {
            // Add menu action for expired auctions
            item = setMenuAction(item, "claim_expired_auction");
        }
        
        return item;
    }
    
    /**
     * Open a custom price input menu
     * @param player The player
     * @param item The item to auction
     */
    public void openCustomPriceMenu(Player player, ItemStack item) {
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 3 * 9, createAuctionTitle + " - Eigener Preis");
        
        // Add border items
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 18; i < 27; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        menu.setItem(9, createMenuItem(borderItem, " ", null));
        menu.setItem(17, createMenuItem(borderItem, " ", null));
        
        // Add item to auction
        menu.setItem(4, item.clone());
        
        // Add price input options
        double minPrice = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.min-price", 10.0);
        double maxPrice = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.max-price", 1000000.0);
        
        // Display current price
        AuctionCommand auctionCommand = (AuctionCommand) plugin.getCommandManager().getCommand("ah");
        double currentPrice = auctionCommand.getAuctionPrice(player);
        if (currentPrice <= 0) {
            currentPrice = minPrice;
            auctionCommand.setAuctionPrice(player, currentPrice);
        }
        
        ItemStack currentPriceItem = createMenuItem(Material.GOLD_INGOT, 
                highlightColor + "Aktueller Preis: " + priceColor + priceFormat.format(currentPrice) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName(), 
                Arrays.asList(
                    primaryColor + "Dies ist der aktuelle Preis für deine Auktion.",
                    primaryColor + "Verwende die Buttons unten, um den Preis anzupassen.",
                    "",
                    primaryColor + "Min: " + priceColor + priceFormat.format(minPrice),
                    primaryColor + "Max: " + priceColor + priceFormat.format(maxPrice)
                ));
        menu.setItem(13, currentPriceItem);
        
        // Add value adjustment buttons
        ItemStack minus100 = createMenuItem(Material.RED_CONCRETE, 
                warningColor + "-100", 
                Arrays.asList(
                    primaryColor + "Reduziere den Preis um 100",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um 100 abzuziehen"
                ));
        minus100 = setMenuAction(minus100, "adjust_price");
        minus100 = setValue(minus100, "-100");
        menu.setItem(10, minus100);
        
        ItemStack minus10 = createMenuItem(Material.RED_CONCRETE, 
                warningColor + "-10", 
                Arrays.asList(
                    primaryColor + "Reduziere den Preis um 10",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um 10 abzuziehen"
                ));
        minus10 = setMenuAction(minus10, "adjust_price");
        minus10 = setValue(minus10, "-10");
        menu.setItem(11, minus10);
        
        ItemStack minus1 = createMenuItem(Material.RED_CONCRETE, 
                warningColor + "-1", 
                Arrays.asList(
                    primaryColor + "Reduziere den Preis um 1",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um 1 abzuziehen"
                ));
        minus1 = setMenuAction(minus1, "adjust_price");
        minus1 = setValue(minus1, "-1");
        menu.setItem(12, minus1);
        
        ItemStack plus1 = createMenuItem(Material.LIME_CONCRETE, 
                highlightColor + "+1", 
                Arrays.asList(
                    primaryColor + "Erhöhe den Preis um 1",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um 1 hinzuzufügen"
                ));
        plus1 = setMenuAction(plus1, "adjust_price");
        plus1 = setValue(plus1, "1");
        menu.setItem(14, plus1);
        
        ItemStack plus10 = createMenuItem(Material.LIME_CONCRETE, 
                highlightColor + "+10", 
                Arrays.asList(
                    primaryColor + "Erhöhe den Preis um 10",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um 10 hinzuzufügen"
                ));
        plus10 = setMenuAction(plus10, "adjust_price");
        plus10 = setValue(plus10, "10");
        menu.setItem(15, plus10);
        
        ItemStack plus100 = createMenuItem(Material.LIME_CONCRETE, 
                highlightColor + "+100", 
                Arrays.asList(
                    primaryColor + "Erhöhe den Preis um 100",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um 100 hinzuzufügen"
                ));
        plus100 = setMenuAction(plus100, "adjust_price");
        plus100 = setValue(plus100, "100");
        menu.setItem(16, plus100);
        
        // Add confirm button
        ItemStack confirm = createMenuItem(confirmItem, 
                highlightColor + "Bestätigen", 
                Arrays.asList(
                    primaryColor + "Bestätige den Preis von " + priceColor + priceFormat.format(currentPrice) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName(),
                    primaryColor + "für deine Auktion.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um den Preis zu bestätigen"
                ));
        confirm = setMenuAction(confirm, "confirm_custom_price");
        menu.setItem(21, confirm);
        
        // Add back button
        ItemStack back = createMenuItem(backItem, 
                warningColor + "Zurück", 
                Arrays.asList(
                    primaryColor + "Zurück zur Preisauswahl.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um zurückzugehen"
                ));
        back = setMenuAction(back, "back_to_price");
        menu.setItem(23, back);
        
        // Open the menu
        player.openInventory(menu);
    }
    
    /**
     * Determine the appropriate category for an item
     * @param item The item to categorize
     * @return The category name
     */
    public String determineItemCategory(ItemStack item) {
        Material material = item.getType();
        String materialName = material.name().toLowerCase();
        
        // Weapons
        if (materialName.contains("sword") || materialName.contains("bow") || 
            materialName.contains("arrow") || materialName.contains("trident") || 
            materialName.contains("crossbow")) {
            return "weapons";
        }
        
        // Tools
        if (materialName.contains("pickaxe") || materialName.contains("axe") || 
            materialName.contains("shovel") || materialName.contains("hoe") || 
            materialName.contains("fishing_rod") || materialName.contains("shears")) {
            return "tools";
        }
        
        // Armor
        if (materialName.contains("helmet") || materialName.contains("chestplate") || 
            materialName.contains("leggings") || materialName.contains("boots") || 
            materialName.contains("shield") || materialName.contains("elytra")) {
            return "armor";
        }
        
        // Food
        if (material.isEdible() || materialName.contains("cake") || 
            materialName.contains("cookie") || materialName.contains("beef") || 
            materialName.contains("pork") || materialName.contains("chicken") || 
            materialName.contains("fish") || materialName.contains("apple") || 
            materialName.contains("carrot") || materialName.contains("potato")) {
            return "food";
        }
        
        // Brewing
        if (materialName.contains("potion") || materialName.contains("brewing") || 
            materialName.contains("blaze") || materialName.contains("spider_eye") || 
            materialName.contains("fermented") || materialName.contains("ghast_tear") || 
            materialName.contains("glistering_melon") || materialName.contains("golden_carrot") || 
            materialName.contains("magma_cream") || materialName.contains("nether_wart") || 
            materialName.contains("phantom_membrane") || materialName.contains("rabbit_foot") || 
            materialName.contains("turtle_helmet") || materialName.contains("dragon_breath")) {
            return "brewing";
        }
        
        // Redstone
        if (materialName.contains("redstone") || materialName.contains("comparator") || 
            materialName.contains("repeater") || materialName.contains("observer") || 
            materialName.contains("hopper") || materialName.contains("dropper") || 
            materialName.contains("dispenser") || materialName.contains("piston") || 
            materialName.contains("rail") || materialName.contains("pressure_plate") || 
            materialName.contains("lever") || materialName.contains("button") || 
            materialName.contains("tripwire") || materialName.contains("detector")) {
            return "redstone";
        }
        
        // Decoration
        if (materialName.contains("flower") || materialName.contains("banner") || 
            materialName.contains("carpet") || materialName.contains("bed") || 
            materialName.contains("sign") || materialName.contains("frame") || 
            materialName.contains("head") || materialName.contains("skull") || 
            materialName.contains("painting") || materialName.contains("pot") || 
            materialName.contains("lantern") || materialName.contains("torch") || 
            materialName.contains("candle") || materialName.contains("glass") || 
            materialName.contains("chandelier")) {
            return "decoration";
        }
        
        // Blocks - check if material is a block and not in another category
        if (material.isBlock() && material.isSolid()) {
            return "blocks";
        }
        
        // Default to misc
        return "misc";
    }
    
    /**
     * Handle inventory click events
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        // Check if inventory is an auction house menu
        if (event.getView().getTitle().startsWith(mainMenuTitle) || 
            event.getView().getTitle().startsWith(createAuctionTitle) || 
            event.getView().getTitle().startsWith(playerAuctionsTitle) || 
            event.getView().getTitle().startsWith(categoriesTitle) || 
            event.getView().getTitle().startsWith(searchTitle)) {
            
            // Cancel the event to prevent item movement
            event.setCancelled(true);
            
            // Check if a valid item was clicked
            if (clickedItem == null) {
                return;
            } else if (clickedItem.getType() == Material.AIR ||
                       !clickedItem.hasItemMeta() ||
                       !clickedItem.getItemMeta().hasDisplayName()) {
                return;

            }
            
            // Handle menu actions
            String menuAction = getMenuAction(clickedItem);
            if (menuAction == null) {
                return;
            }
            
            AuctionCommand auctionCommand = (AuctionCommand) plugin.getCommandManager().getCommand("ah");
            
            switch (menuAction) {
                case "create_auction":
                    // Open create auction menu
                    player.closeInventory();
                    player.chat("/ah sell");
                    break;
                case "my_auctions":
                    // Open player auctions menu
                    openPlayerAuctionsMenu(player);
                    break;
                case "categories":
                    // Open categories menu
                    openCategoriesMenu(player);
                    break;
                case "buy_auction":
                    // Buy an auction
                    int auctionId = getAuctionId(clickedItem);
                    if (auctionId != -1) {
                        Auction auction = auctionAPI.getAuction(auctionId);
                        if (auction != null && auction.isActive()) {
                            // Check if player is the seller
                            if (auction.getSellerUuid().equals(player.getUniqueId())) {
                                player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Du kannst deine eigenen Auktionen nicht kaufen.");
                                return;
                            }
                            // Open confirmation menu before purchase
                            openPurchaseConfirmationMenu(player, auction);
                        } else {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Diese Auktion ist nicht mehr verfügbar.");
                            player.closeInventory();
                        }
                    }
                    break;
                case "confirm_purchase":
                    // Confirm purchase of an auction
                    int confirmAuctionId = getAuctionId(clickedItem);
                    if (confirmAuctionId != -1) {
                        Auction auction = auctionAPI.getAuction(confirmAuctionId);
                        if (auction != null && auction.isActive()) {
                            if (auctionAPI.purchaseAuction(auction, player)) {
                                player.closeInventory();
                                player.sendMessage(plugin.getConfigManager().getPrefix() + highlightColor + "Du hast die Auktion erfolgreich gekauft!");
                            } else {
                                player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Du konntest die Auktion nicht kaufen. Überprüfe, ob du genug Geld hast und ob dein Inventar nicht voll ist.");
                            }
                        } else {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Diese Auktion ist nicht mehr verfügbar.");
                            player.closeInventory();
                        }
                    }
                    break;
                case "cancel_purchase":
                    // Go back to the previous menu
                    player.closeInventory();
                    // Reopen the main menu
                    openMainMenu(player);
                    break;
                case "cancel_player_auction":
                    // Cancel a player's auction
                    int cancelAuctionId = getAuctionId(clickedItem);
                    if (cancelAuctionId != -1) {
                        Auction auction = auctionAPI.getAuction(cancelAuctionId);
                        if (auction != null && auction.isActive()) {
                            if (auctionAPI.cancelAuction(auction, player)) {
                                player.closeInventory();
                                player.sendMessage(plugin.getConfigManager().getPrefix() + highlightColor + "Du hast die Auktion erfolgreich abgebrochen!");
                            } else {
                                player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Du konntest die Auktion nicht abbrechen. Überprüfe, ob dein Inventar nicht voll ist.");
                            }
                        } else {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Diese Auktion ist nicht mehr verfügbar.");
                            player.closeInventory();
                        }
                    }
                    break;
                case "claim_expired_auction":
                    // Claim an expired auction
                    int expiredAuctionId = getAuctionId(clickedItem);
                    if (expiredAuctionId != -1) {
                        Auction auction = auctionAPI.getAuction(expiredAuctionId);
                        if (auction != null && auction.getStatus() == Auction.Status.EXPIRED) {
                            if (auctionAPI.claimExpiredAuction(auction, player)) {
                                player.closeInventory();
                                player.sendMessage(plugin.getConfigManager().getPrefix() + highlightColor + "Du hast das Item erfolgreich zurückgefordert!");
                            } else {
                                player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Du konntest das Item nicht zurückfordern. Überprüfe, ob dein Inventar nicht voll ist.");
                            }
                        } else {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Diese Auktion ist nicht mehr verfügbar.");
                            player.closeInventory();
                        }
                    }
                    break;
                case "set_price":
                    // Set the price for an auction
                    String priceStr = getValue(clickedItem);
                    if (priceStr != null) {
                        try {
                            double price = Double.parseDouble(priceStr);
                            auctionCommand.setAuctionPrice(player, price);
                            
                            // Open duration menu with slight delay to prevent GUI issues
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                openDurationMenu(player, auctionCommand.getCreatingAuctionItem(player), price);
                            }, 2L);
                        } catch (NumberFormatException e) {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Ungültiger Preis.");
                        }
                    }
                    break;
                case "custom_price":
                    // Open custom price menu
                    openCustomPriceMenu(player, auctionCommand.getCreatingAuctionItem(player));
                    break;
                case "adjust_price":
                    // Adjust the custom price
                    String adjustmentStr = getValue(clickedItem);
                    if (adjustmentStr != null) {
                        try {
                            double adjustment = Double.parseDouble(adjustmentStr);
                            double currentPrice = auctionCommand.getAuctionPrice(player);
                            double minPrice = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.min-price", 10.0);
                            double maxPrice = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.max-price", 1000000.0);
                            
                            // Calculate new price
                            double newPrice = currentPrice + adjustment;
                            
                            // Ensure price is within limits
                            newPrice = Math.max(minPrice, Math.min(maxPrice, newPrice));
                            
                            // Update price
                            auctionCommand.setAuctionPrice(player, newPrice);
                            
                            // Refresh menu
                            openCustomPriceMenu(player, auctionCommand.getCreatingAuctionItem(player));
                        } catch (NumberFormatException e) {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Ungültige Preisanpassung.");
                        }
                    }
                    break;
                case "confirm_custom_price":
                    // Confirm custom price and proceed to duration selection
                    double customPrice = auctionCommand.getAuctionPrice(player);
                    if (customPrice > 0) {
                        // Open duration menu with slight delay to prevent GUI issues
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            openDurationMenu(player, auctionCommand.getCreatingAuctionItem(player), customPrice);
                        }, 2L);
                    }
                    break;
                case "set_duration":
                    // Set the duration for an auction
                    String durationStr = getValue(clickedItem);
                    if (durationStr != null) {
                        try {
                            int duration = Integer.parseInt(durationStr);
                            auctionCommand.setAuctionDuration(player, duration);
                            
                            // Auto-detect category
                            String autoCategory = determineItemCategory(auctionCommand.getCreatingAuctionItem(player));
                            auctionCommand.setAuctionCategory(player, autoCategory);
                            
                            // Open category selection menu first instead of directly to confirm
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                try {
                                    openCategoryMenu(player, auctionCommand.getCreatingAuctionItem(player),
                                            auctionCommand.getAuctionPrice(player), duration);
                                } catch (Exception e) {
                                    plugin.getLogger().severe("Error opening category menu: " + e.getMessage());
                                    e.printStackTrace();
                                    player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + 
                                            "Fehler beim Öffnen des Kategoriemenüs. Bitte versuche es erneut.");
                                    player.closeInventory();
                                }
                            }, 2L);
                        } catch (NumberFormatException e) {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Ungültige Dauer.");
                        }
                    }
                    break;
                case "set_category":
                    // Set the category for an auction
                    String category = getValue(clickedItem);
                    if (category != null) {
                        auctionCommand.setAuctionCategory(player, category.equals("null") ? null : category);
                        
                        // Open confirm menu with slight delay to prevent GUI issues
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            openConfirmMenu(player, auctionCommand.getCreatingAuctionItem(player), 
                                    auctionCommand.getAuctionPrice(player), 
                                    auctionCommand.getAuctionDuration(player), 
                                    auctionCommand.getAuctionCategory(player));
                        }, 2L);
                    }
                    break;
                case "confirm_auction":
                    // Confirm auction creation with slight delay to prevent issues
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        auctionCommand.completeAuctionCreation(player);
                        player.closeInventory();
                    }, 2L);
                    break;
                case "cancel_auction":
                    // Cancel auction creation
                    auctionCommand.cancelAuctionCreation(player);
                    player.closeInventory();
                    break;
                case "back_to_price":
                    // Go back to price selection
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        openCreateAuctionMenu(player, auctionCommand.getCreatingAuctionItem(player));
                    }, 2L);
                    break;
                case "back_to_duration":
                    // Go back to duration selection
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            openDurationMenu(player, auctionCommand.getCreatingAuctionItem(player), 
                                    auctionCommand.getAuctionPrice(player));
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error going back to duration menu: " + e.getMessage());
                            e.printStackTrace();
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + 
                                    "Fehler beim Öffnen des Dauermenüs. Bitte versuche es erneut.");
                            player.closeInventory();
                        }
                    }, 2L);
                    break;
                case "back_to_category":
                    // Go back to category selection
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            openCategoryMenu(player, auctionCommand.getCreatingAuctionItem(player),
                                    auctionCommand.getAuctionPrice(player),
                                    auctionCommand.getAuctionDuration(player));
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error going back to category menu: " + e.getMessage());
                            e.printStackTrace();
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + 
                                    "Fehler beim Öffnen des Kategoriemenüs. Bitte versuche es erneut.");
                            player.closeInventory();
                        }
                    }, 2L);
                    break;
                case "back_to_main":
                    // Go back to main menu
                    auctionCommand.clearViewingData(player);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        openMainMenu(player);
                    }, 2L);
                    break;
                case "back_to_categories":
                    // Go back to categories menu
                    auctionCommand.clearViewingData(player);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        openCategoriesMenu(player);
                    }, 2L);
                    break;
                case "view_category":
                    // View a category
                    String viewCategory = getValue(clickedItem);
                    if (viewCategory != null) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            openCategoryMenu(player, viewCategory);
                        }, 2L);
                    }
                    break;
                case "next_page":
                    // Go to next page
                    String nextPageStr = getValue(clickedItem);
                    if (nextPageStr != null) {
                        try {
                            int nextPage = Integer.parseInt(nextPageStr);
                            String currentCategory = auctionCommand.getViewingCategory(player);
                            
                            if (currentCategory != null) {
                                auctionCommand.setViewingPage(player, nextPage);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    openCategoryMenuPage(player, currentCategory, 
                                            auctionAPI.getAuctionsByCategory(currentCategory), nextPage);
                                }, 2L);
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Ungültige Seite.");
                        }
                    }
                    break;
                case "prev_page":
                    // Go to previous page
                    String prevPageStr = getValue(clickedItem);
                    if (prevPageStr != null) {
                        try {
                            int prevPage = Integer.parseInt(prevPageStr);
                            String currentCategory = auctionCommand.getViewingCategory(player);
                            
                            if (currentCategory != null) {
                                auctionCommand.setViewingPage(player, prevPage);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    openCategoryMenuPage(player, currentCategory, 
                                            auctionAPI.getAuctionsByCategory(currentCategory), prevPage);
                                }, 2L);
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(plugin.getConfigManager().getPrefix() + warningColor + "Ungültige Seite.");
                        }
                    }
                    break;
            }
        }
    }
    
    /**
     * Open purchase confirmation menu
     * @param player The player
     * @param auction The auction to purchase
     */
    public void openPurchaseConfirmationMenu(Player player, Auction auction) {
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 3 * 9, mainMenuTitle + " - Kaufbestätigung");
        
        // Add border items
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        for (int i = 18; i < 27; i++) {
            menu.setItem(i, createMenuItem(borderItem, " ", null));
        }
        menu.setItem(9, createMenuItem(borderItem, " ", null));
        menu.setItem(17, createMenuItem(borderItem, " ", null));
        
        // Create a display item with auction details
        ItemStack auctionItem = auction.getItem().clone();
        ItemMeta meta = auctionItem.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        // Add auction info to lore
        if (!lore.isEmpty()) {
            lore.add("");
        }
        
        lore.add(priceColor + "Preis: " + priceFormat.format(auction.getPrice()) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName());
        lore.add(sellerColor + "Verkäufer: " + auction.getSellerName());
        lore.add(timeColor + "Verbleibend: " + auction.getFormattedRemainingTime());
        lore.add("");
        lore.add(highlightColor + "Auktions-ID: " + auction.getId());
        
        meta.setLore(lore);
        auctionItem.setItemMeta(meta);
        
        // Add auction ID to item
        auctionItem = setAuctionId(auctionItem, auction.getId());
        
        // Add item to menu
        menu.setItem(13, auctionItem);
        
        // Add confirm button
        ItemStack confirm = createMenuItem(confirmItem, 
                highlightColor + "Kaufen", 
                Arrays.asList(
                    primaryColor + "Kaufe dieses Item für " + priceColor + priceFormat.format(auction.getPrice()) + " " + plugin.getAPI().getEconomyAPI().getCurrencyName() + primaryColor + ".",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um den Kauf zu bestätigen"
                ));
        confirm = setMenuAction(confirm, "confirm_purchase");
        confirm = setAuctionId(confirm, auction.getId());
        menu.setItem(11, confirm);
        
        // Add cancel button
        ItemStack cancel = createMenuItem(cancelItem, 
                warningColor + "Abbrechen", 
                Arrays.asList(
                    primaryColor + "Bricht den Kauf ab.",
                    "",
                    secondaryColor + "» " + primaryColor + "Klicke, um den Kauf abzubrechen"
                ));
        cancel = setMenuAction(cancel, "cancel_purchase");
        menu.setItem(15, cancel);
        
        // Open the menu
        player.openInventory(menu);
    }

    /**
     * Handle inventory close events
     * @param event The inventory close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Check if player was creating an auction
        AuctionCommand auctionCommand = (AuctionCommand) plugin.getCommandManager().getCommand("ah");
        if (auctionCommand.isCreatingAuction(player)) {
            // Check if the closed inventory was an auction creation menu
            if (event.getView().getTitle().startsWith(createAuctionTitle)) {
                // Check if player has all required data
                if (auctionCommand.getAuctionPrice(player) <= 0 || 
                    auctionCommand.getAuctionDuration(player) <= 0) {
                    // Player didn't complete the auction creation process
                    auctionCommand.cancelAuctionCreation(player);
                }
            }
        }
    }
}