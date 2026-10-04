package ru.fasdev.ratex.main.di.component

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import dagger.Component
import io.ktor.client.HttpClient
import ru.fasdev.ratex.core.domain.boundaries.SharedPrefencesRepo
import ru.fasdev.ratex.main.di.module.ActivityModule
import ru.fasdev.ratex.main.di.module.CiceroneModule
import ru.fasdev.ratex.main.di.scope.ActivityScope
import ru.fasdev.ratex.main.ui.MainActivity

@ActivityScope
@Component(dependencies = [AppComponent::class], modules = [ActivityModule::class, CiceroneModule::class])
interface ActivityComponent {
    // Child dependencies
    fun context(): Context
    fun sharedPrefencesRepo(): SharedPrefencesRepo
    fun appCompatActivity(): AppCompatActivity
    fun httpClient(): HttpClient

    fun inject(mainActivity: MainActivity)
}
