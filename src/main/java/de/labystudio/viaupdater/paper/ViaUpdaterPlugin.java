package de.labystudio.viaupdater.paper;

import de.labystudio.viaupdater.paper.commands.ViaUpdaterCommand;
import de.labystudio.viaupdater.updater.ViaUpdater;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

public class ViaUpdaterPlugin extends JavaPlugin {

    private final ViaUpdater updater = new ViaUpdater();
    private BukkitTask autoUpdateTask;

    @Override
    public void onLoad() {
        this.saveDefaultConfig();

        if (!this.getConfig().getBoolean("startup-update.enabled", false)) {
            return;
        }

        // Load projects
        this.loadConfig();

        // If any configured project is already loaded the server has already started (e.g. PlugMan) — skip.
        boolean alreadyRunning = this.updater.getProjects().stream()
                .anyMatch(p -> this.getServer().getPluginManager().getPlugin(p.name()) != null);

        if (alreadyRunning) {
            this.getLogger().warning("Skipping startup-update: Via plugins are already loaded (not a real server startup).");
            return;
        }

        this.getLogger().info("Running blocking startup update...");
        PaperProviderContext context = new PaperProviderContext(this);
        try {
            this.updater.updateAll(context);
        } catch (Exception e) {
            this.getLogger().warning("Startup update failed: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
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
            this.autoUpdateTask = this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                this.getLogger().info("Running scheduled auto-update...");
                PaperProviderContext context = new PaperProviderContext(this);
                try {
                    this.updater.updateAll(context);
                } catch (Exception e) {
                    this.getLogger().warning("Auto-update failed: " + e.getMessage());
                }
            }, intervalTicks, intervalTicks);
        }
    }

    @Override
    public void onDisable() {
        if (this.autoUpdateTask != null) {
            this.autoUpdateTask.cancel();
        }
    }

    public ViaUpdater getUpdater() {
        return this.updater;
    }
}
