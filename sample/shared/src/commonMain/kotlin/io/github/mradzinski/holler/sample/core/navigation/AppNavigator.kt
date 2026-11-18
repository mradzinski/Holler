package io.github.mradzinski.holler.sample.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The app's one back stack. It lives as long as the app, and is both the [Navigator] view models call
 * and the [NavigatorState] the root composable saves.
 *
 * @param startDestination the screen the app opens on, which is never closed.
 */
internal class AppNavigator(private val startDestination: NavKey) :
    Navigator,
    NavigatorState {

    /** Reads and writes the back stack as JSON, knowing every key of the app. */
    private val json: Json = Json { serializersModule = navKeySerializers }

    /** Serializes a whole back stack, whatever keys it holds. */
    private val stackSerializer: KSerializer<List<NavKey>> = ListSerializer(
        PolymorphicSerializer(NavKey::class),
    )

    /** The back stack itself. Compose observes every change to it. */
    private val stack: SnapshotStateList<NavKey> = mutableStateListOf(startDestination)

    /** The screens currently open, the visible one last. */
    override val backStack: List<NavKey> get() = stack

    /**
     * Opens a screen on top, moving it there if it is already open.
     *
     * @param key the screen to open.
     */
    override fun navigate(key: NavKey) {
        stack.remove(key)
        stack.add(key)
    }

    /**
     * Closes the visible screen, unless it is the start screen.
     *
     * @return `false` when only the start screen is open.
     */
    override fun goBack(): Boolean {
        if (stack.size <= 1) return false

        stack.removeAt(stack.lastIndex)

        return true
    }

    /**
     * Closes every screen above the last one matching [predicate].
     *
     * @param predicate tells which screen to go back to.
     * @return `false` when no open screen matches.
     */
    override fun popUpTo(predicate: (NavKey) -> Boolean): Boolean {
        val index = stack.indexOfLast(predicate)

        if (index < 0) return false

        stack.subList(index + 1, stack.size).clear()

        return true
    }

    /**
     * Writes the back stack as JSON.
     *
     * @return the JSON text.
     */
    override fun save(): String = json.encodeToString(stackSerializer, stack.toList())

    /**
     * Replaces the back stack with one written by [save]. Text that cannot be read is ignored.
     *
     * @param saved the text [save] returned.
     */
    override fun restore(saved: String) {
        val keys = runCatching { json.decodeFromString(stackSerializer, saved) }.getOrNull()

        if (keys.isNullOrEmpty()) return

        stack.clear()
        stack.addAll(keys)
    }
}
