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

public class DiscordBot extends ListenerAdapter {

    private final ControlPlayers plugin;
    private final ConfigManager config;
    private final DatabaseManager database;

    public DiscordBot(ControlPlayers plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.database = plugin.getDatabaseManager();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        String prefix = config.getCommandPrefix();

        if (!message.startsWith(prefix)) return;

        String channelId = event.getChannel().getId();
        String userId = event.getAuthor().getId();

        // Controllo permessi base
        if (!config.isChannelAllowed(channelId)) {
            event.getChannel().sendMessage("❌ Non puoi usare comandi in questo canale!").queue();
            return;
        }

        String[] args = message.substring(prefix.length()).split(" ");
        String command = args[0].toLowerCase();

        // Controllo speciale per histstaff
        if (command.equals("histstaff")) {
            if (!config.hasHistoryPermission(userId)) {
                event.getChannel().sendMessage("❌ Non hai il permesso di usare questo comando!").queue();
                return;
            }
            handleHistStaffCommand(event, args);
            return;
        }

        // Controllo permessi normali per altri comandi
        if (!config.hasPermission(userId)) {
            event.getChannel().sendMessage("❌ Non hai il permesso di usare questi comandi!").queue();
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
            default:
                event.getChannel().sendMessage("❌ Comando non riconosciuto! Usa `" + prefix + "help` per vedere i comandi disponibili.").queue();
        }
    }

    private void handlePlayersCommand(MessageReceivedEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.GREEN);
            embed.setTitle("🎮 Player Online (" + players.size() + "/" + Bukkit.getMaxPlayers() + ")");

            if (players.isEmpty()) {
                embed.setDescription("Nessun player online al momento.");
            } else {
                StringBuilder playerList = new StringBuilder();
                for (Player player : players) {
                    playerList.append("• ").append(player.getName()).append("\n");
                }
                embed.setDescription(playerList.toString());
            }

            embed.setTimestamp(java.time.Instant.now());
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        });

        // Log del comando
        logCommand(event, "PLAYERS", null, null, null, "Lista player richiesta");
    }

    private void handleBanCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "ban <player> [durata] [motivo]`").queue();
            return;
        }

        String playerName = args[1];
        String duration = args.length > 2 ? args[2] : "permanent";
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Violazione regole server";

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);
            if (target == null && Bukkit.getOfflinePlayer(playerName).hasPlayedBefore() == false) {
                event.getChannel().sendMessage("❌ Player `" + playerName + "` non trovato!").queue();
                logCommand(event, "BAN", playerName, duration, reason, "Player non trovato");
                return;
            }

            String command = config.getBanCommand()
                    .replace("%player%", playerName)
                    .replace("%duration%", duration)
                    .replace("%reason%", reason);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.RED);
            embed.setTitle("🔨 Player Bannato");
            embed.addField("Player", playerName, true);
            embed.addField("Durata", duration, true);
            embed.addField("Motivo", reason, false);
            embed.addField("Bannato da", event.getAuthor().getAsTag(), true);
            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "BAN", playerName, duration, reason, "Comando eseguito con successo");
        });
    }

    private void handleKickCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "kick <player> [motivo]`").queue();
            return;
        }

        String playerName = args[1];
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Kickato dallo staff";

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                event.getChannel().sendMessage("❌ Player `" + playerName + "` non è online!").queue();
                logCommand(event, "KICK", playerName, null, reason, "Player non online");
                return;
            }

            String command = config.getKickCommand()
                    .replace("%player%", playerName)
                    .replace("%reason%", reason);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.YELLOW);
            embed.setTitle("👢 Player Kickato");
            embed.addField("Player", playerName, true);
            embed.addField("Motivo", reason, false);
            embed.addField("Kickato da", event.getAuthor().getAsTag(), true);
            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "KICK", playerName, null, reason, "Comando eseguito con successo");
        });
    }

    private void handleMuteCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "mute <player> [durata] [motivo]`").queue();
            return;
        }

        String playerName = args[1];
        String duration = args.length > 2 ? args[2] : "1h";
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Comportamento inappropriato";

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);
            if (target == null && Bukkit.getOfflinePlayer(playerName).hasPlayedBefore() == false) {
                event.getChannel().sendMessage("❌ Player `" + playerName + "` non trovato!").queue();
                logCommand(event, "MUTE", playerName, duration, reason, "Player non trovato");
                return;
            }

            String command = config.getMuteCommand()
                    .replace("%player%", playerName)
                    .replace("%duration%", duration)
                    .replace("%reason%", reason);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.ORANGE);
            embed.setTitle("🔇 Player Mutato");
            embed.addField("Player", playerName, true);
            embed.addField("Durata", duration, true);
            embed.addField("Motivo", reason, false);
            embed.addField("Mutato da", event.getAuthor().getAsTag(), true);
            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "MUTE", playerName, duration, reason, "Comando eseguito con successo");
        });
    }

    private void handleUnbanCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "unban <player>`").queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            String command = config.getUnbanCommand()
                    .replace("%player%", playerName);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.GREEN);
            embed.setTitle("✅ Player Sbannato");
            embed.addField("Player", playerName, true);
            embed.addField("Staff", event.getAuthor().getAsTag(), true);
            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "UNBAN", playerName, null, null, "Comando eseguito con successo");
        });
    }

    private void handleUnmuteCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "unmute <player>`").queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            String command = config.getUnmuteCommand()
                    .replace("%player%", playerName);

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.GREEN);
            embed.setTitle("🔊 Player Smutato");
            embed.addField("Player", playerName, true);
            embed.addField("Staff", event.getAuthor().getAsTag(), true);
            embed.setTimestamp(java.time.Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "UNMUTE", playerName, null, null, "Comando eseguito con successo");
        });
    }

    private void handleIpCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "ip <player>`").queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);

            if (target == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (!offlinePlayer.hasPlayedBefore()) {
                    event.getChannel().sendMessage("❌ Player `" + playerName + "` non trovato!").queue();
                    logCommand(event, "IP", playerName, null, null, "Player non trovato");
                    return;
                }
                event.getChannel().sendMessage("❌ Player `" + playerName + "` non è online al momento!").queue();
                logCommand(event, "IP", playerName, null, null, "Player non online");
                return;
            }

            String playerIP = target.getAddress().getAddress().getHostAddress();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setTitle("🌐 Informazioni IP Player");
            embed.addField("Player", playerName, true);
            embed.addField("Indirizzo IP", playerIP, true);
            embed.addField("Richiesto da", event.getAuthor().getAsTag(), true);
            embed.setTimestamp(java.time.Instant.now());
            embed.setFooter("⚠️ Informazione sensibile - Tratta con riservatezza");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            logCommand(event, "IP", playerName, null, null, "IP recuperato: " + playerIP);
        });
    }

    private void handleHistStaffCommand(MessageReceivedEvent event, String[] args) {
        if (database == null) {
            event.getChannel().sendMessage("❌ Database non disponibile!").queue();
            return;
        }

        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "histstaff <discord_user_id>`").queue();
            return;
        }

        String targetUserId = args[1];
        int limit = config.getHistoryLimit();

        // Esegui la query in un thread separato per non bloccare il bot
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<DatabaseManager.StaffCommandRecord> records = database.getCommandHistory(targetUserId, limit);
                int totalCommands = database.getTotalCommandsCount(targetUserId);

                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Color.BLUE);
                embed.setTitle("📊 Cronologia Comandi Staff");

                if (records.isEmpty()) {
                    embed.setDescription("Nessun comando trovato per l'utente ID: `" + targetUserId + "`");
                } else {
                    StringBuilder history = new StringBuilder();
                    history.append("**Totale comandi eseguiti:** ").append(totalCommands).append("\n");
                    history.append("**Ultimi ").append(Math.min(records.size(), limit)).append(" comandi:**\n\n");

                    for (int i = 0; i < Math.min(records.size(), 10); i++) { // Mostra max 10 per evitare messaggi troppo lunghi
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
                        history.append("*... e altri ").append(records.size() - 10).append(" comandi*");
                    }

                    embed.setDescription(history.toString());
                }

                embed.addField("Utente Discord", "<@" + targetUserId + ">", true);
                embed.addField("Richiesto da", event.getAuthor().getAsTag(), true);
                embed.addField("Limite visualizzazione", String.valueOf(limit), true);
                embed.setTimestamp(java.time.Instant.now());
                embed.setFooter("Usa il comando con cautela - Informazioni sensibili");

                event.getChannel().sendMessageEmbeds(embed.build()).queue();

                // Log anche questo comando
                logCommand(event, "HISTSTAFF", targetUserId, null, null, "Cronologia richiesta per " + targetUserId);

            } catch (Exception e) {
                plugin.getLogger().warning("Errore nel recupero della cronologia: " + e.getMessage());
                event.getChannel().sendMessage("❌ Errore nel recupero della cronologia comandi!").queue();
            }
        });
    }

    private void handleHelpCommand(MessageReceivedEvent event) {
        String prefix = config.getCommandPrefix();
        String userId = event.getAuthor().getId();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.BLUE);
        embed.setTitle("📋 Comandi Disponibili");
        embed.setDescription("Ecco tutti i comandi che puoi utilizzare:");

        embed.addField(prefix + "players", "Mostra la lista dei player online", false);
        embed.addField(prefix + "ban <player> [durata] [motivo]", "Banna un player dal server", false);
        embed.addField(prefix + "unban <player>", "Sbanna un player dal server", false);
        embed.addField(prefix + "kick <player> [motivo]", "Kicka un player dal server", false);
        embed.addField(prefix + "mute <player> [durata] [motivo]", "Muta un player nel server", false);
        embed.addField(prefix + "unmute <player>", "Smuta un player nel server", false);
        embed.addField(prefix + "ip <player>", "Mostra l'IP del player (solo se online)", false);

        // Mostra il comando histstaff solo agli utenti autorizzati
        if (config.hasHistoryPermission(userId)) {
            embed.addField(prefix + "histstaff <discord_user_id>", "Mostra la cronologia comandi di uno staff", false);
        }

        embed.addField(prefix + "help", "Mostra questo messaggio di aiuto", false);

        embed.setFooter("Esempi durata: 1h, 30m, 1d, permanent");
        embed.setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
        logCommand(event, "HELP", null, null, null, "Menu di aiuto visualizzato");
    }

    private void logCommand(MessageReceivedEvent event, String commandType, String targetPlayer,
                            String duration, String reason, String serverResponse) {
        if (database != null) {
            // Esegui il logging in modo asincrono per non bloccare il bot
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