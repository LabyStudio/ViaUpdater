package de.labystudio.viaupdater.bukkit.adventure.impl;

import de.labystudio.viaupdater.bukkit.adventure.ViaComponent;
import de.labystudio.viaupdater.bukkit.adventure.ViaNamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class LegacyViaComponent implements ViaComponent {

    // null entry = newline; non-null = pre-colored text segment
    private final List<String> segments = new ArrayList<>();

    public LegacyViaComponent(String text, ViaNamedTextColor color) {
        this.segments.add(color.getChatColor() + text);
    }

    @Override
    public ViaComponent append(ViaComponent other) {
        this.segments.addAll(((LegacyViaComponent) other).segments);
        return this;
    }

    @Override
    public ViaComponent appendNewline() {
        this.segments.add(null);
        return this;
    }

    @Override
    public void send(CommandSender sender) {
        sender.sendMessage(this.toLines());
    }

    @Override
    public void logToConsole(Plugin plugin) {
        this.send(plugin.getServer().getConsoleSender());
    }

    private String[] toLines() {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String segment : this.segments) {
            if (segment == null) {
                lines.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(segment);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.toArray(new String[0]);
    }

    private String toPlainString() {
        StringBuilder sb = new StringBuilder();
        for (String segment : this.segments) {
            if (segment != null) {
                sb.append(segment);
            }
        }
        return sb.toString();
    }
}

