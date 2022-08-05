package fr.blendman.magnet.server.echo;

import fr.blendman.magnet.server.ServerMagnet;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Blendman974
 */
public class EchoWebSocket extends WebSocketClient {
    private final ServerMagnet magnet;
    private final Consumer<String> onMessage;

    public EchoWebSocket(ServerMagnet magnet, URI serverUri, UUID key, Consumer<String> onMessage) {
        super(serverUri, Collections.singletonMap("Authorization", key.toString()));
        this.magnet = magnet;
        this.onMessage = onMessage;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        magnet.getLogger().info("Echo websocket opened");
    }

    @Override
    public void onMessage(String s) {
        onMessage.accept(s);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        magnet.getLogger().info("Echo websocket closed with code " + code + " and reason " + reason + "(remote: " + remote + ")");
    }

    @Override
    public void onError(Exception e) {
        magnet.getLogger().info("Echo websocket error: " + e.getMessage());
        e.printStackTrace();
    }
}
