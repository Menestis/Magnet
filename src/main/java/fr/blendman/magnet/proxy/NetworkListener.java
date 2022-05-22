package fr.blendman.magnet.proxy;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import fr.blendman.magnet.api.handles.messenger.events.*;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author Blendman974
 */
public class NetworkListener {
    private final VelocityMagnet velocityMagnet;

    public NetworkListener(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }

    public void onNewRoute(NewRouteEvent event) {
        velocityMagnet.getLogger().info("Server starting : {}", event.name);
    }

    public void onServerStart(ServerStartedEvent event) {
        velocityMagnet.getLogger().info("Adding server : {} ({})", event.name, event.addr);
        ServerInfo info = new ServerInfo(event.name, new InetSocketAddress(event.addr, 25565));
        RegisteredServer registeredServer = velocityMagnet.getServer().registerServer(info);
        velocityMagnet.saveServer(event.id, event.kind, registeredServer, event.properties);
    }

    public void onDeleteRoute(DeleteRouteEvent event) {
        velocityMagnet.getLogger().info("Removing server : {}", event.name);
        velocityMagnet.removeServer(event.id);
        velocityMagnet.getServer().getServer(event.name).ifPresent(registeredServer -> velocityMagnet.getServer().unregisterServer(registeredServer.getServerInfo()));
    }

    public void onMoveRequest(MovePlayerEvent event) {
        RegisteredServer server = velocityMagnet.getServerById(event.server);
        if (server == null)
            return;

        Optional<Player> optPlayer = velocityMagnet.getServer().getPlayer(event.player);
        if (!optPlayer.isPresent()) {
            return;
        }

        Player player = optPlayer.get();

        velocityMagnet.getLogger().info("Moving player : {}", server.getServerInfo().getName());

        player.createConnectionRequest(server).connect().thenAccept(result -> {
            switch (result.getStatus()) {
                case SUCCESS:
                    break;
                case ALREADY_CONNECTED:
                    player.sendMessage(Component.text("§4" + "Vous êtes déja connecté sur ce serveur !"));
                    break;
                case CONNECTION_IN_PROGRESS:
                    player.sendMessage(Component.text("§4" + "La connexion est cours !"));
                    break;
                case CONNECTION_CANCELLED:
                    player.sendMessage(Component.text("§4" + "La connexion n'a pas pus aboutir suite a une erreur inconue"));
                    break;
                case SERVER_DISCONNECTED:
                    player.sendMessage(Component.text("§4" + "Impossible de vous connecter : " + result.getReasonComponent().orElse(Component.text("aucune idée mais wallah tu peux pas"))));
                    break;
            }
        }).exceptionally(throwable -> {
            velocityMagnet.getLogger().error("Could not move player : ", throwable);
            return null;
        });


    }

    public void onMoveToAvailable(MovePlayerToAvailableEvent event) {
        velocityMagnet.getServer().getPlayer(event.player).ifPresent(player -> {
            velocityMagnet.getAvailableServerOfKind(event.kind).thenCompose(server ->
                    player.createConnectionRequest(server).connect()
            ).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        });
    }


    public void onDisconnectRequest(DisconnectPlayerEvent event){
        velocityMagnet.getServer().getPlayer(event.player).ifPresent(player -> {
            player.disconnect(Component.text("You have been disconnected !"));
        });
    }
}

