package fr.blendman.magnet.server.listeners;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.MagnetApi;
import fr.blendman.magnet.api.server.ServerLoginPlayerInfo;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.server.managers.ServerChatManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Ariloxe
 */
public class ChatListener implements Listener {

    private final ServerMagnet serverMagnet;

    public ChatListener(ServerMagnet serverMagnet) {
        this.serverMagnet = serverMagnet;
    }

    private final Map<UUID, Long> lastMessageExecution = new HashMap<>();

    @EventHandler
    private void onPreProcessCommandEvent(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();

        ServerLoginPlayerInfo cache = serverMagnet.getInfo(player.getUniqueId());
        if (cache == null) {
            player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Please wait ! (Cache loading)");
            event.setCancelled(true);
            return;
        }

        if ((!player.hasPermission("magnet.chat.bypass_cooldown")) && lastMessageExecution.containsKey(player.getUniqueId()) && ((lastMessageExecution.get(player.getUniqueId()) + 500) > System.currentTimeMillis())) {
            player.sendMessage("§3§lMenestis §f§l» §cMerci d'attendre avant de renvoyer un message.");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        this.lastMessageExecution.put(player.getUniqueId(), System.currentTimeMillis());
        ((ServerChatManager) MagnetApi.MagnetStore.getApi().getChatManager()).onMessage(player, event.getMessage());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        lastMessageExecution.remove(event.getPlayer().getUniqueId());
    }
}