package me.fergs.phantomvoting.objects.voteparty;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;

import java.util.Set;

public class PermissionCommandGroup {
    private final String permission;
    private final ImmutableSet<String> commands;

    public PermissionCommandGroup(String permission, Set<String> commands) {
        if (permission == null || permission.isEmpty()) {
            Bukkit.getLogger().warning("Permission cannot be null or empty.");
        }
        this.permission = permission;
        this.commands = ImmutableSet.copyOf(commands);
    }

    public String getPermission() {
        return permission;
    }

    public Set<String> getCommands() {
        return commands;
    }
}