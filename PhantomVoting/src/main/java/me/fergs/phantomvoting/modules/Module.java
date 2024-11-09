package me.fergs.phantomvoting.modules;

public class Module {
    private final String name;
    private final boolean enabled;

    public Module(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }
}