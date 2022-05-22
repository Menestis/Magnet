package fr.blendman.magnet.server.managers;

import fr.blendman.magnet.api.server.events.PlayerInfoReadyEvent;
import me.frep.vulcan.api.VulcanAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author Blendman974
 */
public class AnticheatManager implements Listener {

    @EventHandler
    public void onCacheRead(PlayerInfoReadyEvent event) {
        if (VulcanAPI.Factory.getApi() == null)
            return;
        Player player = event.getPlayer();
        if (player.hasPermission("vulcan.alerts") && !VulcanAPI.Factory.getApi().hasAlertsEnabled(player)) {
            VulcanAPI.Factory.getApi().toggleAlerts(player);
        }
    }
}
