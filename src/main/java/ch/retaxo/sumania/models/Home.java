package ch.retaxo.sumania.models;

import org.bukkit.Location;

/**
 * Represents a player home
 */
public class Home {

    private final String name;
    private final Location location;
    
    /**
     * Constructor
     * @param name The name of the home
     * @param location The location of the home
     */
    public Home(String name, Location location) {
        this.name = name;
        this.location = location;
    }
    
    /**
     * Get the name of the home
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the location of the home
     * @return The location
     */
    public Location getLocation() {
        return location;
    }
}
