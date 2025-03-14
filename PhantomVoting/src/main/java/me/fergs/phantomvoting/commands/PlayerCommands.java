package me.fergs.phantomvoting.commands;

import dev.jorel.commandapi.CommandAPICommand;
import me.fergs.phantomvoting.PhantomVoting;
import org.bukkit.Bukkit;

public class PlayerCommands{
    /**
     * Registers the player commands.
     *
     * @param plugin The plugin instance.
     */
    public void register(final PhantomVoting plugin) {
        new CommandAPICommand(plugin.getConfigurationManager().getConfig("config").getString("Commands.Base.Command", "vote"))
                .withAliases(plugin.getConfigurationManager().getConfig("config").getStringList("Commands.Base.Aliases").toArray(new String[0]))
                .executesPlayer((player, args) -> {
                    plugin.getMessageManager().sendMessage(player, "VOTE_LIST",
                            "%daily_votes%", String.valueOf(plugin.getVoteStorage().getPlayerVoteCount(player.getUniqueId(), "daily")));
                })
                .withSubcommand(new CommandAPICommand("leaderboard")
                        .executesPlayer((player, args) -> {
                            player.openInventory(plugin.getLeaderboardInventory().createInventory(player));
                        })
                )
                .withSubcommand(new CommandAPICommand("milestones")
                        .executesPlayer((player, args) -> {
                            if (!plugin.getConfigurationManager().isModuleEnabled("Milestones")) {
                                plugin.getMessageManager().sendMessage(player, "MODULE_DISABLED");
                                return;
                            }
                            if (plugin.getConfigurationManager().getConfig("modules").getBoolean("Module-Permissions.Enabled", false)) {
                                if (!player.hasPermission(plugin.getConfigurationManager().getConfig("modules").getString("Module-Permissions.Modules.Milestones.Permission", "phantomvoting.milestones"))) {
                                    plugin.getMessageManager().sendMessage(player, "NO_PERMISSION");
                                    return;
                                }
                            }
                            player.openInventory(plugin.getMilestonesInventory().createInventory(player));
                        })
                )
                .withSubcommand(new CommandAPICommand("streaks")
                        .executesPlayer((player, args) -> {
                            if (!plugin.getConfigurationManager().isModuleEnabled("Streaks-Menu")) {
                                plugin.getMessageManager().sendMessage(player, "MODULE_DISABLED");
                                return;
                            }
                        if (plugin.getConfigurationManager().getConfig("modules").getBoolean("Module-Permissions.Enabled", false)) {
                            if (!player.hasPermission(plugin.getConfigurationManager().getConfig("modules").getString("Module-Permissions.Modules.Streaks.Permission", "phantomvoting.streaks"))) {
                                plugin.getMessageManager().sendMessage(player, "NO_PERMISSION");
                                return;
                            }
                        }
                            player.openInventory(plugin.getStreaksInventory().createInventory(player));
                        })
                )
                .withSubcommand(new CommandAPICommand("toggle")
                        .withSubcommand(new CommandAPICommand("reminder")
                                .executesPlayer((player, args) -> {
                                    if (!plugin.getConfigurationManager().isModuleEnabled("VoteReminder")) {
                                        plugin.getMessageManager().sendMessage(player, "MODULE_DISABLED");
                                        return;
                                    }
                                    String permission = plugin.getConfigurationManager().getConfig("modules/vote_reminder").getString("Permission-Settings.Toggle-Permission", "phantomvoting.votereminder");
                                    if (!player.hasPermission(permission)) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfigurationManager().getConfig("modules/vote_reminder").getString("Permission-Settings.Set-Permission-Command", "this should be a command")
                                                .replace("%player%", player.getName()));

                                        plugin.getMessageManager().sendMessage(player, "VOTE_REMINDER_TOGGLE", "%status%", "enabled");
                                    }
                                    else {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfigurationManager().getConfig("modules/vote_reminder").getString("Permission-Settings.Remove-Permission-Command", "this should be a command")
                                                .replace("%player%", player.getName()));

                                        plugin.getMessageManager().sendMessage(player, "VOTE_REMINDER_TOGGLE", "%status%", "disabled");
                                    }
                                })
                        )
                )
                .register();
    }
}
