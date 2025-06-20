package net.fliuxx.controlPlayers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DiscordBot extends ListenerAdapter {

    private final ControlPlayers plugin;
    private final ConfigManager config;
    private final DatabaseManager database;
    private final MessagesManager messages;

    public DiscordBot(ControlPlayers plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.database = plugin.getDatabaseManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        String prefix = config.getCommandPrefix();

        if (!message.startsWith(prefix)) return;

        String channelId = event.getChannel().getId();
        String userId = event.getAuthor().getId();

        // Basic permission check
        if (!config.isChannelAllowed(channelId)) {
            event.getChannel().sendMessage(messages.getChannelNotAllowedError()).queue();
            return;
        }

        String[] args = message.substring(prefix.length()).split(" ");
        String command = args[0].toLowerCase();

        // Special check for histstaff
        if (command.equals("histstaff")) {
            if (!config.hasHistoryPermission(userId)) {
                event.getChannel().sendMessage(messages.getNoHistoryPermissionError()).queue();
                return;
            }
            handleHistStaffCommand(event, args);
            return;
        }

        // Standard permission check for other commands
        if (!config.hasPermission(userId)) {
            event.getChannel().sendMessage(messages.getNoPermissionError()).queue();
            return;
        }

        switch (command) {
            case "players":
            case "list":
                handlePlayersCommand(event);
                break;
            case "ban":
                handleBanCommand(event, args);
                break;
            case "kick":
                handleKickCommand(event, args);
                break;
            case "mute":
                handleMuteCommand(event, args);
                break;
            case "unban":
                handleUnbanCommand(event, args);
                break;
            case "unmute":
                handleUnmuteCommand(event, args);
                break;
            case "ip":
                handleIpCommand(event, args);
                break;
            case "help":
                handleHelpCommand(event);
                break;
            case "reloadmessages":
                if (!config.hasPermission(userId)) {
                    event.getChannel().sendMessage(messages.getNoPermissionError()).queue();
                    return;
                }
                messages.reloadMessages();
                event.getChannel().sendMessage("✅ Messages successfully reloaded!").queue();
                break;
            default:
                event.getChannel().sendMessage(messages.getCommandNotFoundError(prefix)).queue();
        }
    }

    private void handlePlayersCommand(MessageReceivedEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(messages.getColor("players"));

            Map<String, String> placeholders = messages.createPlaceholders(
                    "online", String.valueOf(players.size()),
                    "max", String.valueOf(Bukkit.getMaxPlayers())
            );
            embed.setTitle(messages.getEmbedTitle("players", placeholders));

            if (players.isEmpty()) {
                embed.setDescription(messages.getMessage("players.no-players"));
            } else {
                StringBuilder playerList = new StringBuilder();
                for (Player player : players) {
                    playerList.append("• ").append(player.getName()).append("\n");
                }
                embed.setDescription(playerList.toString());
            }

            String footer = messages.getFooter("players");
            if (footer != null && !footer.isEmpty()) {
                embed.setFooter(footer);
            }

            embed.setTimestamp(java.time.Instant.now());
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        });

        // Log the command
        logCommand(event, "PLAYERS", null, null, null, messages.getLogMessage("players-requested"));
    }

    private void handleBanCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage(messages.getUsageMessage("ban", config.getCommandPrefix())).queue();
            return;
        }

        String playerName = args[1];
        String duration = args.length > 2 ? args[2] : messages.getDefaultValue("ban-duration");
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : messages.getDefaultValue("ban-reason");

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);
            if (target == null && Bukkit.getOfflinePlayer(playerName).hasPlayedBefore() == false) {
                event.getChannel().sendMessage(messages.getPlayerNotFoundError(playerName)).queue();
                logCommand(event, "BAN", playerName, duration, reason, messages.getLogMessage("player-not-found"));
                return;
            }

            String command = config.getBanCommand()
                    .replace("%player%", playerName)
                    .replace("%duration%", duration)
                    .replace("%reason%", reason);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(messages.getColor("ban"));
            embed.setTitle(messages.getEmbedTitle("moderation.ban"));
            embed.addField(messages.getFieldName("moderation.ban", "player"), playerName, true);
            embed.addField(messages.getFieldName("moderation.ban", "duration"), duration, true);
            embed.addField(messages.getFieldName("moderation.ban", "reason"), reason, false);
            embed.addField(messages.getFieldName("moderation.ban", "staff"), event.getAuthor().getAsTag(), true);

            String footer = messages.getFooter("moderation.ban");
            if (footer != null && !footer.isEmpty()) {
                embed.setFooter(footer);
            }

            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "BAN", playerName, duration, reason, messages.getLogMessage("command-success"));
        });
    }

    private void handleKickCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage(messages.getUsageMessage("kick", config.getCommandPrefix())).queue();
            return;
        }

        String playerName = args[1];
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : messages.getDefaultValue("kick-reason");

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                event.getChannel().sendMessage(messages.getPlayerNotOnlineError(playerName)).queue();
                logCommand(event, "KICK", playerName, null, reason, messages.getLogMessage("player-not-online"));
                return;
            }

            String command = config.getKickCommand()
                    .replace("%player%", playerName)
                    .replace("%reason%", reason);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(messages.getColor("kick"));
            embed.setTitle(messages.getEmbedTitle("moderation.kick"));
            embed.addField(messages.getFieldName("moderation.kick", "player"), playerName, true);
            embed.addField(messages.getFieldName("moderation.kick", "reason"), reason, false);
            embed.addField(messages.getFieldName("moderation.kick", "staff"), event.getAuthor().getAsTag(), true);

            String footer = messages.getFooter("moderation.kick");
            if (footer != null && !footer.isEmpty()) {
                embed.setFooter(footer);
            }

            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "KICK", playerName, null, reason, messages.getLogMessage("command-success"));
        });
    }

    private void handleMuteCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage(messages.getUsageMessage("mute", config.getCommandPrefix())).queue();
            return;
        }

        String playerName = args[1];
        String duration = args.length > 2 ? args[2] : messages.getDefaultValue("mute-duration");
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : messages.getDefaultValue("mute-reason");

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);
            if (target == null && Bukkit.getOfflinePlayer(playerName).hasPlayedBefore() == false) {
                event.getChannel().sendMessage(messages.getPlayerNotFoundError(playerName)).queue();
                logCommand(event, "MUTE", playerName, duration, reason, messages.getLogMessage("player-not-found"));
                return;
            }

            String command = config.getMuteCommand()
                    .replace("%player%", playerName)
                    .replace("%duration%", duration)
                    .replace("%reason%", reason);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(messages.getColor("mute"));
            embed.setTitle(messages.getEmbedTitle("moderation.mute"));
            embed.addField(messages.getFieldName("moderation.mute", "player"), playerName, true);
            embed.addField(messages.getFieldName("moderation.mute", "duration"), duration, true);
            embed.addField(messages.getFieldName("moderation.mute", "reason"), reason, false);
            embed.addField(messages.getFieldName("moderation.mute", "staff"), event.getAuthor().getAsTag(), true);

            String footer = messages.getFooter("moderation.mute");
            if (footer != null && !footer.isEmpty()) {
                embed.setFooter(footer);
            }

            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "MUTE", playerName, duration, reason, messages.getLogMessage("command-success"));
        });
    }

    private void handleUnbanCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage(messages.getUsageMessage("unban", config.getCommandPrefix())).queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            String command = config.getUnbanCommand()
                    .replace("%player%", playerName);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(messages.getColor("unban"));
            embed.setTitle(messages.getEmbedTitle("moderation.unban"));
            embed.addField(messages.getFieldName("moderation.unban", "player"), playerName, true);
            embed.addField(messages.getFieldName("moderation.unban", "staff"), event.getAuthor().getAsTag(), true);

            String footer = messages.getFooter("moderation.unban");
            if (footer != null && !footer.isEmpty()) {
                embed.setFooter(footer);
            }

            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "UNBAN", playerName, null, null, messages.getLogMessage("command-success"));
        });
    }

    private void handleUnmuteCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage(messages.getUsageMessage("unmute", config.getCommandPrefix())).queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            String command = config.getUnmuteCommand()
                    .replace("%player%", playerName);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(messages.getColor("unmute"));
            embed.setTitle(messages.getEmbedTitle("moderation.unmute"));
            embed.addField(messages.getFieldName("moderation.unmute", "player"), playerName, true);
            embed.addField(messages.getFieldName("moderation.unmute", "staff"), event.getAuthor().getAsTag(), true);

            String footer = messages.getFooter("moderation.unmute");
            if (footer != null && !footer.isEmpty()) {
                embed.setFooter(footer);
            }

            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "UNMUTE", playerName, null, null, messages.getLogMessage("command-success"));
        });
    }

    private void handleIpCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage(messages.getUsageMessage("ip", config.getCommandPrefix())).queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);

            if (target == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (!offlinePlayer.hasPlayedBefore()) {
                    event.getChannel().sendMessage(messages.getPlayerNotFoundError(playerName)).queue();
                    logCommand(event, "IP", playerName, null, null, messages.getLogMessage("player-not-found"));
                    return;
                }
                event.getChannel().sendMessage(messages.getPlayerNotOnlineError(playerName)).queue();
                logCommand(event, "IP", playerName, null, null, messages.getLogMessage("player-not-online"));
                return;
            }

            String playerIP = target.getAddress().getAddress().getHostAddress();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(messages.getColor("ip"));
            embed.setTitle(messages.getEmbedTitle("ip"));
            embed.addField(messages.getFieldName("ip", "player"), playerName, true);
            embed.addField(messages.getFieldName("ip", "ip"), playerIP, true);
            embed.addField(messages.getFieldName("ip", "staff"), event.getAuthor().getAsTag(), true);

            String footer = messages.getFooter("ip");
            if (footer != null && !footer.isEmpty()) {
                embed.setFooter(footer);
            }

            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

            Map<String, String> logPlaceholders = messages.createPlaceholders("ip", playerIP);
            logCommand(event, "IP", playerName, null, null, messages.getLogMessage("ip-retrieved", logPlaceholders));
        });
    }

    private void handleHistStaffCommand(MessageReceivedEvent event, String[] args) {
        if (database == null) {
            event.getChannel().sendMessage(messages.getErrorMessage("database-unavailable")).queue();
            return;
        }

        if (args.length < 2) {
            event.getChannel().sendMessage(messages.getUsageMessage("histstaff", config.getCommandPrefix())).queue();
            return;
        }

        String targetUserId = args[1];
        int limit = config.getHistoryLimit();

        // Run the query in a separate thread to avoid blocking the bot
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<DatabaseManager.StaffCommandRecord> records = database.getCommandHistory(targetUserId, limit);
                int totalCommands = database.getTotalCommandsCount(targetUserId);

                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(messages.getColor("history"));
                embed.setTitle(messages.getEmbedTitle("history"));

                if (records.isEmpty()) {
                    Map<String, String> placeholders = messages.createPlaceholders("user_id", targetUserId);
                    embed.setDescription(messages.getMessage("history.no-commands", placeholders));
                } else {
                    StringBuilder history = new StringBuilder();

                    Map<String, String> totalPlaceholders = messages.createPlaceholders("total", String.valueOf(totalCommands));
                    history.append(messages.getMessage("history.total-commands", totalPlaceholders)).append("\n");

                    Map<String, String> countPlaceholders = messages.createPlaceholders("count", String.valueOf(Math.min(records.size(), limit)));
                    history.append(messages.getMessage("history.recent-commands", countPlaceholders)).append("\n\n");

                    for (int i = 0; i < Math.min(records.size(), 10); i++) { // Show max 10 to avoid overly long messages
                        DatabaseManager.StaffCommandRecord record = records.get(i);
                        history.append("**").append(i + 1).append(".** `").append(record.getCommandType()).append("`");

                        if (record.getTargetPlayer() != null && !record.getTargetPlayer().isEmpty()) {
                            history.append(" → ").append(record.getTargetPlayer());
                        }

                        if (record.getDuration() != null && !record.getDuration().isEmpty()) {
                            history.append(" (").append(record.getDuration()).append(")");
                        }

                        history.append("\n   *").append(record.getFormattedTimestamp()).append("*");

                        if (record.getReason() != null && !record.getReason().isEmpty()) {
                            String shortReason = record.getReason().length() > 50 ?
                                    record.getReason().substring(0, 47) + "..." : record.getReason();
                            history.append("\n   📝 ").append(shortReason);
                        }

                        history.append("\n\n");
                    }

                    if (records.size() > 10) {
                        Map<String, String> morePlaceholders = messages.createPlaceholders("count", String.valueOf(records.size() - 10));
                        history.append(messages.getMessage("history.more-commands", morePlaceholders));
                    }

                    embed.setDescription(history.toString());
                }

                embed.addField(messages.getFieldName("history", "user"), "<@" + targetUserId + ">", true);
                embed.addField(messages.getFieldName("history", "requested"), event.getAuthor().getAsTag(), true);
                embed.addField(messages.getFieldName("history", "limit"), String.valueOf(limit), true);

                String footer = messages.getFooter("history");
                if (footer != null && !footer.isEmpty()) {
                    embed.setFooter(footer);
                }

                embed.setTimestamp(java.time.Instant.now());

                event.getChannel().sendMessageEmbeds(embed.build()).queue();

                // Also log this command
                Map<String, String> logPlaceholders = messages.createPlaceholders("user_id", targetUserId);
                logCommand(event, "HISTSTAFF", targetUserId, null, null, messages.getLogMessage("history-requested", logPlaceholders));

            } catch (Exception e) {
                plugin.getLogger().warning("Error retrieving history: " + e.getMessage());
                event.getChannel().sendMessage(messages.getLogMessage("error-history")).queue();
            }
        });
    }

    private void handleHelpCommand(MessageReceivedEvent event) {
        String prefix = config.getCommandPrefix();
        String userId = event.getAuthor().getId();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(messages.getColor("help"));
        embed.setTitle(messages.getEmbedTitle("help"));
        embed.setDescription(messages.getMessage("help.description"));

        embed.addField(prefix + "players", messages.getHelpCommandDescription("players"), false);
        embed.addField(prefix + "ban <player> [durata] [motivo]", messages.getHelpCommandDescription("ban"), false);
        embed.addField(prefix + "unban <player>", messages.getHelpCommandDescription("unban"), false);
        embed.addField(prefix + "kick <player> [motivo]", messages.getHelpCommandDescription("kick"), false);
        embed.addField(prefix + "mute <player> [durata] [motivo]", messages.getHelpCommandDescription("mute"), false);
        embed.addField(prefix + "unmute <player>", messages.getHelpCommandDescription("unmute"), false);
        embed.addField(prefix + "ip <player>", messages.getHelpCommandDescription("ip"), false);

        // Only show the histstaff command to authorized users
        if (config.hasHistoryPermission(userId)) {
            embed.addField(prefix + "histstaff <discord_user_id>", messages.getHelpCommandDescription("histstaff"), false);
        }

        embed.addField(prefix + "help", messages.getHelpCommandDescription("help"), false);

        String footer = messages.getFooter("help");
        if (footer != null && !footer.isEmpty()) {
            embed.setFooter(footer);
        }

        embed.setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
        logCommand(event, "HELP", null, null, null, messages.getLogMessage("help-displayed"));
    }

    private void logCommand(MessageReceivedEvent event, String commandType, String targetPlayer,
                            String duration, String reason, String serverResponse) {
        if (database != null) {
            // Perform logging asynchronously to avoid blocking the bot
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                database.logCommand(
                        event.getAuthor().getId(),
                        event.getAuthor().getAsTag(),
                        commandType,
                        targetPlayer,
                        duration,
                        reason,
                        serverResponse
                );
            });
        }
    }
}