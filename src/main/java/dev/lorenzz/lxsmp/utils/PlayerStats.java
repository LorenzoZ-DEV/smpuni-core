package dev.lorenzz.lxsmp.utils;

public class PlayerStats {

    private final java.util.UUID uuid;
    private int kills;
    private int deaths;

    public PlayerStats(java.util.UUID uuid, int kills, int deaths) {
        this.uuid = uuid;
        this.kills = kills;
        this.deaths = deaths;
    }

    public java.util.UUID getUuid() {
        return uuid;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void incrementKills() {
        this.kills++;
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
}

