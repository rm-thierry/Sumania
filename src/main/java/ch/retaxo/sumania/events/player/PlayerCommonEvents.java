package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.api.player.PlayerAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PlayerCommonEvents implements Listener {


    private final Sumania plugin;

    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public PlayerCommonEvents(Sumania plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        OfflinePlayer player = event.getEntity();
        PlayerAPI playerAPI = new PlayerAPI(Sumania.getInstance());
        int currentDeaths = playerAPI.getDeaths(player);
        plugin.getAPI().getPlayerAPI().setDeaths(player, currentDeaths + 1);
    }


    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            event.setDeathMessage(null);
            OfflinePlayer killer = event.getEntity().getKiller();
            PlayerAPI playerAPI = new PlayerAPI(Sumania.getInstance());

            int currentKills = playerAPI.getKills(killer);
            plugin.getAPI().getPlayerAPI().setKills(killer, currentKills + 1);
        }
    }


}
