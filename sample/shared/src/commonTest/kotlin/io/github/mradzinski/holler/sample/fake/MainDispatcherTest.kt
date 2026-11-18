package io.github.mradzinski.holler.sample.fake

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Base of the view model tests. A view model runs on the main dispatcher, which a unit test does not
 * have, so each test gets a test dispatcher in its place. `runTest` then shares its virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainDispatcherTest {

    /** The dispatcher standing in for the main one. It runs coroutines at once, with no need to advance time. */
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()

    /** Installs the test dispatcher as the main one. */
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    /** Puts the real main dispatcher back. */
    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }
}
