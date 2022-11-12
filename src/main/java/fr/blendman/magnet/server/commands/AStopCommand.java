package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.skynet.models.ServerLoginPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Blendman974
 */
public class AStopCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

    public AStopCommand(ServerMagnet serverMagnet) {
        this.serverMagnet = serverMagnet;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a Player can do that");
            return true;
        }

        Player player = (Player) sender;
        ServerLoginPlayerInfo info = serverMagnet.getInfo(player.getUniqueId());

        if (!player.hasPermission("magnet.admin.servers.stop")) {
            sender.sendMessage(Magnet.getPrefix() + ChatColor.RED + "You don't have the required permissions");
            return true;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.kickPlayer("Server is stopping");
        }

        Bukkit.getScheduler().runTaskLater(ServerMagnet.getPlugin(ServerMagnet.class), () -> serverMagnet.getMagnet().stop().thenAccept(unused -> {
//            sender.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Demande d'arret envoyée !");
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
//            sender.sendMessage(Magnet.getPrefix() + ChatColor.RED + "La demande d'arret n'a pas pu aboutir !");
            return null;
        }), 20*5);


        return true;
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
