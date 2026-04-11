package de.labystudio.viaupdater.updater.exception;

public class CancelledException extends UpdateException {

    public CancelledException() {
        super("Update was cancelled");
    }
}

