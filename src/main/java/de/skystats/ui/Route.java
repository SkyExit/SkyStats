package de.skystats.ui;

import de.skystats.model.*;
import java.util.UUID;

public record Route(Mode mode, Category category, StatKey key, UUID subject, int page, Route parent) {
    public enum Mode { MAIN, SERVER, PERSONAL_CATEGORY, SERVER_CATEGORY, RANKING, MEDALS, PROFILE }
    public static Route main() { return new Route(Mode.MAIN, null, null, null, 0, null); }
    public Route child(Mode mode, Category category, StatKey key, UUID subject) { return new Route(mode, category, key, subject, 0, this); }
    public Route page(int page) { return new Route(mode, category, key, subject, page, parent); }
    public boolean serverView() {
        return mode == Mode.SERVER || mode == Mode.SERVER_CATEGORY || mode == Mode.RANKING || mode == Mode.MEDALS;
    }
    public String title() {
        return switch (mode) {
            case MAIN -> "Statistics";
            case SERVER -> "Server leaderboards";
            case PERSONAL_CATEGORY -> category.shortName();
            case SERVER_CATEGORY -> "Server: " + category.shortName();
            case RANKING -> "Leaderboard";
            case MEDALS -> "Medal ranking";
            case PROFILE -> "Medal profile";
        };
    }
}
