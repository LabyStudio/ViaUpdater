package de.labystudio.viaupdater.updater.source;

import de.labystudio.viaupdater.updater.ViaProject;
import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.exception.ProviderException;

import java.nio.file.Path;

public interface Source<T extends SourceProject> {

    Path provide(
            ProviderContext context,
            ViaProject project,
            T sourceProject
    ) throws ProviderException;

}
