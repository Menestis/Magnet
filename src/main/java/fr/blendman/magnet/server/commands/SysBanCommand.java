package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerBan;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class SysBanCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

    public SysBanCommand(ServerMagnet magnet) {
        this.serverMagnet = magnet;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!sender.hasPermission("magnet.system.admin.sysban")) {
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage("Non");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null)
            return true;


        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            serverMagnet.getMagnet().getPlayerApi().apiPlayersUuidBanPostAsync(player.getUniqueId(), new PlayerBan().reason("Automated ban (Anticheat)"), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        ret.thenAccept(unused -> {
            System.out.println("Auto banned " + player.getUniqueId() + " " + player.getName());
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });


        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
