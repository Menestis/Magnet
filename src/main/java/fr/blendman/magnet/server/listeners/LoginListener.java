package fr.blendman.magnet.server.listeners;

import fr.blendman.magnet.api.server.events.PlayerInfoReadyEvent;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.api.LoginApi;
import fr.blendman.skynet.api.ServerApi;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerStats;
import fr.blendman.skynet.models.ServerLoginPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class LoginListener implements Listener {
    private final ServerMagnet serverMagnet;
    private final ServerApi serverApi;
    private final LoginApi loginApi;
    private BukkitTask idlingTask;

    public LoginListener(ServerMagnet serverMagnet) {
        this.serverMagnet = serverMagnet;
        serverApi = serverMagnet.getMagnet().getServerApi();
        loginApi = serverMagnet.getMagnet().getLoginApi();
        tryStartIdlingTask(null);
    }


    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (idlingTask != null) {
            System.out.println("Cancelling idling task");
            idlingTask.cancel();
            idlingTask = null;
        }

        if (Bukkit.hasWhitelist()) {
            if (serverMagnet.shouldBeWhitelisted(event.getPlayer().getUniqueId())) {
                System.out.println("Whitelisted player " + event.getPlayer().getUniqueId());
                event.getPlayer().setWhitelisted(true);
                event.setResult(PlayerLoginEvent.Result.ALLOWED);
            }
        }

        CompletableFuture<ServerLoginPlayerInfo> ret = new CompletableFuture<>();

        try {
            loginApi.apiPlayersUuidLoginPostAsync(event.getPlayer().getUniqueId(), serverMagnet.getMagnet().getServerId(), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            e.printStackTrace();
            return;
        }

        ret.thenAccept(serverLoginPlayerInfo -> processLoginInfo(event.getPlayer(), serverLoginPlayerInfo)).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });

    }

    private void processLoginInfo(Player player, ServerLoginPlayerInfo info) {

        if (!player.isOnline()) {
            System.out.println("Player disconnected before getting cached");
            return;
        }

        serverMagnet.storeInfo(player.getUniqueId(), info);

        String prefix = info.getPrefix() == null ? "" : info.getPrefix();
        String suffix = info.getSuffix() == null ? "" : info.getSuffix();
        player.setDisplayName(prefix + player.getDisplayName() + suffix);

        PermissionAttachment perms = player.addAttachment(serverMagnet);
        for (String p : info.getPermissions()) {
            if (p.equals("*")) {
                player.setOp(true);
            } else if (p.startsWith("-"))
                perms.setPermission(p.replaceFirst("-", ""), false);
            else
                perms.setPermission(p, true);
        }

        Bukkit.getPluginManager().callEvent(new PlayerInfoReadyEvent(info, player, false));
    }

    public void tryStartIdlingTask(UUID ignore) {
        if (idlingTask != null)
            return;
        if (Bukkit.getOnlinePlayers().stream().noneMatch(player -> player.getUniqueId() != ignore)) {
            serverMagnet.getLogger().info("Started idling task");
            idlingTask = Bukkit.getScheduler().runTaskLater(serverMagnet, () -> serverMagnet.getMagnet().setServerState("Idle")
                    .thenAccept(unused -> serverMagnet.getLogger().info("Server state is now : Idle"))
                    .exceptionally(throwable -> {
                        throwable.printStackTrace();
                        return null;
                    }), 20 * 60 * 5);
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tryStartIdlingTask(event.getPlayer().getUniqueId());

        ServerLoginPlayerInfo info = serverMagnet.removeInfo(event.getPlayer().getUniqueId());
        if (info == null)
            return;

        CompletableFuture<Void> ret = new CompletableFuture<>();
        Map<String, Integer> stats = getStats(event.getPlayer());
        try {
            serverMagnet.getMagnet().getPlayerApi().apiPlayersUuidStatsPostAsync(event.getPlayer().getUniqueId(), new PlayerStats().server(serverMagnet.getMagnet().getServerId()).session(info.getSession()).stats(stats), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            e.printStackTrace();
        }

        ret.thenAccept(unused -> {

        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    private Map<String, Integer> getStats(Player pl) {
        HashMap<String, Integer> map = new HashMap<>();

        for (Statistic stat : Statistic.values()) {
            switch (stat.getType()) {
                case ITEM:
                case BLOCK:
                    for (Material m : Material.values()) {
                        try {
                            int s = pl.getStatistic(stat, m);
                            if (s != 0) {
                                map.put(stat.name() + "_" + m.name(), s);
                                pl.setStatistic(stat, m, 0);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    break;
                case ENTITY:
                    for (EntityType m : EntityType.values()) {
                        try {
                            int s = pl.getStatistic(stat, m);
                            if (s != 0) {
                                map.put(stat.name() + "_" + m.name(), s);
                                pl.setStatistic(stat, m, 0);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    break;
                case UNTYPED:
                    int s = pl.getStatistic(stat);
                    if (s != 0) {
                        map.put(stat.name(), s);
                        pl.setStatistic(stat, 0);
                    }
                    break;
            }
        }
        return map;
    }

}
