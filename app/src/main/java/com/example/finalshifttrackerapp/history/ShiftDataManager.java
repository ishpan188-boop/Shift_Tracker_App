/* Group 8
 *  Name: RENIL, T00732524
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.history;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftDataManager {
    // The name of the file where we save data on the phone
    private static final String PREFS_NAME = "ShiftData";
    private static final String KEY_SHIFTS = "shifts_list";
    private static final String KEY_HOURLY_RATE = "hourly_rate";
    private static final double DEFAULT_HOURLY_RATE = 17.85;

    private SharedPreferences prefs;

    public ShiftDataManager(Context context) {
        // Open / Creates the shared preferences file
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Adds a single new shift to our existing list and saves everything
    public void saveShift(ShiftData shift) {
        List<ShiftData> shifts = getAllShifts();
        shifts.add(shift);
        saveAllShifts(shifts);
    }

    // Converts our list of Java objects into a text string and saves it to the phone's storage.
    private void saveAllShifts(List<ShiftData> shifts) {
        JSONArray jsonArray = new JSONArray();
        for (ShiftData shift : shifts) {
            try {
                // Convert each shift object to JSON text before adding to the list
                jsonArray.put(new JSONObject(shift.toJson()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // Write the final long string to storage
        prefs.edit().putString(KEY_SHIFTS, jsonArray.toString()).apply();
    }

    // Reads the text file from storage and converts it back into a list of Java objects
    public List<ShiftData> getAllShifts() {
        List<ShiftData> shiftList = new ArrayList<>();
        // Get the saved string, or an empty list "[]" if nothing is saved yet
        String jsonString = prefs.getString(KEY_SHIFTS, "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ShiftData shift = ShiftData.fromJson(jsonObject.toString());
                if (shift != null) {
                    shiftList.add(shift);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Sort the list by time (oldest to newest).
        // This ensures our graphs look correct and aren't scrambled.
        Collections.sort(shiftList, new Comparator<ShiftData>() {
            @Override
            public int compare(ShiftData o1, ShiftData o2) {
                return Long.compare(o1.getStartTime(), o2.getStartTime());
            }
        });

        return shiftList;
    }

    // Return Weekly Shifts
    public List<ShiftData> getWeekShifts() {
        List<ShiftData> allShifts = getAllShifts();
        List<ShiftData> weekShifts = new ArrayList<>();

        // Logic to find the exact time when "This Week" started
        Calendar cal = Calendar.getInstance();
        // Rewind the clock to the first day of the week (usually Sunday or Monday)
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0); // Midnight
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long weekStart = cal.getTimeInMillis();

        // Only keep shifts that happened after that start time
        for (ShiftData shift : allShifts) {
            if (shift.getStartTime() >= weekStart) {
                weekShifts.add(shift);
            }
        }
        return weekShifts;
    }

    // Return Monthly Shifts
    public List<ShiftData> getMonthShifts() {
        List<ShiftData> allShifts = getAllShifts();
        List<ShiftData> monthShifts = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        // Rewind clock to the 1st day of the current month
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();

        for (ShiftData shift : allShifts) {
            if (shift.getStartTime() >= monthStart) {
                monthShifts.add(shift);
            }
        }
        return monthShifts;
    }

    // Return Year-To-Date Shifts
    public List<ShiftData> getYTDShifts() {
        List<ShiftData> allShifts = getAllShifts();
        List<ShiftData> ytdShifts = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        // Rewind clock to Jan 1st of this year
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long yearStart = cal.getTimeInMillis();

        for (ShiftData shift : allShifts) {
            if (shift.getStartTime() >= yearStart) {
                ytdShifts.add(shift);
            }
        }
        return ytdShifts;
    }

    // Adds up the duration of every shift in the list
    public double calculateTotalHours(List<ShiftData> shifts) {
        double totalHours = 0;
        for (ShiftData shift : shifts) {
            // Convert milliseconds to hours
            totalHours += shift.getDurationMillis() / (1000.0 * 60 * 60);
        }
        return totalHours;
    }

    public double calculateTotalEarnings(List<ShiftData> shifts) {
        double totalHours = calculateTotalHours(shifts);
        return totalHours * getHourlyRate();
    }

    // Methods to manage the user's hourly pay rate
    public double getHourlyRate() {
        return prefs.getFloat(KEY_HOURLY_RATE, (float) DEFAULT_HOURLY_RATE);
    }

    public void setHourlyRate(double rate) {
        prefs.edit().putFloat(KEY_HOURLY_RATE, (float) rate).apply();
    }

    // Used for the Weekly Graph: Buckets hours into 7 slots (Sun-Sat)
    public double[] getWeeklyHoursByDay() {
        double[] hoursByDay = new double[7]; // Array of 7 zeros
        List<ShiftData> weekShifts = getWeekShifts();

        Calendar cal = Calendar.getInstance();
        for (ShiftData shift : weekShifts) {
            cal.setTimeInMillis(shift.getStartTime());
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // Convert to index 0-6

            // Add this shift's hours to the correct day bucket
            if (dayOfWeek >= 0 && dayOfWeek < 7) {
                hoursByDay[dayOfWeek] += shift.getDurationMillis() / (1000.0 * 60 * 60);
            }
        }

        return hoursByDay;
    }

    // Used for the Yearly Graph: Buckets hours into 12 slots (Jan-Dec)
    public double[] getYearlyHoursByMonth() {
        double[] hoursByMonth = new double[12]; // Array of 12 zeros
        List<ShiftData> ytdShifts = getYTDShifts();

        Calendar cal = Calendar.getInstance();
        for (ShiftData shift : ytdShifts) {
            cal.setTimeInMillis(shift.getStartTime());
            int month = cal.get(Calendar.MONTH); // Jan is 0, Dec is 11

            if (month >= 0 && month < 12) {
                hoursByMonth[month] += shift.getDurationMillis() / (1000.0 * 60 * 60);
            }
        }

        return hoursByMonth;
    }

    // Groups shifts by date. If you worked twice on Monday, this combines them into one entry.
    public List<DailyData> getDailyBreakdown(List<ShiftData> shifts) {
        List<DailyData> dailyData = new ArrayList<>();

        if (shifts.isEmpty()) {
            return dailyData;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (ShiftData shift : shifts) {
            String dateStr = dateFormat.format(new Date(shift.getStartTime()));
            double hours = shift.getDurationMillis() / (1000.0 * 60 * 60);

            // Check if we already have an entry for this specific date
            boolean found = false;
            for (DailyData data : dailyData) {
                if (data.getDate().equals(dateStr)) {
                    // We found it! Just add the new hours to the existing total.
                    data.addHours(hours);
                    found = true;
                    break;
                }
            }

            // If it's a new date, create a new entry
            if (!found) {
                dailyData.add(new DailyData(dateStr, hours));
            }
        }

        return dailyData;
    }

    // Delete everything
    public void clearAllShifts() {
        prefs.edit().remove(KEY_SHIFTS).apply();
    }

    public static class DailyData {
        private String date;
        private double hours;

        public DailyData(String date, double hours) {
            this.date = date;
            this.hours = hours;
        }

        public String getDate() {
            return date;
        }

        public double getHours() {
            return hours;
        }

        public void addHours(double h) {
            this.hours += h;
        }

        public String getFormattedHours() {
            return String.format(Locale.getDefault(), "%.2f h", hours);
        }

        public double getEarnings(double hourlyRate) {
            return hours * hourlyRate;
        }
    }
}