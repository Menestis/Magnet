package fr.blendman.magnet.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.MagnetSide;
import fr.blendman.magnet.api.handles.messenger.events.*;
import fr.blendman.magnet.proxy.commands.*;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.models.Server;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;


/**
 * @author Blendman974
 */
@Plugin(id = "magnet", name = "Magnet", version = "0.1", url = "https://guillaume-etheve.fr", description = "The plugin that interacts with Skynet", authors = {"Blendman974 the great"})
public class VelocityMagnet {
    private final ProxyServer server;
    private final Logger logger;
    private Magnet magnet;
    private final Map<UUID, RegisteredServer> servers = new HashMap<>();
    private final Map<String, RegisteredServer> serversByForcedHost = new HashMap<>();
    private final Map<String, Set<RegisteredServer>> serversByKind = new HashMap<>();
    private final Map<UUID, String> serversKinds = new HashMap<>();
    private final Map<UUID, Map<String, String>> serverProperties = new HashMap<>();

    @Inject
    public VelocityMagnet(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        logger.info("Loading Velocity Magnet !");
        try {
            magnet = new Magnet(MagnetSide.PROXY);

            CompletableFuture<List<Server>> f = new CompletableFuture<>();
            System.out.println("Loading initial servers");
            magnet.getServerApi().apiServersGetAsync(new ApiCallBackToCompletableFuture<>(f));

            f.thenAccept(servers -> servers.forEach(srv -> {
                if (!srv.getKind().equals("proxy")) {
                    getLogger().info("Adding initial server : {} ({})", srv.getLabel(), srv.getIp());
                    ServerInfo info = new ServerInfo(srv.getLabel(), new InetSocketAddress(srv.getIp(), 25565));
                    RegisteredServer registeredServer = getServer().registerServer(info);
                    saveServer(srv.getId(), srv.getKind(), registeredServer, srv.getProperties());
                }
            })).exceptionally(throwable -> {
                throwable.printStackTrace();
                server.shutdown();
                return null;
            });

            magnet.getMessenger().subscribe("proxy.#");
            NetworkListener listener = new NetworkListener(this);
            magnet.getMessenger().registerListener(listener::onNewRoute, NewRouteEvent.class);
            magnet.getMessenger().registerListener(listener::onServerStart, ServerStartedEvent.class);
            magnet.getMessenger().registerListener(listener::onDeleteRoute, DeleteRouteEvent.class);
            magnet.getMessenger().registerListener(listener::onMoveRequest, MovePlayerEvent.class);
            magnet.getMessenger().registerListener(listener::onMoveToAvailable, MovePlayerToAvailableEvent.class);
            magnet.getMessenger().registerListener(listener::onDisconnectRequest, DisconnectPlayerEvent.class);
            magnet.getMessenger().registerListener(listener::onBroadcast, BroadcastEvent.class);

        } catch (Exception e) {
            e.printStackTrace();
            server.shutdown();
        }

        registerCommands();
    }

    private void registerCommands() {
        getServer().getCommandManager().register("hub", new HubCommand(this), "lobby");
        getServer().getCommandManager().register("aban", new AbanCommand(this));
        getServer().getCommandManager().register("rank", new RankCommand(this));
        getServer().getCommandManager().register("send", new SendCommand(this));
        getServer().getCommandManager().register("lookup", new LookupCommand(this));
        getServer().getCommandManager().register("transaction", new TransactionCommand(this));


    }

    void saveServer(UUID id, String kind, RegisteredServer info, Map<String, String> properties) {
        this.servers.put(id, info);
        this.serversKinds.put(id, kind);
        if (properties != null) {
            this.serverProperties.put(id, properties);
            if (properties.containsKey("forcedHost"))
                serversByForcedHost.put(properties.get("forcedHost"), info);
        }
        this.serversByKind.compute(kind, (s, servers) -> {
            if (servers == null) {
                HashSet<RegisteredServer> set = new HashSet<>();
                set.add(info);
                return set;
            } else {
                servers.add(info);
                return servers;
            }
        });

    }

    void removeServer(UUID id) {
        RegisteredServer oldServer = this.servers.remove(id);
        Map<String, String> properties = this.serverProperties.remove(id);
        if (properties != null && properties.containsKey("forcedHost"))
            serversByForcedHost.remove(properties.get("forcedHost"));
        String kind = this.serversKinds.remove(id);
        if (kind != null && oldServer != null)
            this.serversByKind.computeIfPresent(kind, (s, registeredServers) -> {
                registeredServers.remove(oldServer);
                if (registeredServers.isEmpty())
                    return null;
                else
                    return registeredServers;
            });
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new LoginListener(this));
        server.getEventManager().register(this, new MoveListener(this));
    }

    public Magnet getMagnet() {
        return magnet;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }


    public CompletableFuture<RegisteredServer> getAvailableServerOfKind(String kind) {
        Iterator<RegisteredServer> it = serversByKind.getOrDefault(kind, new HashSet<>()).stream().map(srv -> server.getServer(srv.getServerInfo().getName())).filter(Optional::isPresent).map(Optional::get).iterator();
        return recursiveGetNextAvailableServer(it);
    }

    private CompletableFuture<RegisteredServer> recursiveGetNextAvailableServer(Iterator<RegisteredServer> it) {
        if (!it.hasNext())
            return CompletableFuture.completedFuture(null);

        RegisteredServer current = it.next();
        return isServerSuitable(current).thenCompose(suitable -> {
            if (suitable) {
                return CompletableFuture.completedFuture(current);
            } else {
                return recursiveGetNextAvailableServer(it);
            }
        });
    }

    public CompletableFuture<Boolean> isServerSuitable(RegisteredServer server) {
        return server.ping().thenApply(serverPing -> {
            if (!serverPing.getPlayers().isPresent()) {
                return false;
            }
            ServerPing.Players players = serverPing.getPlayers().get();
            return players.getOnline() < players.getMax();
        });
    }

    public RegisteredServer getServerById(UUID id) {
        return servers.get(id);
    }

    public RegisteredServer getForcedHost(String vhost) {
        return serversByForcedHost.get(vhost);
    }

}
