package me.fergs.phantomvoting.modules;

public class Module {
    /**
     * The name of the module.
     */
    private final String name;
    /**
     * Whether the module is enabled.
     */
    private final boolean enabled;
    /**
     * Creates a new Module instance.
     *
     * @param name    The name of the module.
     * @param enabled Whether the module is enabled.
     */
    public Module(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }
    /**
     * Gets the name of the module.
     *
     * @return The name of the module.
     */
    public String getName() {
        return name;
    }
    /**
     * Checks if the module is enabled.
     *
     * @return True if the module is enabled, otherwise false.
     */
    public boolean isEnabled() {
        return enabled;
    }
}