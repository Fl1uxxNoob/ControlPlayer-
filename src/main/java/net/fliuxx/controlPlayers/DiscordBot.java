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

public class DiscordBot extends ListenerAdapter {

    private final ControlPlayers plugin;
    private final ConfigManager config;

    public DiscordBot(ControlPlayers plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        String prefix = config.getCommandPrefix();

        if (!message.startsWith(prefix)) return;

        String channelId = event.getChannel().getId();
        String userId = event.getAuthor().getId();

        // Controllo permessi
        if (!config.isChannelAllowed(channelId)) {
            event.getChannel().sendMessage("❌ Non puoi usare comandi in questo canale!").queue();
            return;
        }

        if (!config.hasPermission(userId)) {
            event.getChannel().sendMessage("❌ Non hai il permesso di usare questi comandi!").queue();
            return;
        }

        String[] args = message.substring(prefix.length()).split(" ");
        String command = args[0].toLowerCase();

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
        });
    }

    // NUOVO: Comando unban
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
        });
    }

    // NUOVO: Comando unmute
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
        });
    }

    // NUOVO: Comando per vedere l'IP del player
    private void handleIpCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Uso corretto: `" + config.getCommandPrefix() + "ip <player>`").queue();
            return;
        }

        String playerName = args[1];

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(playerName);

            if (target == null) {
                // Prova con offline player
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                if (!offlinePlayer.hasPlayedBefore()) {
                    event.getChannel().sendMessage("❌ Player `" + playerName + "` non trovato!").queue();
                    return;
                }
                event.getChannel().sendMessage("❌ Player `" + playerName + "` non è online al momento!").queue();
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
        });
    }

    private void handleHelpCommand(MessageReceivedEvent event) {
        String prefix = config.getCommandPrefix();

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
        embed.addField(prefix + "help", "Mostra questo messaggio di aiuto", false);

        embed.setFooter("Esempi durata: 1h, 30m, 1d, permanent");
        embed.setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}