package com.velpunish.common.models;

import java.util.List;
import java.util.UUID;

public class History {
    private UUID uuid;
    private List<Punishment> punishments;
    private List<IPPunishment> ipPunishments;

    public History(UUID uuid, List<Punishment> punishments, List<IPPunishment> ipPunishments) {
        this.uuid = uuid;
        this.punishments = punishments;
        this.ipPunishments = ipPunishments;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public List<Punishment> getPunishments() {
        return punishments;
    }

    public void setPunishments(List<Punishment> punishments) {
        this.punishments = punishments;
    }

    public List<IPPunishment> getIpPunishments() {
        return ipPunishments;
    }

    public void setIpPunishments(List<IPPunishment> ipPunishments) {
        this.ipPunishments = ipPunishments;
    }
}
