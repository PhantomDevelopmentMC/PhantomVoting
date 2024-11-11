package me.fergs.phantomvoting.commands;

import dev.jorel.commandapi.CommandAPICommand;
import me.fergs.phantomvoting.PhantomVoting;

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
                .register();
    }
}
