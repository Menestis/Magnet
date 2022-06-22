package fr.blendman.magnet.server.chat;

/**
 * @author Ariloxe
 */
public class MessageData {

    private final String id;
    private final String playerName;
    private final String message;
    private boolean alreadySignaled = false;

    public MessageData(String playerName, String message, String messageId){
        this.playerName = playerName;
        this.message = message;
        this.id = messageId;
    }

    public String getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAlreadySignaled() {
        return alreadySignaled;
    }

    public void setAlreadySignaled(boolean alreadySignaled) {
        this.alreadySignaled = alreadySignaled;
    }

}
