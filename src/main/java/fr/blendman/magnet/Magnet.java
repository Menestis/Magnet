package fr.blendman.magnet;

import fr.blendman.magnet.api.MagnetApi;
import fr.blendman.magnet.api.handles.PlayerHandle;
import fr.blendman.magnet.api.handles.TransactionsHandle;
import fr.blendman.magnet.api.server.leaderboards.Leaderboard;
import fr.blendman.magnet.api.server.leaderboards.LeaderboardEntry;
import fr.blendman.magnet.handles.PlayerHandleImpl;
import fr.blendman.magnet.handles.TransactionsHandleImpl;
import fr.blendman.magnet.messenger.MagnetMessenger;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.api.*;
import fr.blendman.skynet.client.ApiClient;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.client.Configuration;
import fr.blendman.skynet.client.auth.ApiKeyAuth;
import fr.blendman.skynet.models.CreateServer;
import fr.blendman.skynet.models.PlayerInfo;
import fr.blendman.skynet.models.Server;

import java.util.*;
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
    private final PlayerHandle playerHandle;
    private final DiscordApi discordApi;
    private final StatsApi statsApi;
    private final Timer timer;
    private final Map<String, String> registry = new HashMap<>();

    public Magnet(MagnetSide side) throws Exception {
        client = Configuration.getDefaultApiClient();
        timer = new Timer();

        String skynetUrl = System.getenv("SKYNET_URL");
        if (skynetUrl == null)
            throw new Exception("Missing SKYNET_URL");

        //client.setDebugging(true);
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
        discordApi = new DiscordApi(client);
        statsApi = new StatsApi(client);

        transactionHandle = new TransactionsHandleImpl(this);
        playerHandle = new PlayerHandleImpl(this);
        MagnetStore.setApi(this);
    }

    public static String getPrefix() {
        return "§3§lMenestis §f§l» ";
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
    public String getServerLabel() {
        return getServer().getLabel();
    }

    @Override
    public Map<String, String> getProperties() {
        return getServer().getProperties();
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
                srv -> new fr.blendman.magnet.api.server.Server(srv.getId(), srv.getDescription(), srv.getIp(), srv.getKind(), srv.getLabel(), srv.getState().toString(), srv.getProperties(), srv.getOnline())
        ).collect(Collectors.toList()));
    }

    public PlayerApi getPlayerApi() {
        return playerApi;
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

    @Override
    public CompletableFuture<Void> setServerDescription(String s) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            getServerApi().apiServersUuidSetdescriptionPostAsync(getServerId(), s, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<Void> sendWebhook(String name, String message) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            getDiscordApi().apiDiscordWebhookNamePostAsync(name, message, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<String> startServer(String nane, String kind, Map<String, String> properties) {
        CompletableFuture<String> ret = new CompletableFuture<>();
        try {
            getServerApi().apiServersPostAsync(new CreateServer().name(nane).kind(kind).properties(properties), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletableFuture<Leaderboard> getLeaderboard(String s) {
        CompletableFuture<fr.blendman.skynet.models.Leaderboard> ret = new CompletableFuture<>();
        try {
            statsApi.apiLeaderboardsNameGetAsync(s, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }

        return ret.thenApply(leaderboard -> new Leaderboard(leaderboard.getLabel(), leaderboard.getLeaderboard().stream().map(entry -> {
            String[] split = entry.split(":");
            if (split.length == 1)
                return new LeaderboardEntry("?", -1);
            return new LeaderboardEntry(split[0], Integer.parseInt(split[1]));
        }).collect(Collectors.toList())));
    }

    @Override
    public String getRegistryValue(String s) {
        return registry.get(s);
    }

    @Override
    public void setRegistryValue(String k, String v) {
        if (v == null)
            registry.remove(k);
        registry.put(k, v);
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
    public PlayerHandle getPlayerHandle() {
        return playerHandle;
    }

    @Override

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

    public DiscordApi getDiscordApi() {
        return discordApi;
    }

    public Timer getTimer() {
        return timer;
    }
}

