package io.github.mradzinski.holler.sample.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * App-wide navigation. It is a Koin singleton, so view models navigate by calling it and no screen
 * passes navigation callbacks around.
 */
internal interface Navigator {

    /** The screens currently open, the visible one last. It is observable: the app's `NavDisplay` draws it. */
    val backStack: List<NavKey>

    /**
     * Opens a screen on top of the others. A key that is already open is moved to the top, not added twice.
     *
     * @param key the screen to open.
     */
    fun navigate(key: NavKey)

    /**
     * Closes the visible screen.
     *
     * @return `false`, leaving the back stack as it is, when only the start screen is open.
     */
    fun goBack(): Boolean

    /**
     * Closes every screen above the last one matching [predicate].
     *
     * @param predicate tells which screen to go back to.
     * @return `false`, leaving the back stack as it is, when no open screen matches.
     */
    fun popUpTo(predicate: (NavKey) -> Boolean): Boolean
}

/**
 * Saves and restores the back stack as text, so it survives the system recreating the app. Only the
 * app's root composable uses it.
 */
internal interface NavigatorState {

    /**
     * Writes the back stack as text.
     *
     * @return the text to hand back to [restore] later.
     */
    fun save(): String

    /**
     * Puts back a stack written by [save]. Text that cannot be read is ignored.
     *
     * @param saved the text [save] returned.
     */
    fun restore(saved: String)
}
