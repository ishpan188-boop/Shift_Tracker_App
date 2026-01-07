/* Group 8
*  Name(s): RENIL, ISH, & MUSTAFA
*  ShiftMate: Shift Tracker App
* */

package com.example.finalshifttrackerapp.main;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.finalshifttrackerapp.R;
import com.example.finalshifttrackerapp.history.ShiftData;
import com.example.finalshifttrackerapp.history.ShiftDataManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    RelativeLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Findings view by IDs
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        mainLayout = findViewById(R.id.main);

        // The Adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Listen for swipes so we can change the background color dynamically.
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                changeBackgroundColor(position);
            }
        });

        // "Mediator" connects the Tabs (Buttons) to the ViewPager (Screens).
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Home");
                            tab.setIcon(R.drawable.home);
                            break;
                        case 1:
                            tab.setText("Work History");
                            tab.setIcon(R.drawable.working_hours);
                            break;
                        case 2:
                            tab.setText("Salary History");
                            tab.setIcon(R.drawable.money_back);
                            break;
                        case 3:
                            tab.setText("Profile");
                            tab.setIcon(R.drawable.profile);
                            break;
                    }
                }).attach();

        // --- TEST DATA GENERATOR ---
        ShiftDataManager manager = new ShiftDataManager(this);
        if (manager.getAllShifts().isEmpty()) {
            populateTestData(manager);
        }
    }

    // Creates dummy data for testing purposes
    private void populateTestData(ShiftDataManager manager) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // If November is in the future, use last year's November
        if (cal.after(Calendar.getInstance())) {
            cal.add(Calendar.YEAR, -1);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());

        // Loop through every day of November (1 to 30)
        for (int i = 1; i <= 30; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);

            // Set default start time to 9:00 AM
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            // Check if it's a weekend. If so, "continue" skips to the next loop iteration.
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                continue;
            }

            // Logic: Normal days are 8 hours, Fridays are 7.5 hours
            double hours = (dayOfWeek == Calendar.FRIDAY) ? 7.5 : 8.0;

            addShift(manager, cal, hours, dateFormat);
        }
    }

    // Helper to save the dummy shift
    private void addShift(ShiftDataManager manager, Calendar cal, double hours, SimpleDateFormat dateFormat) {
        long startTime = cal.getTimeInMillis();
        long durationMillis = (long) (hours * 60 * 60 * 1000);
        long endTime = startTime + durationMillis;
        String dateStr = dateFormat.format(new Date(startTime));

        ShiftData shift = new ShiftData(startTime, endTime, durationMillis, dateStr);
        manager.saveShift(shift);
    }

    // Changes the background color based on which tab is active
    private void changeBackgroundColor(int position) {
        switch (position) {
            case 0: // HOME (Soft Blue-Grey)
                mainLayout.setBackgroundColor(Color.parseColor("#6699CC"));
                break;

            case 1: // WORK HISTORY (Clean White for reading lists)
                mainLayout.setBackgroundColor(Color.parseColor("#CDCDCD"));
                break;

            case 2: // SALARY (Soft Mint Green for Money)
                mainLayout.setBackgroundColor(Color.parseColor("#EFF8F3"));
                break;

            case 3: // PROFILE (Soft Lavender)
                mainLayout.setBackgroundColor(Color.parseColor("#DBC9FA"));
                break;

            default:
                mainLayout.setBackgroundColor(Color.parseColor("#6699CC"));
                break;
        }
    }
}