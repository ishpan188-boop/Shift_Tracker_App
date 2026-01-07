/* Group 8
*  Name: ISH, T00702113
*  ShiftMate: Shift Tracker App
* */

package com.example.finalshifttrackerapp.home;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finalshifttrackerapp.R;
import com.example.finalshifttrackerapp.broadcast.HourLimitCheckReceiver;
import com.example.finalshifttrackerapp.history.ShiftData;
import com.example.finalshifttrackerapp.history.ShiftDataManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// The Main Dashboard. This controls the stopwatch, starts the background service, and saves the final shift data when you press Stop.
public class HomeFragment extends Fragment {

    private TextView dateTextView;
    private TextView timerTextView;
    private Button startButton, stopButton, breakButton;
    private int defaultTextColor;


    private Handler timerHandler = new Handler();

    private long startTime = 0L;
    private long timeSwapBuff = 0L;
    private long originalSessionStartTime = 0L;

    private boolean isonBreak = false;
    private boolean isRunning = false;

    private SharedPreferences prefs;

    // If user pause the timer from the Notification bar, this code hears it and updates the buttons on this screen to match.
    private final BroadcastReceiver serviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_UPDATE_UI.equals(intent.getAction())) {
                String state = intent.getStringExtra("STATE");
                if (TimerService.UI_STATE_PAUSED.equals(state)) {
                    performPauseUI();
                } else if (TimerService.UI_STATE_RESUMED.equals(state)) {
                    performResumeUI();
                }
                saveTimerState();
            }
        }
    };

    // Asking for Notification Permissions
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startTimerAndService();
            });

    // The Timer logic
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            // Calculate how much time has passed since we started
            long updatedTime = timeSwapBuff + (System.currentTimeMillis() - startTime);

            // Math to convert total milliseconds into Hours, Minutes, and Seconds
            int seconds = (int) (updatedTime / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            int hours = minutes / 60;
            minutes %= 60;

            // Update the text view
            timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));

            // "See you again in 100 milliseconds"
            timerHandler.postDelayed(this, 100);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Find Views by ID
        dateTextView = view.findViewById(R.id.date_textview);
        timerTextView = view.findViewById(R.id.timer_textview);
        startButton = view.findViewById(R.id.start_button);
        stopButton = view.findViewById(R.id.stop_button);
        breakButton = view.findViewById(R.id.break_button);
        defaultTextColor = timerTextView.getCurrentTextColor();


        prefs = requireActivity().getSharedPreferences("ShiftTimerPrefs", Context.MODE_PRIVATE);

        // Show today's date at the top
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        dateTextView.setText(sdf.format(new Date()));

        // Start Button logic
        startButton.setOnClickListener(v -> askForNotificationPermission());

        // Stop Button logic
        stopButton.setOnClickListener(v -> {

            // Pause the timer so the numbers stop moving while the user reads the summary
            if (!isonBreak) {
                performPauseUI();
                sendCommandToService(TimerService.ACTION_PAUSE);
            }

            // 2. Show the "Receipt" dialog with the stats
            long currentStopTime = System.currentTimeMillis();
            showStopConfirmationDialog(originalSessionStartTime, currentStopTime, timeSwapBuff);

            saveTimerState();
        });

        // Break Button logic
        breakButton.setOnClickListener(v -> {
            if (!isonBreak) {
                // Going on break: Pause UI and Service
                performPauseUI();
                sendCommandToService(TimerService.ACTION_PAUSE);
            } else {
                // Coming back from break: Resume UI and Service
                performResumeUI();
                Intent serviceIntent = new Intent(requireActivity(), TimerService.class);
                serviceIntent.setAction(TimerService.ACTION_RESUME);
                serviceIntent.putExtra("ELAPSED_TIME", timeSwapBuff);
                requireActivity().startService(serviceIntent);
            }
            saveTimerState();
        });

        // Restore the timer if the user is coming back to the app
        loadTimerState();
        return view;
    }

    // --- UPDATED ALERT DIALOG LOGIC ---
    private void showStopConfirmationDialog(long startTimestamp, long endTimestamp, long durationMillis) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());

        String dateStr = dayFormat.format(new Date(startTimestamp));
        String startTimeStr = timeFormat.format(new Date(startTimestamp));
        String endTimeStr = timeFormat.format(new Date(endTimestamp));

        // Calculate final duration
        int seconds = (int) (durationMillis / 1000);
        int minutes = seconds / 60;
        seconds %= 60;
        int hours = minutes / 60;
        minutes %= 60;
        String durationStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

        String message = "Date: " + dateStr + " " + "\nTime: " + startTimeStr + " - " + endTimeStr + " " + "\nTotal Hours: " + durationStr;

        new AlertDialog.Builder(requireContext())
                .setTitle("Shift Details")
                .setMessage(message)
                .setCancelable(false)

                // POSITIVE: "OK!" -> Actually Save and Finish
                .setPositiveButton("OK!", (dialog, which) -> {
                    // 1. Save the data using our ShiftDataManager
                    ShiftDataManager shiftManager = new ShiftDataManager(requireContext());
                    ShiftData shift = new ShiftData(startTimestamp, endTimestamp,
                            durationMillis, dateStr);
                    shiftManager.saveShift(shift);

                    // 2. Trigger the weekly hour limit check
                    Intent checkIntent = new Intent(HourLimitCheckReceiver.ACTION_CHECK_LIMIT);
                    checkIntent.setPackage(requireContext().getPackageName()); // Important for security
                    requireContext().sendBroadcast(checkIntent);


                    Toast.makeText(requireContext(), "Time Registered. Thank You!", Toast.LENGTH_SHORT).show();
                    performStopLogic(); // Reset the screen to 00:00:00
                })

                // NEGATIVE: "Resume?" -> Oops, I didn't mean to stop. Go back to working.
                .setNegativeButton("Resume?", (dialog, which) -> {
                    if (isonBreak) {
                        performResumeUI();
                        Intent serviceIntent = new Intent(requireActivity(), TimerService.class);
                        serviceIntent.setAction(TimerService.ACTION_RESUME);
                        serviceIntent.putExtra("ELAPSED_TIME", timeSwapBuff);
                        requireActivity().startService(serviceIntent);
                    }
                    Toast.makeText(requireContext(), "Your Shift resumes!", Toast.LENGTH_SHORT).show();
                })

                // NEUTRAL: "Edit Timings" -> Open the time picker to fix mistakes
                .setNeutralButton("Edit Timings", (dialog, which) -> {
                    showEditTimePickers(startTimestamp, endTimestamp);
                })
                .show();
    }

    // Opens a clock popup to let users manually change start/end times
    private void showEditTimePickers(long currentStart, long currentEnd) {
        final Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(currentStart);

        TimePickerDialog startPicker = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    startCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    startCal.set(Calendar.MINUTE, minute);
                    long newStartTime = startCal.getTimeInMillis();

                    final Calendar endCal = Calendar.getInstance();
                    endCal.setTimeInMillis(currentEnd);

                    // Once they pick a start time, immediately show the End Time picker
                    TimePickerDialog endPicker = new TimePickerDialog(requireContext(),
                            (v, h, m) -> {
                                endCal.set(Calendar.HOUR_OF_DAY, h);
                                endCal.set(Calendar.MINUTE, m);
                                long newEndTime = endCal.getTimeInMillis();

                                // Re-open the summary dialog with the NEW corrected times
                                showStopConfirmationDialog(newStartTime, newEndTime, newEndTime - newStartTime);
                            },
                            endCal.get(Calendar.HOUR_OF_DAY),
                            endCal.get(Calendar.MINUTE),
                            false
                    );
                    endPicker.setTitle("Select Shift End Time");
                    endPicker.show();
                },
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE),
                false
        );
        startPicker.setTitle("Select Shift Start Time");
        startPicker.show();
    }


    // Saves variables to storage so if the app crashes or closes, we don't lose the timer
    private void saveTimerState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("IS_RUNNING", isRunning);
        editor.putBoolean("IS_ON_BREAK", isonBreak);
        editor.putLong("START_TIME", startTime);
        editor.putLong("TIME_BUFF", timeSwapBuff);
        editor.putLong("ORIG_START_TIME", originalSessionStartTime);
        editor.apply();
    }

    // Reads the saved variables to restore the screen to how it was
    private void loadTimerState() {
        isRunning = prefs.getBoolean("IS_RUNNING", false);
        isonBreak = prefs.getBoolean("IS_ON_BREAK", false);
        startTime = prefs.getLong("START_TIME", 0L);
        timeSwapBuff = prefs.getLong("TIME_BUFF", 0L);
        originalSessionStartTime = prefs.getLong("ORIG_START_TIME", 0L);

        if (isRunning) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            breakButton.setEnabled(true);

            if (isonBreak) {
                // If we were on break, show RED text
                timerTextView.setTextColor(Color.RED);
                breakButton.setText("RESUME");
                // Calculate time up to the break
                long updatedTime = timeSwapBuff;
                int seconds = (int) (updatedTime / 1000);
                int minutes = seconds / 60;
                seconds %= 60;
                int hours = minutes / 60;
                minutes %= 60;
                timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
            } else {
                // If we were running, start the ticker again
                timerTextView.setTextColor(defaultTextColor);
                breakButton.setText("BREAK");
                timerHandler.postDelayed(timerRunnable, 0);
            }
        } else {
            // Reset buttons if nothing was running
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            breakButton.setEnabled(false);
        }
    }

    // Updates the UI to look "Paused" (Red text, Resume button)
    private void performPauseUI() {
        if (!isonBreak) {
            timeSwapBuff += (System.currentTimeMillis() - startTime);
            timerHandler.removeCallbacks(timerRunnable); // Stop the ticker
            timerTextView.setTextColor(Color.RED);
            breakButton.setText("RESUME");
            isonBreak = true;
        }
    }

    // Updates the UI to look "Running" (Normal text, Break button)
    private void performResumeUI() {
        if (isonBreak) {
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0); // Start the ticker
            timerTextView.setTextColor(defaultTextColor);
            breakButton.setText("BREAK");
            isonBreak = false;
        }
    }

    // Completely resets everything to 0 when the user finishes a shift
    private void performStopLogic() {
        sendCommandToService(TimerService.ACTION_STOP);

        timerHandler.removeCallbacks(timerRunnable);
        timerTextView.setText("00:00:00");
        timeSwapBuff = 0L;
        timerTextView.setTextColor(defaultTextColor);
        breakButton.setText("BREAK");
        isonBreak = false;
        isRunning = false;
        originalSessionStartTime = 0L;

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        breakButton.setEnabled(false);

        saveTimerState();
    }

    // Helper to send simple commands (Start, Stop, Pause) to the background service
    private void sendCommandToService(String action) {
        Intent serviceIntent = new Intent(requireActivity(), TimerService.class);
        serviceIntent.setAction(action);
        requireActivity().startService(serviceIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start listening for messages from the service
        IntentFilter filter = new IntentFilter(TimerService.ACTION_UPDATE_UI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(serviceUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(serviceUpdateReceiver, filter);
        }

        // Check if the user clicked the "STOP" button directly on the Notification bar
        Intent intent = requireActivity().getIntent();
        if (intent != null && "STOP_COMMAND".equals(intent.getStringExtra("COMMAND_FROM_NOTIF"))) {

            if (!isonBreak) {
                performPauseUI();
            }
            sendCommandToService(TimerService.ACTION_PAUSE);

            // Immediately show the save dialog
            long currentStop = System.currentTimeMillis();
            showStopConfirmationDialog(originalSessionStartTime, currentStop, timeSwapBuff);

            intent.removeExtra("COMMAND_FROM_NOTIF");
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop listening for messages when the screen is closed to save battery
        requireActivity().unregisterReceiver(serviceUpdateReceiver);
        saveTimerState();
    }

    // Checks if we have permission to post notifications
    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startTimerAndService();
            }
        } else {
            startTimerAndService();
        }
    }

    // The actual "GO" button logic that starts the timer and service
    private void startTimerAndService() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            originalSessionStartTime = startTime;
            timerHandler.postDelayed(timerRunnable, 0);

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            breakButton.setEnabled(true);
            isRunning = true;

            sendCommandToService(TimerService.ACTION_START);
            saveTimerState();
        }
    }
}
