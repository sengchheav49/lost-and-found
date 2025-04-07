package com.example.lostandfound.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lostandfound.MainActivity;
import com.example.lostandfound.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds
    private ImageView ivLogo;
    private TextView tvAppName, tvTagline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        ivLogo = findViewById(R.id.iv_logo);
        tvAppName = findViewById(R.id.tv_app_name);
        tvTagline = findViewById(R.id.tv_tagline);

        // Set initial alpha to 0 (invisible)
        ivLogo.setAlpha(0f);
        tvAppName.setAlpha(0f);
        tvTagline.setAlpha(0f);

        // Start animations
        startAnimations();

        // Navigate to main activity after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMainScreen, SPLASH_DURATION);
    }

    private void startAnimations() {
        // Logo animation
        ivLogo.animate()
                .alpha(1f)
                .translationYBy(-50f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Start app name animation after logo animation
                        animateAppName();
                    }
                })
                .start();
    }

    private void animateAppName() {
        tvAppName.animate()
                .alpha(1f)
                .translationYBy(-30f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Start tagline animation after app name animation
                        animateTagline();
                    }
                })
                .start();
    }

    private void animateTagline() {
        tvTagline.animate()
                .alpha(1f)
                .translationYBy(-20f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void navigateToMainScreen() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        Intent intent;
        if (user == null) {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }

        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        finish();
    }

} 