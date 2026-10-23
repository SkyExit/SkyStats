package de.skystats.ui;

import de.skystats.model.Category;

/** A symmetric frame around the central leaderboard/medals button. */
public final class MenuLayout {
    public static final int CENTER = 22;
    private MenuLayout() {}

    public static int slot(Category category) {
        return switch (category) {
            case GENERAL -> 4;
            case MINED -> 10;
            case CRAFTED -> 12;
            case USED -> 14;
            case BROKEN -> 16;
            case PICKED_UP -> 19;
            case DROPPED -> 25;
            case KILLED -> 28;
            case KILLED_BY -> 30;
            case MOVEMENT -> 32;
            case PLAY_TIME -> 34;
            case OTHER -> 40;
        };
    }
}
