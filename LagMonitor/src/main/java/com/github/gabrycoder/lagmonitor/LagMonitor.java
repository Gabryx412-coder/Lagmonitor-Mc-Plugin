package com.github.gabrycoder.lagmonitor;

import com.github.gabrycoder.lagmonitor.command.EnvironmentCommand;
import com.github.gabrycoder.lagmonitor.command.GraphCommand;
import com.github.gabrycoder.lagmonitor.command.HelpCommand;
import com.github.gabrycoder.lagmonitor.command.MbeanCommand;
import com.github.gabrycoder.lagmonitor.command.MonitorCommand;
import com.github.gabrycoder.lagmonitor.command.NativeCommand;
import com.github.gabrycoder.lagmonitor.command.NetworkCommand;
import com.github.gabrycoder.lagmonitor.command.PaginationCommand;
import com.github.gabrycoder.lagmonitor.command.StackTraceCommand;
import com.github.gabrycoder.lagmonitor.command.VmCommand;
import com.github.gabrycoder.lagmonitor.command.dump.FlightCommand;
import com.github.gabrycoder.lagmonitor.command.dump.HeapCommand;
import com.github.gabrycoder.lagmonitor.command.dump.ThreadCommand;
import com.github.gabrycoder.lagmonitor.command.minecraft.PingCommand;
import com.github.gabrycoder.lagmonitor.command.minecraft.SystemCommand;
import com.github.gabrycoder.lagmonitor.command.minecraft.TPSCommand;
import com.github.gabrycoder.lagmonitor.command.minecraft.TasksCommand;
import com.github.gabrycoder.lagmonitor.command.timing.PaperTimingsCommand;
import com.github.gabrycoder.lagmonitor.command.timing.SpigotTimingsCommand;
import com.github.gabrycoder.lagmonitor.listener.BlockingConnectionSelector;
import com.github.gabrycoder.lagmonitor.listener.GraphListener;
import com.github.gabrycoder.lagmonitor.listener.PageManager;
import com.github.gabrycoder.lagmonitor.listener.ThreadSafetyListener;
import com.github.gabrycoder.lagmonitor.logging.ForwardingLoggerFactory;
import com.github.gabrycoder.lagmonitor.storage.MonitorSaveTask;
import com.github.gabrycoder.lagmonitor.storage.NativeSaveTask;
import com.github.gabrycoder.lagmonitor.storage.Storage;
import com.github.gabrycoder.lagmonitor.storage.TPSSaveTask;
import com.github.gabrycoder.lagmonitor.task.IODetectorTask;
import com.github.gabrycoder.lagmonitor.task.PingManager;
import com.github.gabrycoder.lagmonitor.task.TPSHistoryTask;
import com.github.gabrycoder.lagmonitor.threading.BlockingActionManager;
import com.github.gabrycoder.lagmonitor.threading.BlockingSecurityManager;
import com.github.gabrycoder.lagmonitor.threading.Injectable;
import com.github.gabrycoder.lagmonitor.traffic.TrafficReader;

import java.net.ProxySelector;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.Timer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class LagMonitor extends JavaPlugin {

    private static final int DETECTION_THRESHOLD = 10;
    private static final int HOURS_PER_DAY = 24;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SECONDS_PER_MINUTE = 60;

    private final BlockingActionManager actionManager = new BlockingActionManager(this);
    private final PageManager pageManager = new PageManager();
    private final TPSHistoryTask tpsHistoryTask = new TPSHistoryTask();
    private final NativeManager nativeData = new NativeManager(getLogger(), getDataFolder().toPath());

    private PingManager pingManager;
    private TrafficReader trafficReader;
    private Timer blockDetectionTimer;
    private Timer monitorTimer;

    @Override
    public void onLoad() {
        ForwardingLoggerFactory.PARENT_LOGGER = getLogger();

        nativeData.setupNativeAdapter();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (Files.notExists(getDataFolder().toPath().resolve("default.jfc"))) {
            saveResource("default.jfc", false);
        }

        try {
            pingManager = new PingManager(this);
        } catch (ReflectiveOperationException reflectiveEx) {
            getLogger().log(Level.SEVERE, "Cannot initialize ping manager", reflectiveEx);
        }

        //register schedule tasks
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, tpsHistoryTask, 20L, TPSHistoryTask.RUN_INTERVAL);
        scheduler.runTaskTimer(this, pingManager, 20L, PingManager.PING_INTERVAL);

        //register listeners
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new GraphListener(), this);
        pluginManager.registerEvents(pageManager, this);
        pluginManager.registerEvents(pingManager, this);

        //add the player to the list in the case the plugin is loaded at runtime
        Bukkit.getOnlinePlayers().forEach(pingManager::addPlayer);

        if (getConfig().getBoolean("traffic-counter")) {
            try {
                trafficReader = new TrafficReader(this);
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "Failed to initialize packet reader", ex);
            }
        }

        if (getConfig().getBoolean("thread-safety-check")) {
            pluginManager.registerEvents(new ThreadSafetyListener(actionManager), this);
        }

        if (getConfig().getBoolean("thread-block-detection")) {
            scheduler.runTask(this, () -> {
                blockDetectionTimer = new Timer(getName() + "-Thread-Blocking-Detection");
                IODetectorTask detectorTask = new IODetectorTask(actionManager, Thread.currentThread());
                blockDetectionTimer.scheduleAtFixedRate(detectorTask, DETECTION_THRESHOLD, DETECTION_THRESHOLD);
            });
        }

        if (getConfig().getBoolean("monitor-database")) {
            setupMonitoringDatabase();
        }

        if (getConfig().getBoolean("socket-block-detection")) {
            scheduler.runTask(this, () -> new BlockingConnectionSelector(actionManager).inject());
        }

        if (getConfig().getBoolean("securityMangerBlockingCheck")) {
            if (Runtime.version().feature() < 17) {
                scheduler.runTask(this, () -> new BlockingSecurityManager(actionManager).inject());
            }
        }

        registerCommands();
    }

    private void setupMonitoringDatabase() {
        try {
            String host = getConfig().getString("host");
            int port = getConfig().getInt("port");
            String database = getConfig().getString("database");
            boolean useSSL = getConfig().getBoolean("useSSL");

            String username = getConfig().getString("username");
            String password = getConfig().getString("password");
            String tablePrefix = getConfig().getString("tablePrefix");
            Storage storage = new Storage(getLogger(), host, port, database, useSSL, username, password, tablePrefix);
            storage.createTables();

            BukkitScheduler scheduler = getServer().getScheduler();
            scheduler.runTaskTimerAsynchronously(this, new TPSSaveTask(tpsHistoryTask, storage), 20L,
                     getConfig().getInt("tps-save-interval") * 20L);
            //this can run async because it runs independently of the main thread
            scheduler.runTaskTimerAsynchronously(this, new MonitorSaveTask(this, storage),
                    20L,getConfig().getInt("monitor-save-interval") * 20L);
            scheduler.runTaskTimerAsynchronously(this, new NativeSaveTask(this, storage),
                    20L,getConfig().getInt("native-save-interval") * 20L);
        } catch (SQLException sqlEx) {
            getLogger().log(Level.SEVERE, "Failed to setup monitoring database", sqlEx);
        }
    }

    @Override
    public void onDisable() {
        if (trafficReader != null) {
            trafficReader.close();
            trafficReader = null;
        }

        close(blockDetectionTimer);
        blockDetectionTimer = null;

        close(monitorTimer);
        monitorTimer = null;

        //restore the security manager
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager instanceof BlockingSecurityManager) {
            ((Injectable) securityManager).restore();
        }

        ProxySelector proxySelector = ProxySelector.getDefault();
        if (proxySelector instanceof BlockingConnectionSelector) {
            ((Injectable) proxySelector).restore();
        }
    }

    private void close(Timer timer) {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    public PageManager getPageManager() {
        return pageManager;
    }

    public Timer getMonitorTimer() {
        return monitorTimer;
    }

    public void setMonitorTimer(Timer monitorTimer) {
        this.monitorTimer = monitorTimer;
    }

    public TrafficReader getTrafficReader() {
        return trafficReader;
    }

    public TPSHistoryTask getTpsHistoryTask() {
        return tpsHistoryTask;
    }

    public Optional<PingManager> getPingManager() {
        return Optional.ofNullable(pingManager);
    }

    public NativeManager getNativeData() {
        return nativeData;
    }

    private void registerCommands() {
        getCommand(getName()).setExecutor(new HelpCommand(this));

        getCommand("ping").setExecutor(new PingCommand(this));
        getCommand("stacktrace").setExecutor(new StackTraceCommand(this));
        getCommand("thread").setExecutor(new ThreadCommand(this));
        getCommand("tpshistory").setExecutor(new TPSCommand(this));
        getCommand("mbean").setExecutor(new MbeanCommand(this));
        getCommand("system").setExecutor(new SystemCommand(this));
        getCommand("env").setExecutor(new EnvironmentCommand(this));
        getCommand("monitor").setExecutor(new MonitorCommand(this));
        getCommand("graph").setExecutor(new GraphCommand(this));
        getCommand("native").setExecutor(new NativeCommand(this));
        getCommand("vm").setExecutor(new VmCommand(this));
        getCommand("tasks").setExecutor(new TasksCommand(this));
        getCommand("heap").setExecutor(new HeapCommand(this));
        getCommand("lagpage").setExecutor(new PaginationCommand(this));
        getCommand("jfr").setExecutor(new FlightCommand(this));
        getCommand("network").setExecutor(new NetworkCommand(this));

        PluginCommand timing = getCommand("timing");
        try {
            //paper moved to class to package co.aikar.timings
            Class.forName("co.aikar.timings.Timing");
            timing.setExecutor(new PaperTimingsCommand(this));
        } catch (ClassNotFoundException e) {
            timing.setExecutor(new SpigotTimingsCommand(this));
        }
    }

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        return String.format("'%d' days '%d' hours '%d' minutes '%d' seconds'",
                duration.toDays(),
                duration.toHours() % HOURS_PER_DAY,
                duration.toMinutes() % MINUTES_PER_HOUR,
                duration.getSeconds() % SECONDS_PER_MINUTE);
    }
}
