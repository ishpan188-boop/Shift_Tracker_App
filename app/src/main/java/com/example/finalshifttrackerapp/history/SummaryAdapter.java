/* Group 8
 *  Name: RENIL, T00732524
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalshifttrackerapp.R;

import java.util.List;

// This Adapter acts like a bridge between data and the RecyclerView.
public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.ViewHolder> {

    private List<SummaryItem> items;

    // We pass the data we want to display into the constructor
    public SummaryAdapter(List<SummaryItem> items) {
        this.items = items;
    }

    // This method is called when the list needs to create a new blank row.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_summary, parent, false);
        return new ViewHolder(view);
    }

    // This method is called to "fill in" the blanks.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SummaryItem item = items.get(position);
        holder.textPeriod.setText(item.getPeriod());
        holder.textValue.setText(item.getValue());
    }

    // Tells the RecyclerView exactly how many rows it needs to draw.
    @Override
    public int getItemCount() {
        return items.size();
    }

    // This inner class holds the UI elements for ONE single row.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textPeriod;
        TextView textValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textPeriod = itemView.findViewById(R.id.text_period);
            textValue = itemView.findViewById(R.id.text_value);
        }
    }
}