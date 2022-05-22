package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.proxy.VelocityMagnet;
import net.kyori.adventure.text.Component;

import java.util.Optional;

/**
 * @author Blendman974
 */
public class HubCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public HubCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player) {
/*
            Optional<ServerConnection> currentServer = ((Player) source).getCurrentServer();
            if (currentServer.isPresent() && velocityMagnet.isServerALobbyByName(currentServer.get().getServerInfo().getName())) {
                source.sendMessage(Component.text(Magnet.getPrefix() + "Vous êtes déja au lobby"));
                return;
            }
*/

            source.sendMessage(Component.text(Magnet.getPrefix() + "Téléportation en cours"));

            velocityMagnet.getAvailableServerOfKind("lobby").thenCompose(server -> ((Player) source).createConnectionRequest(server).connect()).exceptionally(throwable -> {
                velocityMagnet.getLogger().error("Could not send player to lobby : ");
                throwable.printStackTrace();
                return null;
            });
        }
    }
}
