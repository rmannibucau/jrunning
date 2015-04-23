package io.github.rmannibucau.jrunning;

import android.location.Location;
import org.androidannotations.annotations.EBean;

@EBean
public class Session {
    private long startTime;
    private long endTime;
    private Location lastLocation;
    private boolean running;
    private int points = 0;
    private String id;

    public void updateLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
        this.endTime = lastLocation.getTime();
        points++;
    }

    public void start(String sId) {
        id = sId;
        startTime = System.currentTimeMillis();
        lastLocation = null;
        running = true;
    }

    public String getId() {
        return id;
    }

    public int getPoints() {
        return points;
    }

    public void finish() {
        running = false;
    }

    public long getDuration() {
        return endTime == 0 ? 0 : endTime - startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public boolean isRunning() {
        return running;
    }
}
