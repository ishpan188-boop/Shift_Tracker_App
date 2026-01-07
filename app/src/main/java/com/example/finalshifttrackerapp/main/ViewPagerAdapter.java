/* Group 8
 *  Name(s): RENIL, ISH, & MUSTAFA
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.finalshifttrackerapp.profile.ProfileFragment;
import com.example.finalshifttrackerapp.salary.SalaryFragment;
import com.example.finalshifttrackerapp.history.HistoryFragment;
import com.example.finalshifttrackerapp.home.HomeFragment;

// This Adapter manages the Fragments that the user can swipe through.
public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    // This method is called whenever the user swipes to a new page.

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();    // The Main Timer Screen
            case 1:
                return new HistoryFragment(); // The Graphs and Lists
            case 2:
                return new SalaryFragment();  // The Money Calculator
            case 3:
                return new ProfileFragment(); // User Settings
            default:
                return new HomeFragment();    // Default fallback (Safety net)
        }
    }

    // Tells the ViewPager exactly how many screens are in our app.
    @Override
    public int getItemCount() {
        return 4; // We have 4 tabs
    }
}