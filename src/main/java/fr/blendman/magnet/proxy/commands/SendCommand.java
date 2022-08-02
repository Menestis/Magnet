package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.MagnetApi;
import fr.blendman.magnet.proxy.VelocityMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class SendCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public SendCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource commandSource = invocation.source();


        if(invocation.arguments().length != 2) {
            commandSource.sendMessage(Component.text(Magnet.getPrefix() + "Usage : /send <player> <server-id/here>"));
            return;
        }


        if(commandSource instanceof Player){
            if (!commandSource.hasPermission("aspaku.send")) {
                commandSource.sendMessage(Component.text(Magnet.getPrefix() + "Vous n'avez pas la permission nécéssaire"));
                return;
            }

            movePlayer(invocation.arguments()[0], UUID.fromString(invocation.arguments()[1]), commandSource);
        } else
            movePlayer(invocation.arguments()[0], UUID.fromString(invocation.arguments()[1]), null);
    }

    private void movePlayer(String playerName, UUID serverUuid, CommandSource player){
        velocityMagnet.getMagnet().getPlayerHandle().getPlayerInfo(playerName).thenAccept(playerInfo -> {
            UUID uuid = playerInfo.getUuid();
            MagnetApi.MagnetStore.getApi().getPlayerHandle().movePlayerToServer(uuid, serverUuid, true).thenAccept(s -> {
                switch (s) {
                    case "Ok":
                        if(player == null)
                            velocityMagnet.getLogger().warn("Erreur : Le joueur " + playerName + " a bien été déplacé sur le serveur " + serverUuid.toString());
                        else
                            player.sendMessage(Component.text(Magnet.getPrefix() + "§7Vous avez déplacé §a" + playerName + "§7 sur le serveur §e" + serverUuid.toString()));
                        break;
                    case "PlayerOffline":
                        if(player == null)
                            velocityMagnet.getLogger().warn("Erreur : Le joueur " + playerName + " est déconnecté");
                        else
                            player.sendMessage(Component.text(Magnet.getPrefix() + "§cErreur : Le joueur §e" + playerName + "§c n'est pas en ligne."));
                    case "Failed":
                    case "MissingServer":
                    case "MissingServerKind":
                        if(player == null)
                            velocityMagnet.getLogger().warn("Erreur : Le joueur " + playerName + " n'a pas pu être déplacé sur le serveur pour une raison inconnue. (" + s + ")");
                        else
                            player.sendMessage(Component.text(Magnet.getPrefix() + "§cErreur : Une erreur inconnue est survenue, merci de réesayer ultérieurement. (" + s + ")"));
                        break;
                    case "UnlinkedPlayer":
                        if(player == null)
                            velocityMagnet.getLogger().warn("Erreur : Le joueur " + playerName + " n'est pas lié à un compte Discord");
                        else
                            player.sendMessage(Component.text(Magnet.getPrefix() + "§cErreur : Le joueur §e" + playerName + "§c n'est pas lié à Discord."));
                        break;
                    default:
                        break;
                }


            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }


}
