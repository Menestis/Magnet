package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.server.ServerMagnet;
import org.apache.commons.jexl3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Blendman974
 */
public class VanishCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

    public VanishCommand(ServerMagnet magnet) {
        this.serverMagnet = magnet;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a Player can do that");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("magnet.system.admin")) {
            return false;
        }

//        player.hidePlayer();


        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
