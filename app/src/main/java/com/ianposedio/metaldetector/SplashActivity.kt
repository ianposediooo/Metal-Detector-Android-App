package com.ianposedio.metaldetector

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.splash_screen)

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        Handler().postDelayed({
            // Start the main activity
            startActivity(Intent(this, MainActivity::class.java))

            // Close the splash activity
            finish()
        }, SPLASH_SCREEN_TIMEOUT)
    }

    companion object {
        // Splash screen delay time in milliseconds
        private const val SPLASH_SCREEN_TIMEOUT = 2000L
    }
}
