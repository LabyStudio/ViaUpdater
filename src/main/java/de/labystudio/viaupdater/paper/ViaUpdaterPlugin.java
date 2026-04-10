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
    public void onEnable() {
        this.saveDefaultConfig();
        this.setupConfig();

        // Commands
        PluginCommand command = Objects.requireNonNull(this.getCommand("viaupdater"));
        ViaUpdaterCommand executor = new ViaUpdaterCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reload() throws IOException {
        this.reloadConfig();
        if (this.autoUpdateTask != null) {
            this.autoUpdateTask.cancel();
            this.autoUpdateTask = null;
        }
        this.updater.reset();
        this.setupConfig();
    }

    private void setupConfig() {
        try (FileInputStream input = new FileInputStream(new File(this.getDataFolder(), "config.yml"))) {
            this.updater.loadConfig(input);
        } catch (IOException e) {
            this.getLogger().severe("Failed to load config: " + e.getMessage());
            return;
        }

        FileConfiguration config = this.getConfig();

        // Auto updater
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
