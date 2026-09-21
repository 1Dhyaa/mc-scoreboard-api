package com.dhyaa.scoreboard;

import java.sql.*;

public class Database {

    private final String dbPath;
    private Connection connection;

    public Database(String dbPath) {
        this.dbPath = dbPath;
    }

    public void initialize() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS players ("
                + "  uuid TEXT PRIMARY KEY,"
                + "  name TEXT NOT NULL,"
                + "  first_seen INTEGER NOT NULL,"
                + "  last_seen INTEGER NOT NULL"
                + ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS scores ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  player_uuid TEXT NOT NULL,"
                + "  mode TEXT NOT NULL,"
                + "  kills INTEGER DEFAULT 0,"
                + "  deaths INTEGER DEFAULT 0,"
                + "  wins INTEGER DEFAULT 0,"
                + "  playtime_seconds INTEGER DEFAULT 0,"
                + "  FOREIGN KEY (player_uuid) REFERENCES players(uuid),"
                + "  UNIQUE(player_uuid, mode)"
                + ")"
            );
        }

        System.out.println("Database initialized at " + dbPath);
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
