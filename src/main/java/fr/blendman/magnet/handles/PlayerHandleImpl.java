package fr.blendman.magnet.handles;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.handles.PlayerHandle;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.api.PlayerApi;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerInfo;
import fr.blendman.skynet.models.PlayerMove;
import fr.blendman.skynet.models.PlayerSanction;
import fr.blendman.skynet.models.PlayerSanctionResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class PlayerHandleImpl implements PlayerHandle {

    private final Magnet magnet;
    private final PlayerApi playerApi;

    public PlayerHandleImpl(Magnet magnet) {
        this.magnet = magnet;
        playerApi = magnet.getPlayerApi();
    }

    @Override
    public CompletableFuture<String> movePlayerToServer(UUID player, UUID server, boolean whitelist) {
        CompletableFuture<String> ret = new CompletableFuture<>();
        try {
            playerApi.apiPlayersUuidMovePostAsync(player, new PlayerMove().server(server).adminMove(whitelist), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<String> movePlayerToServer(UUID player, String s) {
        CompletableFuture<String> ret = new CompletableFuture<>();
        try {
            playerApi.apiPlayersUuidMovePostAsync(player, new PlayerMove().kind(s), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<UUID> getPlayerUUID(String s) {
        CompletableFuture<UUID> ret = new CompletableFuture<>();
        try {
            playerApi.apiPlayersPlayerUuidGetAsync(s, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<PlayerSanctionResult> sanctionPlayer(UUID uuid, String category, UUID issuer, boolean b) {
        CompletableFuture<PlayerSanctionResult> ret = new CompletableFuture<>();
        try {
            playerApi.apiPlayersUuidSanctionPostAsync(uuid, new PlayerSanction().categorie(category).issuer(issuer).unsanction(b), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            if (e.getCode() == 409) {
                ret.complete(null);
                return ret;
            } else
                ret.completeExceptionally(e);
        }
        return ret.exceptionally(throwable -> {
            if (throwable instanceof ApiException && ((ApiException) throwable).getCode() == 409)
                return null;
            else
                throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public CompletableFuture<PlayerInfo> getPlayerInfo(String player) {
        CompletableFuture<PlayerInfo> ret = new CompletableFuture<>();
        try {
            playerApi.apiPlayersPlayerGetAsync(player, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }

        return ret;
    }
}
