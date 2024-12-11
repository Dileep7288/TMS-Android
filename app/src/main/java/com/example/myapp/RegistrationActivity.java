package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class RegistrationActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput;
    private String username, email, password;
    private final String REGISTER_URL = "http://172.16.20.76:8000/api/register/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);

        findViewById(R.id.register_btn).setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        username = usernameInput.getText().toString().trim();
        email = emailInput.getText().toString().trim();
        password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject registrationData = new JSONObject();
            registrationData.put("username", username);
            registrationData.put("email", email);
            registrationData.put("password", password);

            JsonObjectRequest registerRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    REGISTER_URL,
                    registrationData,
                    response -> {
                        try {
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        String errorMessage = "Registration failed";
                        if (error.networkResponse != null) {
                            errorMessage += " (Error " + error.networkResponse.statusCode + ")";
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
            );
            Volley.newRequestQueue(this).add(registerRequest);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void navigateToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }
}