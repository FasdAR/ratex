package ru.fasdev.ratex.ui.view.activitySplash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.fasdev.ratex.R
import ru.fasdev.ratex.ui.view.activityMain.MainActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Splash)
        super.onCreate(savedInstanceState)

        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}
