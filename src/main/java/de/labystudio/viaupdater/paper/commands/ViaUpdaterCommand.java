package de.labystudio.viaupdater.paper.commands;

import de.labystudio.viaupdater.paper.PaperProviderContext;
import de.labystudio.viaupdater.paper.ViaUpdaterPlugin;
import de.labystudio.viaupdater.updater.ViaProject;
import de.labystudio.viaupdater.updater.ViaUpdater;
import de.labystudio.viaupdater.updater.exception.UpdateException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViaUpdaterCommand implements CommandExecutor, TabCompleter {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ViaUpdaterPlugin plugin;

    public ViaUpdaterCommand(ViaUpdaterPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NonNull [] args
    ) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /viaupdater <update <all|<name>> [source]|reload>", NamedTextColor.GRAY));
            return true;
        }

        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase("reload")) {
            try {
                this.plugin.reload();
                sender.sendMessage(Component.text("Config reloaded.", NamedTextColor.GREEN));
            } catch (Exception e) {
                sender.sendMessage(Component.text("Reload failed: " + e.getMessage(), NamedTextColor.RED));
            }
            return true;
        }

        if (subCommand.equalsIgnoreCase("update")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /viaupdater update <all|<name>> [source]", NamedTextColor.GRAY));
                return true;
            }

            String target = args[1];
            String sourceId = args.length >= 3 ? args[2] : null;
            PaperProviderContext context = new PaperProviderContext(this.plugin, sender);

            this.executorService.submit(() -> {
                try {
                    ViaUpdater updater = this.plugin.getUpdater();
                    if (target.equalsIgnoreCase("all")) {
                        updater.updateAll(context);
                    } else {
                        ViaProject project = updater.getProject(target);
                        if (project == null) {
                            sender.sendMessage(Component.text("Unknown project: " + target, NamedTextColor.RED));
                            return;
                        }
                        if (!updater.isInstalled(context, project)) {
                            sender.sendMessage(Component.text(target + " is not installed in the plugins folder.", NamedTextColor.RED));
                            return;
                        }
                        if (sourceId != null) {
                            if (!project.sources().containsKey(sourceId)) {
                                sender.sendMessage(Component.text("Unknown source: " + sourceId, NamedTextColor.RED));
                                return;
                            }
                            updater.update(context, project, sourceId);
                        } else {
                            updater.update(context, project);
                        }
                    }
                } catch (UpdateException e) {
                    sender.sendMessage(Component.text("Update failed: " + rootCause(e), NamedTextColor.RED));
                    this.plugin.getLogger().log(java.util.logging.Level.WARNING, "Update failed", e);
                }
            });

            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
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
            return this.filter(List.of("update", "reload"), args[0]);
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
