package de.labystudio.viaupdater.bukkit;

import de.labystudio.viaupdater.bukkit.commands.ViaUpdaterCommand;
import de.labystudio.viaupdater.updater.ViaUpdater;
import de.labystudio.viaupdater.updater.exception.CancelledException;
import de.labystudio.viaupdater.bukkit.nms.ServerStateUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ViaUpdaterPlugin extends JavaPlugin {

    private final ViaUpdater updater = new ViaUpdater();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile Future<?> currentTask;
    private BukkitTask autoUpdateTask;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        this.restartAutoUpdater();

        // Commands
        PluginCommand command = Objects.requireNonNull(this.getCommand("viaupdater"));
        ViaUpdaterCommand executor = new ViaUpdaterCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void loadConfig() {
        this.updater.reset();
        try (FileInputStream input = new FileInputStream(new File(this.getDataFolder(), "config.yml"))) {
            this.updater.loadConfig(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config during startup: " + e.getMessage());
        }
    }

    public void reload() {
        this.reloadConfig();
        this.loadConfig();
        this.restartAutoUpdater();
    }

    private void restartAutoUpdater() {
        if (this.autoUpdateTask != null) {
            this.autoUpdateTask.cancel();
            this.autoUpdateTask = null;
        }

        FileConfiguration config = this.getConfig();
        if (config.getBoolean("auto-update.enabled", false)) {
            long intervalTicks = config.getLong("auto-update.interval", 24) * 60 * 60 * 20L;
            this.autoUpdateTask = this.getServer().getScheduler().runTaskTimer(this, () ->
                    this.submitTask(() -> {
                        this.getLogger().info("Running scheduled auto-update...");
                        BukkitProviderContext context = new BukkitProviderContext(this);
                        try {
                            this.updater.updateAll(context);
                        } catch (CancelledException ignored) {
                        } catch (Exception e) {
                            this.getLogger().warning("Auto-update failed: " + e.getMessage());
                        }
                    }), intervalTicks, intervalTicks);
        }
    }

    @Override
    public void onDisable() {
        if (this.autoUpdateTask != null) {
            this.autoUpdateTask.cancel();
        }
        this.cancelCurrentUpdate();

        boolean isServerShutdown = !ServerStateUtil.isServerRunning();

        if (this.getConfig().getBoolean("shutdown-update.enabled", false) && isServerShutdown) {
            this.getLogger().info("Running blocking shutdown update...");
            BukkitProviderContext context = new BukkitProviderContext(this);
            Future<?> task = this.submitTask(() -> {
                try {
                    this.updater.updateAll(context);
                } catch (CancelledException ignored) {
                } catch (Exception e) {
                    this.getLogger().warning("Shutdown update failed: " + e.getMessage());
                }
            });
            try {
                task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ignored) {
            }
        }
    }

    public Future<?> submitTask(Runnable task) {
        Future<?> future = this.executorService.submit(task);
        this.currentTask = future;
        return future;
    }

    public boolean isUpdateRunning() {
        Future<?> task = this.currentTask;
        return task != null && !task.isDone();
    }

    public void cancelCurrentUpdate() {
        this.updater.cancel();
        Future<?> task = this.currentTask;
        if (task != null) {
            task.cancel(true);
        }
    }

    public ViaUpdater getUpdater() {
        return this.updater;
    }
}
