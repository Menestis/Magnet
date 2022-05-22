package fr.blendman.magnet.server;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.MagnetSide;
import fr.blendman.magnet.api.handles.messenger.events.AdminMovePlayerEvent;
import fr.blendman.magnet.api.handles.messenger.events.InvalidatePlayerEvent;
import fr.blendman.magnet.api.server.ServerCacheHandler;
import fr.blendman.magnet.server.commands.AStopCommand;
import fr.blendman.magnet.server.commands.SysCallCommand;
import fr.blendman.magnet.server.listeners.BukkitCommandPreProcessor;
import fr.blendman.magnet.server.listeners.LoginListener;
import fr.blendman.magnet.server.listeners.NetworkListener;
import fr.blendman.magnet.server.managers.AnticheatManager;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import fr.blendman.skynet.models.PlayerMove;
import fr.blendman.skynet.models.Server;
import fr.blendman.skynet.models.ServerLoginPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class ServerMagnet extends JavaPlugin implements ServerCacheHandler {
    private Magnet magnet;
    private final Map<UUID, ServerLoginPlayerInfo> infos = new HashMap<>();
    private final List<UUID> whitelist = new ArrayList<>();

    @Override
    public void onLoad() {
        getLogger().info("Loading Velocity Magnet !");
        ServerCacheHandler.ServerCacheHandlerStore.setServerCacheHandler(this);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {
        try {
            magnet = new Magnet(MagnetSide.SERVER);
            processKindCompat(magnet.getServer());
            NetworkListener networkListener = new NetworkListener(this);
            magnet.getMessenger().registerListener(networkListener::onAdminMove, AdminMovePlayerEvent.class);
            magnet.getMessenger().registerListener(networkListener::onPlayerInvalidation, InvalidatePlayerEvent.class);

        } catch (Exception e) {
            e.printStackTrace();
            getServer().shutdown();
        }
        Bukkit.getPluginManager().registerEvents(new LoginListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AnticheatManager(), this);
        Bukkit.getPluginManager().registerEvents(new BukkitCommandPreProcessor(this), this);
        registerCommands();
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                handlePropertiesActions();
            } catch (ApiException e) {
                e.printStackTrace();
            }
        });
    }

    private void processKindCompat(Server server) {
        if (server.getProperties() != null && server.getProperties().containsKey("startingstate")) {
            magnet.setServerState(server.getProperties().get("startingstate"));
        }
    }

    private void registerCommands() {
        registerCommand(new AStopCommand(this), "astop");
        registerCommand(new SysCallCommand(this), "syscall");
    }

    private void registerCommand(TabExecutor cmd, String cmdName) {
        PluginCommand pluginCommand = Bukkit.getPluginCommand(cmdName);
        assert pluginCommand != null;
        pluginCommand.setExecutor(cmd);
        pluginCommand.setTabCompleter(cmd);
    }

    public Magnet getMagnet() {
        return magnet;
    }

    public ServerLoginPlayerInfo removeInfo(UUID uniqueId) {
        return this.infos.remove(uniqueId);
    }

    public void storeInfo(UUID uniqueId, ServerLoginPlayerInfo info) {
        this.infos.put(uniqueId, info);
    }

    public fr.blendman.magnet.api.server.ServerLoginPlayerInfo getInfo(UUID uniqueId) {
        return fromServerInfo(this.infos.get(uniqueId));
    }

    public ServerLoginPlayerInfo getRawInfo(UUID uuid) {
        return this.infos.get(uuid);
    }

    public fr.blendman.magnet.api.server.ServerLoginPlayerInfo fromServerInfo(ServerLoginPlayerInfo info) {
        if (info == null)
            return null;
        return new fr.blendman.magnet.api.server.ServerLoginPlayerInfo(info.getSession(), info.getProxy(), info.getPrefix(), info.getSuffix(), info.getLocale(), info.getPermissions(), info.getPower(), info.getCurrency(), info.getPremiumCurrency(), info.getBlocked(), info.getInventory(), info.getProperties());
    }

    public boolean shouldBeWhitelisted(UUID uuid) {
        return whitelist.remove(uuid);
    }

    private void handlePropertiesActions() throws ApiException {
        Map<String, String> properties = magnet.getServer().getProperties();
        if (properties == null)
            return;

        if (properties.containsKey("host")) {
            UUID host = UUID.fromString(properties.get("host"));
            Bukkit.setWhitelist(true);
            whitelist.add(host);
            CompletableFuture<Boolean> ret = new CompletableFuture<>();
            getMagnet().getPlayerApi().apiPlayersUuidMovePostAsync(host, new PlayerMove().server(getMagnet().getServerId()), new ApiCallBackToCompletableFuture<>(ret));
            ret.thenAccept(sucess -> {
                getLogger().info("Teleported host to self !");
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        }
    }

    public void addWhitelist(UUID player) {
        whitelist.add(player);
    }



}