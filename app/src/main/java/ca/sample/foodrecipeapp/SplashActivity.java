package ca.sample.foodrecipeapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Make sure this matches your layout file name

        // Delay to show the splash screen for a few seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Redirect to LoginActivity after delay
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish(); // Close SplashActivity
            }
        }, 1000); // Delay time in milliseconds (1 seconds)
    }
}
