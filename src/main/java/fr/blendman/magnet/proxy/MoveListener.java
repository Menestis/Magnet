package fr.blendman.magnet.proxy;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;

import java.net.InetSocketAddress;

/**
 * @author Blendman974
 */
public class MoveListener {
    private final VelocityMagnet velocityMagnet;

    public MoveListener(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }

    @Subscribe
    public EventTask onInitialLogin(PlayerChooseInitialServerEvent event) {
        String vhost = event.getPlayer().getVirtualHost().map(InetSocketAddress::getHostString)
                .orElse(null);

        if (vhost != null && velocityMagnet.getForcedHost(vhost) != null) {
            System.out.println("Sending player to forced host : " + vhost);
            event.setInitialServer(velocityMagnet.getForcedHost(vhost));
            return null;
        }

        return EventTask.resumeWhenComplete(velocityMagnet.getAvailableServerOfKind("lobby").thenAccept(event::setInitialServer).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        }));
    }

    @Subscribe
    public EventTask onKick(KickedFromServerEvent event) {
        if (event.kickedDuringServerConnect()) {
            return null;
        }
        return EventTask.resumeWhenComplete(velocityMagnet.getAvailableServerOfKind("lobby").thenAccept(server -> {
            if (!event.getServer().getServerInfo().getName().equals(server.getServerInfo().getName())) {
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(server));
            }
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        }));
    }
}
