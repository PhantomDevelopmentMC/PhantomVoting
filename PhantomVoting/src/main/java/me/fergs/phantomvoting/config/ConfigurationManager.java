package me.fergs.phantomvoting.config;

import me.fergs.phantomvoting.modules.Module;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * A configuration manager for managing configuration files.
 *
 * @param <T> The plugin type.
 */
public class ConfigurationManager<T extends JavaPlugin> {
    /**
     * The plugin instance.
     */
    private final T plugin;
    /**
     * The configuration cache.
     */
    private final Map<String, YamlConfigFile> configurationCache = new HashMap<>();
    /**
     * Cache for enabled modules.
     */
    private Set<Module> enabledModules;
    /**
     * Creates a new ConfigurationManager instance.
     *
     * @param plugin The plugin instance.
     */
    public ConfigurationManager(final T plugin) {
        this.plugin = plugin;
    }
    /**
     * Loads a configuration file from the plugin's data folder.
     *
     * @param fileName The name of the file to load.
     * @return The loaded configuration file.
     */
    public YamlConfigFile loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!configFile.exists()) {
            plugin.saveResource(fileName + ".yml", false);
        }

        YamlConfigFile config = YamlConfigFile.loadConfiguration(configFile);
        configurationCache.put(fileName, config);
        return config;
    }
    /**
     * Loads the modules configuration (modules.yml).
     */
    public void loadModules() {
        YamlConfigFile modulesConfig = getConfig("modules");
        enabledModules = new HashSet<>();
        if (modulesConfig != null) {
            ConfigurationSection modulesSection = modulesConfig.getConfigurationSection("Modules");

            if (modulesSection != null) {
                for (String moduleName : modulesSection.getKeys(false)) {
                    boolean isEnabled = modulesSection.getBoolean(moduleName + ".Enabled", false);
                    Module module = new Module(moduleName, isEnabled);
                    if (module.isEnabled()) {
                        plugin.getLogger().info("Enabling module: " + moduleName);
                        enabledModules.add(module);
                    }
                }
            } else {
                plugin.getLogger().warning("Modules section is not properly configured.");
            }
        }
    }/**
     * Checks if a module is enabled.
     *
     * @param moduleName The name of the module to check.
     * @return True if the module is enabled, otherwise false.
     */
    public boolean isModuleEnabled(String moduleName) {
        return enabledModules.stream().anyMatch(module -> module.getName().equalsIgnoreCase(moduleName));
    }
    /**
     * Loads multiple configuration files from the plugin's data folder.
     *
     * @param fileNames The names of the files to load.
     */
    public void loadConfigs(String... fileNames) {
        for (String fileName : fileNames) {
            loadConfig(fileName);
        }
    }
    /**
     * Gets a configuration file from the cache.
     *
     * @param fileName The name of the file to get.
     * @return The configuration file.
     */
    public YamlConfigFile getConfig(String fileName) {
        return configurationCache.computeIfAbsent(fileName, this::loadConfig);
    }
    /**
     * Reloads a configuration file.
     *
     * @param fileName The name of the file to reload.
     */
    public void reloadConfig(String fileName) {
        YamlConfigFile config = loadConfig(fileName);
        config.reload();
    }
    /**
     * Reloads all configuration files.
     */
    public void reloadAllConfigs() {
        configurationCache.keySet().forEach(this::reloadConfig);
    }
    /**
     * Gets the plugin instance.
     *
     * @return The plugin instance.
     */
    public T getPlugin() {
        return plugin;
    }
}
