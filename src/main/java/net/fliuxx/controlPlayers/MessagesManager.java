package net.fliuxx.controlPlayers;

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

        // Crea il file se non esiste
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Verifica e aggiorna il file con nuove chiavi se necessario
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
            plugin.getLogger().severe("Impossibile salvare messages.yml: " + e.getMessage());
        }
    }

    public void reloadMessages() {
        loadMessages();
        plugin.getLogger().info("Messages.yml ricaricato!");
    }

    // Metodo principale per ottenere un messaggio con placeholder
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = messagesConfig.getString(path, "Messaggio non trovato: " + path);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return message;
    }

    // Metodo semplificato per messaggi senza placeholder
    public String getMessage(String path) {
        return getMessage(path, null);
    }

    // Metodi di convenienza per messaggi di errore
    public String getErrorMessage(String errorType) {
        return getMessage("errors." + errorType);
    }

    public String getErrorMessage(String errorType, Map<String, String> placeholders) {
        return getMessage("errors." + errorType, placeholders);
    }

    // Metodi di convenienza per messaggi di utilizzo
    public String getUsageMessage(String command, String prefix) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("prefix", prefix);
        return getMessage("usage." + command, placeholders);
    }

    // Metodi per ottenere i colori
    public Color getColor(String colorType) {
        String hexColor = messagesConfig.getString("colors." + colorType, "#0099FF");
        try {
            return Color.decode(hexColor);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Colore non valido per " + colorType + ": " + hexColor);
            return Color.BLUE; // Colore di fallback
        }
    }

    // Metodi specifici per i titoli degli embed
    public String getEmbedTitle(String embedType) {
        return getMessage(embedType + ".title");
    }

    public String getEmbedTitle(String embedType, Map<String, String> placeholders) {
        return getMessage(embedType + ".title", placeholders);
    }

    // Metodi per i field degli embed
    public String getFieldName(String embedType, String fieldType) {
        return getMessage(embedType + "." + fieldType + "-field");
    }

    // Metodi per i footer degli embed
    public String getFooter(String embedType) {
        String footer = getMessage(embedType + ".footer");
        return footer.isEmpty() ? null : footer;
    }

    // Metodi per valori predefiniti
    public String getDefaultValue(String type) {
        return getMessage("defaults." + type);
    }

    // Metodi specifici per tipi comuni di messaggi
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

    // Metodi per i messaggi di log
    public String getLogMessage(String logType) {
        return getMessage("log." + logType);
    }

    public String getLogMessage(String logType, Map<String, String> placeholders) {
        return getMessage("log." + logType, placeholders);
    }

    // Metodo per creare placeholder map velocemente
    public Map<String, String> createPlaceholders(String... keyValuePairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                placeholders.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        return placeholders;
    }

    // Metodi specifici per i comandi help
    public String getHelpCommandDescription(String command) {
        return getMessage("help.commands." + command);
    }

    // Verifica se un messaggio esiste
    public boolean hasMessage(String path) {
        return messagesConfig.contains(path);
    }

    // Debug: stampa tutti i messaggi disponibili
    public void printAvailableMessages() {
        plugin.getLogger().info("=== MESSAGGI DISPONIBILI ===");
        for (String key : messagesConfig.getKeys(true)) {
            if (!messagesConfig.isConfigurationSection(key)) {
                plugin.getLogger().info(key + ": " + messagesConfig.getString(key));
            }
        }
        plugin.getLogger().info("=== FINE MESSAGGI ===");
    }
}