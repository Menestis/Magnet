package fr.blendman.magnet.proxy;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.event.player.PlayerModInfoEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import fr.blendman.magnet.api.handles.messenger.events.DisconnectPlayerEvent;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.magnet.utils.ProxyUtils;
import fr.blendman.skynet.api.LoginApi;
import fr.blendman.skynet.api.ProxyApi;
import fr.blendman.skynet.client.ApiCallback;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.*;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Blendman974
 */
public class LoginListener {
    private final VelocityMagnet velocityMagnet;
    private final ProxyApi proxyApi;
    private final LoginApi loginApi;

    private Map<UUID, ProxyLoginPlayerInfo> playerInfos = new HashMap<>();
    private Map<UUID, UUID> sessions = new HashMap<>();

    public LoginListener(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
        proxyApi = velocityMagnet.getMagnet().getProxyApi();
        loginApi = velocityMagnet.getMagnet().getLoginApi();
    }

    @Subscribe
    public EventTask onPreLogin(PreLoginEvent event) {
        CompletableFuture<ProxyPreLoginResponse> ret = new CompletableFuture<>();

        String address = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
        try {
            loginApi.apiPlayersIpProxyPreloginGetAsync(address, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            e.printStackTrace();
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Une erreur est survenue")));
            return null;
        }

        return EventTask.resumeWhenComplete(ret.thenAccept(preLoginResponse -> {
            if (preLoginResponse.getResult() == ProxyPreLoginResponse.ResultEnum.DENIED) {
                Component reason = ProxyUtils.messageToComponent(preLoginResponse.getMessage());
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(reason));
            }
        }));
    }


    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        CompletableFuture<ProxyLoginResponse> ret = new CompletableFuture<>();

        Player player = event.getPlayer();
        try {
            loginApi.apiPlayersUuidProxyLoginPostAsync(player.getUniqueId(), new ProxyLoginRequest()
                    .ip(player.getRemoteAddress().getAddress().getHostAddress())
                    .username(player.getUsername())
                    .proxy(velocityMagnet.getMagnet().getServerId())
                    .locale(player.getPlayerSettings().getLocale().toString())
                    .version(player.getProtocolVersion().toString()), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            e.printStackTrace();
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("Une erreur est survenue")));
            return null;
        }
        return EventTask.resumeWhenComplete(ret.thenAccept(proxyLoginResponse -> {
            if (proxyLoginResponse.getResult() == ProxyLoginResponse.ResultEnum.DENIED) {
                Component reason = ProxyUtils.messageToComponent(proxyLoginResponse.getMessage());
                event.setResult(ResultedEvent.ComponentResult.denied(reason));
            } else
                processLogin(player, proxyLoginResponse);
        }));
    }


    private void processLogin(Player player, ProxyLoginResponse proxyLoginResponse) {
        velocityMagnet.getLogger().info("Initialized " + player.getUsername() + " session : " + proxyLoginResponse.getSession());
        ProxyLoginPlayerInfo playerInfo = proxyLoginResponse.getPlayerInfo();

        //playerInfo.getLocale() //TODO
        this.playerInfos.put(player.getUniqueId(), playerInfo);
        this.sessions.put(player.getUniqueId(), proxyLoginResponse.getSession());
    }

    @Subscribe
    private EventTask onPostLogin(PostLoginEvent event){
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            velocityMagnet.getMagnet().getProxyApi().apiProxyUuidPlayercountPostAsync(velocityMagnet.getMagnet().getServerId(), velocityMagnet.getServer().getPlayerCount(), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return EventTask.resumeWhenComplete(ret);
    }

    @Subscribe
    private EventTask onDisconnect(DisconnectEvent event) throws ApiException {
        playerInfos.remove(event.getPlayer().getUniqueId());
        UUID session = sessions.remove(event.getPlayer().getUniqueId());
        if (session == null)
            return null;

        velocityMagnet.getLogger().info("Closing " + event.getPlayer().getUsername() + " (" + event.getPlayer().getUniqueId() + ")" + " session");
        CompletableFuture<Void> ret = new CompletableFuture<>();

        velocityMagnet.getMagnet().getSessionApi().apiPlayersUuidSessionDeleteAsync(event.getPlayer().getUniqueId(), new ApiCallBackToCompletableFuture<>(ret));

        return EventTask.resumeWhenComplete(ret.thenCompose(unused -> {
            CompletableFuture<Void> ret2 = new CompletableFuture<>();
            try {
                velocityMagnet.getMagnet().getProxyApi().apiProxyUuidPlayercountPostAsync(velocityMagnet.getMagnet().getServerId(), velocityMagnet.getServer().getPlayerCount(), new ApiCallBackToCompletableFuture<>(ret));
            } catch (ApiException e) {
                ret2.completeExceptionally(e);
            }
            return ret2;
        }));
    }

    @Subscribe
    private EventTask onPing(ProxyPingEvent event) throws ApiException {
        CompletableFuture<PingInfo> ret = new CompletableFuture<>();

        proxyApi.apiProxyPingGetAsync(new ApiCallBackToCompletableFuture<>(ret));

        return EventTask.resumeWhenComplete(ret.thenAccept(pingInfo -> event.setPing(ServerPing.builder().version(event.getPing().getVersion()).onlinePlayers(pingInfo.getOnline()).description(Component.text(pingInfo.getMotd())).maximumPlayers(pingInfo.getSlots()).build())));
    }


    @Subscribe
    public void onPermissionSetup(PermissionsSetupEvent event) {
        event.setProvider(this::createPermissionFunction);
    }


    @Subscribe
    public EventTask onClientBrand(PlayerClientBrandEvent event) {
        if (this.sessions.containsKey(event.getPlayer().getUniqueId())) {
            CompletableFuture<Void> ret = new CompletableFuture<>();

            try {
                loginApi.apiSessionsSessionClientbrandPostAsync(sessions.get(event.getPlayer().getUniqueId()), event.getBrand(), new ApiCallBackToCompletableFuture<>(ret));
            } catch (ApiException e) {
                ret.completeExceptionally(e);
            }

            return EventTask.resumeWhenComplete(ret);
        } else {
            velocityMagnet.getLogger().warn("No session for client brand info : " + event.getPlayer().getUniqueId());
            return null;

        }
    }

    @Subscribe
    public EventTask onModInfo(PlayerModInfoEvent event) {
        if (this.sessions.containsKey(event.getPlayer().getUniqueId())) {
            CompletableFuture<Void> ret = new CompletableFuture<>();


            try {
                loginApi.apiSessionsSessionModsinfoPostAsync(sessions.get(event.getPlayer().getUniqueId()),
                        event.getModInfo().getMods().stream()
                                .map(mod -> new fr.blendman.skynet.models.ModInfo().id(mod.getId()).version(mod.getVersion()))
                                .collect(Collectors.toList()), new ApiCallBackToCompletableFuture<>(ret));
            } catch (ApiException e) {
                ret.completeExceptionally(e);
            }

            return EventTask.resumeWhenComplete(ret);
        } else {
            velocityMagnet.getLogger().warn("No session for client mod info : " + event.getPlayer().getUniqueId());
            return null;

        }
    }


    public PermissionFunction createPermissionFunction(PermissionSubject subject) {
        if (!(subject instanceof Player)) {
            return permission -> Tristate.FALSE;
        } else {

            return permission -> {
                ProxyLoginPlayerInfo info = playerInfos.get(((Player) subject).getUniqueId());
                if (info != null && info.getPermissions().contains(permission))
                    return Tristate.TRUE;
                else
                    return Tristate.FALSE;
            };
        }
    }

}
