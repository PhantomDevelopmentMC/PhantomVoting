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
    }
    /**
     * Reloads the modules configuration.
     */
    public void reloadModules() {
        enabledModules.clear();
        loadModules();
    }
    /**
     * Gets the list of enabled modules.
     *
     * @return The list of enabled modules.
     */
    public List<Module> getEnabledModules() {
        return enabledModules != null ? new ArrayList<>(enabledModules) : new ArrayList<>();
    }
    /**
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
     * @return The reloaded configuration file.
     */
    public YamlConfigFile reloadConfig(String fileName) {
        YamlConfigFile config = loadConfig(fileName);
        config.reload();
        return config;
    }
    /**
     * Saves a configuration file.
     *
     * @param fileName The name of the file to save.
     */
    public void saveConfig(String fileName) {
        YamlConfigFile config = configurationCache.get(fileName);
        if (config != null) {
            config.save();
        }
    }
    /**
     * Saves all configuration files.
     */
    public void saveAllConfigs() {
        configurationCache.values().forEach(YamlConfigFile::save);
    }
    /**
     * Reloads all configuration files.
     */
    public void reloadAllConfigs() {
        configurationCache.keySet().forEach(this::reloadConfig);
    }
    /**
     * Removes a configuration file from the cache.
     *
     * @param fileName The name of the file to remove.
     */
    public void removeConfig(String fileName) {
        configurationCache.remove(fileName);
    }
    /**
     * Clears the configuration cache.
     */
    public void clearCache() {
        configurationCache.clear();
    }
    /**
     * Gets the plugin instance.
     *
     * @return The plugin instance.
     */
    public T getPlugin() {
        return plugin;
    }
    /**
     * Gets the configuration cache.
     *
     * @return The configuration cache.
     */
    public Map<String, YamlConfigFile> getConfigurationCache() {
        return configurationCache;
    }
}
