package ch.retaxo.sumania.api;

import ch.retaxo.sumania.Sumania;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This class will be registered through the register-method in the 
 * plugins onEnable-method.
 */
public class SumaniaPlaceholderExpansion extends PlaceholderExpansion {

    private final Sumania plugin;

    /**
     * Since we register the expansion inside our own plugin, we
     * can simply use this method here to get an instance of our
     * plugin.
     *
     * @param plugin The instance of our plugin.
     */
    public SumaniaPlaceholderExpansion(Sumania plugin) {
        this.plugin = plugin;
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI
     * know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * <br>For convenience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "sumania";
    }

    /**
     * This is the version of the expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * For convenience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param player A {@link org.bukkit.OfflinePlayer OfflinePlayer}.
     * @param identifier A String containing the identifier/value.
     *
     * @return Possibly-null String of the requested identifier.
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }
        
        // Check if player is online for placeholders that require online player
        Player onlinePlayer = player.getPlayer();

        // %sumania_balance% or %sumania_balance_formatted%
        if (identifier.equals("balance") || identifier.equals("balance_formatted")) {
            double balance = plugin.getAPI().getEconomyAPI().getBalance(player);
            return plugin.getAPI().getEconomyAPI().format(balance);
        }

        // %sumania_balance_raw%
        if (identifier.equals("balance_raw")) {
            double balance = plugin.getAPI().getEconomyAPI().getBalance(player);
            return String.format("%.2f", balance);
        }

        // %sumania_currency% 
        if (identifier.equals("currency")) {
            return plugin.getAPI().getEconomyAPI().getCurrencyName();
        }

        // %sumania_currency_symbol%
        if (identifier.equals("currency_symbol")) {
            return plugin.getAPI().getEconomyAPI().getCurrencySymbol();
        }

        // %sumania_kills%
        if (identifier.equals("kills")) {
            return String.valueOf(plugin.getAPI().getPlayerAPI().getKills(player));
        }

        // %sumania_deaths%
        if (identifier.equals("deaths")) {
            return String.valueOf(plugin.getAPI().getPlayerAPI().getDeaths(player));
        }

        // %sumania_homes_count%
        if (identifier.equals("homes_count")) {
            return String.valueOf(plugin.getAPI().getPlayerAPI().getHomes(player).size());
        }

        // %sumania_homes_limit%
        if (identifier.equals("homes_limit")) {
            return String.valueOf(plugin.getAPI().getPlayerAPI().getHomeLimit(player));
        }

        // %sumania_is_banned%
        if (identifier.equals("is_banned")) {
            return plugin.getAPI().getPlayerAPI().isBanned(player) ? "true" : "false";
        }

        // For online player-only placeholders
        if (onlinePlayer != null) {
            // Add any online-player specific placeholders here
        }

        // Placeholder not found
        return null;
    }
}