package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        findViewById(R.id.login_btn).setOnClickListener(v -> navigateToLogin());
        findViewById(R.id.register_btn).setOnClickListener(v -> navigateToRegister());
        findViewById(R.id.admin_main).setOnClickListener(v -> navigateToAdminMainPage());
    }
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
    private void navigateToRegister() {
        Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }

    private void navigateToAdminMainPage(){
        Intent i = new Intent(MainActivity.this,AdminMainPage.class);
        startActivity(i);
    }
}
