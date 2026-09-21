package com.dhyaa.scoreboard;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.io.FileInputStream;

public class Main {

    private static final String DEFAULT_PORT = "8080";

    public static void main(String[] args) throws IOException {
        Properties config = loadConfig();
        int port = Integer.parseInt(config.getProperty("port", DEFAULT_PORT));

        Database db = new Database(config.getProperty("db.path", "scoreboard.db"));
        db.initialize();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        PlayerHandler playerHandler = new PlayerHandler(db);
        LeaderboardHandler leaderboardHandler = new LeaderboardHandler(db);

        server.createContext("/api/players", playerHandler);
        server.createContext("/api/leaderboard", leaderboardHandler);

        server.setExecutor(null);
        server.start();

        System.out.println("Scoreboard API running on port " + port);
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("No config file found, using defaults");
        }
        return props;
    }
}
