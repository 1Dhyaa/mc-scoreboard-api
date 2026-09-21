package com.dhyaa.scoreboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.sql.*;

public class PlayerHandler implements HttpHandler {

    private final Database db;

    public PlayerHandler(Database db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        try {
            if (method.equals("GET") && parts.length == 3) {
                listPlayers(exchange);
            } else if (method.equals("GET") && parts.length == 4) {
                getPlayer(exchange, parts[3]);
            } else if (method.equals("POST") && parts.length == 5 && parts[4].equals("score")) {
                updateScore(exchange, parts[3]);
            } else if (method.equals("DELETE") && parts.length == 4) {
                deletePlayer(exchange, parts[3]);
            } else {
                sendResponse(exchange, 404, "{\"error\": \"not found\"}");
            }
        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void listPlayers(HttpExchange exchange) throws IOException, SQLException {
        StringBuilder json = new StringBuilder("[");
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, name, last_seen FROM players ORDER BY last_seen DESC")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(String.format(
                    "{\"uuid\":\"%s\",\"name\":\"%s\",\"last_seen\":%d}",
                    rs.getString("uuid"), rs.getString("name"), rs.getLong("last_seen")
                ));
                first = false;
            }
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private void getPlayer(HttpExchange exchange, String uuid) throws IOException, SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT p.uuid, p.name, s.mode, s.kills, s.deaths, s.wins, s.playtime_seconds "
                + "FROM players p LEFT JOIN scores s ON p.uuid = s.player_uuid WHERE p.uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    sendResponse(exchange, 404, "{\"error\": \"player not found\"}");
                    return;
                }
                String name = rs.getString("name");
                StringBuilder scores = new StringBuilder("[");
                boolean first = true;
                do {
                    String mode = rs.getString("mode");
                    if (mode != null) {
                        if (!first) scores.append(",");
                        scores.append(String.format(
                            "{\"mode\":\"%s\",\"kills\":%d,\"deaths\":%d,\"wins\":%d,\"playtime\":%d}",
                            mode, rs.getInt("kills"), rs.getInt("deaths"),
                            rs.getInt("wins"), rs.getInt("playtime_seconds")
                        ));
                        first = false;
                    }
                } while (rs.next());
                scores.append("]");
                sendResponse(exchange, 200,
                    String.format("{\"uuid\":\"%s\",\"name\":\"%s\",\"scores\":%s}", uuid, name, scores));
            }
        }
    }

    private void updateScore(HttpExchange exchange, String uuid) throws IOException, SQLException {
        String body = new String(exchange.getRequestBody().readAllBytes());
        // simple json parsing (no library)
        String mode = extractJsonString(body, "mode");
        int kills = extractJsonInt(body, "kills");
        int deaths = extractJsonInt(body, "deaths");
        boolean won = body.contains("\"won\": true") || body.contains("\"won\":true");

        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT INTO scores (player_uuid, mode, kills, deaths, wins) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(player_uuid, mode) DO UPDATE SET "
                + "kills = kills + ?, deaths = deaths + ?, wins = wins + ?")) {
            ps.setString(1, uuid);
            ps.setString(2, mode);
            ps.setInt(3, kills);
            ps.setInt(4, deaths);
            ps.setInt(5, won ? 1 : 0);
            ps.setInt(6, kills);
            ps.setInt(7, deaths);
            ps.setInt(8, won ? 1 : 0);
            ps.executeUpdate();
        }
        sendResponse(exchange, 200, "{\"status\": \"updated\"}");
    }

    private void deletePlayer(HttpExchange exchange, String uuid) throws IOException, SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                sendResponse(exchange, 404, "{\"error\": \"player not found\"}");
            } else {
                sendResponse(exchange, 200, "{\"status\": \"deleted\"}");
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private String extractJsonString(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i == -1) return "";
        int start = json.indexOf("\"", i + key.length() + 3) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractJsonInt(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i == -1) return 0;
        int start = json.indexOf(":", i) + 1;
        StringBuilder num = new StringBuilder();
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (Character.isDigit(c) || c == '-') num.append(c);
            else if (num.length() > 0) break;
        }
        return num.length() > 0 ? Integer.parseInt(num.toString()) : 0;
    }
}
