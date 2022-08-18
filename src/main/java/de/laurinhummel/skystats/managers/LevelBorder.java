package de.laurinhummel.skystats.managers;

public class LevelBorder {
    private int min;
    private int max;

    public LevelBorder(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() { return min; }

    public int getMax() { return max; }
}
