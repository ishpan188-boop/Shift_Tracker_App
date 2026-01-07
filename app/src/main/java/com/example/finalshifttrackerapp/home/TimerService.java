/* Group 8
*  Name: ISH, T00702113
*  ShiftMate: Shift Tracker App
* */

package com.example.finalshifttrackerapp.home;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.finalshifttrackerapp.R;
import com.example.finalshifttrackerapp.main.MainActivity;

// This class is a "Foreground Service."
public class TimerService extends Service {

    public static final String CHANNEL_ID = "TimerChannel";
    public static final int NOTIFICATION_ID = 1;


    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";


    public static final String ACTION_UPDATE_UI = "com.example.finalshifttrackerapp.UPDATE_UI";
    public static final String UI_STATE_PAUSED = "STATE_PAUSED";
    public static final String UI_STATE_RESUMED = "STATE_RESUMED";

    private long mBaseTime = 0L;

    // This method runs every time you call startService() or click a notification button
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_START.equals(action)) {
                mBaseTime = System.currentTimeMillis();
                // Create the notification
                updateNotification(mBaseTime, true, false);
            }
            else if (ACTION_PAUSE.equals(action)) {
                // Calculate how much time passed
                long elapsedTime = System.currentTimeMillis() - mBaseTime;

                // Tell the App to turn the text RED
                sendBroadcastToUI(UI_STATE_PAUSED, elapsedTime);

                // Update the Notification bar to show "Paused"
                updateNotification(0, false, true);
            }
            else if (ACTION_RESUME.equals(action)) {
                // Get the old time from the intent so we continue where we left off
                long elapsedTime = intent.getLongExtra("ELAPSED_TIME", 0L);
                mBaseTime = System.currentTimeMillis() - elapsedTime;

                // Tell the App to turn the text NORMAL color
                sendBroadcastToUI(UI_STATE_RESUMED, elapsedTime);

                // Update the Notification bar to show the counting timer again
                updateNotification(mBaseTime, true, false);
            }
            else if (ACTION_STOP.equals(action)) {
                stopTimer();
            }
        }
        // If the system kills this service, do not auto-restart it (keep it simple)
        return START_NOT_STICKY;
    }


    private void sendBroadcastToUI(String state, long elapsedTime) {
        Intent intent = new Intent(ACTION_UPDATE_UI);
        intent.putExtra("STATE", state);
        intent.putExtra("ELAPSED_TIME", elapsedTime);
        intent.setPackage(getPackageName()); // Security: Only our app can hear this message
        sendBroadcast(intent);
    }

    // Builds the visual notification you see in the status bar
    private void updateNotification(long baseTime, boolean useChronometer, boolean isOnBreak) {
        createNotificationChannel();

        // 1. What happens if you tap the notification body? -> Open the App
        Intent appIntent = new Intent(this, MainActivity.class);
        // "Single Top" means: If app is already open, just switch to it (don't restart it)
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE
        );

        // 2. Define the Break/Resume Button
        Intent pauseResumeIntent = new Intent(this, TimerService.class);
        String buttonText;
        int buttonIcon;

        if (isOnBreak) {
            // If currently on break, the button should say "RESUME"
            pauseResumeIntent.setAction(ACTION_RESUME);

            // Logic to remember time while paused
            long currentElapsed = System.currentTimeMillis() - mBaseTime;
            pauseResumeIntent.putExtra("ELAPSED_TIME", currentElapsed);

            buttonText = "RESUME";
            buttonIcon = android.R.drawable.ic_media_play;
        } else {
            // If currently running, the button should say "BREAK"
            pauseResumeIntent.setAction(ACTION_PAUSE);
            buttonText = "BREAK";
            buttonIcon = android.R.drawable.ic_media_pause;
        }

        // A "PendingIntent"
        PendingIntent pauseResumePendingIntent = PendingIntent.getService(
                this, 1, pauseResumeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Define the Stop Button logic
        Intent stopIntent = new Intent(this, MainActivity.class);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // We add a special extra here so HomeFragment knows the user clicked STOP in the notification
        stopIntent.putExtra("COMMAND_FROM_NOTIF", "STOP_COMMAND");

        PendingIntent stopPendingIntent = PendingIntent.getActivity(
                this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 4. Actually build the notification box
        String contentText = isOnBreak ? "On Break!" : "Shift Running";
        String titleText = isOnBreak ? "Shift Paused" : "Shift Active";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentPendingIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(buttonIcon, buttonText, pauseResumePendingIntent)
                .addAction(android.R.drawable.ic_delete, "STOP", stopPendingIntent);

        // Should the notification show a ticking clock?
        if (useChronometer) {
            builder.setWhen(baseTime)
                    .setUsesChronometer(true); // Android's built-in ticking clock UI
        } else {
            builder.setUsesChronometer(false);
        }


        startForeground(NOTIFICATION_ID, builder.build());
    }

    // Kills the service and removes the notification
    private void stopTimer() {
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Timer Channel", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }
}