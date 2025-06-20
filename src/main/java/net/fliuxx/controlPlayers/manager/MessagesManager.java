package net.fliuxx.controlPlayers.manager;

import net.fliuxx.controlPlayers.ControlPlayers;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager {

    private final ControlPlayers plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessagesManager(ControlPlayers plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Create the file if it doesn't exist
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Check and update the file with new keys if needed
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
            messagesConfig.options().copyDefaults(true);
            saveMessages();
        }
    }

    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to save messages.yml: " + e.getMessage());
        }
    }

    public void reloadMessages() {
        loadMessages();
        plugin.getLogger().info("messages.yml reloaded!");
    }

    // Main method of getting a placeholder message
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = messagesConfig.getString(path, "Message not found: " + path);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return message;
    }

    // Simplified method for messages without placeholders
    public String getMessage(String path) {
        return getMessage(path, null);
    }

    // Convenience methods for error messages
    public String getErrorMessage(String errorType) {
        return getMessage("errors." + errorType);
    }

    public String getErrorMessage(String errorType, Map<String, String> placeholders) {
        return getMessage("errors." + errorType, placeholders);
    }

    // Convenience methods for usage messages
    public String getUsageMessage(String command, String prefix) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("prefix", prefix);
        return getMessage("usage." + command, placeholders);
    }

    // Methods of obtaining colors
    public Color getColor(String colorType) {
        String hexColor = messagesConfig.getString("colors." + colorType, "#0099FF");
        try {
            return Color.decode(hexColor);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid color for " + colorType + ": " + hexColor);
            return Color.BLUE; // Fallback color
        }
    }

    // Specific methods for embed titles
    public String getEmbedTitle(String embedType) {
        return getMessage(embedType + ".title");
    }

    public String getEmbedTitle(String embedType, Map<String, String> placeholders) {
        return getMessage(embedType + ".title", placeholders);
    }

    // Methods for embed fields
    public String getFieldName(String embedType, String fieldType) {
        return getMessage(embedType + "." + fieldType + "-field");
    }

    // Methods for embed footers
    public String getFooter(String embedType) {
        String footer = getMessage(embedType + ".footer");
        return footer.isEmpty() ? null : footer;
    }

    // Methods for default values
    public String getDefaultValue(String type) {
        return getMessage("defaults." + type);
    }

    // Specific methods for common types of messages
    public String getPlayerNotFoundError(String playerName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        return getErrorMessage("player-not-found", placeholders);
    }

    public String getPlayerNotOnlineError(String playerName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        return getErrorMessage("player-not-online", placeholders);
    }

    public String getNoPermissionError() {
        return getErrorMessage("no-permission");
    }

    public String getNoHistoryPermissionError() {
        return getErrorMessage("no-history-permission");
    }

    public String getChannelNotAllowedError() {
        return getErrorMessage("channel-not-allowed");
    }

    public String getCommandNotFoundError(String prefix) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("prefix", prefix);
        return getErrorMessage("command-not-found", placeholders);
    }

    // Methods for log messages
    public String getLogMessage(String logType) {
        return getMessage("log." + logType);
    }

    public String getLogMessage(String logType, Map<String, String> placeholders) {
        return getMessage("log." + logType, placeholders);
    }

    // Method to create placeholder map quickly
    public Map<String, String> createPlaceholders(String... keyValuePairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                placeholders.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        return placeholders;
    }

    // Specific methods for help commands
    public String getHelpCommandDescription(String command) {
        return getMessage("help.commands." + command);
    }

    // Check if a message exists
    public boolean hasMessage(String path) {
        return messagesConfig.contains(path);
    }

    // Debug: print all available messages
    public void printAvailableMessages() {
        plugin.getLogger().info("=== MESSAGES AVAILABLE ===");
        for (String key : messagesConfig.getKeys(true)) {
            if (!messagesConfig.isConfigurationSection(key)) {
                plugin.getLogger().info(key + ": " + messagesConfig.getString(key));
            }
        }
        plugin.getLogger().info("=== END MESSAGES ===");
    }
}