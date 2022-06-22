package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.api.MagnetApi;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Ariloxe
 */
public class ReportMessCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 0)
            return true;

        Player player = ((Player) sender);
        MagnetApi.MagnetStore.getApi().getChatManager().reportPlayer(player, strings[0]);

        return true;
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
