package dev.aari.playerreset;

import org.bukkit.OfflinePlayer;
import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final PlayerReset plugin;
    private Connection connection;

    public DatabaseManager(PlayerReset plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            String dbFile = plugin.getConfig().getString("database.file", "playerdata.db");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, dbFile));
            createTables();
            plugin.getLogger().info("Database initialized successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS player_resets (
                uuid TEXT PRIMARY KEY,
                player_name TEXT,
                kills_reset INTEGER DEFAULT 0,
                deaths_reset INTEGER DEFAULT 0,
                playtime_reset INTEGER DEFAULT 0,
                money_reset INTEGER DEFAULT 0,
                last_reset TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (PreparedStatement statement = connection.prepareStatement(createTable)) {
            statement.execute();
        }
    }

    public CompletableFuture<Void> savePlayerReset(OfflinePlayer player, String resetType) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = """
                    INSERT OR REPLACE INTO player_resets 
                    (uuid, player_name, kills_reset, deaths_reset, playtime_reset, money_reset, last_reset)
                    VALUES (?, ?, 
                        COALESCE((SELECT kills_reset FROM player_resets WHERE uuid = ?), 0) + ?,
                        COALESCE((SELECT deaths_reset FROM player_resets WHERE uuid = ?), 0) + ?,
                        COALESCE((SELECT playtime_reset FROM player_resets WHERE uuid = ?), 0) + ?,
                        COALESCE((SELECT money_reset FROM player_resets WHERE uuid = ?), 0) + ?,
                        CURRENT_TIMESTAMP)
                    """;

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    String uuid = player.getUniqueId().toString();
                    statement.setString(1, uuid);
                    statement.setString(2, player.getName());
                    statement.setString(3, uuid);
                    statement.setInt(4, resetType.equals("kills") ? 1 : 0);
                    statement.setString(5, uuid);
                    statement.setInt(6, resetType.equals("deaths") ? 1 : 0);
                    statement.setString(7, uuid);
                    statement.setInt(8, resetType.equals("playtime") ? 1 : 0);
                    statement.setString(9, uuid);
                    statement.setInt(10, resetType.equals("money") ? 1 : 0);
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player reset data: " + e.getMessage());
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
}
