package de.labystudio.viaupdater.bukkit.nms;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;

public class ServerStateUtil {

    private static final String[] RUNNING_FIELD_NAMES = {"running", "isRunning"};

    public static boolean isServerRunning() {
        try {
            Class<?> minecraftServerClass = resolveMinecraftServerClass();
            Object minecraftServer = resolveMinecraftServerInstance();

            Field runningField = findRunningField(minecraftServerClass);
            if (runningField != null) {
                runningField.setAccessible(true);
                return (boolean) runningField.get(minecraftServer);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Class<?> resolveMinecraftServerClass() throws ClassNotFoundException {
        // 1.17+ Paper / Spigot with Mojang mappings
        try {
            return Class.forName("net.minecraft.server.MinecraftServer");
        } catch (ClassNotFoundException ignored) {
        }

        // Pre-1.17: net.minecraft.server.v1_16_R3.MinecraftServer etc.
        String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
        String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
        return Class.forName("net.minecraft.server." + version + ".MinecraftServer");
    }

    private static Object resolveMinecraftServerInstance() throws ReflectiveOperationException {
        Object craftServer = Bukkit.getServer();
        Field consoleField = findFieldInHierarchy(craftServer.getClass(), "console");
        if (consoleField == null) {
            throw new NoSuchFieldException("console field not found on " + craftServer.getClass().getName());
        }
        consoleField.setAccessible(true);
        return consoleField.get(craftServer);
    }

    private static Field findRunningField(Class<?> minecraftServerClass) {
        // Try known names first (works for Mojang-mapped Paper and legacy Spigot NMS)
        for (String name : RUNNING_FIELD_NAMES) {
            Field field = findFieldInHierarchy(minecraftServerClass, name);
            if (field != null) {
                return field;
            }
        }

        // Last resort: scan for any boolean field whose name contains "running" (case-insensitive)
        Class<?> current = minecraftServerClass;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == boolean.class && field.getName().toLowerCase().contains("running")) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field findFieldInHierarchy(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}

