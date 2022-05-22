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
public class SysCallCommand implements TabExecutor {

    private final ServerMagnet serverMagnet;

    public SysCallCommand(ServerMagnet magnet) {
        this.serverMagnet = magnet;
    }

    private final JexlEngine engine = new JexlBuilder().create();
    private final HashMap<Object, Object> context = new HashMap<>();

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


        String expression = String.join(" ", args);
        JexlExpression e = engine.createExpression(expression);

        JexlContext ctx = new MapContext();

        ctx.set("ctx", context);
        ctx.set("magnet", serverMagnet);
        ctx.set("serverMagner", serverMagnet.getMagnet());
        ctx.set("players", Bukkit.getOnlinePlayers().stream().collect(Collectors.toMap(HumanEntity::getName, o -> o)));
        ctx.set("server", Bukkit.getServer());
        ctx.set("self", player);
        try {
            Object evaluate = e.evaluate(ctx);
            if (evaluate != null) {
                player.sendMessage(evaluate.toString());
            }
        } catch (Exception exception) {
            player.sendMessage(exception.getMessage());
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
