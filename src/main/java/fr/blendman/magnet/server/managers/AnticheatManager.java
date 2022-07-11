package fr.blendman.magnet.server.managers;

import fr.blendman.magnet.api.server.events.PlayerInfoReadyEvent;
import fr.blendman.magnet.server.ServerMagnet;
import me.frep.vulcan.api.VulcanAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Blendman974
 */
public class AnticheatManager implements Listener {

    @EventHandler
    public void onCacheRead(PlayerInfoReadyEvent event) {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(ServerMagnet.class), () -> {
            if (VulcanAPI.Factory.getApi() == null)
                return;
            Player player = event.getPlayer();
//            if (player.hasPermission("vulcan.alerts") && !VulcanAPI.Factory.getApi().hasAlertsEnabled(player)) {
//                VulcanAPI.Factory.getApi().toggleAlerts(player);
//            }
        });
    }
}
