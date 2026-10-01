# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Проект

Ratex — Android-приложение (Kotlin) со списком курсов валют относительно выбранной базовой валюты. 
Пакет `ru.fasdev.ratex`, minSdk 23, compile/targetSdk 34. README в репозитории нет.

## Команды

Сборка через Gradle wrapper (Gradle 8.9, AGP 8.6.1, Kotlin 1.9.24; нужен JDK и Android SDK — путь в `local.properties`, он не коммитится).

```bash
./gradlew assembleDebug                      # сборка debug APK
./gradlew test                               # все unit-тесты (domain, data, app)
./gradlew :domain:test                       # тесты одного модуля
./gradlew :app:testDebugUnitTest --tests "ru.fasdev.ratex.ui.view.bottomSheetSelectCurrency.SelectCurrencyPresenterTest"   # один класс
./gradlew :data:testDebugUnitTest --tests "*CurrencyRateRepoTest.someMethod"                                               # один метод
./gradlew connectedDebugAndroidTest          # инструментальные UI-тесты (нужен эмулятор/устройство)
```

Для `:domain` (чистый JVM-модуль) задача тестов называется `test`, для `:app` и `:data` (Android-модули) — `testDebugUnitTest`.

Стиль кода проверяет ktlint (Gradle-плагин `org.jlleitschuh.gradle.ktlint`, подключён ко всем подпроектам в корневом `build.gradle`, правила — в `.editorconfig`):

```bash
./gradlew ktlintCheck     # проверка стиля всех модулей
./gradlew ktlintFormat    # автоисправление
```

Gradle 8.9 не работает с Java 25 (JBR из Android Studio) — запускайте с JDK 17, например `JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`.

## Архитектура

Три Gradle-модуля, Clean Architecture; зависимости направлены внутрь: `app → data → domain`, `app → domain`.

- **`:domain`** — чистый Kotlin/JVM (без Android). Сущности (`CurrencyDomain`, `RateCurrencyDomain`), интерфейсы `boundaries/` (интеракторы и репозитории) и реализации интеракторов (`*InteractorImpl`). Репозитории здесь только интерфейсы — реализации лежат в `:data`.
- **`:data`** — реализации репозиториев (`*RepoImpl`), `ExchangeRateDataStore` + Retrofit `ExchangeRateApi`, SharedPreferences. Цепочка получения курсов: `CurrencyRateRepoImpl` берёт базовую валюту из `CurrencyBaseRepo` и передаёт её в `CurrencyRateDataStore`. Ответ API парсится вручную через Gson `JsonParser` (API возвращает `ResponseBody`).
- **`:app`** — UI и DI. MVP на Moxy (`MvpPresenter` + `*View` интерфейс + Fragment/BottomSheet), списки на Epoxy (`ui/adapter/epoxy`), навигация через Cicerone (`ui/cicerone`, экраны вроде `ListCurrencyRateScreen`), картинки через Glide.

Асинхронность — везде RxJava 2 (`Single`/`Observable`); презентеры подписываются с `subscribeOn(io)` / `observeOn(mainThread)` и складывают подписки в `CompositeDisposable`.

### DI (Dagger 2, kapt) — иерархия компонентов через `dependencies`

`AppComponent` (`@AppScope`: Context, SharedPreferences, Retrofit) → `ActivityComponent` (`@ActivityScope`, Cicerone) → `FragmentListCurrencyRateComponent` (`@FragmentScope`, модуль `CurrencyModule` собирает Api → репозитории → интеракторы) → `SelectCurrencyBottomSheetComponent` (`@BottomSheetScope`; зависит от фрагментного компонента и переиспользует его интеракторы).

Это не subcomponents: дочерний компонент видит только то, что родитель явно **объявил provision-методом** (`fun retrofit(): Retrofit` и т. п.). Если дочернему компоненту нужна новая зависимость из родителя — добавляйте provision-метод в каждый промежуточный компонент. `AppComponent` хранится в `RatexApp.DI.appComponent`, `ActivityComponent` — в `MainActivity.activitySubComponent`; фрагменты строят свои компоненты лениво через `Dagger*Component.builder().activityComponent(...)`.

### Особенности сборки

- Версии зависимостей и SDK — в `ext.ver` / `ext.androidVer` корневого `build.gradle` (не version catalog).
- `android.nonFinalResIds=false` в `gradle.properties` нужен для Epoxy `@EpoxyModelClass(layout = R.layout...)` — не убирать.
- Базовый URL API (`https://api.exchangeratesapi.io`) захардкожен в `RetrofitModule` (есть TODO).

## Тесты

- Unit-тесты презентеров/репозиториев/интеракторов на JUnit4 + Mockito + AssertJ; репозитории `:data` тестируются с MockWebServer и Robolectric.
- Для презентеров с Rx используйте правило `ru.fasdev.ratex.rule.InitScheduler` (`app/src/test`) — подменяет io/main-планировщики на trampoline.
- `app/src/androidTest` — Espresso-тесты `MainActivity`.
