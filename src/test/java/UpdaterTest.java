import de.labystudio.viaupdater.updater.ViaUpdater;
import de.labystudio.viaupdater.updater.source.provider.ProviderContext;
import de.labystudio.viaupdater.updater.source.provider.StatusType;

void main() throws Exception {
    ViaUpdater updater = new ViaUpdater();

    // Extract config from resources
    Path out = Paths.get("./config.yml");
    try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
        if (input == null) {
            throw new RuntimeException("Failed to load config.yml from resources");
        }
        Files.copy(input, out, StandardCopyOption.REPLACE_EXISTING);
    }

    // Patch GitHub token
    String token = System.getenv("GITHUB_TOKEN");
    if (token != null) {
        String content = Files.readString(out);
        Files.writeString(out, content.replaceFirst("(?m)^(\\s*token:).*$", "$1 " + token));
    }

    // Load config
    try (InputStream input = Files.newInputStream(out)) {
        updater.loadConfig(input);
    }

    // Update all
    updater.updateAll(new ProviderContext() {
        @Override
        public void updateStatus(StatusType type, String message) {
            System.out.println(message);
        }

        @Override
        public Path pluginsDirectory() {
            return Path.of("plugins");
        }

        @Override
        public Path tmpDirectory() {
            return Path.of(".tmp");
        }
    });

}
