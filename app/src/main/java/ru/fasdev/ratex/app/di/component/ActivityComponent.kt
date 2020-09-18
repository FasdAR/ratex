package ru.fasdev.ratex.app.di.component

import android.content.Context
import dagger.Component
import ru.fasdev.ratex.app.di.module.activity.ActivityModule
import ru.fasdev.ratex.app.di.module.activity.CiceroneModule
import ru.fasdev.ratex.app.di.scope.ActivityScope
import ru.fasdev.ratex.domain.main.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.ui.view.activityMain.MainActivity

@ActivityScope
@Component(dependencies = [AppComponent::class], modules = [ActivityModule::class, CiceroneModule::class])
interface ActivityComponent
{
    //Child dependencies
    fun context(): Context
    fun sharedPrefencesRepo(): SharedPrefencesRepo

    fun inject(mainActivity: MainActivity)
}