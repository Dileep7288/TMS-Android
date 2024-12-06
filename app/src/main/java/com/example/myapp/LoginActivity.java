package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private String username, password;
    private final String LOGIN_URL = "http://172.16.20.76:8000/api/login/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);

        findViewById(R.id.login_btn).setOnClickListener(v -> login());

        findViewById(R.id.register_redirect_btn).setOnClickListener(v -> navigateToRegister());
    }

    public void login() {
        username = usernameInput.getText().toString().trim();
        password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject loginData = new JSONObject();
            loginData.put("username", username);
            loginData.put("password", password);

            JsonObjectRequest loginRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    LOGIN_URL,
                    loginData,
                    response -> {
                        try {
                            if (response.has("access") && response.has("refresh")) {
                                String accessToken = response.getString("access");
                                String refreshToken = response.getString("refresh");
                                storeTokens(accessToken, refreshToken);
                                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                                navigateToDashBoard();
                            } else {
                                Toast.makeText(this, "Unexpected server response", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        String errorMessage = "Login failed";
                        if (error.networkResponse != null) {
                            errorMessage += " (Error " + error.networkResponse.statusCode + ")";
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "Error: ", error);
                    }
            );
            Volley.newRequestQueue(this).add(loginRequest);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void storeTokens(String accessToken, String refreshToken) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.apply();
    }

    private void navigateToDashBoard() {
        Intent intent = new Intent(LoginActivity.this, DashBoardActivity.class);
        startActivity(intent);
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }
}
