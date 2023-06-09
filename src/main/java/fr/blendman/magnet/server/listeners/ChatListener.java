package fr.blendman.magnet.server.listeners;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.server.ServerCacheHandler;
import fr.blendman.magnet.api.server.events.PlayerInfoReadyEvent;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.utils.NumberUtils;
import fr.blendman.skynet.models.Mute;
import fr.blendman.skynet.models.ServerLoginPlayerInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/**
 * @author Ariloxe
 */
public class ChatListener implements Listener {

    private final ServerMagnet serverMagnet;
    private final List<UUID> muted = new ArrayList<>();

    public ChatListener(ServerMagnet serverMagnet) {
        this.serverMagnet = serverMagnet;
    }

    private final Map<UUID, Long> lastMessageExecution = new HashMap<>();

    @EventHandler
    private void onPreProcessCommandEvent(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        event.setCancelled(true);

        ServerLoginPlayerInfo cache = serverMagnet.getInfo(player.getUniqueId());
        if (cache == null) {
            player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Please wait ! (Cache loading)");
            return;
        }

        if ((!player.hasPermission("magnet.chat.bypass_cooldown")) && lastMessageExecution.containsKey(player.getUniqueId()) && ((lastMessageExecution.get(player.getUniqueId()) + 500) > System.currentTimeMillis())) {
            player.sendMessage(Magnet.getPrefix() + "§cMerci d'attendre avant de renvoyer un message.");
            return;
        }

        if (muted.contains(player.getUniqueId())) {
            Mute mute = ServerCacheHandler.ServerCacheHandlerStore.getServerCacheHandler().getInfo(player.getUniqueId()).getMute();
            if (mute == null) {
                muted.remove(player.getUniqueId());
                return;
            }

            player.playSound(player.getLocation(), "ANVIL_BREAK", 1, 1);
            player.sendMessage("");
            player.sendMessage("§8┃ §cVous avez été réduit(e) au silence.");
            player.sendMessage("");
            player.sendMessage("§8• §fRaison: §c" + mute.getReason());
            player.sendMessage("§8• §fDate de fin: §c" + mute.getEnd());
            player.sendMessage("§8• §fDate de début: §c" + mute.getStart());
            player.sendMessage("");
            player.sendMessage("§8• §fTemps restant: §e" + NumberUtils.timeToStringAll(mute.getRemaining()));
            player.sendMessage("");
            return;
        }

        this.lastMessageExecution.put(player.getUniqueId(), System.currentTimeMillis());
        serverMagnet.getChatManager().onMessage(player, event.getMessage());
    }


    @EventHandler
    public void onPlayerInfoReadyEvent(PlayerInfoReadyEvent event) {
        Mute mute = event.getInfo().getMute();
        if (mute != null) {

            //TODO schedule a task (but optimized) to unmute the player once his sanction is finished

            if (event.isReCache() && !muted.contains(event.getPlayer().getUniqueId())) {
                Player player = event.getPlayer();

                player.playSound(player.getLocation(), "ANVIL_BREAK", 1, 1);
                player.sendMessage("");
                player.sendMessage("§8┃ §cVous avez été réduit(e) au silence.");
                player.sendMessage("");
                player.sendMessage("§8• §fRaison: §c" + mute.getReason());
                player.sendMessage("§8• §fDate de fin: §c" + mute.getEnd());
                player.sendMessage("§8• §fDate de début: §c" + mute.getStart());
                player.sendMessage("");
                player.sendMessage("§8• §fTemps restant: §e" + NumberUtils.timeToStringAll(mute.getRemaining()));
                player.sendMessage("");
            }
            muted.add(event.getPlayer().getUniqueId());

        } else if (event.isReCache())
            muted.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        lastMessageExecution.remove(event.getPlayer().getUniqueId());
        muted.remove(event.getPlayer().getUniqueId());
    }
}