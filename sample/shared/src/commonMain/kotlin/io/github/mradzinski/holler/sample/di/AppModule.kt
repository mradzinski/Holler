package io.github.mradzinski.holler.sample.di

import io.github.mradzinski.holler.Holler
import io.github.mradzinski.holler.sample.app.AppViewModel
import io.github.mradzinski.holler.sample.core.navigation.AppNavigator
import io.github.mradzinski.holler.sample.core.navigation.InboxKey
import io.github.mradzinski.holler.sample.core.navigation.Navigator
import io.github.mradzinski.holler.sample.core.navigation.NavigatorState
import io.github.mradzinski.holler.sample.data.connectivity.Connectivity
import io.github.mradzinski.holler.sample.data.connectivity.MonitoredConnectivity
import io.github.mradzinski.holler.sample.data.email.EmailRepository
import io.github.mradzinski.holler.sample.data.email.InMemoryEmailRepository
import io.github.mradzinski.holler.sample.data.settings.InMemorySettingsRepository
import io.github.mradzinski.holler.sample.data.settings.SettingsRepository
import io.github.mradzinski.holler.sample.ui.email.EmailViewModel
import io.github.mradzinski.holler.sample.ui.inbox.InboxViewModel
import io.github.mradzinski.holler.sample.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Everything the app shares across platforms: navigation, the repositories, the Holler host state and
 * the view models. The one thing missing is the `NetworkMonitor`, which each platform's own module binds.
 */
internal val appModule: Module = module {
    // One back stack for the app, seen as a Navigator by view models and as a NavigatorState by the root.
    single { AppNavigator(startDestination = InboxKey) } binds arrayOf(
        Navigator::class,
        NavigatorState::class,
    )

    // The scope of work that must outlive a screen, such as sending a reply.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<SettingsRepository> { InMemorySettingsRepository() }
    single<Connectivity> { MonitoredConnectivity(monitor = get(), settings = get()) }
    single<EmailRepository> { InMemoryEmailRepository(connectivity = get(), scope = get()) }

    // One host state for the app. View models show messages through it and the app's HollerHost draws
    // them, so a message does not depend on the screen that caused it.
    single { Holler() }

    viewModelOf(::AppViewModel)
    viewModelOf(::InboxViewModel)
    viewModelOf(::SettingsViewModel)

    // The email screen's view model takes the id of its email, which the screen passes when asking for it.
    viewModel { (emailId: Long) ->
        EmailViewModel(
            emailId = emailId,
            emails = get(),
            holler = get(),
            navigator = get(),
        )
    }
}

/**
 * Starts Koin with the shared module and a platform's own. Each platform's `startSample` calls it, once,
 * before the first screen is drawn.
 *
 * @param platformModule the platform's bindings: its `NetworkMonitor` and its `DeviceInput`.
 */
internal fun startSampleKoin(platformModule: Module) {
    startKoin { modules(appModule, platformModule) }
}
