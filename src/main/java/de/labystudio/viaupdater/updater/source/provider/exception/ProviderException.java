package de.labystudio.viaupdater.updater.source.provider.exception;

import java.io.IOException;

public class ProviderException extends IOException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
