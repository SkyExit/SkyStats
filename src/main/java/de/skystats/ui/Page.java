package de.skystats.ui;

public record Page(int index, int count, int from, int to) {
    public static final int CONTENT_SIZE = 45;
    public static Page of(int entries, int requested) {
        int count = Math.max(1, (int) ((entries + 44L) / CONTENT_SIZE));
        int index = Math.max(0, Math.min(requested, count - 1));
        int from = index * CONTENT_SIZE;
        return new Page(index, count, from, Math.min(from + CONTENT_SIZE, entries));
    }
    public boolean hasPrevious() { return index > 0; }
    public boolean hasNext() { return index + 1 < count; }
}
