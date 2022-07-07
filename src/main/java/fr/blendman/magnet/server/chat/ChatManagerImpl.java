package fr.blendman.magnet.server.chat;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.server.ServerCacheHandler;
import fr.blendman.magnet.api.server.chat.ChatManager;
import fr.blendman.magnet.api.server.chat.PlayerChatConsumer;
import fr.blendman.magnet.utils.InteractiveMessage;
import fr.blendman.magnet.utils.TextComponentBuilder;
import fr.blendman.skynet.client.ApiCallback;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.Broadcast;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author Ariloxe
 */
public class ChatManagerImpl implements ChatManager {

    int queueMaxSize = 50;
    int messageCount = 0;

    private final Deque<MessageData> messageQueue = new ArrayDeque<>();
    private final Map<String, MessageData> idMessageDataMap = new HashMap<>();

    private final Map<String, List<UUID>> confirmationMap = new HashMap<>();

    private boolean ranVisible = true;
    private final Magnet magnet;

    public ChatManagerImpl(Magnet magnet) {
        this.magnet = magnet;
    }

    public void onMessage(Player player, String message) {
        messageCount++;
        String messageId = message.hashCode() + "-" + messageCount;
        MessageData messageData = new MessageData(player.getName(), message, messageId);

        if (messageQueue.size() >= queueMaxSize) {
            MessageData oldMessageData = messageQueue.pop();
            idMessageDataMap.remove(oldMessageData.getId());
            confirmationMap.remove(oldMessageData.getId());
        }

        this.messageQueue.add(messageData);
        this.idMessageDataMap.put(messageId, messageData);
        this.confirmationMap.put(messageId, new ArrayList<>());

        this.playerStringBiConsumer.accept(player, message, messageId);
    }

    @Override
    public void reportPlayer(Player reporter, String string) {
        if (!idMessageDataMap.containsKey(string)) {
            reporter.sendMessage(Magnet.getPrefix() + "§cCe message a malheureusement expiré..");
            return;
        }

        MessageData messageData = idMessageDataMap.get(string);
        if (messageData.isAlreadySignaled()) {
            reporter.sendMessage(Magnet.getPrefix() + "§cCe message a déjà été signalé !");
            return;
        }

        if (confirmationMap.containsKey(string) && !confirmationMap.get(string).contains(reporter.getUniqueId())) {
            reporter.sendMessage("");
            reporter.sendMessage("§8§l■ §7Souhaitez-vous vraiment signaler ce message de §e" + messageData.getPlayerName() + "§7 ?");
            reporter.sendMessage("§8➥ §f" + messageData.getMessage());
            reporter.sendMessage("");
            new InteractiveMessage().add(new TextComponentBuilder("§8§l■ §aCliquez ici pour confirmer").setHoverMessage("§8- §aCliquez pour confirmer le signalement §8-").setClickAction(ClickEvent.Action.RUN_COMMAND, "/reportmsg " + messageData.getId()).build()).sendMessage(reporter);
            reporter.sendMessage("");
            return;
        }


        messageData.setAlreadySignaled(true);
        reporter.sendMessage(Magnet.getPrefix() + "§aVotre signalement a bien été enregistré et envoyé à notre équipe !");
        reporter.sendMessage("     §c⚠ Notez qu'abuser de cette fonction est sanctionnable ⚠");

//        Bukkit.getOnlinePlayers().stream().filter(player1 -> player1.hasPermission("moderation.reports.see")).forEach(target -> {
//            target.sendMessage("§8(§c!§8) §7Le joueur §e" + messageData.getPlayerName() + "§7 a été signalé pour son message §a" + messageData.getMessage());
//        });

        try {
            //TODO trouver utf8 emojis
            magnet.getDiscordApi().apiDiscordWebhookNamePostAsync("ingame-reports", "**(!)** Le joueur `" + messageData.getPlayerName() + "` a été signalé pour son message " + messageData.getMessage() + " (par `" + reporter.getName() + "`)", new ApiCallback<Void>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    e.printStackTrace();
                }

                @Override
                public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
                }

                @Override
                public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                }
            });
        } catch (ApiException e) {
            e.printStackTrace();
        }

        try {
            magnet.getServerApi().apiServersBroadcastPostAsync(new Broadcast().message("§8(§c!§8) §7Le joueur §e" + messageData.getPlayerName() + "§7 a été signalé pour son message §a" + messageData.getMessage()).permission("moderation.reports.see"), new ApiCallback<Void>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    e.printStackTrace();
                }

                @Override
                public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
                }

                @Override
                public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                }
            });
        } catch (ApiException e) {
            e.printStackTrace();
        }

    }

    @Override
    public PlayerChatConsumer getChatProcessor() {
        return playerStringBiConsumer;
    }

    @Override
    public void setChatProcessor(PlayerChatConsumer playerChatConsumer) {
        playerStringBiConsumer = playerChatConsumer;
    }

    @Override
    public boolean isRankVisible() {
        return ranVisible;
    }

    @Override
    public void setRankVisible(boolean b) {
        ranVisible = b;
    }


    private PlayerChatConsumer playerStringBiConsumer = (player, message, id) -> {

        String prefix = ServerCacheHandler.ServerCacheHandlerStore.getServerCacheHandler().getInfo(player.getUniqueId()).getPrefix();

        String[] checkMention = message.split(" ");
        for (String word : checkMention) {
            Player target = Bukkit.getPlayerExact(word);
            if (target != null) {
                message = message.replaceFirst(word, "§6@" + word + "§f");
                target.playSound(target.getLocation(), Sound.valueOf("ORB_PICKUP"), 1.0F, 0.0F);
            }
        }

        InteractiveMessage interactiveMessage = new InteractiveMessage().add(
                        new TextComponentBuilder("§4Ⓒ ").setHoverMessage("§8§l▪ §cCliquez pour signaler le message du joueur §e" + player.getName())
                                .setClickAction(ClickEvent.Action.RUN_COMMAND, "/reportmsg " + id).build())
                .add((prefix == null ? "§7" : prefix) + player.getName() + " » §f" + message);

        Bukkit.getOnlinePlayers().forEach(interactiveMessage::sendMessage);
    };
}
