package ru.fasdev.ratex.app.di.module.activity

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import dagger.Module
import dagger.Provides
import ru.fasdev.ratex.app.di.scope.ActivityScope
import ru.fasdev.ratex.domain.currencyRate.entity.CurrencyDomain
import javax.inject.Named

@Module
class ActivityModule (val fragmentActivity: FragmentActivity)
{
    @Provides
    @ActivityScope
    fun provideFragmentActivity(): FragmentActivity = fragmentActivity

    @Provides
    @ActivityScope
    fun provideAppCompatActivity(fragmentActivity: FragmentActivity): AppCompatActivity = fragmentActivity as AppCompatActivity
}