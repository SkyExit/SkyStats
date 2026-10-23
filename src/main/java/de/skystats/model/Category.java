package de.skystats.model;

public enum Category {
    GENERAL("Allgemein", "Allgemeine Statistiken", "BOOK", "custom", "Wert"),
    MINED("Abgebaute Blöcke", "Abgebaute Blöcke", "IRON_PICKAXE", "mined", "Abgebaut"),
    CRAFTED("Hergestellte Items", "Hergestellte Items", "CRAFTING_TABLE", "crafted", "Hergestellt"),
    USED("Benutzte Items", "Benutzte Items / platzierte Blöcke", "OAK_PLANKS", "used", "Benutzt"),
    BROKEN("Zerbrochene Items", "Zerbrochene / verbrauchte Items", "ANVIL", "broken", "Zerbrochen"),
    PICKED_UP("Aufgehobene Items", "Aufgehobene Items", "HOPPER", "picked_up", "Aufgehoben"),
    DROPPED("Weggeworfene Items", "Weggeworfene Items", "DROPPER", "dropped", "Weggeworfen"),
    KILLED("Getötete Kreaturen", "Getötete Kreaturen", "IRON_SWORD", "killed", "Getötet"),
    KILLED_BY("Tode durch Kreaturen", "Von Kreaturen getötet", "SKELETON_SKULL", "killed_by", "Tode durch diese Kreatur"),
    MOVEMENT("Fortbewegung", "Fortbewegung", "LEATHER_BOOTS", "custom", "Strecke"),
    PLAY_TIME("Spielzeit", "Spielzeit", "CLOCK", "custom", "Spielzeit"),
    OTHER("Weitere Statistiken", "Weitere Vanilla-Statistiken", "PAPER", "", "Wert");

    private final String shortName, title, icon, type, valueLabel;
    Category(String shortName, String title, String icon, String type, String valueLabel) {
        this.shortName = shortName; this.title = title; this.icon = icon; this.type = type; this.valueLabel = valueLabel;
    }
    public String shortName() { return shortName; }
    public String title() { return title; }
    public String icon() { return icon; }
    public String valueLabel() { return valueLabel; }
    public boolean contains(StatKey key) {
        if (this == MOVEMENT) return key.isDistance();
        if (this == PLAY_TIME) return key.isPlayTime();
        if (this == OTHER) {
            for (Category category : values()) {
                if (category != OTHER && category.contains(key)) return false;
            }
            return true;
        }
        return key.type().equals("minecraft:" + type);
    }
}
