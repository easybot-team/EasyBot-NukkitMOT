package com.springwater.easybot.nukkitmot;

import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.ClientProfile;
import com.springwater.easybot.nukkitmot.bridge.NukkitBridgeBehavior;
import com.springwater.easybot.nukkitmot.bridge.NukkitBridgeLogger;
import com.springwater.easybot.nukkitmot.command.EasyBotCommand;
import com.springwater.easybot.nukkitmot.command.SyncCommand;
import com.springwater.easybot.nukkitmot.listener.PlayerEventListener;
import com.springwater.easybot.nukkitmot.service.EconomyService;
import com.springwater.easybot.nukkitmot.service.MainThreadExecutor;
import com.springwater.easybot.nukkitmot.service.PlaceholderService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class EasyBotNukkitMOT extends PluginBase {
    private volatile ExecutorService networkExecutor;
    private volatile BridgeClient bridgeClient;
    private MainThreadExecutor mainThreadExecutor;
    private PlaceholderService placeholderService;
    private EconomyService economyService;
    private NukkitBridgeBehavior bridgeBehavior;
    private volatile boolean debugEnabled;
    private volatile boolean ignoreBridgeErrors;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String token = getConfig().getString("service.token", "").trim();
        if (token.isEmpty()) {
            getLogger().critical("EasyBot 已禁用：请先在 config.yml 中填写 service.token。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.networkExecutor = Executors.newCachedThreadPool(new EasyBotThreadFactory());
        this.mainThreadExecutor = new MainThreadExecutor(this);
        this.placeholderService = PlaceholderService.detect(this);
        this.economyService = EconomyService.detect(this);
        this.bridgeBehavior = new NukkitBridgeBehavior(this, mainThreadExecutor);

        refreshClientProfile();
        BridgeClient.setLogger(new NukkitBridgeLogger(this));

        registerCommands();
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        openBridgeClient();

        getLogger().info("EasyBot Nukkit-MOT 适配已启用。");
        getLogger().info("PlaceholderAPI：" + (placeholderService.isAvailable() ? "已接入" : "未安装"));
        getLogger().info("EconomyAPI：" + (economyService.isAvailable() ? "已接入" : "未安装"));
    }

    @Override
    public void onDisable() {
        BridgeClient client = this.bridgeClient;
        this.bridgeClient = null;
        if (client != null) {
            try {
                client.close();
            } catch (RuntimeException exception) {
                getLogger().error("关闭 EasyBot Bridge 时发生异常", exception);
            }
        }
        ExecutorService executor = this.networkExecutor;
        this.networkExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void registerCommands() {
        PluginCommand<?> easyBot = pluginCommand("easybot");
        PluginCommand<?> sync = pluginCommand("esay");
        if (easyBot == null || sync == null) {
            throw new IllegalStateException("plugin.yml 中缺少 easybot 或 esay 命令定义");
        }
        easyBot.setExecutor(new EasyBotCommand(this));
        sync.setExecutor(new SyncCommand(this));
    }

    public synchronized void reloadPlugin() {
        reloadConfig();
        String token = getConfig().getString("service.token", "").trim();
        if (token.isEmpty()) {
            throw new IllegalStateException("service.token 不能为空");
        }

        this.placeholderService = PlaceholderService.detect(this);
        this.economyService = EconomyService.detect(this);
        refreshClientProfile();

        BridgeClient previous = this.bridgeClient;
        this.bridgeClient = null;
        if (previous != null) {
            try {
                previous.close();
            } catch (RuntimeException exception) {
                getLogger().warning("关闭旧 EasyBot Bridge 连接时发生异常：" + exception.getMessage());
            }
        }
        openBridgeClient();
    }

    private void refreshClientProfile() {
        this.debugEnabled = getConfig().getBoolean("debug", false);
        this.ignoreBridgeErrors = getConfig().getBoolean("service.ignore_error", false);
        ClientProfile.setPluginVersion(getDescription().getVersion());
        ClientProfile.setServerDescription(getServer().getMotd());
        ClientProfile.setDebugMode(debugEnabled);
        ClientProfile.setCommandSupported(true);
        ClientProfile.setPapiSupported(placeholderService != null && placeholderService.isAvailable());
        ClientProfile.setOnlineMode(getServer().xboxAuth);
        ClientProfile.setHasGeyser(false);
        ClientProfile.setHasFloodgate(false);
        ClientProfile.setHasBungeeChatApi(false);
        ClientProfile.setHasPaperSkinApi(false);
        ClientProfile.setHasSkinsRestorer(false);
        ClientProfile.setHasItemsAdder(false);
        ClientProfile.setHasQFaces(false);
    }

    private void openBridgeClient() {
        String url = getConfig().getString("service.url", "ws://127.0.0.1:26990/bridge").trim();
        String token = getConfig().getString("service.token", "").trim();
        BridgeClient client = new BridgeClient(url, bridgeBehavior);
        client.setToken(token);
        this.bridgeClient = client;
    }

    public BridgeClient getBridgeClient() {
        return bridgeClient;
    }

    public boolean isBridgeReady() {
        BridgeClient client = bridgeClient;
        return client != null && client.isReady();
    }

    public PlaceholderService getPlaceholderService() {
        return placeholderService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public MainThreadExecutor getMainThreadExecutor() {
        return mainThreadExecutor;
    }

    public void executeNetwork(Runnable task) {
        ExecutorService executor = this.networkExecutor;
        if (executor == null || executor.isShutdown()) {
            throw new IllegalStateException("EasyBot 网络线程池未运行");
        }
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException exception) {
                getLogger().error("EasyBot 网络任务执行失败", exception);
            }
        });
    }

    public void runSync(Runnable task) {
        if (getServer().isPrimaryThread()) {
            task.run();
        } else {
            getServer().getScheduler().scheduleTask(this, task);
        }
    }

    public static String colorize(String message) {
        return message == null ? "" : message.replace('&', '§');
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isIgnoreBridgeErrors() {
        return ignoreBridgeErrors;
    }

    private PluginCommand<?> pluginCommand(String name) {
        return getCommand(name) instanceof PluginCommand<?> command ? command : null;
    }

    private static final class EasyBotThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "EasyBot-NukkitMOT-Worker-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
