package ru.fasdev.ratex.ui.view.activityMain

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import ru.fasdev.ratex.R

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSystemUiVisibility()
    }

    private fun setSystemUiVisibility()
    {
        window.decorView.setSystemUiVisibility(
            window.decorView.systemUiVisibility
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }
}