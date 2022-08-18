package de.laurinhummel.skystats.managers;

public class Level {
    private int level;
    private LevelBorder levelBorder;
    private int totalEXP;

    public Level(int level, LevelBorder levelBorder, int totalEXP) {
        this.level = level;
        this.levelBorder = levelBorder;
        this.totalEXP = totalEXP;
    }

    public int getLevel() {
        return level;
    }

    public int getMinEXP() {
        return levelBorder.getMin();
    }

    public int getMaxEXP() {
        return levelBorder.getMax();
    }

    public double getLevelDuration() { return getMaxEXP() - getMinEXP(); }

    public double getLevelProgress() { return totalEXP - getMinEXP(); }

    public int getPercentage() { return (int) Math.round((getLevelProgress() / getLevelDuration()) * 100); }
}
