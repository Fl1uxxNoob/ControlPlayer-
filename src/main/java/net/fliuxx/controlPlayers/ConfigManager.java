package net.fliuxx.controlPlayers;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final ControlPlayers plugin;
    private FileConfiguration config;

    public ConfigManager(ControlPlayers plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public String getDiscordToken() {
        return config.getString("discord.token", "");
    }

    public String getCommandPrefix() {
        return config.getString("discord.prefix", "!");
    }

    public String getBanCommand() {
        return config.getString("commands.ban", "ban %player% %duration% %reason%");
    }

    public String getKickCommand() {
        return config.getString("commands.kick", "kick %player% %reason%");
    }

    public String getMuteCommand() {
        return config.getString("commands.mute", "mute %player% %duration% %reason%");
    }

    // Nuovi metodi per unban e unmute
    public String getUnbanCommand() {
        return config.getString("commands.unban", "unban %player%");
    }

    public String getUnmuteCommand() {
        return config.getString("commands.unmute", "unmute %player%");
    }

    public boolean isChannelAllowed(String channelId) {
        return config.getStringList("discord.allowed-channels").contains(channelId);
    }

    public boolean hasPermission(String userId) {
        return config.getStringList("discord.allowed-users").contains(userId);
    }

    // Nuovo metodo per controllare i permessi di histstaff
    public boolean hasHistoryPermission(String userId) {
        return config.getStringList("discord.history-allowed-users").contains(userId);
    }

    // Metodo per ottenere il limite massimo di record da mostrare
    public int getHistoryLimit() {
        return config.getInt("database.max-history-records", 50);
    }

    // Metodo per controllare se il database è abilitato
    public boolean isDatabaseEnabled() {
        return config.getBoolean("database.enabled", true);
    }
}