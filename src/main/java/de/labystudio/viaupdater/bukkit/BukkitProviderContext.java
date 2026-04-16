package de.labystudio.viaupdater.bukkit;

import de.labystudio.viaupdater.bukkit.adventure.ViaComponent;
import de.labystudio.viaupdater.bukkit.adventure.ViaNamedTextColor;
import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.StatusType;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.nio.file.Path;

public class BukkitProviderContext implements ProviderContext {

    private final ViaUpdaterPlugin plugin;
    private final CommandSender sender;

    public BukkitProviderContext(ViaUpdaterPlugin plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public BukkitProviderContext(ViaUpdaterPlugin plugin) {
        this(plugin, null);
    }

    @Override
    public void updateStatus(StatusType type, String message) {
        ViaNamedTextColor color = switch (type) {
            case SUCCESS -> ViaNamedTextColor.GREEN;
            case ERROR -> ViaNamedTextColor.RED;
            case PROGRESS -> ViaNamedTextColor.YELLOW;
        };
        ViaComponent component = ViaComponent.text(message, color);

        component.logToConsole(this.plugin);
        if (this.sender != null && !(this.sender instanceof ConsoleCommandSender)) {
            component.send(this.sender);
        }
    }

    @Override
    public Path pluginsDirectory() {
        return this.plugin.getDataFolder().getParentFile().toPath();
    }

    @Override
    public Path tmpDirectory() {
        return this.plugin.getDataFolder().toPath().resolve(".tmp");
    }
}

