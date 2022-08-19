package de.laurinhummel.skystats.managers;

import de.laurinhummel.skystats.misc.McColors;
import org.bukkit.entity.Player;

public class Level {
    private int level;
    private int min;
    private int max;
    private int totalEXP;

    public Level(int level, int min, int max, int totalEXP) {
        this.level = level;
        this.min = min;
        this.max = max;
        this.totalEXP = totalEXP;
    }

    public int getLevel() {
        return level;
    }

    public int getMinEXP() {
        return min;
    }

    public int getMaxEXP() {
        return max;
    }

    public int getLevelDuration() { return getMaxEXP() - getMinEXP(); }

    public int getLevelProgress() { return totalEXP - getMinEXP(); }

    public int getPercentage() { return (int) Math.round(((double) getLevelProgress() / getLevelDuration()) * 100); }

    public String getProgressBar(int size) {
        long r1 = Math.round(size * ((double)getPercentage() / 100));
        long r2 = size - r1;

        return McColors.AQUA + "["
                + McColors.GOLD + "▒".repeat((int) r1)
                + McColors.YELLOW + "░".repeat((int) r2)
                + McColors.AQUA + "] "
                + McColors.LIGHT_PURPLE + "(" + McColors.AQUA + getPercentage() + "%"
                + McColors.LIGHT_PURPLE + ")";
    }

    public String getProgressBarUnderline() {
        return McColors.GREEN + "Level: " + getLevel()
                + McColors.BLUE + "(" + getMinEXP() + " - " + getMaxEXP() + ")";
    }
}
