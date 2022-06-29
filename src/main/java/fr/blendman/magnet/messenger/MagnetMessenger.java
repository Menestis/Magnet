package fr.blendman.magnet.messenger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.rabbitmq.client.*;
import fr.blendman.magnet.MagnetSide;
import fr.blendman.magnet.api.handles.messenger.MagnetNetworkEvent;
import fr.blendman.magnet.api.handles.messenger.MessengerHandle;
import fr.blendman.skynet.models.Broadcast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author Blendman974
 */
public class MagnetMessenger implements RecoveryListener, MessengerHandle {


    private final Connection connection;
    private final RecoverableChannel channel;
    private final DefaultConsumer defaultConsumer;
    private final Map<Class<? extends MagnetNetworkEvent>, Set<Consumer<MagnetNetworkEvent>>> listeners = new HashMap<>();
    private UUID id;

    public MagnetMessenger(MagnetSide side, UUID id) throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(System.getenv("AMQP_ADDRESS"));
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        connection = factory.newConnection(id.toString());
        channel = (RecoverableChannel) connection.createChannel();
        channel.addRecoveryListener(this);

        defaultConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                MagnetMessenger.this.handleDelivery(consumerTag, envelope, properties, body);
            }
        };
        this.id = id;

        init(side);
    }

    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public <T extends MagnetNetworkEvent> void registerListener(Consumer<T> handler, Class<T> clazz) {
        this.listeners.putIfAbsent(clazz, new HashSet<>());
        this.listeners.get(clazz).add((Consumer<MagnetNetworkEvent>) handler);
    }

    private void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        channel.basicAck(envelope.getDeliveryTag(), false);
        MagnetNetworkEvent event;
        try {
            String json = new String(body, StandardCharsets.UTF_8);
            JsonElement jsonElement = new Gson().fromJson(json, JsonElement.class);
            String className = "fr.blendman.magnet.api.handles.messenger.events." + jsonElement.getAsJsonObject().get("event").getAsString() + "Event";
            System.out.println("Received event (" + className + ")");
            event = (MagnetNetworkEvent) new Gson().fromJson(json, Class.forName(className));
        } catch (ClassNotFoundException | ClassCastException | NullPointerException e) {
            e.printStackTrace();
            return;
        }
        Set<Consumer<MagnetNetworkEvent>> l = this.listeners.get(event.getClass());
        if (l != null) {
            for (Consumer<MagnetNetworkEvent> consumer : l) {
                consumer.accept(event);
            }
        }
    }

//    public void sendEvent(MagnetNetworkEvent event) {
//        try {
//            JsonElement jsonElement = new Gson().toJsonTree(event);
//            jsonElement.getAsJsonObject().addProperty("event", event.getClass().getName().replace("fr.blendman.magnet.api.handles.messenger.events.", "").replace("Event", ""));
//            channel.basicPublish(event.getTarget().name().toLowerCase(), event.getRoutingKey(), null, jsonElement.toString().getBytes(StandardCharsets.UTF_8));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void handleRecovery(Recoverable recoverable) {
        System.out.println("RabbitMq error, recovered successfully !");
    }

    @Override
    public void handleRecoveryStarted(Recoverable recoverable) {
        System.out.println("RabbitMq error, attempting recovery...");
    }


    private void init(MagnetSide side) throws IOException {
        channel.queueDelete(this.id.toString());
        channel.queueDeclare(this.id.toString(), true, true, true, null);
        channel.exchangeDeclare("direct", "direct", true, false, null);
        channel.exchangeDeclare("events", "topic", true, false, null);

        channel.queueBind(this.id.toString(), "direct", this.id.toString());
        //channel.queueBind(this.id.toString(), "events", side.toString().toLowerCase() + ".#");
        channel.basicConsume(this.id.toString(), defaultConsumer);
    }

    public void subscribe(String key) throws IOException {
        channel.queueBind(id.toString(), "events", key);
    }
}
