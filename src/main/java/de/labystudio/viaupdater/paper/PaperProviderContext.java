package de.labystudio.viaupdater.paper;

import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.StatusType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.nio.file.Path;

public class PaperProviderContext implements ProviderContext {

    private final ViaUpdaterPlugin plugin;
    private final CommandSender sender;

    public PaperProviderContext(ViaUpdaterPlugin plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public PaperProviderContext(ViaUpdaterPlugin plugin) {
        this(plugin, null);
    }

    @Override
    public void updateStatus(StatusType type, String message) {
        NamedTextColor color = switch (type) {
            case SUCCESS -> NamedTextColor.GREEN;
            case ERROR -> NamedTextColor.RED;
            case PROGRESS -> NamedTextColor.YELLOW;
        };
        Component component = Component.text(message, color);

        this.plugin.getComponentLogger().info(component);
        if (this.sender != null && !(this.sender instanceof ConsoleCommandSender)) {
            this.sender.sendMessage(component);
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

