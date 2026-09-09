package com.gbuxml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private final Path dbPath;

    public DatabaseService(Path dbPath) {
        this.dbPath = dbPath;
    }

    public static DatabaseService defaultDatabase() {
        Path appDir = Paths.get(System.getProperty("user.home"), ".gbuxml");
        try {
            Files.createDirectories(appDir);
        } catch (Exception ignored) {
        }
        return new DatabaseService(appDir.resolve("gbuxml.db"));
    }

    public void initialize() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS master_data (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT NOT NULL, label TEXT NOT NULL, UNIQUE(category, label));");
        }
    }

    public void saveMasterValue(String category, String value) throws SQLException {
        if (category == null || category.isBlank() || value == null || value.isBlank()) {
            return;
        }
        String sql = "INSERT OR IGNORE INTO master_data(category, label) VALUES(?, ?)";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.trim());
            statement.setString(2, value.trim());
            statement.executeUpdate();
        }
    }

    public List<String> loadMasterValues(String category) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT label FROM master_data WHERE category = ? ORDER BY label COLLATE NOCASE";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category == null ? "" : category.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString("label"));
                }
            }
        }
        return values;
    }

    private Connection openConnection() throws SQLException {
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        return DriverManager.getConnection(jdbcUrl);
    }
}
