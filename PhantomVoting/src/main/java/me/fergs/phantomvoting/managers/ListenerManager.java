package me.fergs.phantomvoting.managers;

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
     * Checks if a listener class is registered.
     *
     * @param listenerClass the listener class to check
     * @return true if the listener is registered, false otherwise
     */
    public boolean isListenerRegistered(Class<? extends Listener> listenerClass) {
        return registeredListeners.contains(listenerClass);
    }
    /**
     * Gets all registered listener classes.
     *
     * @return a set of all registered listener classes
     */
    public Set<Class<? extends Listener>> getRegisteredListeners() {
        return new HashSet<>(registeredListeners);
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
