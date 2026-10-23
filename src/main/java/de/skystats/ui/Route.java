package de.skystats.ui;

import de.skystats.model.*;
import java.util.UUID;

public record Route(Mode mode, Category category, StatKey key, UUID subject, int page, Route parent) {
    public enum Mode { MAIN, SERVER, PERSONAL_CATEGORY, SERVER_CATEGORY, RANKING, MEDALS, PROFILE }
    public static Route main() { return new Route(Mode.MAIN, null, null, null, 0, null); }
    public Route child(Mode mode, Category category, StatKey key, UUID subject) { return new Route(mode, category, key, subject, 0, this); }
    public Route page(int page) { return new Route(mode, category, key, subject, page, parent); }
    public String title() {
        return switch (mode) {
            case MAIN -> "Statistiken";
            case SERVER -> "Server-Bestenlisten";
            case PERSONAL_CATEGORY -> category.shortName();
            case SERVER_CATEGORY -> "Server: " + category.shortName();
            case RANKING -> "Einzelrangliste";
            case MEDALS -> "Medaillen-Ranking";
            case PROFILE -> "Medaillenprofil";
        };
    }
}
