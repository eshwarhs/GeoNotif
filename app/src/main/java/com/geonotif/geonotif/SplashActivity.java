package com.geonotif.geonotif;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIMEOUT = 5000; // Splash screen duration in milliseconds
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mAuth = FirebaseAuth.getInstance();

        new Handler().postDelayed(() -> {
            // This method will be executed once the timer is over
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(SplashActivity.this, HomePage.class);
                this.startActivity(intent);
            } else {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                this.startActivity(intent);
            }

        }, SPLASH_TIMEOUT);
    }
}

