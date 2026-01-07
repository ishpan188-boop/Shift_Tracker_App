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

import com.example.finalshifttrackerapp.R;

// This activity handles new user registration.
public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Finding Views by IDs
        etEmail = findViewById(R.id.etSignUpEmail);
        etPassword = findViewById(R.id.etSignUpPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Logic for when the "Sign Up" button is clicked
        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // 1. Basic Validation: Did the user leave anything blank?
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return; // Stop here, don't save anything
            }

            // 2. Security Validation: Is the password strong enough?
            if (password.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return; // Stop here
            }

            // 3. Save the Data
            SharedPreferences prefs = getSharedPreferences("UserAccounts", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // We use the Email as the "Key" and the Password as the "Value".
            editor.putString(email, password);
            editor.apply(); // Save immediately in the background

            Toast.makeText(this, "Account created! You can log in now.", Toast.LENGTH_SHORT).show();

            // 4. Return to the Login Screen so they can actually sign in
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }
}