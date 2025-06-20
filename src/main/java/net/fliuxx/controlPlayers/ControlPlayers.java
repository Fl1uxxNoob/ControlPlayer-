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
    private MessagesManager messagesManager; // NUOVO
    private DiscordBot discordBot;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize messages manager
        messagesManager = new MessagesManager(this);

        // Initialize database if enabled
        if (configManager.isDatabaseEnabled()) {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
        }

        // Initialize Discord bot
        initializeDiscordBot();

        getLogger().info("ControlPlayers Plugin enabled! - Developed By Fl1uxxNoob");
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("ControlPlayers Plugin disabled! - Developed By Fl1uxxNoob");
    }

    private void initializeDiscordBot() {
        String token = configManager.getDiscordToken();
        if (token == null || token.isEmpty()) {
            getLogger().severe("Discord token not configured! I disable the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    // Enable the MESSAGE_CONTENT intent
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .setActivity(Activity.playing("Minecraft Server"))
                    .build();

            discordBot = new DiscordBot(this);
            jda.addEventListener(discordBot);

            jda.awaitReady();
            getLogger().info("Discord bot connected!");

        } catch (Exception e) {
            getLogger().severe("Error initializing Discord bot: " + e.getMessage());
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

    public MessagesManager getMessagesManager() { // NUOVO
        return messagesManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}