package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.api.MagnetApi;
import fr.blendman.magnet.server.ServerMagnet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Ariloxe
 */
public class ReportMessageCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

    public ReportMessageCommand(ServerMagnet serverMagnet) {

        this.serverMagnet = serverMagnet;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0)
            return true;

        Player player = ((Player) sender);
        serverMagnet.getChatManager().reportPlayer(player, strings[0]);

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
