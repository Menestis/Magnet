package fr.blendman.magnet.handles;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.handles.TransactionsHandle;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.api.PlayerApi;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerTransaction;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class TransactionsHandleImpl implements TransactionsHandle {
    private final Magnet magnet;
    private final PlayerApi playerApi;

    public TransactionsHandleImpl(Magnet magnet) {
        this.magnet = magnet;
        playerApi = magnet.getPlayerApi();
    }

    @Override
    public CompletableFuture<Boolean> transaction(UUID uuid, int currency, int premiumCurrency) {
        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        try {
            playerApi.apiPlayersUuidTransactionPostAsync(uuid, new PlayerTransaction().currency(currency).premiumCurrency(premiumCurrency), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<Boolean> inventoryTransaction(UUID uuid, String item, int transaction) {
        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        try {
            playerApi.apiPlayersUuidInventoryTransactionPostAsync(uuid, Collections.singletonMap(item, transaction), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }
}
