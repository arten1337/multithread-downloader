package com.example.downloader.util;

public final class FormatUtils {

    private FormatUtils() {}

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        exp = Math.min(exp, units.length);
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), units[exp - 1]);
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
}
