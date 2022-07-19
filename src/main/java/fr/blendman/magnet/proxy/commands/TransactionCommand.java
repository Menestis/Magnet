package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.proxy.VelocityMagnet;
import net.kyori.adventure.text.Component;

/**
 * @author Blendman974
 */
public class TransactionCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public TransactionCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player))
            return;

        if (!source.hasPermission("aspaku.transaction")) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous n'avez pas la permission nécéssaire"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 3) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous devez spécifier un pseudonyme et deux nombres"));
            source.sendMessage(Component.text(Magnet.getPrefix() + "Exemple : /transaction R2D2Nico_ 100 -10"));
            return;
        }
        int currency;
        int premiumCurrency;
        try {
            currency = Integer.parseInt(args[1]);
            premiumCurrency = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            source.sendMessage(Component.text("Arguments invalide !"));
            return;
        }

        source.sendMessage(Component.text("Récupération du joueur " + args[0]));

        velocityMagnet.getMagnet().getPlayerHandle().getPlayerInfo(args[0])
                .thenCompose(playerInfo -> velocityMagnet.getMagnet().getTransactionHandle().transaction(playerInfo.getUuid(), currency, premiumCurrency))
                .thenAccept(aBoolean -> {
                    if (aBoolean)
                        source.sendMessage(Component.text("Transaction terminée !"));
                    else {
                        source.sendMessage(Component.text("Transaction impossible (le joueur n'a probalement pas assez d'argent)"));
                    }
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    source.sendMessage(Component.text("Une erreur est survenue (le joueur existe-t-il ?)!"));
                    return null;
                });
    }


}
