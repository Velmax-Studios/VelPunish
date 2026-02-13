package com.velpunish.common.models;

public class IPPunishment {
    private int id;
    private String ip;
    private PunishmentType type;
    private String reason;
    private String operator;
    private long startTime;
    private long endTime;
    private boolean active;
    private String server;

    public IPPunishment(int id, String ip, PunishmentType type, String reason, String operator, long startTime,
            long endTime, boolean active, String server) {
        this.id = id;
        this.ip = ip;
        this.type = type;
        this.reason = reason;
        this.operator = operator;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
        this.server = server;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public PunishmentType getType() {
        return type;
    }

    public void setType(PunishmentType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
