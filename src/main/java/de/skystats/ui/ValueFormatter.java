package de.skystats.ui;

import de.skystats.model.StatKey;
import java.text.NumberFormat;
import java.util.Locale;

public final class ValueFormatter {
    private ValueFormatter() {}
    public static String number(long value) { return NumberFormat.getIntegerInstance(Locale.US).format(value); }
    public static String decimal(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }
    public static String format(StatKey key, long value) {
        if (key.isTime()) return time(value);
        if (key.isDistance()) return value >= 100_000 ? decimal(value / 100_000.0) + " km" : decimal(value / 100.0) + " m (blocks)";
        if (key.isDamage()) return decimal(value / 10.0) + " damage points";
        return number(value);
    }
    public static String time(long ticks) {
        long minutes = ticks / 1200;
        return number(minutes / 1440) + "d " + (minutes / 60 % 24) + "h " + (minutes % 60) + "m " + (ticks / 20 % 60) + "s";
    }
}
