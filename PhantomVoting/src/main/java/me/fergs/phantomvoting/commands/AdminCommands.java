package me.fergs.phantomvoting.commands;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import me.fergs.phantomvoting.PhantomVoting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AdminCommands {
    /**
     * Registers the admin commands.
     *
     * @param plugin The plugin instance.
     */
    public void register(final PhantomVoting plugin) {
        new CommandAPICommand(plugin.getConfigurationManager().getConfig("config").getString("Commands.Admin.Base", "phantomvoting"))
                .withAliases(plugin.getConfigurationManager().getConfig("config").getStringList("Commands.Admin.Aliases").toArray(new String[0]))
                .withPermission("phantomvoting.admin")
                .executes((player, args) -> {
                    plugin.getMessageManager().sendMessage(player, "ADMIN_HELP", "%admin_command%", plugin.getConfigurationManager().getConfig("config").getString("Commands.Admin.Base", "phantomvoting"));
                })
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((player, args) -> {
                            plugin.getConfigurationManager().reloadAllConfigs();
                            plugin.getVotePartyManager().reloadThreshold();
                            plugin.getLeaderboardInventory().reloadInventory();
                            if (plugin.getConfigurationManager().isModuleEnabled("Milestones")) {
                                plugin.getMilestonesInventory().reloadInventory();
                            }
                            if (plugin.getConfigurationManager().isModuleEnabled("VoteReminder")) {
                                plugin.getVoteReminderManager().reloadTask("VoteReminder");
                            }
                            plugin.getMessageManager().sendMessage(player, "RELOAD");
                        })
                )
                .withSubcommand(new CommandAPICommand("givevote")
                        .withArguments(new PlayerArgument("player"))
                        .executes((player, args) -> {
                            Player target = (Player) args.get("player");
                            assert target != null;
                            plugin.getVoteStorage().addVote(target.getUniqueId());
                            plugin.getMessageManager().sendMessage(player, "GIVE_VOTE", "%player%", target.getName());
                        })
                )
                .withSubcommand(new CommandAPICommand("testvote")
                        .withArguments(new PlayerArgument("player"))
                        .executes((player, args) -> {
                            Player target = (Player) args.get("player");
                            if (target == null) {
                                throw new IllegalArgumentException("Player not found");
                            }
                            Vote vote;
                            vote = new Vote(
                                    "TestVote",
                                    target.getName(),
                                    "127.0.0.1",
                                    Long.toString(System.currentTimeMillis(), 10)
                            );
                            Bukkit.getPluginManager().callEvent(new VotifierEvent(vote));
                        })
                )
                .withSubcommand(new CommandAPICommand("removevote")
                        .withArguments(new PlayerArgument("player"), new IntegerArgument("amount"))
                        .executes((player, args) -> {
                            Player target = (Player) args.get("player");
                            int amount = (int) args.get("amount");
                            assert target != null && amount > 0;
                            plugin.getVoteStorage().removeVote(target.getUniqueId(), amount);
                            plugin.getMessageManager().sendMessage(player, "REMOVE_VOTE", "%player%", target.getName());
                        })
                )
                .withSubcommand(new CommandAPICommand("voteparty")
                        .withSubcommand(new CommandAPICommand("forcestart")
                                .withArguments(new BooleanArgument("reset_vote_progress"))
                                .executes((player, args) -> {
                                    boolean resetVoteProgress = (boolean) args.get("reset_vote_progress");
                                    plugin.getVotePartyManager().forceVoteParty(resetVoteProgress);
                                })
                        )
                        .withSubcommand(new CommandAPICommand("add")
                                .withArguments(new IntegerArgument("amount"))
                                .executes((player, args) -> {
                                    int amount = (int) args.get("amount");
                                    plugin.getVotePartyManager().forceAddAmount(amount);
                                })
                        )
                        .withSubcommand(new CommandAPICommand("set")
                                .withArguments(new IntegerArgument("amount"))
                                .executes((player, args) -> {
                                    int amount = (int) args.get("amount");
                                    plugin.getVotePartyManager().setCurrentVoteCount(amount);
                                })
                        )
                )
                .register();
    }
}
