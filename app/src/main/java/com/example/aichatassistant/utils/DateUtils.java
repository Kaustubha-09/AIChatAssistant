package com.example.aichatassistant.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    private DateUtils() {}

    /**
     * Returns a human-friendly timestamp string:
     *   Same calendar day  → "3:45 PM"
     *   Same calendar year → "Mar 5, 3:45 PM"
     *   Older              → "Mar 5 2023, 3:45 PM"
     */
    public static String formatTimestamp(long millis) {
        Date     date = new Date(millis);
        Calendar now  = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTime(date);

        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
            if (now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
                return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(date);
            }
            return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date);
        }
        return new SimpleDateFormat("MMM d yyyy, h:mm a", Locale.getDefault()).format(date);
    }
}
