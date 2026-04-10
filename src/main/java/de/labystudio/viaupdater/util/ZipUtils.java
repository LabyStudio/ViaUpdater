package de.labystudio.viaupdater.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    public static void extract(Path zip, Path destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target);
                }
                zis.closeEntry();
            }
        }
    }

    public static void extractStrippingRoot(Path zip, Path destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Strip the leading root folder (e.g. GitHub's "Repo-abc1234/")
                String name = entry.getName();
                int slash = name.indexOf('/');
                if (slash == -1) {
                    zis.closeEntry();
                    continue;
                }
                String stripped = name.substring(slash + 1);
                if (stripped.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                Path target = destination.resolve(stripped);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target);
                }
                zis.closeEntry();
            }
        }
    }
}
