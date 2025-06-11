package ch.retaxo.sumania.api.teleport;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API for teleportation-related operations
 */
public class TeleportAPI {

    private final Sumania plugin;
    private final Map<UUID, TeleportRequest> teleportRequests = new HashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public TeleportAPI(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Teleport a player to a location with a delay
     * @param player The player to teleport
     * @param location The location to teleport to
     * @return True if the teleport was initiated successfully
     */
    public boolean teleport(Player player, Location location) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if teleportation is enabled
        if (!config.getBoolean("teleportation.enabled", true)) {
            return false;
        }
        
        // Check if player is in cooldown
        if (isInCooldown(player)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(getCooldownTimeLeft(player)));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleport-cooldown",
                    replacements
            );
            
            return false;
        }
        
        // Get teleport delay - for RTP we'll override this to be instant
        int delay = 0;
        // Only use configured delay for non-RTP teleports (check the stack trace)
        boolean isRandomTeleport = Thread.currentThread().getStackTrace()[2].getClassName().contains("RandomTeleport");
        if (!isRandomTeleport) {
            delay = config.getInt("teleportation.delay", 5);
        }
        
        // Store player's last location for movement check
        lastLocations.put(player.getUniqueId(), player.getLocation());
        
        // Send teleporting message if delay > 0 and it's not a random teleport
        if (delay > 0 && !isRandomTeleport) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(delay));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleporting",
                    replacements
            );
        }
        
        // Cancel any existing teleport task
        if (teleportTasks.containsKey(player.getUniqueId())) {
            teleportTasks.get(player.getUniqueId()).cancel();
        }
        
        // Create teleport task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(location);
                
                // Send success message only for non-RTP teleports
                if (!isRandomTeleport) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "teleport.teleport-success"
                    );
                }
                
                // Set cooldown
                setCooldown(player);
                
                // Remove task and last location
                teleportTasks.remove(player.getUniqueId());
                lastLocations.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, delay * 20L);
        
        // Store task
        teleportTasks.put(player.getUniqueId(), task);
        
        return true;
    }

    
    /**
     * Set a teleport cooldown for a player
     * @param player The player
     */
    private void setCooldown(Player player) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        int cooldown = config.getInt("teleportation.cooldown", 60);
        
        if (cooldown > 0) {
            teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        }
    }
    
    /**
     * Check if a player is in teleport cooldown
     * @param player The player
     * @return True if the player is in cooldown
     */
    private boolean isInCooldown(Player player) {
        if (!teleportCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        
        return System.currentTimeMillis() < teleportCooldowns.get(player.getUniqueId());
    }
    
    /**
     * Get the time left on a player's cooldown
     * @param player The player
     * @return The time left in seconds
     */
    private int getCooldownTimeLeft(Player player) {
        if (!teleportCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        
        long timeLeft = teleportCooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
        return Math.max(0, (int) (timeLeft / 1000));
    }
    
    /**
     * Create a teleport request from one player to another
     * @param from The player requesting the teleport
     * @param to The player to teleport to
     * @param type The type of teleport request
     * @return True if the request was created successfully
     */
    public boolean createTeleportRequest(Player from, Player to, TeleportRequestType type) {
        // Create teleport request
        TeleportRequest request = new TeleportRequest(from.getUniqueId(), to.getUniqueId(), type);
        teleportRequests.put(to.getUniqueId(), request);
        
        // Send request messages
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", to.getName());
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                from,
                "teleport.teleport-request-sent",
                replacements
        );
        
        replacements = new HashMap<>();
        replacements.put("player", from.getName());
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                to,
                "teleport.teleport-request-received",
                replacements
        );
        
        // Expire request after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (teleportRequests.containsKey(to.getUniqueId()) &&
                    teleportRequests.get(to.getUniqueId()).equals(request)) {
                teleportRequests.remove(to.getUniqueId());
            }
        }, 1200L);
        
        return true;
    }
    
    /**
     * Accept a teleport request
     * @param player The player accepting the request
     * @return True if the request was accepted successfully
     */
    public boolean acceptTeleportRequest(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (!teleportRequests.containsKey(playerUUID)) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleport-no-requests"
            );
            
            return false;
        }
        
        TeleportRequest request = teleportRequests.get(playerUUID);
        Player requester = Bukkit.getPlayer(request.getFromUUID());
        
        if (requester == null || !requester.isOnline()) {
            teleportRequests.remove(playerUUID);
            return false;
        }
        
        // Send acceptance message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", player.getName());
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                requester,
                "teleport.teleport-request-accepted",
                replacements
        );
        
        // Perform teleport based on request type
        if (request.getType() == TeleportRequestType.TO_PLAYER) {
            teleport(requester, player.getLocation());
        } else if (request.getType() == TeleportRequestType.FROM_PLAYER) {
            teleport(player, requester.getLocation());
        }
        
        // Remove request
        teleportRequests.remove(playerUUID);
        
        return true;
    }
    
    /**
     * Deny a teleport request
     * @param player The player denying the request
     * @return True if the request was denied successfully
     */
    public boolean denyTeleportRequest(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (!teleportRequests.containsKey(playerUUID)) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleport-no-requests"
            );
            
            return false;
        }
        
        TeleportRequest request = teleportRequests.get(playerUUID);
        Player requester = Bukkit.getPlayer(request.getFromUUID());
        
        if (requester != null && requester.isOnline()) {
            // Send denial message
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", player.getName());
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    requester,
                    "teleport.teleport-request-denied",
                    replacements
            );
        }
        
        // Remove request
        teleportRequests.remove(playerUUID);
        
        return true;
    }
    
    /**
     * Set a warp location
     * @param name The name of the warp
     * @param location The location of the warp
     */
    public void setWarp(String name, Location location) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        
        // Format location as string
        String locationStr = String.format(
                "%s,%f,%f,%f,%f,%f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
        
        // Set the warp
        data.set("warps." + name, locationStr);
        
        // Save the data file
        plugin.getConfigManager().saveConfig("data.yml");
    }
    
    /**
     * Get a warp location
     * @param name The name of the warp
     * @return The warp location, or null if not found
     */
    public Location getWarp(String name) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String locationStr = data.getString("warps." + name);
        
        if (locationStr != null) {
            String[] parts = locationStr.split(",");
            
            if (parts.length == 6) {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                float yaw = Float.parseFloat(parts[4]);
                float pitch = Float.parseFloat(parts[5]);
                
                return new Location(
                        Bukkit.getWorld(worldName),
                        x, y, z, yaw, pitch
                );
            }
        }
        
        return null;
    }
    
    /**
     * Delete a warp
     * @param name The name of the warp
     * @return True if the warp was deleted successfully
     */
    public boolean deleteWarp(String name) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        
        if (data.contains("warps." + name)) {
            data.set("warps." + name, null);
            plugin.getConfigManager().saveConfig("data.yml");
            return true;
        }
        
        return false;
    }
    
    /**
     * Get all warps
     * @return A map of warp names to locations
     */
    public Map<String, Location> getWarps() {
        Map<String, Location> warps = new HashMap<>();
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        
        if (data.contains("warps")) {
            for (String name : data.getConfigurationSection("warps").getKeys(false)) {
                Location location = getWarp(name);
                
                if (location != null) {
                    warps.put(name, location);
                }
            }
        }
        
        return warps;
    }
    
    /**
     * Open the warp menu for a player
     * @param player The player to open the menu for
     */
    public void openWarpMenu(Player player) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        
        // Check if warp menu is enabled
        if (!config.getBoolean("teleportation.warp-menu-enabled", true)) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.feature-disabled",
                    null
            );
            return;
        }
        
        // Get all warps
        Map<String, Location> warps = getWarps();
        
        if (warps.isEmpty()) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "warp.warp-list",
                    Map.of("warps", "Keine")
            );
            return;
        }
        
        // Get menu title
        String menuTitle = messages.getString("warp.warp-menu-title", "&8[&6Warp-Menü&8]");
        menuTitle = menuTitle.replace("&", "§");
        
        // Get menu rows (minimum 1, maximum 6)
        int menuRows = Math.min(6, Math.max(1, config.getInt("teleportation.warp-menu-rows", 3)));
        
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, menuRows * 9, menuTitle);
        
        // Initialize menu with glass panes as decoration
        ItemStack glassPane = createDecorativeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, glassPane);
        }
        
        // Filter warps the player can access
        List<Map.Entry<String, Location>> accessibleWarps = new ArrayList<>();
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            String warpName = entry.getKey();
            if (player.hasPermission("sumania.warp.use." + warpName.toLowerCase()) ||
                player.hasPermission("sumania.warp.use.*")) {
                accessibleWarps.add(entry);
            }
        }
        
        // Calculate grid layout
        int warpCount = accessibleWarps.size();
        int rows = Math.min(menuRows - 2, (int) Math.ceil(warpCount / 7.0));
        int startRow = (menuRows - rows) / 2;
        
        // Add warp items in a grid layout
        for (int i = 0; i < accessibleWarps.size(); i++) {
            Map.Entry<String, Location> entry = accessibleWarps.get(i);
            String warpName = entry.getKey();
            Location warpLocation = entry.getValue();
            
            // Calculate position in grid
            int row = i / 7;
            int col = i % 7 + 1; // +1 to center in the row
            
            // Create item for warp
            ItemStack item = createWarpItem(warpName, warpLocation);
            
            // Add to inventory
            int slot = (startRow + row) * 9 + col;
            if (slot < menu.getSize()) {
                menu.setItem(slot, item);
            } else {
                break;
            }
        }
        
        // Open inventory for player
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
     * Create an item for the warp menu
     * @param warpName The name of the warp
     * @param location The location of the warp
     * @return The item
     */
    private ItemStack createWarpItem(String warpName, Location location) {
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        
        // Determine item material based on location environment and warp name
        Material material;
        
        // Check warp name for specific keywords
        String lowerName = warpName.toLowerCase();
        if (lowerName.contains("spawn")) {
            material = Material.BEACON;
        } else if (lowerName.contains("shop") || lowerName.contains("markt") || lowerName.contains("laden")) {
            material = Material.EMERALD;
        } else if (lowerName.contains("mine") || lowerName.contains("bergwerk")) {
            material = Material.DIAMOND_PICKAXE;
        } else if (lowerName.contains("farm")) {
            material = Material.WHEAT;
        } else if (lowerName.contains("arena") || lowerName.contains("pvp")) {
            material = Material.IRON_SWORD;
        } else if (lowerName.contains("wald") || lowerName.contains("forest")) {
            material = Material.OAK_LOG;
        } else if (lowerName.contains("desert") || lowerName.contains("wüste")) {
            material = Material.SAND;
        } else if (lowerName.contains("end")) {
            material = Material.END_PORTAL_FRAME;
        } else if (lowerName.contains("nether")) {
            material = Material.NETHERRACK;
        } else {
            // Default based on world type
            if (location.getWorld().getEnvironment() == World.Environment.NETHER) {
                material = Material.NETHERRACK;
            } else if (location.getWorld().getEnvironment() == World.Environment.THE_END) {
                material = Material.END_STONE;
            } else {
                material = Material.GRASS_BLOCK;
            }
        }
        
        // Create item
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        String displayName = messages.getString("warp.warp-menu-item", "&a%warp%");
        displayName = displayName.replace("&", "§").replace("%warp%", warpName);
        meta.setDisplayName(displayName);
        
        // Set lore
        String tooltip = messages.getString("warp.warp-menu-tooltip", "&7Klicke zum Teleportieren zu &6%warp%");
        tooltip = tooltip.replace("&", "§").replace("%warp%", warpName);
        
        // Add location info
        List<String> lore = new ArrayList<>();
        lore.add(tooltip);
        lore.add("§7Welt: §e" + location.getWorld().getName());
        lore.add(String.format("§7Position: §e%d, %d, %d", 
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        
        // Add a hint about permissions
        lore.add("");
        lore.add("§7Permission: §esumania.warp.use." + warpName.toLowerCase());
        
        meta.setLore(lore);
        
        // Store warp name in item NBT
        NamespacedKey key = new NamespacedKey(plugin, "warp-name");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, warpName);
        
        // Apply meta to item
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Handle inventory click in warp menu
     * @param event The inventory click event
     */
    public void handleWarpMenuClick(InventoryClickEvent event) {
        // Check if click is in warp menu
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        String menuTitle = messages.getString("warp.warp-menu-title", "&8[&6Warp-Menü&8]");
        menuTitle = menuTitle.replace("&", "§");
        
        if (event.getView().getTitle().equals(menuTitle)) {
            event.setCancelled(true);
            
            // Check if clicked on an item
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta()) {
                ItemMeta meta = clickedItem.getItemMeta();
                
                // Check if it's a warp item
                NamespacedKey key = new NamespacedKey(plugin, "warp-name");
                if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    String warpName = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                    
                    // Get player
                    Player player = (Player) event.getWhoClicked();
                    
                    // Close inventory
                    player.closeInventory();
                    
                    // Get warp location
                    Location warpLocation = getWarp(warpName);
                    
                    if (warpLocation != null) {
                        // Play a sound for teleport
                        player.playSound(player.getLocation(), "entity.enderman.teleport", 0.7f, 1.0f);
                        
                        // Send teleport message
                        Map<String, String> replacements = new HashMap<>();
                        replacements.put("warp", warpName);
                        
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "warp.warp-teleport",
                                replacements
                        );
                        
                        // Teleport player
                        teleport(player, warpLocation);
                    }
                }
                // Ignore clicks on decorative items
            }
        }
    }
}
