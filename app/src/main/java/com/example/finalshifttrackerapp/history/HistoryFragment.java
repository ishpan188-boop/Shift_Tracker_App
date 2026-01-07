/* Group 8
*  Name: RENIL, T00732524
*  ShiftMate: Shift Tracker App
* */

package com.example.finalshifttrackerapp.history;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalshifttrackerapp.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class HistoryFragment extends Fragment {

    // UI components
    private RecyclerView recyclerView;
    private TextView textValue;
    private BarChart barChart;


    private ShiftDataManager shiftManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // tool to help us calculate shift hours
        shiftManager = new ShiftDataManager(requireContext());

        // Finding the view by its ID
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        textValue = view.findViewById(R.id.text_value);
        recyclerView = view.findViewById(R.id.recycler_view);
        barChart = view.findViewById(R.id.bar_chart);

        // Setup the list view
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create the 3 tabs for different time periods
        tabLayout.addTab(tabLayout.newTab().setText("Week"));
        tabLayout.addTab(tabLayout.newTab().setText("Month"));
        tabLayout.addTab(tabLayout.newTab().setText("YTD")); // Year To Date

        // Load the "Week" data by default when the app starts (0 is the first tab)
        updateData(0);

        // Listen for when the user taps a different tab
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // When a tab is clicked, update the screen with that specific data
                updateData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the user leaves this screen and comes back, refresh the data
        // This ensures the numbers are correct if they just added a new shift
        TabLayout tabLayout = getView().findViewById(R.id.tab_layout);
        if (tabLayout != null) {
            updateData(tabLayout.getSelectedTabPosition());
        }
    }

    // This is the main function that decides what to show based on which tab is active
    private void updateData(int tabPosition) {
        // These lists will hold the data for our list view and chart
        List<SummaryItem> items = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Switch logic: check which tab number was sent here
        switch (tabPosition) {
            case 0: // Week
                updateWeekData(items, entries, labels);
                break;
            case 1: // Month
                updateMonthData(items, entries, labels);
                break;
            case 2: // Year To Date
                updateYTDData(items, entries, labels);
                break;
        }

        // Send the prepared data to the list view and the chart
        recyclerView.setAdapter(new SummaryAdapter(items));
        setupChart(entries, labels);
    }

    // Logic to calculate Weekly stats
    private void updateWeekData(List<SummaryItem> items, List<BarEntry> entries, List<String> labels) {

        List<ShiftData> weekShifts = shiftManager.getWeekShifts();
        double totalHours = shiftManager.calculateTotalHours(weekShifts);

        // Show total hours at the top
        textValue.setText(String.format(Locale.getDefault(), "%.2f Hours", totalHours));

        // Get the hours broken down by each day
        double[] hoursByDay = shiftManager.getWeeklyHoursByDay();
        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] dayLabels = {"S", "M", "T", "W", "T", "F", "S"};

        // We want the list to start on Monday (index 1), not Sunday (index 0)
        int[] order = {1, 2, 3, 4, 5, 6, 0};

        // Loop through the days in our specific order
        for (int i = 0; i < order.length; i++) {
            int dayIndex = order[i];
            double hours = hoursByDay[dayIndex];

            // Add to the list view data
            items.add(new SummaryItem(dayNames[dayIndex],
                    String.format(Locale.getDefault(), "%.2f h", hours)));

            // Add to the chart data
            entries.add(new BarEntry(i, (float) hours));
            labels.add(dayLabels[dayIndex]);
        }

        // If user hasn't worked at all, show a friendly message
        if (totalHours == 0) {
            textValue.setText("0.00 Hours");
            items.clear();
            items.add(new SummaryItem("No shifts recorded", "Start tracking your shifts!"));
        }
    }

    // Logic to calculate Monthly stats
    private void updateMonthData(List<SummaryItem> items, List<BarEntry> entries, List<String> labels) {
        List<ShiftData> monthShifts = shiftManager.getMonthShifts();
        double totalHours = shiftManager.calculateTotalHours(monthShifts);

        textValue.setText(String.format(Locale.getDefault(), "%.2f Hours", totalHours));

        // Get a list of days where work actually happened
        List<ShiftDataManager.DailyData> dailyData = shiftManager.getDailyBreakdown(monthShifts);

        if (dailyData.isEmpty()) {
            textValue.setText("0.00 Hours");
            items.add(new SummaryItem("No shifts recorded", "Start tracking your shifts!"));
        } else {
            // Loop through every day we have data for
            for (int i = 0; i < dailyData.size(); i++) {
                ShiftDataManager.DailyData data = dailyData.get(i);

                // Add the date and hours to our lists
                items.add(new SummaryItem(data.getDate(), data.getFormattedHours()));
                entries.add(new BarEntry(i, (float) data.getHours()));
                labels.add(String.valueOf(i + 1)); // Label the chart 1, 2, 3, etc.
            }
        }
    }

    // Logic to calculate Yearly stats
    private void updateYTDData(List<SummaryItem> items, List<BarEntry> entries, List<String> labels) {
        List<ShiftData> ytdShifts = shiftManager.getYTDShifts();
        double totalHours = shiftManager.calculateTotalHours(ytdShifts);

        textValue.setText(String.format(Locale.getDefault(), "%.2f Hours", totalHours));

        // Get hours for every month (Jan-Dec)
        double[] hoursByMonth = shiftManager.getYearlyHoursByMonth();
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        // Find out what month it is right now so we don't show future months
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);

        // Loop from January (0) up to the current month
        for (int i = 0; i <= currentMonth; i++) {
            double hours = hoursByMonth[i];

            items.add(new SummaryItem(monthNames[i],
                    String.format(Locale.getDefault(), "%.2f h", hours)));

            entries.add(new BarEntry(i, (float) hours));
            labels.add(monthNames[i].substring(0, 3)); // Use short names like "Jan", "Feb"
        }

        if (totalHours == 0) {
            textValue.setText("0.00 Hours");
            items.clear();
            items.add(new SummaryItem("No shifts recorded", "Start tracking your shifts!"));
        }
    }

    // Configures how the Bar Chart looks
    private void setupChart(List<BarEntry> entries, List<String> labels) {
        if (entries.isEmpty()) {
            // If we have no data, make a dummy entry so the chart doesn't crash
            entries.add(new BarEntry(0, 0));
            labels.add("");
        }

        // Create the dataset and set colors
        BarDataSet dataSet = new BarDataSet(entries, "Hours Worked");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        // Attach data to the chart and refresh it
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false); // Hide the description text
        barChart.invalidate(); // Tells Android to redraw the chart now

        // Configure the X-Axis (the bottom line with labels)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Ensure labels don't get squished together
        xAxis.setGranularityEnabled(true);
    }
}