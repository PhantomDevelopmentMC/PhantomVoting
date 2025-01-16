package me.fergs.phantomvoting.objects.voteparty;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;

import java.util.Set;

public class ChanceCommandGroup {
    private final double chance;
    private final ImmutableSet<String> commands;

    public ChanceCommandGroup(double chance, Set<String> commands) {
        if (chance < 0.0 || chance > 100.0) {
            Bukkit.getLogger().warning("Chance must be between 0 and 100.");
        }
        this.chance = chance;
        this.commands = ImmutableSet.copyOf(commands);
    }
    /**
     * Gets the chance as a percentage (0-100).
     *
     * @return The chance percentage.
     */
    public double getChance() {
        return chance;
    }
    /**
     * Gets the commands associated with this chance group.
     *
     * @return An immutable set of commands.
     */
    public Set<String> getCommands() {
        return commands;
    }
}
