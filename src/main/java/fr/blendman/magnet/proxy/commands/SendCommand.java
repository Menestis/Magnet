package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.MagnetApi;
import fr.blendman.magnet.proxy.VelocityMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Blendman974
 */
public class SendCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public SendCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return invocation.arguments().length > 1 ? new ArrayList<>(velocityMagnet.getAvailableServerNames()) : Collections.emptyList();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource commandSource = invocation.source();

        if (!commandSource.hasPermission("aspaku.send")) {
            commandSource.sendMessage(Component.text(Magnet.getPrefix() + "Vous n'avez pas la permission nécéssaire"));
            return;
        }

        if(invocation.arguments().length == 0) {
            commandSource.sendMessage(Component.text(Magnet.getPrefix() + "Usage : /send <player> (server-id/server-name)"));
            return;
        }

        UUID serverUuid;

        if (invocation.arguments().length == 1 && commandSource instanceof Player) {
            Optional<UUID> opt = velocityMagnet.getServer().getPlayer(((Player) commandSource).getUniqueId()).flatMap(Player::getCurrentServer).map(serverConnection -> velocityMagnet.getServerId(serverConnection.getServerInfo().getName()));
            if (!opt.isPresent()) {
                commandSource.sendMessage(Component.text(Magnet.getPrefix() + "You are not connected to a server"));
                return;
            }else {
                serverUuid = opt.get();
            }
        }else {
            try {
                serverUuid = UUID.fromString(invocation.arguments()[1]);
            }catch (IllegalArgumentException e){
                serverUuid = velocityMagnet.getServerId(invocation.arguments()[1]);
            }
        }

        if (serverUuid == null){
            commandSource.sendMessage(Component.text(Magnet.getPrefix() + "Server not found"));
            return;
        }

        if(commandSource instanceof Player){
            movePlayer(invocation.arguments()[0], serverUuid, commandSource);
        } else
            movePlayer(invocation.arguments()[0], serverUuid, null);
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
