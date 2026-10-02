package ru.fasdev.ratex.main.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.androidx.AppNavigator
import javax.inject.Inject
import moxy.MvpAppCompatActivity
import ru.fasdev.ratex.R
import ru.fasdev.ratex.currency.ui.fragmentListCurrencyRate.ListCurrencyRateScreen
import ru.fasdev.ratex.main.RatexApp
import ru.fasdev.ratex.main.di.component.DaggerActivityComponent
import ru.fasdev.ratex.main.di.module.ActivityModule
import ru.fasdev.ratex.main.di.module.CiceroneModule
import ru.fasdev.ratex.main.navigation.FragmentProvider

class MainActivity :
    MvpAppCompatActivity(),
    FragmentProvider {
    @Inject
    lateinit var cicerone: Cicerone<Router>

    @Inject
    lateinit var navigationHolder: NavigatorHolder

    @Inject
    lateinit var routerCicerone: Router

    @Inject
    lateinit var navigator: AppNavigator

    val activitySubComponent by lazy {
        return@lazy DaggerActivityComponent
            .builder()
            .appComponent(RatexApp.DI.appComponent)
            .activityModule(ActivityModule(this))
            .ciceroneModule(CiceroneModule(R.id.main_container))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activitySubComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSystemUiVisibility()

        if (getCurrentFragment() == null) {
            routerCicerone.newRootScreen(ListCurrencyRateScreen())
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        navigationHolder.setNavigator(navigator)
    }

    override fun onPause() {
        super.onPause()

        navigationHolder.removeNavigator()
    }

    private fun setSystemUiVisibility() {
        window.decorView.setSystemUiVisibility(
            window.decorView.systemUiVisibility
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun getCurrentFragment(): Fragment? = supportFragmentManager.findFragmentById(R.id.main_container)
}
