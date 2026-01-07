/* Group 8
 *  Name: RENIL, T00732524
 *  ShiftMate: Shift Tracker App
 * */


package com.example.finalshifttrackerapp.history;

import org.json.JSONException;
import org.json.JSONObject;

public class ShiftData {
    private long startTime;
    private long endTime;
    private long durationMillis;
    private String date;

    public ShiftData(long startTime, long endTime, long durationMillis, String date) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMillis = durationMillis;
        this.date = date;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getDate() {
        return date;
    }

    // Converts the time from milliseconds to hours
    public double getDurationHours() {
        return durationMillis / (1000.0 * 60.0 * 60.0);
    }


    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("startTime", startTime);
            json.put("endTime", endTime);
            json.put("durationMillis", durationMillis);
            json.put("date", date);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // This does the opposite of toJson(). It takes a saved string and turns it
    // back into a usable Java object we can work with in the app.
    public static ShiftData fromJson(String jsonStr) {
        try {
            // First, try to read it as a standard JSON object
            JSONObject json = new JSONObject(jsonStr);
            return new ShiftData(
                    json.getLong("startTime"),
                    json.getLong("endTime"),
                    json.getLong("durationMillis"),
                    json.getString("date")
            );
        } catch (JSONException e) {
            // BACKUP PLAN: If the string isn't JSON (maybe it's from an older version of the app),
            // try to read it as a simple list separated by commas.
            String[] parts = jsonStr.split(",");
            if (parts.length == 4) {
                try {
                    return new ShiftData(
                            Long.parseLong(parts[0]),
                            Long.parseLong(parts[1]),
                            Long.parseLong(parts[2]),
                            parts[3]
                    );
                } catch (NumberFormatException nfe) {
                    return null; // The data is corrupted, return nothing
                }
            }
            return null;
        }
    }
}