package fr.blendman.magnet.server.managers;

import fr.blendman.magnet.api.server.ChatManager;
import fr.blendman.magnet.api.server.ServerCacheHandler;
import fr.blendman.magnet.api.server.chat.PlayerChatConsumer;
import fr.blendman.magnet.server.chat.MessageData;
import fr.blendman.magnet.utils.InteractiveMessage;
import fr.blendman.magnet.utils.TextComponentBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author Ariloxe
 */
public class ServerChatManager implements ChatManager {


    int queueMaxSize = 50;
    int messageCount = 0;

    private final Deque<MessageData> messageQueue = new ArrayDeque<>();
    private final Map<String, MessageData> idMessageDataMap = new HashMap<>();

    private final Map<String, List<UUID>> confirmationMap = new HashMap<>();

    private boolean rankEnabled = true;


    public void onMessage(Player player, String message){
        messageCount++;
        String messageId = message.hashCode() + "-" + messageCount;
        MessageData messageData = new MessageData(player.getName(), message, messageId);

        if(messageQueue.size() >= queueMaxSize){
            MessageData oldMessageData = messageQueue.pop();
            idMessageDataMap.remove(oldMessageData.getId());
            confirmationMap.remove(oldMessageData.getId());
        }

        this.messageQueue.add(messageData);
        this.idMessageDataMap.put(messageId, messageData);
        this.confirmationMap.put(messageId, new ArrayList<>());

        this.playerStringBiConsumer.accept(player, message, messageId);
    }


    //TODO coucou faire ici le weebhook
    @Override
    public void reportPlayer(Player reporter, String string) {
        if(!idMessageDataMap.containsKey(string)){
            reporter.sendMessage("§3§lMenestis §f§l» §cCe message a malheureusement expiré..");
            return;
        }

        MessageData messageData = idMessageDataMap.get(string);
        if(messageData.isAlreadySignaled()){
            reporter.sendMessage("§3§lMenestis §f§l» §cCe message a déjà été signalé !");
            return;
        }

        if(confirmationMap.containsKey(string) && !confirmationMap.get(string).contains(reporter.getUniqueId())){
            reporter.sendMessage("");
            reporter.sendMessage("§8§l■ §7Souhaitez-vous vraiment signaler ce message de §e" + messageData.getPlayerName() + "§7 ?");
            reporter.sendMessage("§8➥ §f" + messageData.getMessage());
            reporter.sendMessage("");
            new InteractiveMessage().add(new TextComponentBuilder("§8§l■ §aCliquez ici pour confirmer").setHoverMessage("§8- §aCliquez pour confirmer le signalement §8-").setClickAction(ClickEvent.Action.RUN_COMMAND, "/reportmess " + messageData.getId()).build()).sendMessage(reporter);
            reporter.sendMessage("");
            return;
        }


        //TODO changer la permission
        messageData.setAlreadySignaled(true);
        reporter.sendMessage("§3§lMenestis §f§l» §aVotre signalement a bien été enregistré et envoyé à notre équipe !");
        reporter.sendMessage("     §c⚠ Notez qu'abuser de cette fonction est sanctionnable ⚠");

        Bukkit.getOnlinePlayers().stream().filter(player1 -> player1.hasPermission("modo.use")).forEach(target -> {
            target.sendMessage("§8(§c!§8) §7Le joueur §e" + messageData.getPlayerName() + "§7 a été signalé pour son message §a" + messageData.getMessage());

        });
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
    public boolean isRankEnabled() {
        return rankEnabled;
    }

    @Override
    public void setRankEnabled(boolean b) {
        rankEnabled = b;
    }



    private PlayerChatConsumer playerStringBiConsumer = new PlayerChatConsumer() {
        @Override
        public void accept(Player player, String message, String id) {

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
                                    .setClickAction(ClickEvent.Action.RUN_COMMAND, "/reportmess " + id).build())
                    .add((prefix) + player.getName() + " » §f" + message);

            Bukkit.getOnlinePlayers().forEach(interactiveMessage::sendMessage);
        }
    };
}
