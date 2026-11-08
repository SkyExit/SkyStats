package de.skystats.ui;

import de.skystats.model.*;
import java.util.UUID;

public record Route(Mode mode, Category category, StatKey key, UUID subject, int page, Route parent, int medalTier) {
    public enum Mode { MAIN, SERVER, PERSONAL_CATEGORY, SERVER_CATEGORY, STATISTIC, RANKING, MEDALS, PROFILE }
    public Route(Mode mode, Category category, StatKey key, UUID subject, int page, Route parent) {
        this(mode, category, key, subject, page, parent, 0);
    }
    public static Route main() { return new Route(Mode.MAIN, null, null, null, 0, null); }
    public Route child(Mode mode, Category category, StatKey key, UUID subject) { return new Route(mode, category, key, subject, 0, this); }
    public Route page(int page) { return new Route(mode, category, key, subject, page, parent, medalTier); }
    public Route medalPage(int rank, int page) { return new Route(mode, category, key, subject, page, parent, rank); }
    public boolean serverView() {
        return mode == Mode.SERVER || mode == Mode.SERVER_CATEGORY || mode == Mode.RANKING || mode == Mode.MEDALS;
    }
    public String title() {
        return switch (mode) {
            case MAIN -> "Statistics";
            case SERVER -> "Server leaderboards";
            case PERSONAL_CATEGORY -> category.shortName();
            case SERVER_CATEGORY -> "Server: " + category.shortName();
            case STATISTIC -> "Statistic details";
            case RANKING -> "Leaderboard";
            case MEDALS -> "Medal ranking";
            case PROFILE -> "Medal profile";
        };
    }
}
