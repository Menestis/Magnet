package fr.blendman.magnet.server.listeners;

import fr.blendman.magnet.api.handles.messenger.events.AdminMovePlayerEvent;
import fr.blendman.magnet.api.handles.messenger.events.InvalidatePlayerEvent;
import fr.blendman.magnet.api.server.events.PlayerInfoReadyEvent;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerInfo;
import fr.blendman.skynet.models.ServerLoginPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class NetworkListener {

    private final ServerMagnet serverMagnet;

    public NetworkListener(ServerMagnet serverMagnet) {
        this.serverMagnet = serverMagnet;
    }

    public void onAdminMove(AdminMovePlayerEvent event) {
        System.out.println("Admin move : " + event.player + " | " + event.server);
        serverMagnet.addWhitelist(event.player);
        serverMagnet.getMagnet().movePlayerToServer(event.player, event.server, false).thenAccept(unused -> {

        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    public void onPlayerInvalidation(InvalidatePlayerEvent event) {
        Player player = Bukkit.getPlayer(event.getUuid());
        ServerLoginPlayerInfo info = serverMagnet.getInfo(event.getUuid());
        if (player == null || info == null)
            return;

        CompletableFuture<PlayerInfo> ret = new CompletableFuture<>();
        try {
            serverMagnet.getMagnet().getPlayerApi().apiPlayersPlayerGetAsync(player.getUniqueId().toString(), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        ret.thenAccept(newInfo -> {
            info.setLocale(newInfo.getLocale());
            info.setCurrency(newInfo.getCurrency());
            info.setPremiumCurrency(newInfo.getPremiumCurrency());
            info.setBlocked(newInfo.getBlocked());
            info.setInventory(newInfo.getInventory());
            if (Bukkit.getPlayer(event.getUuid()) != null)
                Bukkit.getPluginManager().callEvent(new PlayerInfoReadyEvent(info, player, true));
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }
}
