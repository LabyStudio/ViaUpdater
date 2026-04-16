package de.labystudio.viaupdater.bukkit.adventure.impl;

import de.labystudio.viaupdater.bukkit.adventure.ViaComponent;
import de.labystudio.viaupdater.bukkit.adventure.ViaNamedTextColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class AdventureViaComponent implements ViaComponent {

    private Component component;

    private AdventureViaComponent(Component component) {
        this.component = component;
    }

    @Override
    public ViaComponent append(ViaComponent other) {
        this.component = this.component.append(((AdventureViaComponent) other).component);
        return this;
    }

    @Override
    public ViaComponent appendNewline() {
        this.component = this.component.append(Component.newline());
        return this;
    }

    @Override
    public void send(CommandSender sender) {
        sender.sendMessage(this.component);
    }

    @Override
    public void logToConsole(Plugin plugin) {
        try {
            plugin.getComponentLogger().info(this.component);
        } catch (Throwable ignored) {
            plugin.getLogger().info(this.component.toString());
        }
    }

    private static NamedTextColor toAdventureColor(ViaNamedTextColor color) {
        return switch (color) {
            case GREEN -> NamedTextColor.GREEN;
            case RED -> NamedTextColor.RED;
            case YELLOW -> NamedTextColor.YELLOW;
            case GRAY -> NamedTextColor.GRAY;
            case GOLD -> NamedTextColor.GOLD;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case WHITE -> NamedTextColor.WHITE;
        };
    }

    public static AdventureViaComponent of(String text, ViaNamedTextColor color) {
        return new AdventureViaComponent(Component.text(text, toAdventureColor(color)));
    }
}
