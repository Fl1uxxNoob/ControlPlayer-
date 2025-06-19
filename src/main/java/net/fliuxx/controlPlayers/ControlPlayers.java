package net.fliuxx.controlPlayers;

import org.bukkit.plugin.java.JavaPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.EnumSet;
import org.bukkit.plugin.java.JavaPlugin;

public final class ControlPlayers extends JavaPlugin {

    private static ControlPlayers instance;
    private JDA jda;
    private ConfigManager configManager;
    private DiscordBot discordBot;

    @Override
    public void onEnable() {
        instance = this;

        // Inizializza configurazione
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Inizializza bot Discord
        initializeDiscordBot();

        getLogger().info("DiscordManager Plugin abilitato!");
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
        }
        getLogger().info("DiscordManager Plugin disabilitato!");
    }

    private void initializeDiscordBot() {
        String token = configManager.getDiscordToken();
        if (token == null || token.isEmpty()) {
            getLogger().severe("Token Discord non configurato! Disabilito il plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    // Abilita l’intento MESSAGE_CONTENT
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    // (opzionale: se vuoi solo i messaggi di server, aggiungi anche GUILD_MESSAGES)
                    //.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setActivity(Activity.playing("Minecraft Server"))
                    .build();

            discordBot = new DiscordBot(this);
            jda.addEventListener(discordBot);

            jda.awaitReady();
            getLogger().info("Bot Discord connesso!");

        } catch (Exception e) {
            getLogger().severe("Errore nell'inizializzazione del bot Discord: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public static ControlPlayers getInstance() {
        return instance;
    }

    public JDA getJDA() {
        return jda;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
