package me.fergs.phantomvoting.utils;

public class FormatUtil {
    /**
     * Formats a timestamp into a readable format.
     *
     * @param timeInSeconds The time to format.
     * @return The formatted time.
     */
    public static String formatTimeStamp(long timeInSeconds) {
        if (timeInSeconds <= 0) {
            return "0s";
        }
        long days = timeInSeconds / 86400;
        long hours = (timeInSeconds % 86400) / 3600;
        long minutes = (timeInSeconds % 3600) / 60;
        long seconds = timeInSeconds % 60;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) {
            formattedTime.append(days).append("d ");
        }
        if (hours > 0) {
            formattedTime.append(hours).append("h ");
        }
        if (minutes > 0) {
            formattedTime.append(minutes).append("m ");
        }
        if (seconds > 0 || formattedTime.length() == 0) {
            formattedTime.append(seconds).append("s");
        }
        return formattedTime.toString().trim();
    }
}
