/* Group 8
 *  Name: RENIL, T00732524
 *  ShiftMate: Shift Tracker App
 * */


package com.example.finalshifttrackerapp.salary;

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
import com.example.finalshifttrackerapp.history.ShiftData;
import com.example.finalshifttrackerapp.history.ShiftDataManager;
import com.example.finalshifttrackerapp.history.SummaryAdapter;
import com.example.finalshifttrackerapp.history.SummaryItem;
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

// This Fragment controls the "Salary History" screen.
public class SalaryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView textValue;
    private BarChart barChart;
    private ShiftDataManager shiftManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_salary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shiftManager = new ShiftDataManager(requireContext());

        // Finding Views by IDs
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        textValue = view.findViewById(R.id.text_value);
        recyclerView = view.findViewById(R.id.recycler_view);
        barChart = view.findViewById(R.id.bar_chart);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create Tabs
        tabLayout.addTab(tabLayout.newTab().setText("Week"));
        tabLayout.addTab(tabLayout.newTab().setText("Month"));
        tabLayout.addTab(tabLayout.newTab().setText("YTD"));

        // Load the first tab (Week) by default
        updateData(0);

        // Listen for tab clicks
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
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
        // If users changes the Hoursly wage rate, then this updates it here
        TabLayout tabLayout = getView().findViewById(R.id.tab_layout);
        if (tabLayout != null) {
            updateData(tabLayout.getSelectedTabPosition());
        }
    }

    // Main logic to switch between Week, Month, and Year views
    private void updateData(int tabPosition) {
        List<SummaryItem> items = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        switch (tabPosition) {
            case 0: // Week
                updateWeekData(items, entries, labels);
                break;
            case 1: // Month
                updateMonthData(items, entries, labels);
                break;
            case 2: // YTD
                updateYTDData(items, entries, labels);
                break;
        }

        recyclerView.setAdapter(new SummaryAdapter(items));
        setupChart(entries, labels);
    }

    // Logic for Weekly Earnings
    private void updateWeekData(List<SummaryItem> items, List<BarEntry> entries, List<String> labels) {
        List<ShiftData> weekShifts = shiftManager.getWeekShifts();
        double totalEarnings = shiftManager.calculateTotalEarnings(weekShifts);
        double hourlyRate = shiftManager.getHourlyRate();

        textValue.setText(String.format(Locale.getDefault(), "$%.2f", totalEarnings));

        // Get the hours worked each day
        double[] hoursByDay = shiftManager.getWeeklyHoursByDay();
        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] dayLabels = {"S", "M", "T", "W", "T", "F", "S"};

        // Start on Monday (index 1)
        int[] order = {1, 2, 3, 4, 5, 6, 0};

        for (int i = 0; i < order.length; i++) {
            int dayIndex = order[i];
            double hours = hoursByDay[dayIndex];

            // MATH: Money = Hours * Hourly Rate
            double earnings = hours * hourlyRate;

            items.add(new SummaryItem(dayNames[dayIndex],
                    String.format(Locale.getDefault(), "$%.2f (%.2fh)", earnings, hours)));

            // Add to chart (Y-axis is Money now, not Hours)
            entries.add(new BarEntry(i, (float) earnings));
            labels.add(dayLabels[dayIndex]);
        }

        if (totalEarnings == 0) {
            textValue.setText("$0.00");
            items.clear();
            items.add(new SummaryItem("No earnings yet", "Start tracking your shifts!"));
        }
    }

    // Logic for Monthly Earnings
    private void updateMonthData(List<SummaryItem> items, List<BarEntry> entries, List<String> labels) {
        List<ShiftData> monthShifts = shiftManager.getMonthShifts();
        double totalEarnings = shiftManager.calculateTotalEarnings(monthShifts);
        double hourlyRate = shiftManager.getHourlyRate();

        textValue.setText(String.format(Locale.getDefault(), "$%.2f", totalEarnings));

        List<ShiftDataManager.DailyData> dailyData = shiftManager.getDailyBreakdown(monthShifts);

        if (dailyData.isEmpty()) {
            textValue.setText("$0.00");
            items.add(new SummaryItem("No earnings yet", "Start tracking your shifts!"));
        } else {
            for (int i = 0; i < dailyData.size(); i++) {
                ShiftDataManager.DailyData data = dailyData.get(i);

                // Helper method handles the calculation (hours * rate)
                double earnings = data.getEarnings(hourlyRate);

                items.add(new SummaryItem(data.getDate(),
                        String.format(Locale.getDefault(), "$%.2f (%.2fh)", earnings, data.getHours())));
                entries.add(new BarEntry(i, (float) earnings));
                labels.add(String.valueOf(i + 1));
            }
        }
    }

    // Logic for Yearly Earnings
    private void updateYTDData(List<SummaryItem> items, List<BarEntry> entries, List<String> labels) {
        List<ShiftData> ytdShifts = shiftManager.getYTDShifts();
        double totalEarnings = shiftManager.calculateTotalEarnings(ytdShifts);
        double hourlyRate = shiftManager.getHourlyRate();

        textValue.setText(String.format(Locale.getDefault(), "$%.2f", totalEarnings));

        double[] hoursByMonth = shiftManager.getYearlyHoursByMonth();
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);

        for (int i = 0; i <= currentMonth; i++) {
            double hours = hoursByMonth[i];
            double earnings = hours * hourlyRate;

            items.add(new SummaryItem(monthNames[i],
                    String.format(Locale.getDefault(), "$%.2f (%.2fh)", earnings, hours)));
            entries.add(new BarEntry(i, (float) earnings));
            labels.add(monthNames[i].substring(0, 3));
        }

        if (totalEarnings == 0) {
            textValue.setText("$0.00");
            items.clear();
            items.add(new SummaryItem("No earnings yet", "Start tracking your shifts!"));
        }
    }

    // Configure the chart (Green color for money!)
    private void setupChart(List<BarEntry> entries, List<String> labels) {
        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 0));
            labels.add("");
        }

        BarDataSet dataSet = new BarDataSet(entries, "Earnings");
        dataSet.setColor(Color.GREEN); // Money = Green
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
    }
}