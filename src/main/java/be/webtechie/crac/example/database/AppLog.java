package be.webtechie.crac.example.database;

import java.time.ZonedDateTime;

public class AppLog {
    private Integer id;
    private ZonedDateTime timestamp;
    private Integer duration;
    private String description;

    public AppLog() {
        this("", 0);
    }

    public AppLog(Integer id, ZonedDateTime timestamp, Integer duration, String description) {
        this.id = id;
        this.timestamp = timestamp;
        this.duration = duration;
        this.description = description;
    }

    public AppLog(String description) {
        this(description, 0);
    }

    public AppLog(String description, int duration) {
        this.timestamp = ZonedDateTime.now();
        this.duration = duration;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "AppLog["
                + "id=" + id
                + ", timestamp=" + timestamp
                + ", duration=" + duration
                + ", description=" + description
                + ']';
    }
}
