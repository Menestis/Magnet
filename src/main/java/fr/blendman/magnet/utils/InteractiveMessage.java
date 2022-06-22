package fr.blendman.magnet.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * @author Ariloxe
 */
public class InteractiveMessage {
    private TextComponent message = new TextComponent("");

    public InteractiveMessage add(TextComponent part) {
        this.message.addExtra((BaseComponent)part);
        return this;
    }

    public InteractiveMessage add(String part) {
        this.message.addExtra((BaseComponent)new TextComponent(part));
        return this;
    }

    public void sendMessage(Player... players) {
        Bukkit.getOnlinePlayers().stream().filter(player -> Arrays.stream(players).collect(Collectors.toList()).contains(player)).forEach(player -> player.spigot().sendMessage(this.message));
    }

    public void sendMessage(Player player){
        player.spigot().sendMessage(this.message);
    }

    public void sendToEveryone(){
        Bukkit.getOnlinePlayers().forEach(player -> player.spigot().sendMessage(message));
    }
}