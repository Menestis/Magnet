package fr.blendman.magnet.server.listeners;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.skynet.models.ServerLoginPlayerInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Blendman974
 */
public class BukkitCommandPreProcessor implements Listener {

    private final ServerMagnet serverMagnet;

    public BukkitCommandPreProcessor(ServerMagnet serverMagnet) {
        this.serverMagnet = serverMagnet;
    }

    private final Map<UUID, Long> lastCommandExecution = new HashMap<>();

    @EventHandler(priority = EventPriority.LOW)
    private void onPreProcessCommandEvent(PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        ServerLoginPlayerInfo cache = serverMagnet.getInfo(player.getUniqueId());
        if (cache == null) {
            player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Please wait ! (Cache loading)");
            event.setCancelled(true);
            return;
        }

        if ((!player.hasPermission("magnet.chat.bypass_cooldown")) && lastCommandExecution.containsKey(player.getUniqueId()) && ((lastCommandExecution.get(player.getUniqueId()) + 500) > System.currentTimeMillis())) {
            player.sendMessage("Vous envoyez des commandes trop rapidement !");
            event.setCancelled(true);
            return;
        }

        this.lastCommandExecution.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        lastCommandExecution.remove(event.getPlayer().getUniqueId());
    }
}