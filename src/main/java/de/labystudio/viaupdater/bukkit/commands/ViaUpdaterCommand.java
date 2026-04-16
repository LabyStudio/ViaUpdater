package de.labystudio.viaupdater.bukkit.commands;

import de.labystudio.viaupdater.bukkit.BukkitProviderContext;
import de.labystudio.viaupdater.bukkit.ViaUpdaterPlugin;
import de.labystudio.viaupdater.bukkit.adventure.ViaComponent;
import de.labystudio.viaupdater.bukkit.adventure.ViaNamedTextColor;
import de.labystudio.viaupdater.updater.ViaProject;
import de.labystudio.viaupdater.updater.ViaUpdater;
import de.labystudio.viaupdater.updater.exception.CancelledException;
import de.labystudio.viaupdater.updater.exception.UpdateException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ViaUpdaterCommand implements CommandExecutor, TabCompleter {

    private final ViaUpdaterPlugin plugin;

    public ViaUpdaterCommand(ViaUpdaterPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (args.length == 0) {
            // Note: Use deprecated getter to support legacy bukkit versions
            String version = this.plugin.getDescription().getVersion();
            this.sendMessage(sender,
                    ViaComponent.text("ViaUpdater ", ViaNamedTextColor.GREEN)
                            .append(ViaComponent.text(version, ViaNamedTextColor.RED))
                            .appendNewline()
                            .append(ViaComponent.text("Commands:", ViaNamedTextColor.GOLD))
                            .appendNewline()
                            .append(ViaComponent.text("/viaupdater update <all|<name>> [source]", ViaNamedTextColor.DARK_GREEN))
                            .append(ViaComponent.text(" - ", ViaNamedTextColor.WHITE))
                            .append(ViaComponent.text("Update all or a specific installed plugin.", ViaNamedTextColor.GOLD))
                            .appendNewline()
                            .append(ViaComponent.text("/viaupdater cancel", ViaNamedTextColor.DARK_GREEN))
                            .append(ViaComponent.text(" - ", ViaNamedTextColor.WHITE))
                            .append(ViaComponent.text("Cancel the currently running update.", ViaNamedTextColor.GOLD))
                            .appendNewline()
                            .append(ViaComponent.text("/viaupdater reload", ViaNamedTextColor.DARK_GREEN))
                            .append(ViaComponent.text(" - ", ViaNamedTextColor.WHITE))
                            .append(ViaComponent.text("Reload the config from disk.", ViaNamedTextColor.GOLD))
            );
            return true;
        }

        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase("cancel")) {
            if (!this.plugin.isUpdateRunning()) {
                this.sendMessage(sender, ViaComponent.text("No update is currently running.", ViaNamedTextColor.YELLOW));
                return true;
            }

            this.sendMessage(sender, ViaComponent.text("Cancelling current update...", ViaNamedTextColor.YELLOW));
            this.plugin.cancelCurrentUpdate();
            return true;
        }

        if (subCommand.equalsIgnoreCase("reload")) {
            try {
                this.plugin.reload();
                this.sendMessage(sender, ViaComponent.text("Config reloaded.", ViaNamedTextColor.GREEN));
            } catch (Exception e) {
                this.sendMessage(sender, ViaComponent.text("Reload failed: " + e.getMessage(), ViaNamedTextColor.RED));
            }
            return true;
        }

        if (subCommand.equalsIgnoreCase("update")) {
            if (args.length < 2) {
                this.sendMessage(sender, ViaComponent.text("Usage: /viaupdater update <all|<name>> [source]", ViaNamedTextColor.GRAY));
                return true;
            }

            String target = args[1];
            String sourceId = args.length >= 3 ? args[2] : null;
            BukkitProviderContext context = new BukkitProviderContext(this.plugin, sender);

            this.plugin.submitTask(() -> {
                try {
                    ViaUpdater updater = this.plugin.getUpdater();
                    if (target.equalsIgnoreCase("all")) {
                        updater.updateAll(context);
                    } else {
                        ViaProject project = updater.getProject(target);
                        if (project == null) {
                            this.sendMessage(sender, ViaComponent.text("Unknown project: " + target, ViaNamedTextColor.RED));
                            return;
                        }
                        if (!updater.isInstalled(context, project)) {
                            this.sendMessage(sender, ViaComponent.text(target + " is not installed in the plugins folder.", ViaNamedTextColor.RED));
                            return;
                        }
                        if (sourceId != null) {
                            if (!project.sources().containsKey(sourceId)) {
                                this.sendMessage(sender, ViaComponent.text("Unknown source: " + sourceId, ViaNamedTextColor.RED));
                                return;
                            }
                            updater.update(context, project, sourceId);
                        } else {
                            updater.update(context, project);
                        }
                    }
                } catch (CancelledException e) {
                    this.sendMessage(sender, ViaComponent.text("Update was cancelled.", ViaNamedTextColor.YELLOW));
                } catch (UpdateException e) {
                    this.sendMessage(sender, ViaComponent.text("Update failed: " + this.rootCause(e), ViaNamedTextColor.RED));
                    this.plugin.getLogger().log(java.util.logging.Level.WARNING, "Update failed", e);
                }
            });

            return true;
        }

        this.sendMessage(sender, ViaComponent.text("Unknown subcommand: " + subCommand, ViaNamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (args.length == 1) {
            return this.filter(List.of("update", "cancel", "reload"), args[0]);
        }

        if (args[0].equalsIgnoreCase("update")) {
            if (args.length == 2) {
                List<String> completions = new ArrayList<>();
                completions.add("all");
                for (ViaProject project : this.plugin.getUpdater().getProjects()) {
                    completions.add(project.name());
                }
                return this.filter(completions, args[1]);
            }

            if (args.length == 3 && !args[1].equalsIgnoreCase("all")) {
                ViaProject project = this.plugin.getUpdater().getProject(args[1]);
                List<String> ids = project != null
                        ? new ArrayList<>(project.sources().keySet())
                        : this.plugin.getUpdater().getProjects().stream()
                          .flatMap(p -> p.sources().keySet().stream())
                          .distinct()
                          .toList();
                return this.filter(ids, args[2]);
            }
        }

        return List.of();
    }

    private void sendMessage(CommandSender receiver, ViaComponent component) {
        component.send(receiver);
    }

    private String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return cause.getClass().getSimpleName() + (msg != null ? ": " + msg : "");
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return list.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
