package fr.blendman.magnet.server;

import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.MagnetSide;
import fr.blendman.magnet.api.handles.EchoHandle;
import fr.blendman.magnet.api.handles.messenger.events.AdminMovePlayerEvent;
import fr.blendman.magnet.api.handles.messenger.events.BroadcastEvent;
import fr.blendman.magnet.api.handles.messenger.events.InvalidatePlayerEvent;
import fr.blendman.magnet.api.server.ServerCacheHandler;
import fr.blendman.magnet.server.chat.ChatManagerImpl;
import fr.blendman.magnet.server.commands.*;
import fr.blendman.magnet.server.echo.EchoHandleImpl;
import fr.blendman.magnet.server.listeners.BukkitCommandPreProcessor;
import fr.blendman.magnet.server.listeners.ChatListener;
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
    private ChatManagerImpl chatManager;
    private EchoHandleImpl echoHandle;

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
            magnet.getMessenger().subscribe("server." + magnet.getServer().getKind() + ".#");
            magnet.getMessenger().registerListener(networkListener::onAdminMove, AdminMovePlayerEvent.class);
            magnet.getMessenger().registerListener(networkListener::onPlayerInvalidation, InvalidatePlayerEvent.class);
            magnet.getMessenger().registerListener(networkListener::onBroadcast, BroadcastEvent.class);

        } catch (Exception e) {
            e.printStackTrace();
            getServer().shutdown();
        }
        Bukkit.getPluginManager().registerEvents(new LoginListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AnticheatManager(), this);
        Bukkit.getPluginManager().registerEvents(new BukkitCommandPreProcessor(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        registerCommands();
        chatManager = new ChatManagerImpl(this);
        echoHandle = new EchoHandleImpl(this);
    }

    private void processKindCompat(Server server) {
        if (server.getProperties() != null && server.getProperties().containsKey("directly_waiting")) {
            setServerState("Waiting").thenAccept(unused -> {
                System.out.println("Server is now Waiting for players");
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        }
    }


    @Override
    public CompletableFuture<Void> setServerState(String s) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            magnet.getServerApi().apiServersUuidSetstatePostAsync(magnet.getServerId(), s, new ApiCallBackToCompletableFuture<>(ret));
        } catch (ApiException e) {
            ret.completeExceptionally(e);
        }
        if (s.equals("Waiting")) {
            return ret.thenCompose(unused -> handlePropertiesActions());
        }else {
            return ret;
        }
    }

    private void registerCommands() {
        registerCommand(new AStopCommand(this), "astop");
        registerCommand(new SysCallCommand(this), "syscall");
        registerCommand(new LinkCommand(this), "link");
        registerCommand(new ReportMessageCommand(this), "reportmsg");
        registerCommand(new ReCommand(this), "re");
        registerCommand(new SysBanCommand(this), "sysban");
        registerCommand(new EchoCommand(this), "echo");
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

    public ServerLoginPlayerInfo getInfo(UUID uniqueId) {
        return this.infos.get(uniqueId);
    }

    public ServerLoginPlayerInfo getRawInfo(UUID uuid) {
        return this.infos.get(uuid);
    }

    public boolean shouldBeWhitelisted(UUID uuid) {
        return whitelist.remove(uuid);
    }

    private CompletableFuture<Void> handlePropertiesActions() {
        Map<String, String> properties = magnet.getServer().getProperties();
        if (properties == null)
            return CompletableFuture.completedFuture(null);

        if (properties.containsKey("host")) {
            UUID host = UUID.fromString(properties.get("host"));
            Bukkit.setWhitelist(true);
            whitelist.add(host);
            CompletableFuture<String> ret = new CompletableFuture<>();
            try {
                getMagnet().getPlayerApi().apiPlayersUuidMovePostAsync(host, new PlayerMove().server(getMagnet().getServerId()), new ApiCallBackToCompletableFuture<>(ret));
            } catch (ApiException e) {
                ret.completeExceptionally(e);
            }
            return ret.thenAccept(sucess -> {
                getLogger().info("Teleported host to self : " + sucess);
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    public void addWhitelist(UUID player) {
        whitelist.add(player);
    }

    @Override
    public ChatManagerImpl getChatManager() {
        return this.chatManager;
    }

    @Override
    public EchoHandle getEchoHandle() {
        return echoHandle;
    }
}
