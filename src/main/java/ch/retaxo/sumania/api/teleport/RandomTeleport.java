package ch.retaxo.sumania.api.teleport;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Random teleportation utility for SMP worlds
 */
public class RandomTeleport {

    private final Sumania plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> randomTpCooldowns = new HashMap<>();
    
    // Safe blocks to spawn on
    private final Set<Material> SAFE_BLOCKS = new HashSet<>(Arrays.asList(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.SAND,
            Material.RED_SAND,
            Material.STONE,
            Material.TERRACOTTA,
            Material.SNOW_BLOCK,
            Material.PODZOL,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.MOSS_BLOCK,
            Material.GRAVEL
    ));
    
    // Unsafe blocks to avoid
    private final Set<Material> UNSAFE_BLOCKS = new HashSet<>(Arrays.asList(
            Material.WATER,
            Material.LAVA,
            Material.CACTUS,
            Material.FIRE,
            Material.MAGMA_BLOCK,
            Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW,
            Material.POINTED_DRIPSTONE,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT
    ));

    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public RandomTeleport(Sumania plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the SMP world
     * @return The SMP world
     */
    public World getSMPWorld() {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        String worldName = config.getString("smp.world-name");
        
        if (worldName == null || worldName.isEmpty()) {
            return Bukkit.getWorlds().get(0); // Default to main world
        }
        
        World world = Bukkit.getWorld(worldName);
        return world != null ? world : Bukkit.getWorlds().get(0);
    }
    
    /**
     * Set the SMP world
     * @param worldName The world name
     * @return True if the world was set successfully
     */
    public boolean setSMPWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            return false;
        }
        
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        config.set("smp.world-name", worldName);
        plugin.getConfigManager().saveConfig("config.yml");
        
        return true;
    }

    /**
     * Check if a player is in cooldown
     * @param player The player
     * @return True if the player is in cooldown
     */
    public boolean isInCooldown(Player player) {
        if (!randomTpCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        
        return System.currentTimeMillis() < randomTpCooldowns.get(player.getUniqueId());
    }
    
    /**
     * Get the time left on a player's cooldown
     * @param player The player
     * @return The time left in seconds
     */
    public int getCooldownTimeLeft(Player player) {
        if (!randomTpCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        
        long timeLeft = randomTpCooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
        return Math.max(0, (int) (timeLeft / 1000));
    }
    
    /**
     * Set a cooldown for a player
     * @param player The player
     */
    private void setCooldown(Player player) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        int cooldown = config.getInt("smp.rtp-cooldown", 300); // Default 5 minutes
        
        if (cooldown > 0) {
            randomTpCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        }
    }
    
    /**
     * Randomly teleport a player within the SMP world
     * @param player The player to teleport
     * @return CompletableFuture that resolves to true if teleported successfully
     */
    public CompletableFuture<Boolean> randomTeleport(Player player) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Check if teleportation is enabled
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        if (!config.getBoolean("smp.enabled", true)) {
            future.complete(false);
            return future;
        }
        
        // Check if player is in cooldown
        if (isInCooldown(player)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(getCooldownTimeLeft(player)));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "smp.rtp-cooldown",
                    replacements
            );
            
            future.complete(false);
            return future;
        }
        
        // Get SMP world
        World smpWorld = getSMPWorld();
        
        // Get teleport range from config
        int minRange = config.getInt("smp.min-teleport-range", 1000);
        int maxRange = config.getInt("smp.max-teleport-range", 10000);
        
        // Notify player
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "smp.searching-location",
                null
        );
        
        // Start async task to find a safe location
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Try to find a safe location (max 10 attempts)
                for (int attempts = 0; attempts < 10; attempts++) {
                    // Generate random coordinates
                    int x = randomRange(minRange, maxRange) * (random.nextBoolean() ? 1 : -1);
                    int z = randomRange(minRange, maxRange) * (random.nextBoolean() ? 1 : -1);
                    
                    // Find safe Y asynchronously
                    int safeY = findSafeY(smpWorld, x, z);
                    
                    if (safeY > 0) {
                        // Found a safe location, complete the teleport on the main thread
                        int finalX = x;
                        int finalY = safeY;
                        int finalZ = z;
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Location location = new Location(smpWorld, finalX + 0.5, finalY + 1, finalZ + 0.5);
                            
                            // Make sure the chunk is loaded
                            if (!location.getChunk().isLoaded()) {
                                location.getChunk().load(true);
                            }
                            
                            // Check again on the main thread for block safety
                            if (isSafeLocation(location)) {
                                // Teleport the player
                                plugin.getAPI().getTeleportAPI().teleport(player, location);
                                
                                // Set cooldown
                                setCooldown(player);
                                
                                // Complete future
                                future.complete(true);
                            } else {
                                // Try again
                                randomTeleport(player).thenAccept(future::complete);
                            }
                        });
                        
                        return;
                    }
                }
                
                // Failed to find a safe location after max attempts
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "smp.no-safe-location",
                            null
                    );
                    
                    future.complete(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> future.complete(false));
            }
        });
        
        return future;
    }
    
    /**
     * Generate a random number within a range
     * @param min The minimum value
     * @param max The maximum value
     * @return A random number between min and max
     */
    private int randomRange(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
    
    /**
     * Find a safe Y coordinate at the given X,Z
     * @param world The world
     * @param x The X coordinate
     * @param z The Z coordinate
     * @return A safe Y coordinate, or -1 if none found
     */
    private int findSafeY(World world, int x, int z) {
        // Check if the chunk is generated
        Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
        if (!chunk.isGenerated()) {
            return -1;
        }
        
        // Get the highest block at this location
        int highestY = Math.min(world.getHighestBlockYAt(x, z), world.getMaxHeight() - 2);
        
        // Start from the highest block and move down to find a safe block
        for (int y = highestY; y > world.getMinHeight() + 10; y--) {
            // Skip air blocks
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.AIR) {
                continue;
            }
            
            // Check if the block is safe to stand on
            if (SAFE_BLOCKS.contains(block.getType())) {
                // Check for 2 blocks of air above
                Block above1 = block.getRelative(BlockFace.UP);
                Block above2 = above1.getRelative(BlockFace.UP);
                
                if (above1.getType() == Material.AIR && above2.getType() == Material.AIR) {
                    // Check for unsafe blocks nearby
                    boolean dangerous = false;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            Block nearby = world.getBlockAt(x + dx, y + 1, z + dz);
                            if (UNSAFE_BLOCKS.contains(nearby.getType())) {
                                dangerous = true;
                                break;
                            }
                        }
                        if (dangerous) break;
                    }
                    
                    if (!dangerous) {
                        return y;
                    }
                }
            }
        }
        
        return -1; // No safe Y found
    }
    
    /**
     * Check if a location is safe for teleportation
     * @param location The location to check
     * @return True if the location is safe
     */
    private boolean isSafeLocation(Location location) {
        Block block = location.getBlock().getRelative(BlockFace.DOWN);
        
        // Check if the block below is solid
        if (!block.getType().isSolid()) {
            return false;
        }
        
        // Check if there are unsafe blocks nearby
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block nearby = location.getBlock().getRelative(x, y, z);
                    if (UNSAFE_BLOCKS.contains(nearby.getType())) {
                        return false;
                    }
                }
            }
        }
        
        // Validate there's enough headroom
        Block headBlock = location.getBlock().getRelative(BlockFace.UP);
        if (headBlock.getType() != Material.AIR) {
            return false;
        }
        
        return true;
    }
}