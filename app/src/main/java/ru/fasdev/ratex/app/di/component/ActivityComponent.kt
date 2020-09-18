package ru.fasdev.ratex.app.di.component

import dagger.Component
import ru.fasdev.ratex.app.di.module.activity.ActivityModule
import ru.fasdev.ratex.app.di.module.activity.CiceroneModule
import ru.fasdev.ratex.app.di.scope.ActivityScope

@ActivityScope
@Component(modules = [CiceroneModule::class, ActivityModule::class])
interface ActivityComponent
{

}