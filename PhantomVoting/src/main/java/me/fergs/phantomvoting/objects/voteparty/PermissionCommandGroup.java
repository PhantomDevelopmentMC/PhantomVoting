package me.fergs.phantomvoting.objects.voteparty;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;

import java.util.Set;

public class PermissionCommandGroup {
    private final String permission;
    private final ImmutableSet<String> commands;
    /**
     * Creates a new permission command group.
     *
     * @param permission The permission required to execute the commands.
     * @param commands   The commands associated with this permission group.
     */
    public PermissionCommandGroup(String permission, Set<String> commands) {
        if (permission == null || permission.isEmpty()) {
            Bukkit.getLogger().warning("Permission cannot be null or empty.");
        }
        this.permission = permission;
        this.commands = ImmutableSet.copyOf(commands);
    }
    /**
     * Gets the permission required to execute the commands.
     *
     * @return The permission.
     */
    public String getPermission() {
        return permission;
    }
    /**
     * Gets the commands associated with this permission group.
     *
     * @return An immutable set of commands.
     */
    public Set<String> getCommands() {
        return commands;
    }
}