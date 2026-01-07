/* Group 8
 *  Name: MUSTAFA, T00737910
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.finalshifttrackerapp.R;
import com.example.finalshifttrackerapp.history.ShiftDataManager;
import com.example.finalshifttrackerapp.signup.LoginActivity;

import java.util.Locale;

// This Fragment manages the "Profile" tab.
public class ProfileFragment extends Fragment {

    // UI Components
    private TextView hourlyRateText;
    private Button changeRateButton;
    private Button clearDataButton;
    private ShiftDataManager shiftManager;

    private EditText etUsername, etPassword, etPhone, etHoursLimit;
    private TextView txtName, txtEmail, txtRole;
    private Button btnSave, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shiftManager = new ShiftDataManager(requireContext());

        // 1. SECURITY CHECK: Is the user actually logged in?
        SharedPreferences sessionPrefs = requireActivity()
                .getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String loggedUser = sessionPrefs.getString("LOGGED_IN_USER", null);

        if (loggedUser == null) {
            // If not logged in, kick them back to the Login Screen immediately
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        // 2. Link the Java code to the visual XML elements
        initializeViews(view);

        // 3. Populate the screen with data specific to THIS user
        loadUserProfile(loggedUser);

        // 4. Activate the buttons (Save, Logout, etc.)
        setupButtonListeners(loggedUser, sessionPrefs);
    }

    private void initializeViews(View view) {
        // Find Views by IDs
        txtName = view.findViewById(R.id.txtName);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtRole = view.findViewById(R.id.txtRole);
        hourlyRateText = view.findViewById(R.id.hourly_rate_text);

        etUsername = view.findViewById(R.id.etProfileUsername);
        etPassword = view.findViewById(R.id.etProfilePassword);
        etPhone = view.findViewById(R.id.etPhone);
        etHoursLimit = view.findViewById(R.id.etHoursLimit);

        changeRateButton = view.findViewById(R.id.change_rate_button);
        clearDataButton = view.findViewById(R.id.clear_data_button);
        btnSave = view.findViewById(R.id.btnSaveProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    // Logic to fill in the profile fields
    private void loadUserProfile(String loggedUser) {
        String defaultName;
        String defaultEmail;
        String defaultRole;
        double defaultWage;
        double defaultWeeklyLimit = 24.0;

        if (loggedUser.equals("A")) {
            defaultName = "Ish";
            defaultEmail = "ish@email.com";
            defaultRole = "Manager";
            defaultWage = 25.0;
        } else if (loggedUser.equals("B")) {
            defaultName = "Renil";
            defaultEmail = "renil@email.com";
            defaultRole = "Supervisor";
            defaultWage = 22.0;
        } else if (loggedUser.equals("C")) {
            defaultName = "Mustafa";
            defaultEmail = "mustafa@email.com";
            defaultRole = "Employee";
            defaultWage = 19.0;
        } else {
            defaultEmail = loggedUser;
            String nameFromEmail = defaultEmail;
            int atIndex = defaultEmail.indexOf('@');
            if (atIndex > 0) {
                nameFromEmail = nameFromEmail.substring(0, atIndex);
            }

            if (!nameFromEmail.isEmpty()) {
                nameFromEmail = nameFromEmail.substring(0, 1).toUpperCase()
                        + nameFromEmail.substring(1);
            }

            defaultName = nameFromEmail;
            defaultRole = "Employee";
            defaultWage = 17.85;
        }


        SharedPreferences profilePrefs = requireActivity()
                .getSharedPreferences("UserProfile_" + loggedUser, Context.MODE_PRIVATE);

        String savedUsername = profilePrefs.getString("USERNAME", defaultName);
        String savedPassword = profilePrefs.getString("PASSWORD", "");
        String savedWage = profilePrefs.getString("WAGE_RATE", String.valueOf(defaultWage));
        String savedPhone = profilePrefs.getString("PHONE", "");
        String savedHoursLimit = profilePrefs.getString("HOURS_LIMIT", String.valueOf(defaultWeeklyLimit));

        // Update the UI
        txtName.setText("Name: " + savedUsername);
        txtEmail.setText("Email: " + defaultEmail);
        txtRole.setText("Role: " + defaultRole);

        etUsername.setText(savedUsername);
        etPassword.setText(savedPassword);
        etPhone.setText(savedPhone);
        etHoursLimit.setText(savedHoursLimit);

        // Sync the wage with our ShiftDataManager
        try {
            double wageRate = Double.parseDouble(savedWage);
            shiftManager.setHourlyRate(wageRate);
        } catch (NumberFormatException e) {
            // Ignore errors
        }
        updateHourlyRateDisplay();
    }

    private void setupButtonListeners(String loggedUser, SharedPreferences sessionPrefs) {
        SharedPreferences profilePrefs = requireActivity()
                .getSharedPreferences("UserProfile_" + loggedUser, Context.MODE_PRIVATE);

        // SAVE BUTTON: Writes all the text boxes to the storage file
        btnSave.setOnClickListener(v -> {
            String name = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String hoursLimit = etHoursLimit.getText().toString().trim();

            double currentWage = shiftManager.getHourlyRate();

            SharedPreferences.Editor editor = profilePrefs.edit();
            editor.putString("USERNAME", name);
            editor.putString("PASSWORD", pass);
            editor.putString("WAGE_RATE", String.valueOf(currentWage));
            editor.putString("PHONE", phone);
            editor.putString("HOURS_LIMIT", hoursLimit.isEmpty() ? "24.0" : hoursLimit);
            editor.apply();

            txtName.setText("Name: " + name);
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
        });

        // CHANGE RATE: Opens the popup dialog
        changeRateButton.setOnClickListener(v -> showChangeRateDialog(loggedUser));

        // CLEAR DATA: Opens the warning dialog
        clearDataButton.setOnClickListener(v -> showClearDataDialog());

        // LOGOUT: Deletes the session token and restarts the app
        btnLogout.setOnClickListener(v -> {
            sessionPrefs.edit().remove("LOGGED_IN_USER").apply();

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            // These flags clear the "Back Stack" so the user can't press Back to get in again
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHourlyRateDisplay();
    }

    private void updateHourlyRateDisplay() {
        double currentRate = shiftManager.getHourlyRate();
        hourlyRateText.setText(String.format(Locale.getDefault(), "$%.2f per hour", currentRate));
    }

    // Creates a popup window (AlertDialog) to type in a new number
    private void showChangeRateDialog(String loggedUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Set Hourly Rate");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter hourly rate (e.g., 17.85)");
        input.setText(String.valueOf(shiftManager.getHourlyRate()));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String rateStr = input.getText().toString().trim();
            if (!rateStr.isEmpty()) {
                try {
                    double rate = Double.parseDouble(rateStr);
                    if (rate > 0) {
                        // Update the manager used for calculations
                        shiftManager.setHourlyRate(rate);

                        // Update the user profile storage
                        SharedPreferences profilePrefs = requireActivity()
                                .getSharedPreferences("UserProfile_" + loggedUser, Context.MODE_PRIVATE);
                        profilePrefs.edit().putString("WAGE_RATE", String.valueOf(rate)).apply();

                        updateHourlyRateDisplay();
                        Toast.makeText(requireContext(), "Hourly rate updated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Please enter a valid rate", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // A warning popup before deleting everything
    private void showClearDataDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("This will delete all your shift history and earnings data. This action cannot be undone.\n\nAre you sure?")
                .setPositiveButton("Yes, Clear All", (dialog, which) -> {
                    shiftManager.clearAllShifts();
                    Toast.makeText(requireContext(), "All shift data cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }
}