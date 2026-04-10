package de.labystudio.viaupdater.updater.source.provider;

import java.nio.file.Path;

public interface ProviderContext {

    void updateStatus(StatusType type, String message);

    Path pluginsDirectory();

    Path tmpDirectory();
}
