package de.raidcraft.achievements.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {}

    public static String formatDateTime(@Nullable Instant instant, String format) {

        if (instant == null) {
            return "N/A";
        }

        return DateTimeFormatter.ofPattern(format)
                .withLocale(Locale.GERMAN)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    public static String formatDateTime(Instant instant) {

        return formatDateTime(instant, "dd.MM.yyyy HH:mm");
    }
}
