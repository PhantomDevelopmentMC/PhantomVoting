package me.fergs.phantomvoting.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.fergs.phantomvoting.PhantomVoting;
import me.fergs.phantomvoting.config.ConfigurationManager;
import me.fergs.phantomvoting.utils.Color;
import me.fergs.phantomvoting.utils.MessageParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager<T extends PhantomVoting> {
    private final ConfigurationManager<?> configurationManager;
    private final T plugin;
    private final Pattern CLICKABLE_PATTERN = Pattern.compile("\\((.*?)\\)\\[(.*?)]");

    public MessageManager(T plugin, ConfigurationManager<?> config) {
        this.configurationManager = config;
        this.plugin = plugin;
    }

    public void sendMessage(CommandSender executor, String key, String... placeholders) {
        Optional<List<String>> message = getMessage(key);
        Optional<String> sound = getSound(key);

        if (message.isPresent() && isMessageEnabled(key)) {
            if (executor instanceof Player) {
                Player player = (Player) executor;

                if (key.equalsIgnoreCase("VOTE_LIST")) {
                    sendVoteMessage(player, message.get(), placeholders);
                } else {
                    message.get().forEach(line -> {
                        String formattedMessage = MessageParser.parse(line, placeholders);
                        executor.sendMessage(Component.text(Color.hex(PlaceholderAPI.setPlaceholders(player, formattedMessage))));
                    });
                }
            } else {
                message.get().forEach(line -> {
                    String formattedMessage = MessageParser.parse(line, placeholders);
                    executor.sendMessage(Component.text(Color.hex(formattedMessage)));
                });
            }
        }

        if (isTitleEnabled(key) && executor instanceof Player) {
            Optional<String> title = getTitle(key);
            Optional<String> subtitle = getSubtitle(key);
            title.ifPresent(titleString -> ((Player) executor).sendTitle(
                    Color.hex(MessageParser.parseKeyedValues(titleString, placeholders)),
                    subtitle.map(s -> Color.hex(MessageParser.parse(s, placeholders))).orElse(null)
            ));
        }

        if (sound.isPresent() && isSoundEnabled(key) && executor instanceof Player) {
            playSound((Player) executor, sound.get());
        }
    }

    private void sendVoteMessage(Player player, List<String> lines, String... placeholders) {
        lines.forEach(line -> {
            line = Color.hex(PlaceholderAPI.setPlaceholders(player, line));
            String formattedLine = MessageParser.parse(line, placeholders);
            Component component = parseLine(formattedLine);
            player.sendMessage(component);
        });
    }

    public Component parseLine(String line) {
        Matcher matcherTest = CLICKABLE_PATTERN.matcher(line);
        if (!matcherTest.find()) {
            return Component.text(line);
        }

        TextComponent.Builder builder = Component.text();
        int lastIndex = 0;
        Matcher matcher = CLICKABLE_PATTERN.matcher(line);
        while (matcher.find()) {
            int start = matcher.start();
            if (start > lastIndex) {
                builder.append(Component.text(line.substring(lastIndex, start)));
            }
            String clickableText = matcher.group(1);
            String url = matcher.group(2);
            Component clickableComponent = Component.text(clickableText)
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(Component.text("Click me!")));
            builder.append(clickableComponent);
            lastIndex = matcher.end();
        }
        if (lastIndex < line.length()) {
            builder.append(Component.text(line.substring(lastIndex)));
        }
        return builder.build();
    }
    /**
     * Broadcast a message to all players if enabled in the config.
     * @param key The key for the message in the config.
     * @param placeholders A map of placeholders to replace in the message.
     */
    public void broadcastMessage(String key, String... placeholders) {
        Optional<List<String>> message = getMessage(key);
        Optional<String> sound = getSound(key);

        if (message.isPresent() && isMessageEnabled(key)) {
            message.get().forEach(line -> {
                String formattedMessage = MessageParser.parse(line, placeholders);
                Bukkit.getServer().broadcast(Component.text(Color.hex(PlaceholderAPI.setPlaceholders(null, formattedMessage))));
            });
        }

        if (isTitleEnabled(key)) {
            Optional<String> title = getTitle(key);
            Optional<String> subtitle = getSubtitle(key);

            plugin.getPlayerManager().getPlayers().forEach(player -> {
                title.ifPresent(titleString -> player.sendTitle(Color.hex(MessageParser.parseKeyedValues(titleString, placeholders)), subtitle.map(s -> Color.hex(MessageParser.parse(s, placeholders))).orElse(null)));
            });
        }

        if (sound.isPresent() && isSoundEnabled(key)) {
            plugin.getPlayerManager().getPlayers().forEach(player -> playSound(player, sound.get()));
        }
    }
    /**
     * Get the message from the config.
     * @param key The key for the message in the config.
     * @return The message from the config, if available.
     */
    private Optional<List<String>> getMessage(String key) {
        List<String> messages = configurationManager.getConfig("messages").getStringList("Messages." + key + ".Message.Value");
        if (!messages.isEmpty()) {
            return Optional.of(messages);
        } else {
            return Optional.ofNullable(configurationManager.getConfig("messages").getString("Messages." + key + ".Message.Value"))
                    .map(Collections::singletonList);
        }
    }
    /**
     * Get the sound from the config.
     * @param key The key for the sound in the config.
     * @return The sound from the config, if available.
     */
    private Optional<String> getSound(String key) {
        return Optional.ofNullable(configurationManager.getConfig("messages").getString("Messages." + key + ".Sound.Value"));
    }
    /**
     * Get the title from the config.
     * @param key The key for the title in the config.
     * @return The title from the config, if available.
     */
    private Optional<String> getTitle(String key) {
        return Optional.ofNullable(configurationManager.getConfig("messages").getString("Messages." + key + ".Title.Title"));
    }
    /**
     * Get the subtitle from the config.
     * @param key The key for the subtitle in the config.
     * @return The subtitle from the config, if available.
     */
    private Optional<String> getSubtitle(String key) {
        return Optional.ofNullable(configurationManager.getConfig("messages").getString("Messages." + key + ".Title.Subtitle"));
    }
    /**
     * Check if the message is enabled.
     * @param key The key for the message in the config.
     * @return True if the message is enabled, false otherwise.
     */
    private boolean isMessageEnabled(String key) {
        return configurationManager.getConfig("messages").getBoolean("Messages." + key + ".Message.Enable", false);
    }
    /**
     * Check if the sound is enabled.
     * @param key The key for the sound in the config.
     * @return True if the sound is enabled, false otherwise.
     */
    private boolean isSoundEnabled(String key) {
        return configurationManager.getConfig("messages").getBoolean("Messages." + key + ".Sound.Enable", false);
    }
    /**
     * Check if the title is enabled.
     * @param key The key for the title in the config.
     * @return True if the title is enabled, false otherwise.
     */
    private boolean isTitleEnabled(String key) {
        return configurationManager.getConfig("messages").getBoolean("Messages." + key + ".Title.Enable", false);
    }
    /**
     * Play the sound for the player.
     * @param player The player to play the sound to.
     * @param soundData The sound data in the format: "sound;volume;pitch".
     */
    private void playSound(Player player, String soundData) {
        String[] parts = soundData.split(";");
        if (parts.length >= 3) {
            String soundName = parts[0];
            float volume = Float.parseFloat(parts[1]);
            float pitch = Float.parseFloat(parts[2]);

            Sound sound = Sound.valueOf(soundName);
            player.playSound(player, sound, volume, pitch);
        }
    }
}
