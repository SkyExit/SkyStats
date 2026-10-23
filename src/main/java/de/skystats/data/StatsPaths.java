package de.skystats.data;

import java.nio.file.Path;
import java.nio.file.Files;

public final class StatsPaths {
    private StatsPaths() {}
    public static Path primaryStatistics(Path worldFolder) {
        Path storage = worldFolder.toAbsolutePath().normalize();
        boolean dimensionLayout = false;
        // Recent Minecraft versions expose dimension folders through World#getWorldFolder.
        // Player statistics belong to shared storage, never to an individual dimension.
        for (Path ancestor = storage; ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor.getFileName() != null && ancestor.getFileName().toString().equals("dimensions")) {
                storage = ancestor.getParent(); dimensionLayout = true; break;
            }
        }
        // Since 26.1 all player storage moved below "players". Prefer that layout
        // even if an obsolete stats folder was left behind during migration.
        return dimensionLayout || Files.isDirectory(storage.resolve("players"))
                ? storage.resolve("players/stats") : storage.resolve("stats");
    }
}
