package de.labystudio.viaupdater.updater.exception;

import java.io.IOException;

public class UpdateException extends IOException {

    public UpdateException(String message) {
        super(message);
    }

    public UpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
