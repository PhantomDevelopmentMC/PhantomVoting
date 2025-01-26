package me.fergs.phantomvoting.managers;

import me.fergs.phantomvoting.utils.ConsoleUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

/**
 * A registry for managing event listeners.
 *
 * @param <T> the plugin type
 */
public class ListenerManager<T extends Plugin> {
    private final T plugin;
    private final Set<Class<? extends Listener>> registeredListeners = new HashSet<>();
    /**
     * Constructs a ListenerRegistry for managing event listeners.
     *
     * @param plugin the plugin instance used for listener registration
     */
    public ListenerManager(T plugin) {
        this.plugin = plugin;
    }
    /**
     * Registers a new event listener class.
     *
     * @param listenerClass the listener class to register
     */
    public void registerListener(Class<? extends Listener> listenerClass) {
        if (registeredListeners.contains(listenerClass)) {
            plugin.getLogger().warning(listenerClass.getName() + " is already registered.");
            return;
        }
        try {
            Listener listenerInstance = listenerClass.getDeclaredConstructor().newInstance();
            Bukkit.getPluginManager().registerEvents(listenerInstance, plugin);
            registeredListeners.add(listenerClass);
            Bukkit.getLogger().info(ConsoleUtil.translateColors("&6[&e!&6] &eRegistered listener &f" + listenerClass.getSimpleName() + "&e."));
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().severe("Failed to register listener " + listenerClass.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Registers multiple listener classes at once.
     *
     * @param listenerClasses varargs array of listener classes to register
     */
    @SafeVarargs
    public final void registerListeners(Class<? extends Listener>... listenerClasses) {
        for (Class<? extends Listener> listenerClass : listenerClasses) {
            registerListener(listenerClass);
        }
    }
    /**
     * Retrieves the plugin instance used for listener registration.
     *
     * @return the plugin instance
     */
    public T getPlugin() {
        return plugin;
    }
}
