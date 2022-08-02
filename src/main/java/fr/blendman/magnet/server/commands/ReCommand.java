package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.skynet.models.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Blendman974
 */
public class ReCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

    public ReCommand(ServerMagnet magnet) {
        this.serverMagnet = magnet;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a Player can do that");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("magnet.system.re")) {
            return false;
        }

        Server server = serverMagnet.getMagnet().getServer();
        String enabled = serverMagnet.getMagnet().getRegistryValue("enable-re-command");
        if (enabled == null || !enabled.equals("true"))
            return false;

        serverMagnet.getMagnet().getPlayerHandle().movePlayerToServer(player.getUniqueId(), server.getKind()).thenAccept(s1 -> {
            if (s1.equals("Ok"))
                player.sendMessage("§3§lMenestis §f» §7Vous rejoignez la §efile d'attente§7...");
            else
                player.sendMessage("§3§lMenestis §f» §4Vous ne pouvez pas faire ça ici");
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });


//        player.hidePlayer();


        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
