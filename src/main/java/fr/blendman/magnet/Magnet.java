package fr.blendman.magnet;

import fr.blendman.magnet.api.MagnetApi;
import fr.blendman.magnet.api.handles.transactions.TransactionsHandle;
import fr.blendman.magnet.api.server.ChatManager;
import fr.blendman.magnet.handles.TransactionsHandleImpl;
import fr.blendman.magnet.messenger.MagnetMessenger;
import fr.blendman.magnet.server.managers.ServerChatManager;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.api.*;
import fr.blendman.skynet.client.ApiClient;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.client.Configuration;
import fr.blendman.skynet.client.auth.ApiKeyAuth;
import fr.blendman.skynet.models.PlayerInfo;
import fr.blendman.skynet.models.PlayerMove;
import fr.blendman.skynet.models.Server;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Blendman974
 */
public class Magnet implements MagnetApi {


    private final Server server;
    private final ServerApi serverApi;
    private final MagnetMessenger messenger;
    private final ApiClient client;
    private final ProxyApi proxyApi;
    private final PlayerApi playerApi;
    private final LoginApi loginApi;
    private final SessionApi sessionApi;
    private final TransactionsHandle transactionHandle;

    private final ChatManager chatManager;

    public Magnet(MagnetSide side) throws Exception {
        client = Configuration.getDefaultApiClient();

        String skynetUrl = System.getenv("SKYNET_URL");
        if (skynetUrl == null)
            throw new Exception("Missing SKYNET_URL");

//        client.setDebugging(true);
        client.setBasePath(skynetUrl);

        String s = System.getenv("SERVER_NAME") == null ? System.getenv("HOSTNAME") : System.getenv("SERVER_NAME");

        server = new RegistrationApi(client).apiServersLabelRegisterGet(s);

        ApiKeyAuth auth = (ApiKeyAuth) client.getAuthentication("auth");

        auth.setApiKey(side.toString() + " " + server.getKey());

        messenger = new MagnetMessenger(side, getServerId());

        proxyApi = new ProxyApi(client);
        serverApi = new ServerApi(client);
        playerApi = new PlayerApi(client);
        loginApi = new LoginApi(client);
        sessionApi = new SessionApi(client);

        chatManager = new ServerChatManager();
        transactionHandle = new TransactionsHandleImpl(this);
        MagnetStore.setApi(this);
    }

    public static String getPrefix() {
        return "§cMagnet > §f";
    }

    public ApiClient getClient() {
        return client;
    }

    public ProxyApi getProxyApi() {
        return proxyApi;
    }

    public ServerApi getServerApi() {
        return serverApi;
    }

    public Server getServer() {
        return server;
    }

    public UUID getServerId() {
        return server.getId();
    }

    @Override
    public CompletableFuture<Integer> getPlayerCount() {
        CompletableFuture<Integer> ret = new CompletableFuture<>();
        try {
            getServerApi().apiOnlinecountGetAsync(new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<List<fr.blendman.magnet.api.server.Server>> getAllServers() {
        CompletableFuture<List<Server>> ret = new CompletableFuture<>();
        try {
            getServerApi().apiServersGetAsync(new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret.thenApply(servers -> servers.stream().map(
                srv -> new fr.blendman.magnet.api.server.Server(srv.getId(), srv.getDescription(), srv.getIp(), srv.getKey(), srv.getKind(), srv.getLabel(), srv.getState(), srv.getProperties())
        ).collect(Collectors.toList()));
    }

    public PlayerApi getPlayerApi() {
        return playerApi;
    }

    @Override
    public CompletableFuture<Boolean> movePlayerToServer(UUID player, UUID server, boolean whitelist) {
        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        try {
            getPlayerApi().apiPlayersUuidMovePostAsync(player, new PlayerMove().server(server).adminMove(whitelist), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<Boolean> movePlayerToServer(UUID player, String s) {
        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        try {
            getPlayerApi().apiPlayersUuidMovePostAsync(player, new PlayerMove().kind(s), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<Void> setServerState(String s) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            getServerApi().apiServersUuidSetstatePostAsync(getServerId(), s, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    public CompletableFuture<PlayerInfo> getPlayerInfo(String player) {
        CompletableFuture<PlayerInfo> ret = new CompletableFuture<>();
        try {
            getPlayerApi().apiPlayersPlayerGetAsync(player, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }

        return ret;
    }

    public MagnetMessenger getMessenger() {
        return messenger;
    }

    @Override
    public TransactionsHandle getTransactionHandle() {
        return transactionHandle;
    }

    @Override
    public ChatManager getChatManager(){
        return this.chatManager;
    }

    public CompletableFuture<Void> stop() {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            getServerApi().apiServersLabelDeleteAsync(getServer().getLabel(), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public LoginApi getLoginApi() {
        return loginApi;
    }

    public SessionApi getSessionApi() {
        return sessionApi;
    }
}
