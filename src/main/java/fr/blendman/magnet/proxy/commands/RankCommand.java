package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.proxy.VelocityMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerInfo;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Blendman974
 */
public class RankCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public RankCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player))
            return;

        if (!source.hasPermission("aspaku.rank")) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous n'avez pas la permission nécéssaire"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 2) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous devez spécifier un pseudonyme et un groupe"));
            source.sendMessage(Component.text(Magnet.getPrefix() + "Exemple : /rank R2D2Nico_ Lead"));
            return;
        }
        source.sendMessage(Component.text("Récupération du joueur " + args[0]));
        velocityMagnet.getMagnet().getPlayerHandle().getPlayerInfo(args[0]).thenCompose(playerInfo -> {

            CompletableFuture<Void> ret = new CompletableFuture<>();
            try {
                velocityMagnet.getMagnet().getPlayerApi().apiPlayersUuidGroupsUpdatePostAsync(playerInfo.getUuid(), Collections.singletonList(args[1]), new ApiCallBackToCompletableFuture<>(ret));
            } catch (ApiException e) {
                ret.completeExceptionally(e);
            }
            return ret.thenAccept(unused -> {
                source.sendMessage(Component.text("Groupe ajouté..."));
            });
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            source.sendMessage(Component.text("Une erreur est survenue !"));
            return null;
        });
    }


}
