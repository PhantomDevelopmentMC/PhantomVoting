package me.fergs.phantomvoting.utils;

public class MessageParser {

    /**
     * Parse a message and replace placeholders with actual values.
     * @param message The message to parse.
     * @param placeholders A map of placeholder keys and values.
     * @return The parsed message with placeholders replaced.
     */
    public static String parse(String message, String... placeholders) {
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return message;
    }
}
