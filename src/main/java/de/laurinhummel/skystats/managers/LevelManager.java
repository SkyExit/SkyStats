package de.laurinhummel.skystats.managers;

import de.laurinhummel.skystats.misc.McColors;

public final class LevelManager {
    private final long totalEXP;
    private final int level;
    private final long min;
    private final long max;

    public LevelManager(long totalEXP) {
        this.totalEXP = Math.max(0, totalEXP);
        // Level 1 needs 220 EXP, each following level needs 20 more.
        // EXP at the start of level (n + 1): 10*n*n + 210*n.
        long low = 0;
        long high = Math.min(this.totalEXP / 220, 960_383_884L) + 1;
        while (low + 1 < high) {
            long mid = low + (high - low) / 2;
            if (mid <= this.totalEXP / (10 * mid + 210)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        level = Math.toIntExact(low + 1);
        min = threshold(low);
        max = threshold(low + 1);
    }

    private static long threshold(long completedLevels) {
        if (completedLevels > Long.MAX_VALUE / (10 * completedLevels + 210)) {
            return Long.MAX_VALUE;
        }
        return completedLevels * (10 * completedLevels + 210);
    }

    public int getLevel() { return level; }
    public long getMinEXP() { return min; }
    public long getMaxEXP() { return max; }
    public long getLevelDuration() { return max - min; }
    public long getLevelProgress() { return totalEXP - min; }

    public int getPercentage() {
        return (int) Math.min(100, Math.round(100.0 * getLevelProgress() / getLevelDuration()));
    }

    public String getProgressBar(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Progress bar size must not be negative");
        }
        int filled = (int) Math.round(size * (getPercentage() / 100.0));
        return McColors.AQUA + "[" + McColors.GOLD + "▒".repeat(filled)
                + McColors.YELLOW + "░".repeat(size - filled) + McColors.AQUA + "] "
                + McColors.LIGHT_PURPLE + "(" + McColors.AQUA + getPercentage() + "%"
                + McColors.LIGHT_PURPLE + ")";
    }

    public String getProgressBarUnderline() {
        return McColors.GREEN + "Level: " + level
                + McColors.BLUE + " (" + min + " <-> " + max + ")";
    }
}
