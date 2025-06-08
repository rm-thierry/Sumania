package ch.retaxo.sumania.commands.shop;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to manage the shop system
 */
public class ShopCommand implements CommandExecutor, Listener {

    private final Sumania plugin;
    private final NamespacedKey categoryKey;
    private final NamespacedKey itemKey;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public ShopCommand(Sumania plugin) {
        this.plugin = plugin;
        this.categoryKey = new NamespacedKey(plugin, "shop-category");
        this.itemKey = new NamespacedKey(plugin, "shop-item");
        
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if shop is enabled
        if (!config.getBoolean("shop.enabled", true)) {
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.feature-disabled",
                        null
                );
            } else {
                sender.sendMessage("§cDer Shop ist deaktiviert.");
            }
            
            return true;
        }
        
        // Check if command is run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("sumania.shop")) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.no-permission",
                    null
            );
            
            return true;
        }
        
        if (args.length == 0) {
            // Open main shop menu
            openMainShopMenu(player);
            return true;
        } else {
            // Open category menu
            String categoryId = args[0].toLowerCase();
            openCategoryMenu(player, categoryId);
            return true;
        }
    }
    
    /**
     * Open the main shop menu
     * @param player The player to open the menu for
     */
    private void openMainShopMenu(Player player) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        
        // Get categories
        ConfigurationSection categoriesSection = config.getConfigurationSection("shop.categories");
        
        if (categoriesSection == null) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.error",
                    null
            );
            
            return;
        }
        
        // Fixed size for better design
        int rows = 6;
        
        // Create inventory
        String menuTitle = messages.getString("shop.shop-menu-title", "&8[&6Shop&8]");
        menuTitle = menuTitle.replace("&", "§");
        
        Inventory menu = Bukkit.createInventory(null, rows * 9, menuTitle);
        
        // Initialize menu with glass panes as decoration
        ItemStack glassPane = createDecorativeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, glassPane);
        }
        
        // Add a banner for header
        ItemStack banner = createDecorativeItem(Material.WHITE_BANNER, "§6§lAdmin Shop");
        List<String> bannerLore = new ArrayList<>();
        bannerLore.add("§7Willkommen im Admin Shop!");
        bannerLore.add("§7Hier kannst du Items kaufen und verkaufen.");
        bannerLore.add("");
        bannerLore.add("§a§lLinksklick§7: Kaufen");
        bannerLore.add("§c§lRechtsklick§7: Verkaufen");
        bannerLore.add("");
        bannerLore.add("§7Guthaben: §e" + plugin.getAPI().getEconomyAPI().format(plugin.getAPI().getEconomyAPI().getBalance(player)));
        
        ItemMeta bannerMeta = banner.getItemMeta();
        bannerMeta.setLore(bannerLore);
        banner.setItemMeta(bannerMeta);
        
        // Add header banner
        menu.setItem(4, banner);
        
        // Get category list
        List<String> categoryIds = new ArrayList<>(categoriesSection.getKeys(false));
        
        // Calculate layout
        int totalCategories = categoryIds.size();
        int middle = Math.min(7, totalCategories);
        int startSlot = (9 - middle) / 2 + 9; // Center in second row
        
        // Add category items in a grid layout
        for (int i = 0; i < totalCategories; i++) {
            String categoryId = categoryIds.get(i);
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
            
            if (categorySection != null) {
                // Create category item
                ItemStack item = createCategoryItem(categoryId, categorySection);
                
                // Calculate position
                int row = (i / 7) + 2; // Start from row 2
                int col = (i % 7) + 1; // Center in the row
                
                int slot = row * 9 + col;
                if (slot < menu.getSize()) {
                    menu.setItem(slot, item);
                }
            }
        }
        
        // Open inventory
        player.openInventory(menu);
    }
    
    /**
     * Create a decorative item for the menu
     * @param material The material to use
     * @param name The display name
     * @return The created item
     */
    private ItemStack createDecorativeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create a category item
     * @param categoryId The category ID
     * @param categorySection The category configuration section
     * @return The category item
     */
    private ItemStack createCategoryItem(String categoryId, ConfigurationSection categorySection) {
        // Get category icon
        String iconName = categorySection.getString("icon", "GRASS_BLOCK");
        Material material = Material.valueOf(iconName);
        
        // Create item
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        String displayName = categorySection.getString("display-name", "§a" + categoryId);
        meta.setDisplayName(displayName);
        
        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add("§7Klicke, um diese Kategorie zu öffnen");
        
        // Count items in category
        ConfigurationSection itemsSection = categorySection.getConfigurationSection("items");
        int itemCount = 0;
        
        if (itemsSection != null) {
            itemCount = itemsSection.getKeys(false).size();
        }
        
        lore.add("§7Enthält §e" + itemCount + " §7Items");
        
        meta.setLore(lore);
        
        // Store category ID in item NBT
        meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
        
        // Apply meta to item
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Open a category menu
     * @param player The player to open the menu for
     * @param categoryId The category ID
     */
    private void openCategoryMenu(Player player, String categoryId) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        
        // Get category
        ConfigurationSection categorySection = config.getConfigurationSection("shop.categories." + categoryId);
        
        if (categorySection == null) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.not-found",
                    Map.of("item", categoryId)
            );
            
            return;
        }
        
        // Get items
        List<Map<?, ?>> items = categorySection.getMapList("items");
        
        if (items.isEmpty()) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.empty",
                    Map.of("item", categoryId)
            );
            
            return;
        }
        
        // Fixed size for better design
        int rows = 6;
        
        // Create inventory
        String categoryName = categorySection.getString("display-name", "§a" + categoryId);
        String menuTitle = messages.getString("shop.category-menu-title", "&8[&6Kategorie: %category%&8]");
        menuTitle = menuTitle.replace("&", "§").replace("%category%", categoryName.replace("§", ""));
        
        Inventory menu = Bukkit.createInventory(null, rows * 9, menuTitle);
        
        // Initialize menu with glass panes as decoration
        ItemStack glassPane = createDecorativeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, glassPane);
        }
        
        // Add a header with instructions
        ItemStack infoItem = createDecorativeItem(Material.BOOK, "§6§lShop: " + categoryName);
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Klicke auf ein Item, um es zu kaufen oder zu verkaufen.");
        infoLore.add("");
        infoLore.add("§a§lLinksklick§7: Kaufen");
        infoLore.add("§c§lRechtsklick§7: Verkaufen");
        infoLore.add("");
        infoLore.add("§7Guthaben: §e" + plugin.getAPI().getEconomyAPI().format(plugin.getAPI().getEconomyAPI().getBalance(player)));
        
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        
        menu.setItem(4, infoItem);
        
        // Add back button
        ItemStack backButton = createDecorativeItem(Material.ARROW, "§c§lZurück zum Hauptmenü");
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setLore(List.of("§7Klicke, um zurück zum Hauptmenü zu gelangen"));
        backMeta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, "back");
        backButton.setItemMeta(backMeta);
        menu.setItem(0, backButton);
        
        // Calculate grid layout for items
        int totalItems = items.size();
        int availableSlots = rows * 9 - 9; // Reserve top row for navigation
        int effectiveRows = rows - 1;
        
        // Add shop items in a grid layout
        for (int i = 0; i < Math.min(totalItems, availableSlots); i++) {
            Map<?, ?> itemMap = items.get(i);
            
            // Get item data
            String itemName = (String) itemMap.get("item");
            double buyPrice = ((Number) itemMap.get("buy-price")).doubleValue();
            double sellPrice = ((Number) itemMap.get("sell-price")).doubleValue();
            
            // Create shop item
            ItemStack item = createShopItem(itemName, buyPrice, sellPrice);
            
            // Calculate position
            int row = (i / 7) + 1; // Start from row 1 (after header)
            int col = (i % 7) + 1; // Center in the row
            
            int slot = row * 9 + col;
            if (slot < menu.getSize()) {
                menu.setItem(slot, item);
            }
        }
        
        // Open inventory
        player.openInventory(menu);
    }
    
    /**
     * Create a shop item
     * @param itemName The item name
     * @param buyPrice The buy price
     * @param sellPrice The sell price
     * @return The shop item
     */
    private ItemStack createShopItem(String itemName, double buyPrice, double sellPrice) {
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        String currencySymbol = config.getString("economy.currency-symbol", "$");
        String currencyName = config.getString("economy.currency-name", "Coins");
        
        // Create item
        Material material = Material.valueOf(itemName);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name - make it nicer with proper casing
        String displayName = material.name().replace("_", " ");
        displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1).toLowerCase();
        meta.setDisplayName("§a" + displayName);
        
        // Set lore
        List<String> lore = new ArrayList<>();
        
        // Add divider
        lore.add("§8§m---------------------------");
        
        // Add buy price with color based on affordability
        String buyPriceFormat = "§a▶ §7Kaufen: §e%price% %currency%";
        String buyPriceText = buyPriceFormat
                .replace("%price%", String.format("%.0f", buyPrice))
                .replace("%currency%", currencySymbol);
        lore.add(buyPriceText);
        
        // Add sell price with highlight
        String sellPriceFormat = "§c◀ §7Verkaufen: §e%price% %currency%";
        String sellPriceText = sellPriceFormat
                .replace("%price%", String.format("%.0f", sellPrice))
                .replace("%currency%", currencySymbol);
        lore.add(sellPriceText);
        
        // Add divider
        lore.add("§8§m---------------------------");
        
        // Add click instructions with colors
        lore.add("§a▶ §fLinksklick §7zum Kaufen");
        lore.add("§c◀ §fRechtsklick §7zum Verkaufen");
        
        // Add price difference info if buy/sell has a difference
        if (buyPrice > sellPrice) {
            double priceDiff = buyPrice - sellPrice;
            double percentage = (priceDiff / buyPrice) * 100;
            lore.add("");
            lore.add(String.format("§7Händlergebühr: §c%.0f %s §7(%.0f%%)", 
                    priceDiff, currencySymbol, percentage));
        }
        
        meta.setLore(lore);
        
        // Store item data in item NBT
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, 
                material.name() + ":" + buyPrice + ":" + sellPrice);
        
        // Apply meta to item
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Handle inventory click event
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        
        // Check if click is in shop menu
        String shopMenuTitle = messages.getString("shop.shop-menu-title", "&8[&6Shop&8]")
                .replace("&", "§");
        
        String categoryMenuTitleFormat = messages.getString("shop.category-menu-title", "&8[&6Kategorie: %category%&8]")
                .replace("&", "§").replace("%category%", "");
        
        if (event.getView().getTitle().equals(shopMenuTitle) || 
                event.getView().getTitle().startsWith(categoryMenuTitleFormat.replace("%category%", ""))) {
            event.setCancelled(true);
            
            // Check if clicked on an item
            if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                Player player = (Player) event.getWhoClicked();
                
                // Check if clicked on a category item
                if (meta.getPersistentDataContainer().has(categoryKey, PersistentDataType.STRING)) {
                    String categoryId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                    
                    if (categoryId != null) {
                        // Check if it's the back button
                        if (categoryId.equals("back")) {
                            // Close current inventory
                            player.closeInventory();
                            
                            // Open main menu
                            openMainShopMenu(player);
                        } else {
                            // Close current inventory
                            player.closeInventory();
                            
                            // Open category menu
                            openCategoryMenu(player, categoryId);
                        }
                    }
                    
                    return;
                }
                
                // Check if clicked on a shop item
                if (meta.getPersistentDataContainer().has(itemKey, PersistentDataType.STRING)) {
                    String itemData = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
                    
                    if (itemData != null) {
                        // Parse item data
                        String[] parts = itemData.split(":");
                        String itemName = parts[0];
                        double buyPrice = Double.parseDouble(parts[1]);
                        double sellPrice = Double.parseDouble(parts[2]);
                        
                        // Handle click
                        if (event.getClick() == ClickType.LEFT) {
                            // Buy item
                            buyItem(player, itemName, buyPrice);
                            
                            // Update balance display in menu after purchase
                            updateBalanceDisplay(player, event.getInventory());
                        } else if (event.getClick() == ClickType.RIGHT) {
                            // Sell item
                            sellItem(player, itemName, sellPrice);
                            
                            // Update balance display in menu after sale
                            updateBalanceDisplay(player, event.getInventory());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Update the balance display in a menu
     * @param player The player
     * @param inventory The inventory to update
     */
    private void updateBalanceDisplay(Player player, Inventory inventory) {
        ItemStack infoItem = inventory.getItem(4);
        
        if (infoItem != null && infoItem.hasItemMeta() && infoItem.getItemMeta().hasLore()) {
            ItemMeta meta = infoItem.getItemMeta();
            List<String> lore = meta.getLore();
            
            // Update balance line (last line)
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).startsWith("§7Guthaben:")) {
                    lore.set(i, "§7Guthaben: §e" + plugin.getAPI().getEconomyAPI().format(plugin.getAPI().getEconomyAPI().getBalance(player)));
                    break;
                }
            }
            
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
        }
    }
    
    /**
     * Buy an item
     * @param player The player buying the item
     * @param itemName The item name
     * @param price The item price
     */
    private void buyItem(Player player, String itemName, double price) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        String currencySymbol = config.getString("economy.currency-symbol", "$");
        
        // Check if player has enough money
        if (plugin.getAPI().getEconomyAPI().getBalance(player) < price) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "shop.not-enough-money",
                    null
            );
            
            return;
        }
        
        // Create item
        Material material = Material.valueOf(itemName);
        ItemStack item = new ItemStack(material);
        
        // Add item to player's inventory
        Map<Integer, ItemStack> notAdded = player.getInventory().addItem(item);
        
        if (!notAdded.isEmpty()) {
            // Drop items that couldn't be added
            for (ItemStack notAddedItem : notAdded.values()) {
                player.getWorld().dropItem(player.getLocation(), notAddedItem);
            }
        }
        
        // Withdraw money
        plugin.getAPI().getEconomyAPI().withdraw(player, price);
        
        // Send confirmation
        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", "1");
        replacements.put("item", material.name().replace("_", " "));
        replacements.put("price", String.format("%.0f", price));
        replacements.put("currency", currencySymbol);
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "shop.item-bought",
                replacements
        );
    }
    
    /**
     * Sell an item
     * @param player The player selling the item
     * @param itemName The item name
     * @param price The item price
     */
    private void sellItem(Player player, String itemName, double price) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        String currencySymbol = config.getString("economy.currency-symbol", "$");
        
        // Create item
        Material material = Material.valueOf(itemName);
        ItemStack item = new ItemStack(material);
        
        // Check if player has the item
        if (!player.getInventory().containsAtLeast(item, 1)) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "shop.not-enough-items",
                    null
            );
            
            return;
        }
        
        // Remove item from player's inventory
        player.getInventory().removeItem(item);
        
        // Deposit money
        plugin.getAPI().getEconomyAPI().deposit(player, price);
        
        // Send confirmation
        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", "1");
        replacements.put("item", material.name().replace("_", " "));
        replacements.put("price", String.format("%.0f", price));
        replacements.put("currency", currencySymbol);
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "shop.item-sold",
                replacements
        );
    }
}