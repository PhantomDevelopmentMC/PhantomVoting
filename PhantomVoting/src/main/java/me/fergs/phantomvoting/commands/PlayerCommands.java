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
                            player.openInventory(plugin.getMilestonesInventory().createInventory(player));
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
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfigurationManager().getConfig("modules/vote_reminder").getString("Permission-Settings.Set-Permission-Command")
                                                .replace("%player%", player.getName()));

                                        plugin.getMessageManager().sendMessage(player, "VOTE_REMINDER_TOGGLE", "%status%", "enabled");
                                    }
                                    else {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfigurationManager().getConfig("modules/vote_reminder").getString("Permission-Settings.Remove-Permission-Command")
                                                .replace("%player%", player.getName()));

                                        plugin.getMessageManager().sendMessage(player, "VOTE_REMINDER_TOGGLE", "%status%", "disabled");
                                    }
                                })
                        )
                )
                .register();
    }
}
