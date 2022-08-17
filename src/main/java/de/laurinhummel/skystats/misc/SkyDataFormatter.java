package de.laurinhummel.skystats.misc;

import java.util.Locale;

public class SkyDataFormatter {
    public static String formatTicks(long ticks) {
        int totalSecs = (int) (ticks / 20);
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

       return hours > 1 ? String.format(Locale.ENGLISH, "%02dh %02dm %02ds", hours, minutes, seconds) : String.format(Locale.ENGLISH, "%02dm %02ds", minutes, seconds);
    }

    public static long formatTicksMS(long ticks) { return ticks / 20; }

    public static String formatNumber(long number) {
        return String.format(Locale.ENGLISH, "%,d", number);
    }
    public static String formatNumber(double number) {
        return String.format(Locale.ENGLISH, "%.2f", number);
    }

    public static String formatNumberCM(long number)  {
        return String.format(Locale.ENGLISH, "%,d", number/100) + "m";
    }
}