package me.fergs.phantomvoting.utils;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class ConsoleUtil {
    private static final Map<Character, String> colorMap = new HashMap<>();
    public static void printAsciiArt() {
        String[] art =  new String[]{
                " ",
                " ",
                "&e      /$$$$$$$  /$$                             /$$",
                "&e      | $$__  $$| $$                            | $$",
                "&e      | $$  \\ $$| $$$$$$$   /$$$$$$  /$$$$$$$  /$$$$$$    /$$$$$$  /$$$$$$/$$$$",
                "&e      | $$$$$$$/| $$__  $$ |____  $$| $$__  $$|_  $$_/   /$$__  $$| $$_  $$_  $$",
                "&e      | $$____/ | $$  \\ $$  /$$$$$$$| $$  \\ $$  | $$    | $$  \\ $$| $$ \\ $$ \\ $$",
                "&e      | $$      | $$  | $$ /$$__  $$| $$  | $$  | $$ /$$| $$  | $$| $$ | $$ | $$",
                "&e      | $$      | $$  | $$|  $$$$$$$| $$  | $$  |  $$$$/|  $$$$$$/| $$ | $$ | $$",
                "&e      |__/      |__/  |__/ \\_______/|__/  |__/   \\___/   \\______/ |__/ |__/ |__/",
                " ",
                "&6                  /$$    /$$            /$$     /$$",
                "&6                  | $$   | $$           | $$    |__/",
                "&6                  | $$   | $$ /$$$$$$  /$$$$$$   /$$ /$$$$$$$   /$$$$$$",
                "&6                  |  $$ / $$//$$__  $$|_  $$_/  | $$| $$__  $$ /$$__  $$",
                "&6                  \\  $$ $$/| $$  \\ $$  | $$    | $$| $$  \\ $$| $$  \\ $$",
                "&6                   \\  $$$/ | $$  | $$  | $$ /$$| $$| $$  | $$| $$  | $$",
                "&6                    \\  $/  |  $$$$$$/  |  $$$$/| $$| $$  | $$|  $$$$$$$",
                "&6                     \\_/    \\______/    \\___/  |__/|__/  |__/ \\____  $$",
                "&6                                                              /$$  \\ $$",
                "&6                                                             |  $$$$$$/",
                "&6                                                              \\______/",
                " ",
                "                           &eDeveloped by &6f.#0001 &f<3",
                " ",
                " "
        };
        for (String s : art) {
            Bukkit.getLogger().info(translateColors(s));
        }
    }
    /**
     * Converts a string with Minecraft-style color codes (&x) into ANSI-colored text.
     *
     * @param input The input string containing Minecraft-style color codes.
     * @return A string with ANSI escape codes for console output.
     */
    public static String translateColors(String input) {
        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length && colorMap.containsKey(chars[i + 1])) {
                result.append(colorMap.get(chars[i + 1]));
                i++;
            } else {
                result.append(chars[i]);
            }
        }
        result.append(colorMap.get('r'));
        return result.toString();
    }

    static {
        colorMap.put('0', "\033[0;30m");
        colorMap.put('1', "\033[0;34m");
        colorMap.put('2', "\033[0;32m");
        colorMap.put('3', "\033[0;36m");
        colorMap.put('4', "\033[0;31m");
        colorMap.put('5', "\033[0;35m");
        colorMap.put('6', "\033[0;33m");
        colorMap.put('7', "\033[0;37m");
        colorMap.put('8', "\033[0;90m");
        colorMap.put('9', "\033[0;94m");
        colorMap.put('a', "\033[0;92m");
        colorMap.put('b', "\033[0;96m");
        colorMap.put('c', "\033[0;91m");
        colorMap.put('d', "\033[0;95m");
        colorMap.put('e', "\033[0;93m");
        colorMap.put('f', "\033[0;97m");
        colorMap.put('r', "\033[0m");
    }
}
