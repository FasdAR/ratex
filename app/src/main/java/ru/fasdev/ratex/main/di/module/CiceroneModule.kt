package ru.fasdev.ratex.main.di.module

import androidx.fragment.app.FragmentActivity
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.androidx.AppNavigator
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.main.di.scope.ActivityScope
import ru.fasdev.ratex.main.navigation.MainNavigator

@Module
class CiceroneModule(val idContainer: Int) {
    @Provides
    @ActivityScope
    fun provideCicerone(): Cicerone<Router> = Cicerone.create()

    @Provides
    @ActivityScope
    fun provideCiceroneNavigationHelper(cicerone: Cicerone<Router>): NavigatorHolder = cicerone.getNavigatorHolder()

    @Provides
    @ActivityScope
    fun provideCiceroneRouter(cicerone: Cicerone<Router>): Router = cicerone.router

    // Provide Default MainNavigator
    @Provides
    @ActivityScope
    fun provideNavigator(fragmentActivity: FragmentActivity): AppNavigator = MainNavigator(fragmentActivity, idContainer)
}
