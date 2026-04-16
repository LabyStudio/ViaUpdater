package de.labystudio.viaupdater.bukkit.adventure;

import de.labystudio.viaupdater.bukkit.adventure.impl.AdventureViaComponent;
import de.labystudio.viaupdater.bukkit.adventure.impl.LegacyViaComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public interface ViaComponent {

    boolean ADVENTURE_AVAILABLE = checkAdventure();

    ViaComponent append(ViaComponent other);

    ViaComponent appendNewline();

    void send(CommandSender sender);

    void logToConsole(Plugin plugin);

    static boolean checkAdventure() {
        try {
            Class.forName("net.kyori.adventure.text.Component");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    static ViaComponent text(String text, ViaNamedTextColor color) {
        if (ADVENTURE_AVAILABLE) {
            return AdventureViaComponent.of(text, color);
        }
        return new LegacyViaComponent(text, color);
    }
}
