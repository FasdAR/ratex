package ru.fasdev.ratex.main.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import ru.fasdev.ratex.R

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Splash)
        super.onCreate(savedInstanceState)

        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}
