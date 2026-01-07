/* Group 8
 *  Name: RENIL, T00732524
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.history;

// This is a simple class that holds the data for ONE single row in the list.
public class SummaryItem {
    private String period; // The label on the left (e.g., "Monday", "January")
    private String value;  // The info on the right (e.g., "8.00 h")

    public SummaryItem(String period, String value) {
        this.period = period;
        this.value = value;
    }

    public String getPeriod() {
        return period;
    }

    public String getValue() {
        return value;
    }
}