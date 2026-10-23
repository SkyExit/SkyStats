package de.skystats.data;

import java.nio.file.Path;

public final class StatsPaths {
    private StatsPaths() {}
    public static Path primaryStatistics(Path worldFolder) {
        Path storage = worldFolder.toAbsolutePath().normalize();
        // Recent Minecraft versions expose dimension folders through World#getWorldFolder.
        // Player statistics still belong to the shared world storage above "dimensions".
        for (Path ancestor = storage; ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor.getFileName() != null && ancestor.getFileName().toString().equals("dimensions")) {
                storage = ancestor.getParent(); break;
            }
        }
        return storage.resolve("stats");
    }
}
