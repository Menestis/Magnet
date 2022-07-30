package fr.blendman.magnet.server.commands;

import com.google.gson.Gson;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.EchoUserDefinition;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class EchoCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;
    private UUID key;
    private List<UUID> players = new ArrayList<>();

    public EchoCommand(ServerMagnet serverMagnet) {
        this.serverMagnet = serverMagnet;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a Player can do that");
            return true;
        }

        Player player = (Player) sender;


        if (!player.hasPermission("magnet.echo")) {
            sender.sendMessage(Magnet.getPrefix() + ChatColor.RED + "You don't have the required permissions");
            return true;
        }

        if (args.length != 1)
            return false;

        UUID serverId = serverMagnet.getMagnet().getServerId();

        if (args[0].equals("enable")) {
            CompletableFuture<UUID> ret = new CompletableFuture<>();

            try {
                sender.sendMessage("ID: " + serverId);
                serverMagnet.getMagnet().getEchoApi().apiServersUuidEchoEnableGetAsync(serverId, new ApiCallBackToCompletableFuture<>(ret));
            } catch (ApiException e) {
                ret.completeExceptionally(e);
            }

            ret.thenAccept(key -> {
                this.key = key;
                sender.sendMessage("Oui " + key);
                startTask();
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        } else {
            sender.sendMessage("" + serverId);
            CompletableFuture<Void> ret = new CompletableFuture<>();

            try {
                serverMagnet.getMagnet().getEchoApi().apiPlayersUuidEchoPostAsync(player.getUniqueId(), new EchoUserDefinition().server(serverId).ip(player.getAddress().getAddress().getHostAddress()), new ApiCallBackToCompletableFuture<>(ret));
            } catch (ApiException e) {
                ret.completeExceptionally(e);
            }

            ret.thenAccept(vo -> {
                sender.sendMessage("Feature gate started : " + player.getAddress().getAddress().getHostAddress());
                players.add(player.getUniqueId());
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        }


        return true;
    }

    private void startTask() {
        AsyncHttpClient client = Dsl.asyncHttpClient();
        BukkitRunnable runnable = new BukkitRunnable() {
            private HashMap<UUID, double[]> positions = new HashMap<>();

            @Override
            public void run() {
                if (key == null) cancel();
                for (UUID id : players) {
                    Player pl = Bukkit.getPlayer(id);
                    if (pl == null) {
                        continue;
                    }
                    Location l = pl.getLocation();
                    Vector direction = pl.getLocation().getDirection();
                    positions.put(pl.getUniqueId(), new double[]{l.getZ(), l.getX(), l.getY(), direction.getZ(), direction.getX(), direction.getY()});
                }

                Request req = new RequestBuilder(HttpConstants.Methods.POST)
                        //.setUrl("http://localhost:8888/positions")
                        .setUrl("http://echo.echo:8888/players/positions")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", key.toString())
                        .setBody(new Gson().toJson(positions))
                        .build();

                positions.clear();
                client.executeRequest(req, new AsyncCompletionHandler<Object>() {
                    @Override
                    public Object onCompleted(Response response) throws Exception {
                        return null;
                    }
                });
            }
        };
        runnable.runTaskTimer(serverMagnet, 1, 1);
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
