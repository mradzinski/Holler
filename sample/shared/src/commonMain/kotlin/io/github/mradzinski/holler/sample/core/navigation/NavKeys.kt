package io.github.mradzinski.holler.sample.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// The screens of the app, as the keys the back stack holds. They are serializable so the back stack can
// be saved and put back when the system recreates the app.

/** The inbox: the list of emails, and the start of the app. */
@Serializable
internal data object InboxKey : NavKey

/**
 * One email, opened from the inbox.
 *
 * @property emailId the id of the email to show.
 */
@Serializable
internal data class EmailKey(val emailId: Long) : NavKey

/** The settings of the app. */
@Serializable
internal data object SettingsKey : NavKey

/** The playground: one button per kind of message Holler can show. */
@Serializable
internal data object PlaygroundKey : NavKey

/**
 * Tells `kotlinx.serialization` which classes a [NavKey] can be. Outside Android there is no reflection
 * to find them, so every key of the app is listed here: add a line when adding a screen.
 */
internal val navKeySerializers: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(InboxKey::class)
        subclass(EmailKey::class)
        subclass(SettingsKey::class)
        subclass(PlaygroundKey::class)
    }
}
