package fr.blendman.magnet.server.commands;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.api.server.players.ServerLoginPlayerInfo;
import fr.blendman.magnet.server.ServerMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class LinkCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

    public LinkCommand(ServerMagnet serverMagnet) {
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

        if (info.getDiscordId() != null && (args.length == 0 || !args[0].equals("relink"))) {
            sender.sendMessage(Magnet.getPrefix() + ChatColor.RED + "Votre compte discord est déjà relié (/link relink pour lier de nouveau votre compte) !");
        }

        CompletableFuture<String> ret = new CompletableFuture<>();

        try {
            serverMagnet.getMagnet().getDiscordApi().apiDiscordLinkUuidGetAsync(player.getUniqueId(), new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }

        ret.thenAccept(code -> {
            sender.sendMessage(Magnet.getPrefix() + "§7Votre code de liaison est: §b" + code + " §7!");
            sender.sendMessage(Magnet.getPrefix() + "§7Utilisez la commande §b/link §l" + code + "§r§7 sur notre discord afin de completer votre profil !");

        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });

        return true;
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
