# ControlPlayers

A powerful Minecraft 1.8.8 Spigot plugin that allows server administrators to manage players directly from Discord using bot commands. Control your server moderation, view player information, and track staff activity - all from your Discord server.

## 🌟 Features

- **Discord Integration**: Execute server commands directly from Discord
- **Player Management**: Ban, kick, mute, unban, and unmute players
- **Player Information**: View online players list and get player IP addresses
- **Staff History Tracking**: Track and view staff command history with SQLite database
- **Multi-Plugin Support**: Compatible with vanilla commands and all moderation plugins
- **Customizable Messages**: Fully customizable Discord embed messages and colors
- **Permission System**: Role-based access control for different commands
- **Channel Restrictions**: Limit bot usage to specific Discord channels
- **Real-time Synchronization**: Commands are executed in real-time on the Minecraft server

## 📋 Requirements

- **Minecraft Server**: Spigot/Paper 1.8.8+
- **Java**: Java 15+
- **Discord Bot**: A Discord bot with appropriate permissions
- **Optional**: LiteBans, EssentialsX, AdvancedBan or others for enhanced moderation features

## 🚀 Installation

### Step 1: Download and Install
1. Download the latest `ControlPlayers.jar` from the releases page
2. Place the jar file in your server's `plugins` folder
3. Restart your Minecraft server

### Step 2: Create Discord Bot
1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application and bot
3. Copy the bot token (you'll need this for configuration)
4. Invite the bot to your Discord server with the following permissions:
   - Read Messages
   - Send Messages
   - Use Slash Commands
   - Embed Links
   - Read Message History

### Step 3: Initial Configuration
1. Start your server to generate default configuration files
2. Stop the server
3. Configure the plugin (see Configuration section below)
4. Start the server again

## ⚙️ Configuration

### config.yml

The main configuration file located in `plugins/ControlPlayers/config.yml`:

```yaml
discord:
  # Discord bot token (REQUIRED)
  token: "YOUR_BOT_TOKEN_HERE"
  
  # Command prefix for Discord
  prefix: "!"
  
  # Allowed Discord channels (Channel IDs)
  allowed-channels:
    - "123456789012345678"
  
  # Users allowed to use basic commands (User IDs)
  allowed-users:
    - "123456789012345678"
  
  # Users allowed to use histstaff command (User IDs)
  # WARNING: This reveals sensitive information!
  history-allowed-users:
    - "123456789012345678"

database:
  # Enable/disable command logging
  enabled: true
  
  # Maximum records shown in history
  max-history-records: 50

# Minecraft commands to execute
commands:
  ban: "ban %player% %duration% %reason%"
  kick: "kick %player% %reason%"
  mute: "mute %player% %duration% %reason%"
  unban: "unban %player%"
  unmute: "unmute %player%"
```

### Plugin-Specific Commands (Example of plugins already preset, others can be used)

#### For LiteBans:
```yaml
commands:
  ban: "litebans:ban %player% %duration% %reason%"
  kick: "litebans:kick %player% %reason%"
  mute: "litebans:mute %player% %duration% %reason%"
  unban: "litebans:unban %player%"
  unmute: "litebans:unmute %player%"
```

#### For EssentialsX:
```yaml
commands:
  ban: "essentials:ban %player% %reason%"
  kick: "essentials:kick %player% %reason%"
  mute: "essentials:mute %player% %duration% %reason%"
  unban: "essentials:unban %player%"
  unmute: "essentials:mute %player% remove"
```

#### For AdvancedBan:
```yaml
commands:
  ban: "advancedban:ban %player% %duration% %reason%"
  kick: "advancedban:kick %player% %reason%"
  mute: "advancedban:mute %player% %duration% %reason%"
  unban: "advancedban:unban %player%"
  unmute: "advancedban:unmute %player%"
```

### messages.yml

Customize all Discord messages, embed colors, and text in `plugins/ControlPlayers/messages.yml`. The file includes:

- Error messages
- Command usage instructions
- Embed titles and descriptions
- Field names and footers
- Default values for durations and reasons
- Color schemes for different embed types

## 🎮 Discord Commands

### Basic Commands (for allowed users)

| Command | Usage | Description |
|---------|--------|-------------|
| `!players` or `!list` | `!players` | Show online players list |
| `!ban` | `!ban <player> [duration] [reason]` | Ban a player from the server |
| `!unban` | `!unban <player>` | Unban a player |
| `!kick` | `!kick <player> [reason]` | Kick a player from the server |
| `!mute` | `!mute <player> [duration] [reason]` | Mute a player |
| `!unmute` | `!unmute <player>` | Unmute a player |
| `!ip` | `!ip <player>` | Get a player's IP address (if online) |
| `!help` | `!help` | Show available commands |
| `!reloadmessages` | `!reloadmessages` | Reload message configuration |

### Advanced Commands (for history-allowed users)

| Command | Usage | Description |
|---------|--------|-------------|
| `!histstaff` | `!histstaff <discord_user_id>` | View staff command history |

### Duration Examples

- `1h` - 1 hour
- `30m` - 30 minutes  
- `1d` - 1 day
- `7d` - 7 days
- `permanent` - Permanent ban/mute

## 🔧 Getting Discord IDs

To configure user and channel permissions:

1. Enable Developer Mode in Discord:
   - User Settings → Advanced → Developer Mode (ON)

2. Get Channel ID:
   - Right-click on the channel → Copy ID

3. Get User ID:
   - Right-click on the user → Copy ID

## 🗄️ Database

The plugin uses SQLite to store command history in `plugins/ControlPlayers/staff_commands.db`. This includes:

- Command executor (Discord user)
- Command type and target
- Timestamp and duration
- Reason and server response
- Complete audit trail

## 🔒 Security Features

### Permission Levels

1. **Basic Users** (`allowed-users`):
   - Can execute moderation commands
   - Can view player lists and IPs
   - Cannot access command history

2. **History Users** (`history-allowed-users`):
   - All basic permissions
   - Can view staff command history
   - Should only include trusted administrators

### Channel Restrictions

Commands only work in configured channels (`allowed-channels`), preventing unauthorized usage in public channels.

### Audit Trail

All commands are logged with:
- Who executed the command
- When it was executed
- What action was taken
- Server response status

## 🎨 Customization

### Message Customization

Edit `messages.yml` to customize:
- Error messages and responses
- Embed titles and descriptions
- Field names and footers
- Colors (hex format: #RRGGBB)
- Default values

### Available Placeholders

- `%prefix%` - Discord command prefix
- `%player%` - Target player name
- `%duration%` - Action duration
- `%reason%` - Action reason
- `%staff%` - Staff member name
- `%online%` - Online player count
- `%max%` - Maximum players
- `%ip%` - IP address

## 🔄 Commands & Permissions

### In-Game Permissions

The plugin executes commands through the console, so no specific Minecraft permissions are required. However, ensure your moderation plugins are properly configured.

### Discord Permissions

Ensure your Discord bot has:
- `Send Messages`
- `Embed Links`
- `Use External Emojis`
- `Read Message History`
- `Manage Messages` (optional, for better UX)

## 📊 Monitoring & Logs

### Console Logs

The plugin logs important events to the server console:
- Bot connection status
- Command executions
- Database operations
- Error messages

### Database Queries

Access the SQLite database directly for advanced reporting:
```sql
SELECT * FROM staff_commands 
WHERE discord_user_id = 'USER_ID' 
ORDER BY timestamp DESC 
LIMIT 50;
```

## 🛠️ Troubleshooting

### Common Issues

1. **Bot not responding**:
   - Check bot token in config.yml
   - Verify bot permissions in Discord
   - Check server console for errors

2. **Commands not working**:
   - Verify channel is in `allowed-channels`
   - Check user is in `allowed-users`
   - Ensure correct command syntax

3. **Database errors**:
   - Check file permissions in plugin folder
   - Verify SQLite driver is loaded
   - Check server console for SQL errors

4. **Player not found**:
   - Ensure exact player name spelling
   - Check if player has ever joined the server
   - For IP command, ensure player is online

### Debug Mode

Enable debug logging by checking server console output. All database operations and command executions are logged.

## 📝 Development

### Build Requirements

- Java 15+
- Gradle 7.1.2+
- Spigot API 1.8.8
- JDA 5.0.0-beta.6

### Building from Source

```bash
git clone <repository-url>
cd ControlPlayers
gradle shadowJar
```

The compiled jar will be in `build/libs/ControlPlayers-1.0.jar`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This plugin is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).

## 👨‍💻 Developer

**Developed by Fl1uxxNoob**

For support, feature requests, or bug reports, please create an issue on the GitHub repository.

---

## ⚠️ Important Security Notes

- **Never share your Discord bot token**
- **Limit `history-allowed-users` to trusted administrators only**
- **Regularly review command logs for suspicious activity**
- **Use channel restrictions to prevent abuse**
- **Keep the plugin updated for security patches**

The `histstaff` command reveals sensitive information including who executed commands, when they were executed, and against which players. Only grant access to your most trusted staff members.
