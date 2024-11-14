# Phantom Voting

![Phantom Voting](https://github.com/user-attachments/assets/3514cb33-70bb-4c37-97bf-74d89e9748b9)
*Seamless and optimized voting system for Minecraft servers.*

---

## 📊 Project Stats

- **📈 Clones**: null total, with null unique cloners
- **👀 Views**: null total, with null unique visitors
- **🌐 Languages**: Java (100%)
- **🛠️ Dependencies**: [NuVotifier](https://www.spigotmc.org/resources/nuvotifier.13488/) (for vote handling)
- **📅 Last Updated**: ![Last Commit](https://img.shields.io/github/last-commit/PhantomDevelopmentMC/PhantomVoting)

---

## 📖 Overview

**Phantom Voting** is an open-source Minecraft plugin designed to streamline and enhance server voting experiences. Integrated with **NuVotifier**, Phantom Voting offers a powerful system for tracking player votes, rewarding participation, and fostering community engagement. Easily configure vote tracking, streaks, and reward distributions to create an interactive and rewarding environment for your players.

### Key Highlights
- **Leaderboard GUI**: View top voters in-game with a stylish, dynamic leaderboard.
- **Comprehensive Vote Tracking**: Track daily, weekly, monthly, yearly, and all-time votes per player.
- **Vote Party System**: Reward all players after a set number of votes.
- **Custom Rewards**: Tailor rewards to player contributions with easy-to-configure options.

---

## 🚀 Features

- **Test Voting**: Run test votes for players to ensure everything is working smoothly.
- **Vote Streaks**: Track and reward players based on their voting streaks.
- **Comprehensive Leaderboards**: Display top voters with in-game GUI.
- **Vote Party Support**: Set up vote goals for server-wide rewards.
- **Fully Customizable**: All messages and rewards can be customized in `messages.yml`.
- **Lightweight & Optimized**: Designed to minimize impact on server performance.

---

## 📥 Installation

1. **Download Phantom Voting**  
   Download the latest release from the [GitHub Releases](https://github.com/PhantomDevelopmentMC/PhantomVoting/releases) page and place it in your server’s `plugins` directory.

2. **Install NuVotifier**  
   Make sure [NuVotifier](https://www.spigotmc.org/resources/nuvotifier.13488/) is installed and configured on your server.

3. **Restart Your Server**  
   Start or restart your server to load Phantom Voting. Configure the plugin by editing `config.yml` as desired.

---

## 🛠️ Commands & Placeholders

### Player Commands
- `/vote` - Displays a list of voting sites (customizable in `messages.yml`).
- `/vote leaderboard` - Opens the leaderboard GUI with the top 10 players.

### Admin Commands
- `/votingadmin reload` - Reloads all configuration files.
- `/votingadmin givevote {player}` - Gives 1 vote to the specified player.
- `/votingadmin removevote {player} [amount]` - Removes a specified amount of votes from a player.
- `/votingadmin testvote {player}` - Simulates a vote for testing purposes.

### Placeholders
- `%phantomvoting_daily_votes%` - Returns the player’s daily votes.
- `%phantomvoting_weekly_votes%` - Returns the player’s weekly votes.
- `%phantomvoting_monthly_votes%` - Returns the player’s monthly votes.
- `%phantomvoting_yearly_votes%` - Returns the player’s yearly votes.
- `%phantomvoting_all_time_votes%` - Returns the player’s all-time votes.
- `%phantomvoting_vote_party_count%` - Returns the current count towards a vote party.
- `%phantomvoting_vote_party_threshold%` - Shows the votes needed for a vote party.
- `%phantomvoting_vote_streak%` - Displays the player’s current voting streak.

---

## 🤝 Support

If you encounter issues, have feature requests, or find bugs, please report them on our [GitHub Issues page](https://github.com/PhantomDevelopmentMC/PhantomVoting/issues). For general questions, join our [Discord Community](https://discord.gg/3Vb8w9b8kg) for help and discussions.

---

## 📝 License

Phantom Voting is open-source and licensed under the MIT License. See [LICENSE](LICENSE) for more details.

---
