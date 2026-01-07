/* Group 8
 *  Name: ISH, T00702113
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.broadcast;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.finalshifttrackerapp.R;
import com.example.finalshifttrackerapp.main.MainActivity;

import java.util.Locale;

// Showing notification for Work Hours Limit
public class NotificationHelper {

    private static final String CHANNEL_ID = "weekly_hour_limit_channel";

    private static final int NOTIFICATION_ID = 101;

    private final Context mContext;

    public NotificationHelper(Context context) {
        this.mContext = context;
        // Creating New Notification Channel
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Weekly Hour Limit Warnings";
            String description = "Notifies when you are approaching your weekly work hour limit";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void sendHourLimitNotification(double hoursWorked, double hoursRemaining, int threshold) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Set up what happens when the user TAPS the notification
        Intent intent = new Intent(mContext, MainActivity.class);
        // These flags ensure that opening the notification resets the app view cleanly
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Wrap the intent in a PendingIntent so the system can trigger it later
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Format the text messages
        String title = String.format(Locale.getDefault(), "Approaching Weekly Hour Limit (%d hours)", threshold);
        String content = String.format(Locale.getDefault(),
                "You have worked %.1f hours this week. %.1f hours remaining.",
                hoursWorked,
                Math.max(0, hoursRemaining) //Prevents showing negative numbers in the notification
        );

        // Building the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Shows the notification!
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);

        // We add 'threshold' to the ID. This ensures the "5 hour" warning and "2 hour" warning have different IDs, so they can both exist in the tray at the same time if needed.
        notificationManager.notify(NOTIFICATION_ID + threshold, builder.build());
    }
}