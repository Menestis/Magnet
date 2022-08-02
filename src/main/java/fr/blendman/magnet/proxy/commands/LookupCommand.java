package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.proxy.VelocityMagnet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * @author Blendman974
 */
public class LookupCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public LookupCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player))
            return;

        if (!source.hasPermission("aspaku.lookup")) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous n'avez pas la permission nécéssaire"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 1) {
            source.sendMessage(Component.text(Magnet.getPrefix() + "Vous devez spécifier un pseudonyme"));
            source.sendMessage(Component.text(Magnet.getPrefix() + "Exemple : /lookup R2D2Nico_"));
            return;
        }

        source.sendMessage(Component.text("Récupération du joueur " + args[0]));

        velocityMagnet.getMagnet().getPlayerHandle().getPlayerInfo(args[0])
                .thenAccept(info -> {
                    source.sendMessage(Component.text("UUID: ").append(Component.text(info.getUuid().toString()).color(NamedTextColor.GRAY)));
                    source.sendMessage(Component.text("Power: ").append(Component.text(info.getPower()).color(NamedTextColor.GRAY)));
                    source.sendMessage(Component.text("Locale: ").append(Component.text(info.getLocale()).color(NamedTextColor.GRAY)));
                    source.sendMessage(Component.text("Prefix: ").append(Component.text(info.getPrefix() == null ? "None" : info.getPrefix())));
                    source.sendMessage(Component.text("Suffix: ").append(Component.text(info.getSuffix() == null ? "None" : info.getSuffix())));
                    source.sendMessage(Component.text("Currency: ").append(Component.text(info.getCurrency() == null ? 0 : info.getCurrency()).color(NamedTextColor.GRAY)));
                    source.sendMessage(Component.text("PremiumCurrency: ").append(Component.text(info.getPremiumCurrency() == null ? 0 : info.getPremiumCurrency()).color(NamedTextColor.GRAY)));
                    source.sendMessage(Component.text("Ban: ").append(Component.text(info.getBan() == null ? "None" : info.getBan().toString()).color(NamedTextColor.GRAY)));
                    source.sendMessage(Component.text("Proxy/Server: ").append(Component.text((info.getProxy() == null ? "None" : info.getProxy().toString()) + " / " + (info.getServer() == null ? "None" : info.getServer().toString())).color(NamedTextColor.GRAY)));
                    source.sendMessage(Component.text("Inventory: "));
                    info.getInventory().forEach((s, integer) -> source.sendMessage(Component.text("- " + s + ": ").append(Component.text(integer).color(NamedTextColor.GRAY))));
                }).exceptionally(throwable -> {
                    throwable.printStackTrace();
                    source.sendMessage(Component.text("Une erreur est survenue (le joueur existe-t-il ?)!"));
                    return null;
                });
    }


}
