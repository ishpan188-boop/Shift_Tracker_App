/* Group 8
 *  Name: ISH, T00702113
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.finalshifttrackerapp.history.ShiftData;
import com.example.finalshifttrackerapp.history.ShiftDataManager;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Checking Work Hours Limit and sending notification if needed
public class HourLimitCheckReceiver extends BroadcastReceiver {

    public static final String ACTION_CHECK_LIMIT = "com.example.finalshifttrackerapp.action.CHECK_HOUR_LIMIT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context != null && intent != null && ACTION_CHECK_LIMIT.equals(intent.getAction())) {
            // Running the heavy calculation on a background thread so the app doesn't freeze for the user
            new Thread(() -> checkWeeklyHours(context)).start();
        }
    }

    private void checkWeeklyHours(Context context) {
        // Find out who is currently using the app
        SharedPreferences sessionPrefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String loggedUser = sessionPrefs.getString("LOGGED_IN_USER", null);

        // If nobody is logged in, we can't check hours, so we stop here
        if (loggedUser == null) {
            return;
        }

        // Look up the specific hour limit for this user (default is 24 hours if not set)
        SharedPreferences profilePrefs = context.getSharedPreferences("UserProfile_" + loggedUser, Context.MODE_PRIVATE);
        String hoursLimitStr = profilePrefs.getString("HOURS_LIMIT", "24.0");
        double weeklyLimitHours;
        try {
            weeklyLimitHours = Double.parseDouble(hoursLimitStr);
        } catch (NumberFormatException e) {
            weeklyLimitHours = 24.0;
        }

        // Calculate how much work they have done so far this week
        ShiftDataManager shiftManager = new ShiftDataManager(context);
        List<ShiftData> allShifts = shiftManager.getAllShifts();
        long totalMillisThisWeek = 0;

        // Set up a calendar to find the exact moment this week started (Sunday/Monday midnight depending on locale)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfWeekMillis = cal.getTimeInMillis();

        // Loop through all their shifts and add up time ONLY if the shift happened after the week started
        for (ShiftData shift : allShifts) {
            if (shift.getStartTime() >= startOfWeekMillis) {
                totalMillisThisWeek += shift.getDurationMillis();
            }
        }

        // Convert the total time from milliseconds into hours
        double hoursWorkedThisWeek = TimeUnit.MILLISECONDS.toMinutes(totalMillisThisWeek) / 60.0;

        // Decide if we need to send a warning notification
        double remainingHours = weeklyLimitHours - hoursWorkedThisWeek;

        // We use this file to remember if we already warned them, so we don't spam notifications
        SharedPreferences notifPrefs = context.getSharedPreferences("NotificationPrefs_" + loggedUser, Context.MODE_PRIVATE);
        int currentWeekNumber = getWeekOfYear();

        // If a new week has started since we last checked, clear the old "warned" flags
        int lastCleanupWeek = notifPrefs.getInt("LAST_CLEANUP_WEEK", 0);
        if(currentWeekNumber != lastCleanupWeek) {
            notifPrefs.edit().clear().putInt("LAST_CLEANUP_WEEK", currentWeekNumber).apply();
        }

        // Check if we already sent warnings for this specific week
        boolean sent5HourWarning = notifPrefs.getBoolean("SENT_5_HOUR_WARNING_WEEK_" + currentWeekNumber, false);
        boolean sent2HourWarning = notifPrefs.getBoolean("SENT_2_HOUR_WARNING_WEEK_" + currentWeekNumber, false);

        NotificationHelper notificationHelper = new NotificationHelper(context);

        // If less than 2 hours remain AND we haven't warned them about the 2-hour mark yet.
        if (remainingHours <= 2 && !sent2HourWarning) {
            notificationHelper.sendHourLimitNotification(hoursWorkedThisWeek, remainingHours, 2);
            // Mark this warning as "sent" so we don't do it again this week
            notifPrefs.edit().putBoolean("SENT_2_HOUR_WARNING_WEEK_" + currentWeekNumber, true).apply();

            // Otherwise, if less than 5 hours remain AND we haven't warned them about the 5-hour mark yet.
        } else if (remainingHours <= 5 && !sent5HourWarning) {
            notificationHelper.sendHourLimitNotification(hoursWorkedThisWeek, remainingHours, 5);
            // Mark this warning as "sent"
            notifPrefs.edit().putBoolean("SENT_5_HOUR_WARNING_WEEK_" + currentWeekNumber, true).apply();
        }
    }

    // A small helper to just get the number of the current week
    private int getWeekOfYear() {
        return Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
    }
}