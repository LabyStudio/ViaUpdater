package de.labystudio.viaupdater.bukkit.adventure;

import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public enum ViaNamedTextColor {
    GREEN(ChatColor.GREEN),
    RED(ChatColor.RED),
    YELLOW(ChatColor.YELLOW),
    GRAY(ChatColor.GRAY),
    GOLD(ChatColor.GOLD),
    DARK_GREEN(ChatColor.DARK_GREEN),
    WHITE(ChatColor.WHITE);

    final ChatColor chatColor;

    ViaNamedTextColor(ChatColor chatColor) {
        this.chatColor = chatColor;
    }

    public ChatColor getChatColor() {
        return this.chatColor;
    }
}
