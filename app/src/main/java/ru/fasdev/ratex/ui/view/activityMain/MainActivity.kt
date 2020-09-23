package ru.fasdev.ratex.ui.view.activityMain

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import moxy.MvpAppCompatActivity
import ru.fasdev.ratex.R
import ru.fasdev.ratex.app.RatexApp
import ru.fasdev.ratex.app.di.component.DaggerActivityComponent
import ru.fasdev.ratex.app.di.module.activity.ActivityModule
import ru.fasdev.ratex.app.di.module.activity.CiceroneModule
import ru.fasdev.ratex.ui.view.fragmentListCurrencyRate.ListCurrencyRateScreen
import ru.fasdev.ratex.ui.provider.FragmentProvider
import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import ru.terrakok.cicerone.android.support.SupportAppNavigator
import javax.inject.Inject


class MainActivity : MvpAppCompatActivity(), FragmentProvider
{
    @Inject
    lateinit var cicerone: Cicerone<Router>

    @Inject
    lateinit var navigationHolder: NavigatorHolder

    @Inject
    lateinit var routerCicerone: Router

    @Inject
    lateinit var navigator: SupportAppNavigator

    val activitySubComponent by lazy {
        return@lazy DaggerActivityComponent
            .builder()
            .appComponent(RatexApp.DI.appComponent)
            .activityModule(ActivityModule(this))
            .ciceroneModule(CiceroneModule(R.id.main_container))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        activitySubComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSystemUiVisibility()

        if (getCurrentFragment() == null)
            routerCicerone.newRootScreen(ListCurrencyRateScreen())
    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        navigationHolder.setNavigator(navigator)
    }

    override fun onPause() {
        super.onPause()

        navigationHolder.removeNavigator()
    }

    private fun setSystemUiVisibility()
    {
        window.decorView.setSystemUiVisibility(
            window.decorView.systemUiVisibility
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun getCurrentFragment(): Fragment?
    {
        return supportFragmentManager.findFragmentById(R.id.main_container)
    }
}