package com.uberclone.clone.uberclone.HistoryRecyclerView;

/**
 * Created by Admin on 12/20/2018.
 */

public class HistoryObject {

    private String time;
    private String rideId;

    public HistoryObject(String rideId, String time) {
        this.time = time;
        this.rideId = rideId;
    }

    public String getRideId() {
        return rideId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }
}
