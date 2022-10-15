package fr.blendman.magnet.server.echo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.blendman.magnet.api.handles.EchoHandle;
import fr.blendman.magnet.api.handles.messenger.MagnetNetworkEvent;
import fr.blendman.magnet.api.handles.messenger.events.EchoStartTrackingPlayerEvent;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.api.EchoApi;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.EchoUserDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * @author Blendman974
 */
public class EchoHandleImpl implements EchoHandle, Listener {
    private final ServerMagnet magnet;
    private final EchoApi echoApi;
    private UUID key;

    private final Map<String, BiConsumer<UUID, EchoEvent>> listeners = new HashMap<>();
    private WebSocketClient websocket;
    private final String ECHO_URL = "ws://echo.echo:8888/servers/ws";
    private final Gson gson = new Gson();
    private final Map<UUID, EchoPlayerStatus> playerStatus = new HashMap<>();
    private boolean enabled = false;

    public EchoHandleImpl(ServerMagnet magnet) {
        this.magnet = magnet;
        echoApi = magnet.getMagnet().getEchoApi();
    }

    @Override
    public CompletableFuture<Void> enableFeature() {
        if (enabled)
            return CompletableFuture.completedFuture(null);
        enabled = true;
        CompletableFuture<UUID> ret = new CompletableFuture<>();

        magnet.getMagnet().getMessenger().registerListener(this::onNewPlayerToTrack, EchoStartTrackingPlayerEvent.class);
        try {
            echoApi.apiServersUuidEchoEnableGetAsync(magnet.getMagnet().getServerId(), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret.thenAccept(uuid -> {
            this.key = uuid;
            openWebsocket();
            Bukkit.getPluginManager().registerEvents(this, magnet);
            Bukkit.getScheduler().runTaskTimerAsynchronously(magnet, this::tick, 1, 1);
        });
    }

    private void onNewPlayerToTrack(EchoStartTrackingPlayerEvent ev) {
        magnet.getLogger().info("ECHO: New player to track : " + ev.player);
        playerStatus.put(ev.player, new EchoPlayerStatus());
    }

    private void tick() {
        if (websocket == null) {
            return;
        } else if (!websocket.isOpen()) {
            openWebsocket();
            return;
        }
        Map<UUID, double[]> positions = new HashMap<>();
        JsonObject jsonObject = new JsonObject();
        for (UUID uuid : playerStatus.keySet()) {
            Player pl = Bukkit.getPlayer(uuid);
            if (pl == null) {
                continue;
            }
            Location l = pl.getLocation();
            Vector direction = pl.getLocation().getDirection();

            positions.put(pl.getUniqueId(), new double[]{l.getZ(), l.getX(), l.getY(), direction.getZ(), direction.getX(), direction.getY()});
        }
        jsonObject.addProperty("event", "Positions");
        jsonObject.add("positions", gson.toJsonTree(positions));
        websocket.send(jsonObject.toString());
    }

    @Override
    public CompletableFuture<Integer> addPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return CompletableFuture.completedFuture(-1);
        }
        playerStatus.put(uuid, new EchoPlayerStatus());
        CompletableFuture<Integer> ret = new CompletableFuture<>();
        try {
            echoApi.apiPlayersUuidEchoPostAsync(uuid, new EchoUserDefinition().ip(player.getAddress().getAddress().getHostAddress()).server(magnet.getMagnet().getServerId()), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public EchoPlayerStatus getPlayerStatus(UUID uuid) {
        if (playerStatus.containsKey(uuid))
            return playerStatus.get(uuid);
        else {
            magnet.getLogger().warning("Getting player echo status for player who was not added");
            return new EchoPlayerStatus();
        }
    }

    @Override
    public void setMuted(UUID uuid, boolean b) {
        if (websocket == null || !websocket.isOpen()) {
            magnet.getLogger().warning("Echo websocket not open, cannot set player state");
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("event", "SetPlayerStatus");
        jsonObject.addProperty("player", uuid.toString());
        jsonObject.addProperty("mute", b);
        websocket.send(jsonObject.toString());
    }

    @Override
    public void setDeafen(UUID uuid, boolean b) {
        if (websocket == null || !websocket.isOpen()) {
            magnet.getLogger().warning("Echo websocket not open, cannot set player state");
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("event", "SetPlayerStatus");
        jsonObject.addProperty("player", uuid.toString());
        jsonObject.addProperty("deaf", b);
        websocket.send(jsonObject.toString());
    }

    @Override
    public void setBroadcast(UUID uuid, boolean b) {
        if (websocket == null || !websocket.isOpen()) {
            magnet.getLogger().warning("Echo websocket not open, cannot set player state");
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("event", "SetPlayerStatus");
        jsonObject.addProperty("player", uuid.toString());
        jsonObject.addProperty("broadcast", b);
        websocket.send(jsonObject.toString());
    }

    @Override
    public void startVirtualAudio(UUID uuid, String name, double[] position, String world, boolean broadcas) {
        if (websocket == null || !websocket.isOpen()) {
            magnet.getLogger().warning("Echo websocket not open, cannot set player state");
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("event", "CreateVirtualAudio");
        jsonObject.addProperty("id", uuid.toString());
        jsonObject.addProperty("audio_name", name);
        jsonObject.add("position", gson.toJsonTree(position));
        jsonObject.addProperty("world", world);
        jsonObject.addProperty("broadcast", broadcas);
        websocket.send(jsonObject.toString());
    }

    @Override
    public void stopVirtualAudio(UUID uuid) {
        if (websocket == null || !websocket.isOpen()) {
            magnet.getLogger().warning("Echo websocket not open, cannot set player state");
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("event", "StopVirtualAudio");
        jsonObject.addProperty("id", uuid.toString());
        websocket.send(jsonObject.toString());
    }

    @Override
    public void setWorld(UUID uuid, String s) {
        if (websocket == null || !websocket.isOpen() || websocket.isOpen()) {
            magnet.getLogger().warning("Echo websocket not open, cannot set player state");
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("event", "SetPlayerStatus");
        jsonObject.addProperty("player", uuid.toString());
        jsonObject.addProperty("world", s);
        websocket.send(jsonObject.toString());
    }

    //open websocket connection to echo server
    private void openWebsocket() {
        try {
            websocket = new EchoWebSocket(magnet, new URI(ECHO_URL), key, this::onMessage);
            websocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void onMessage(String s) {
        JsonObject jsonObject = gson.fromJson(s, JsonObject.class);
        String event = jsonObject.get("event").getAsString();
        UUID player = UUID.fromString(jsonObject.get("player").getAsString());

        EchoEvent ev;
        switch (event) {
            case "Disconnected":
                ev = EchoEvent.Disconnected;
                this.getPlayerStatus(player).setConnected(false);
                break;
            case "Connecting":
                ev = EchoEvent.Connecting;
                break;
            case "Connected":
                ev = EchoEvent.Connected;
                this.getPlayerStatus(player).setConnected(true);
                break;
            case "PlayerStatus":
                ev = EchoEvent.Status;
                boolean mute = jsonObject.get("mute").getAsBoolean();
                boolean deaf = jsonObject.get("deaf").getAsBoolean();
                boolean serverMute = jsonObject.get("server_mute").getAsBoolean();
                boolean serverDeaf = jsonObject.get("server_deaf").getAsBoolean();
                EchoPlayerStatus st = this.getPlayerStatus(player);
                st.setDeafen(deaf);
                st.setMuted(mute);
                st.setServerDeafen(serverMute);
                st.setServerMuted(serverDeaf);
                break;
            default:
                magnet.getLogger().warning("Unḱnown event reveived :" + event);
                return;
        }
        for (BiConsumer<UUID, EchoEvent> c : listeners.values()) {
            c.accept(player, ev);
        }

    }


    @Override
    public void addEchoEventListener(String s, BiConsumer<UUID, EchoEvent> consumer) {
        listeners.put(s, consumer);
    }

    @Override
    public void removeEchoEventListener(String s) {
        listeners.remove(s);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        playerStatus.remove(event.getPlayer().getUniqueId());
    }
}
