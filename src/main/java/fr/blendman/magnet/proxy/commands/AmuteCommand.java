package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.proxy.VelocityMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerMute;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class AmuteCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public AmuteCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player))
            return;

        if (!source.hasPermission("aspaku.mute")) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous n'avez pas la permission nécéssaire"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 2) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous devez spécifier un pseudonyme, une durée et une optionellement une raison"));
            source.sendMessage(Component.text(Magnet.getPrefix() + "Exemple : /amute R2D2Nico_ 1y toutes les raisons du monde"));
            return;
        }

        int duration = parseDuration(args[1]);
        boolean umute = false;
        boolean perm = false;
        if (duration == -5) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Durée invalide !"));
            return;
        } else if (duration == 0) {
            umute = true;
        } else if (duration == -1) {
            perm = true;
        }

        String message = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;

        boolean finalUmute = umute;
        boolean finalPerm = perm;
        velocityMagnet.getMagnet().getPlayerInfo(args[0]).thenCompose(toBan ->
                velocityMagnet.getMagnet().getPlayerInfo(((Player) source).getUniqueId().toString()).thenCompose(self -> {

                    if (self.getPower() <= toBan.getPower()) {
                        source.sendMessage(Component.text(Magnet.getPrefix() + "Vous n'avez pas un pouvoir de mute suffisant !"));
                        return CompletableFuture.completedFuture(null);
                    } else {
                        PlayerMute mute = new PlayerMute().issuer(self.getUuid());
                        if (message != null)
                            mute.reason(message);
                        if (finalUmute)
                            mute.unban(true);
                        if (!finalPerm)
                            mute.duration(duration);

                        CompletableFuture<Void> ret = new CompletableFuture<>();
                        try {
                            velocityMagnet.getMagnet().getPlayerApi().apiPlayersUuidMutePostAsync(toBan.getUuid(), mute, new ApiCallBackToCompletableFuture<>(ret));
                        } catch (ApiException e) {
                            ret.completeExceptionally(e);
                        }
                        return ret;
                    }


                }).thenAccept(unused -> {
                    source.sendMessage(Component.text(Magnet.getPrefix() + "Le joueur " + args[0] + " a été " + (finalUmute ? "unmute" : "mute")));
                }).exceptionally(throwable -> {
                    source.sendMessage(Component.text(Magnet.getPrefix() + "Impossible de charger vos informations !"));
                    throwable.printStackTrace();
                    return null;
                })).exceptionally(throwable -> {
            if (throwable instanceof ApiException && ((ApiException) throwable).getCode() == 404) {
                source.sendMessage(Component.text(Magnet.getPrefix() + "Ce joueur n'existe pas !"));
            } else {
                throwable.printStackTrace();
                source.sendMessage(Component.text(Magnet.getPrefix() + "Une erreur est survenue"));
            }
            return null;
        }).exceptionally(throwable -> {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Error !"));
            throwable.printStackTrace();
            return null;
        });
    }

    private int parseDuration(String duration) {
        if (duration.equals("perm") || duration.equals("p")) {
            return -1;
        }
        try {
            if (duration.endsWith("s")) {
                return Integer.parseInt(duration.replace("s", ""));
            } else if (duration.endsWith("m")) {
                return Integer.parseInt(duration.replace("m", "")) * 60;
            } else if (duration.endsWith("h")) {
                return Integer.parseInt(duration.replace("h", "")) * 60 * 60;
            } else if (duration.endsWith("d")) {
                return Integer.parseInt(duration.replace("d", "")) * 60 * 60 * 24;
            } else if (duration.endsWith("M")) {
                return Integer.parseInt(duration.replace("M", "")) * 60 * 60 * 24 * 31;
            } else if (duration.endsWith("y")) {
                return Integer.parseInt(duration.replace("y", "")) * 60 * 60 * 24 * 365;
            } else {
                return Integer.parseInt(duration);
            }
        } catch (NumberFormatException e) {
            return -5;
        }
    }

}
