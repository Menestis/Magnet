package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.handles.EchoHandle;
import fr.blendman.magnet.server.ServerMagnet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Blendman974
 */
public class EchoCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

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

        if (args.length < 1)
            return false;

        if (args[0].equals("enable")) {
            serverMagnet.getEchoHandle().enableFeature().thenAccept(voir -> {
                player.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "Echo feature gate enabled, stop the server to disable it");
                serverMagnet.getEchoHandle().addEchoEventListener("test", (uuid, echoEvent) -> {
                    Player pl = Bukkit.getPlayer(uuid);
                    Bukkit.broadcastMessage(Magnet.getPrefix() + ChatColor.GREEN + "Echo event: " + echoEvent + " from " + (pl == null ? "null" : pl.getName()));
                    if (echoEvent.equals(EchoHandle.EchoEvent.Status)){
                        EchoHandle.EchoPlayerStatus playerStatus = serverMagnet.getEchoHandle().getPlayerStatus(uuid);
                        Bukkit.broadcastMessage(Magnet.getPrefix() + ChatColor.GREEN + "Player status: " + playerStatus);
                    }
                });
            }).exceptionally(e -> {
                player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Error while enabling echo feature gate");
                e.printStackTrace();
                return null;
            });
            return true;
        } else if (args[0].equals("start")) {
            serverMagnet.getEchoHandle().addPlayer(player.getUniqueId()).thenAccept(code -> {
                player.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "Echo player started");
                player.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "Code: " + code);
            }).exceptionally(e -> {
                player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Error: " + e.getMessage());
                e.printStackTrace();
                return null;
            });
            return true;
        }else if (args[0].equals("test")){
            Location l = player.getLocation();
            Vector direction = player.getLocation().getDirection();
            serverMagnet.getEchoHandle().startVirtualAudio(UUID.randomUUID(), "test", new double[]{l.getZ(), l.getX(), l.getY(), direction.getZ(), direction.getX(), direction.getY()},"world", false);
        }


        if (args.length < 2)
            return false;

        Player target = Bukkit.getPlayer(args[1]);
        switch (args[0]) {
            case "mute":
                if (target == null) {
                    player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Player not found");
                    return true;
                }
                serverMagnet.getEchoHandle().setMuted(target.getUniqueId(), true);
                target.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "You have been muted");
                player.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "Player" + target.getName() + "muted");
                return true;
            case "unmute":
                if (target == null) {
                    player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Player not found");
                    return true;
                }
                serverMagnet.getEchoHandle().setMuted(target.getUniqueId(), false);
                target.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "You have been unmuted");
                player.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "Player" + target.getName() + "unmuted");
                return true;
            case "deafen":
                if (target == null) {
                    player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Player not found");
                    return true;
                }
                serverMagnet.getEchoHandle().setDeafen(target.getUniqueId(), true);
                target.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "You have been deafened");
                player.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "Player" + target.getName() + "deafened");
                return true;
            case "undeafen":
                if (target == null) {
                    player.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Player not found");
                    return true;
                }
                serverMagnet.getEchoHandle().setDeafen(target.getUniqueId(), false);
                target.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "You have been undeafened");
                player.sendMessage(Magnet.getPrefix() + ChatColor.GREEN + "Player" + target.getName() + "undeafened");
                return true;
        }


        return true;
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
