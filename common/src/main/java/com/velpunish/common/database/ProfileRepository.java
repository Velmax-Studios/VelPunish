package com.velpunish.common.database;

import com.velpunish.common.models.PlayerProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileRepository {

    private final DatabaseManager databaseManager;
    private final ExecutorService executor;

    public ProfileRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.executor = Executors.newFixedThreadPool(2);
    }

    public CompletableFuture<Void> createTables() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE TABLE IF NOT EXISTS player_profiles (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "username VARCHAR(16) NOT NULL," +
                        "latest_ip VARCHAR(45) NOT NULL," +
                        "last_login BIGINT NOT NULL" +
                        ")");

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_profiles_username ON player_profiles(username)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_profiles_ip ON player_profiles(latest_ip)");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public CompletableFuture<Void> saveOrUpdateProfile(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "MERGE INTO player_profiles (uuid, username, latest_ip, last_login) KEY(uuid) VALUES (?, ?, ?, ?)")) {

                stmt.setString(1, profile.getUuid().toString());
                stmt.setString(2, profile.getUsername());
                stmt.setString(3, profile.getLatestIp());
                stmt.setLong(4, profile.getLastLogin());
                stmt.executeUpdate();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public CompletableFuture<Optional<PlayerProfile>> getProfileByName(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn
                            .prepareStatement("SELECT * FROM player_profiles WHERE username = ? LIMIT 1")) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new PlayerProfile(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("username"),
                                rs.getString("latest_ip"),
                                rs.getLong("last_login")));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, executor);
    }

    public CompletableFuture<Optional<PlayerProfile>> getProfileByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_profiles WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new PlayerProfile(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("username"),
                                rs.getString("latest_ip"),
                                rs.getLong("last_login")));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, executor);
    }

    public CompletableFuture<List<String>> getAllKnownNames() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> names = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement("SELECT username FROM player_profiles");
                    ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("username"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return names;
        }, executor);
    }
}
