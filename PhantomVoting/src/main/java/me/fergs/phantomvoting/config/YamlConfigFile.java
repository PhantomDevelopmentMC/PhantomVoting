package me.fergs.phantomvoting.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * A configuration file that uses the YAML format.
 */
public class YamlConfigFile extends YamlConfiguration {
    private final File file;
    /**
     * Creates a new YamlConfigFile instance.
     *
     * @param file The file to load.
     */
    public YamlConfigFile(File file) {
        this.file = file;
        reload();
    }
    /**
     * Loads a configuration file from the specified file.
     *
     * @param file The file to load.
     * @return The loaded configuration file.
     */
    public static @NotNull YamlConfigFile loadConfiguration(@NotNull File file) {
        return new YamlConfigFile(file);
    }
    /**
     * Reloads the configuration file.
     */
    public void reload() {
        try {
            load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Saves the configuration file.
     */
    public void save() {
        try {
            super.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Gets a configuration section from the configuration file.
     *
     * @param path The path to the section.
     * @return The configuration section.
     */
    @Override
    public ConfigurationSection getConfigurationSection(@NotNull String path) {
        return super.getConfigurationSection(path);
    }
    /**
     * Gets a string from the configuration file.
     *
     * @return The string.
     */
    public File getFile() {
        return file;
    }
}
