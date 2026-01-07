/* Group 8
 *  Name: MUSTAFA, T00737910
 *  ShiftMate: Shift Tracker App
 * */

package com.example.finalshifttrackerapp.signup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.finalshifttrackerapp.R;
import com.example.finalshifttrackerapp.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Show the branding splash screen while the app loads
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // --- AUTO-LOGIN CHECK ---

        SharedPreferences sessionPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String loggedUser = sessionPrefs.getString("LOGGED_IN_USER", null);

        if (loggedUser != null) {
            // User is remembered -> Go straight to the Main App
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }


        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnCreate = findViewById(R.id.btnCreate);

        // Handle Login Click
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the email/password combo is correct
            String userCode = validateUser(email, password);

            if (userCode != null) {
                // Success! Save the "Session" token so the app remembers who this is.
                SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                prefs.edit().putString("LOGGED_IN_USER", userCode).apply();

                // Navigate to the main dashboard
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle "Sign Up" Click
        btnCreate.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    // Logic to check if the user exists and the password matches
    private String validateUser(String email, String password) {
        // 1. First, check the list of users who signed up manually via the app
        SharedPreferences userPrefs = getSharedPreferences("UserAccounts", MODE_PRIVATE);
        String storedPassword = userPrefs.getString(email, null);

        if (storedPassword != null && storedPassword.equals(password)) {
            return email;
        }

        // 2. If not found, check our hardcoded "Admin/Demo" users
        String userCode = null;
        String defaultPassword = null;

        if (email.equals("ish@email.com")) {
            userCode = "A";
            defaultPassword = "ishtru";
        } else if (email.equals("renil@email.com")) {
            userCode = "B";
            defaultPassword = "reniltru";
        } else if (email.equals("mustafa@email.com")) {
            userCode = "C";
            defaultPassword = "mustafatru";
        } else {
            return null;
        }

        // 3. Special Case: We check the specific profile file for an updated password.
        SharedPreferences profilePrefs =
                getSharedPreferences("UserProfile_" + userCode, MODE_PRIVATE);

        String savedPassword = profilePrefs.getString("PASSWORD", defaultPassword);

        // Compare what they typed vs what is saved
        return password.equals(savedPassword) ? userCode : null;
    }
}