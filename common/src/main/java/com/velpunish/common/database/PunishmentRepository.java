package com.velpunish.common.database;

import com.velpunish.common.models.History;
import com.velpunish.common.models.IPPunishment;
import com.velpunish.common.models.Punishment;
import com.velpunish.common.models.PunishmentType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PunishmentRepository {
    private final DatabaseManager databaseManager;
    private final ExecutorService executor;

    public PunishmentRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.executor = Executors.newFixedThreadPool(4);
    }

    public CompletableFuture<Void> createTables() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE TABLE IF NOT EXISTS punishments (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "ip VARCHAR(45)," +
                        "type VARCHAR(16) NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "operator VARCHAR(36) NOT NULL," +
                        "start_time BIGINT NOT NULL," +
                        "end_time BIGINT NOT NULL," +
                        "active BOOLEAN NOT NULL," +
                        "server VARCHAR(64) NOT NULL" +
                        ")");

                stmt.execute("CREATE TABLE IF NOT EXISTS ip_punishments (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "ip VARCHAR(45) NOT NULL," +
                        "type VARCHAR(16) NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "operator VARCHAR(36) NOT NULL," +
                        "start_time BIGINT NOT NULL," +
                        "end_time BIGINT NOT NULL," +
                        "active BOOLEAN NOT NULL," +
                        "server VARCHAR(64) NOT NULL" +
                        ")");

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_uuid ON punishments(uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_active ON punishments(active)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip_punishments_ip ON ip_punishments(ip)");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public CompletableFuture<Punishment> savePunishment(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO punishments (uuid, ip, type, reason, operator, start_time, end_time, active, server) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, punishment.getUuid().toString());
                stmt.setString(2, punishment.getIp());
                stmt.setString(3, punishment.getType().name());
                stmt.setString(4, punishment.getReason());
                stmt.setString(5, punishment.getOperator());
                stmt.setLong(6, punishment.getStartTime());
                stmt.setLong(7, punishment.getEndTime());
                stmt.setBoolean(8, punishment.isActive());
                stmt.setString(9, punishment.getServer());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        punishment.setId(rs.getInt(1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return punishment;
        }, executor);
    }

    public CompletableFuture<IPPunishment> saveIPPunishment(IPPunishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO ip_punishments (ip, type, reason, operator, start_time, end_time, active, server) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, punishment.getIp());
                stmt.setString(2, punishment.getType().name());
                stmt.setString(3, punishment.getReason());
                stmt.setString(4, punishment.getOperator());
                stmt.setLong(5, punishment.getStartTime());
                stmt.setLong(6, punishment.getEndTime());
                stmt.setBoolean(7, punishment.isActive());
                stmt.setString(8, punishment.getServer());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        punishment.setId(rs.getInt(1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return punishment;
        }, executor);
    }

    public CompletableFuture<Void> revokePunishment(int id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn
                            .prepareStatement("UPDATE punishments SET active = false WHERE id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public CompletableFuture<Void> revokeIPPunishment(int id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn
                            .prepareStatement("UPDATE ip_punishments SET active = false WHERE id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public CompletableFuture<History> getHistory(UUID uuid, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            List<IPPunishment> ipPunishments = new ArrayList<>();

            try (Connection conn = databaseManager.getConnection()) {
                try (PreparedStatement stmt = conn
                        .prepareStatement("SELECT * FROM punishments WHERE uuid = ? ORDER BY start_time DESC")) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            punishments.add(new Punishment(
                                    rs.getInt("id"),
                                    UUID.fromString(rs.getString("uuid")),
                                    rs.getString("ip"),
                                    PunishmentType.valueOf(rs.getString("type")),
                                    rs.getString("reason"),
                                    rs.getString("operator"),
                                    rs.getLong("start_time"),
                                    rs.getLong("end_time"),
                                    rs.getBoolean("active"),
                                    rs.getString("server")));
                        }
                    }
                }

                if (ip != null) {
                    try (PreparedStatement stmt = conn
                            .prepareStatement("SELECT * FROM ip_punishments WHERE ip = ? ORDER BY start_time DESC")) {
                        stmt.setString(1, ip);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                ipPunishments.add(new IPPunishment(
                                        rs.getInt("id"),
                                        rs.getString("ip"),
                                        PunishmentType.valueOf(rs.getString("type")),
                                        rs.getString("reason"),
                                        rs.getString("operator"),
                                        rs.getLong("start_time"),
                                        rs.getLong("end_time"),
                                        rs.getBoolean("active"),
                                        rs.getString("server")));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return new History(uuid, punishments, ipPunishments);
        }, executor);
    }
}
