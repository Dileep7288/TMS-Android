package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AdminMainPage extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_admin_main);

        Button adminLoginBtn = findViewById(R.id.admin_login_btn);
        adminLoginBtn.setOnClickListener(v -> navigateToAdminLogin());

        Button backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> finish());
    }

    private void navigateToAdminLogin() {
        Intent intent = new Intent(AdminMainPage.this, AdminLoginPage.class);
        startActivity(intent);
    }
}