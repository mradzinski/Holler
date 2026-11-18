package io.github.mradzinski.holler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/** Width of the area a screenshot shows, in dp: a message at its full width with room for its shadow. */
private const val STAGE_WIDTH: Int = 440

/** Height of the area a screenshot shows, in dp: room for an expanded message or a pile, and their shadows. */
private const val STAGE_HEIGHT: Int = 320

/** The tag of the area a screenshot shows. */
private const val STAGE_TAG: String = "stage"

/** How long a message is given to enter before its picture is taken, in milliseconds. */
private const val ENTER_MILLIS: Long = 1_000L

/** How far the swipe screenshot drags the card: past the touch slop, well short of dismissing it. */
private const val SWIPE_DRAG: Float = 60f

/** The long text of the messages in the screenshots. */
private const val DETAILS: String = "All 412 files were copied to your cloud storage. You can keep working while the rest uploads."

/** The color of the icon in the screenshots. The library has no icons of its own, so a dot stands in for one. */
private val ShotGreen: Color = Color(0xFF2E7D32)

/** The messages the screenshots show, each drawn its own way by [ShotMessage]. */
private enum class Shot {
    /** A message with an icon, long text, two actions in the strip at the bottom and a countdown. */
    WithEverything,

    /** A message with colors and a style of its own. */
    CustomLook,
}

/**
 * Draws one of the messages of the screenshots.
 *
 * @param shot the message to draw.
 */
@Composable
private fun HollerHostScope.ShotMessage(shot: Shot) {
    when (shot) {
        Shot.WithEverything -> HollerMessage(
            title = "Invitation received",
            details = DETAILS,
            icon = {
                HollerIconRing(tint = ShotGreen) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ShotGreen, CircleShape),
                    )
                }
            },
            actions = {
                TextButton(onClick = { dismiss() }) { Text("Accept") }
                TextButton(onClick = { dismiss() }) { Text("Decline") }
            },
            countdown = { Text("Closes in ${it.inWholeSeconds} s") },
            actionsPosition = HollerActionsPosition.Bottom,
            duration = HollerDuration.Custom(10.seconds),
        )

        Shot.CustomLook -> HollerMessage(
            title = "Focus mode is on",
            details = DETAILS,
            countdown = { Text("Closes in ${it.inWholeSeconds} s") },
            duration = HollerDuration.Custom(10.seconds),
            colors = HollerDefaults.colors().copy(
                container = Color(0xFF0F172A),
                title = Color.White,
                controls = Color(0xFF94A3B8),
                border = Color.Transparent,
                footerContainer = Color(0xFF1E293B),
                countdown = Color(0xFF94A3B8),
                progress = Color(0xFF34D399),
                progressTrack = Color(0xFF1E293B),
            ),
            style = HollerDefaults.style(shape = RoundedCornerShape(28.dp)),
        )
    }
}

/**
 * Pictures of messages as Android draws them, compared with the pictures kept in
 * `src/androidHostTest/screenshots`. They check the look on Android, which the other tests cannot: a
 * difference in a picture fails the test. After a change that is meant to alter the look, record the
 * pictures again with `./gradlew :holler:recordRoborazziAndroidHostTest` and review them.
 */
@OptIn(ExperimentalTestApi::class)
class HollerScreenshotTest : UiTest() {

    @Test
    fun `a collapsed text message`() = runComposeUiTest {
        stage { show(title = "Changes saved", details = DETAILS) }

        shoot("collapsed")
    }

    @Test
    fun `a collapsed text message in a dark theme`() = runComposeUiTest {
        stage(dark = true) { show(title = "Changes saved", details = DETAILS) }

        shoot("collapsed-dark")
    }

    @Test
    fun `an expanded text message`() = runComposeUiTest {
        stage { show(title = "Changes saved", details = DETAILS) }

        onNodeWithContentDescription(EXPAND_DESCRIPTION).performTouchInput { click() }
        advanceTimeBy(ENTER_MILLIS)

        shoot("expanded")
    }

    @Test
    fun `a message with an icon and actions and a countdown at the bottom`() = runComposeUiTest {
        stage { show(Shot.WithEverything) }

        shoot("with-everything")
    }

    @Test
    fun `a message with its own colors and style`() = runComposeUiTest {
        stage { show(Shot.CustomLook) }

        shoot("custom-look")
    }

    @Test
    fun `a pile of three messages`() = runComposeUiTest {
        stage(stackingStyle = HollerStackingStyle.Pile) {
            show(title = "First")
            show(title = "Second")
            show(title = "Third")
        }

        shoot("pile")
    }

    @Test
    fun `a message keeps its shadow while it is being swiped`() = runComposeUiTest {
        stage { show(title = "Changes saved", duration = HollerDuration.Infinite) }

        onNodeWithText("Changes saved").performTouchInput {
            down(center)
            moveBy(Offset(SWIPE_DRAG, 0f))
        }

        advanceTimeByFrame()

        shoot("swiping")

        onNodeWithText("Changes saved").performTouchInput { up() }
    }

    /**
     * Puts a host on a plain surface of a fixed size, shows messages through it and lets them enter.
     *
     * @param dark whether the app's theme is Material's dark one.
     * @param stackingStyle how the host places several messages.
     * @param show shows the messages of the picture.
     */
    private fun ComposeUiTest.stage(
        dark: Boolean = false,
        stackingStyle: HollerStackingStyle = HollerStackingStyle.Column,
        show: Holler.() -> Unit,
    ) {
        val holler = Holler()
        val colorScheme = if (dark) darkColorScheme() else lightColorScheme()

        mainClock.autoAdvance = false
        setContent {
            MaterialTheme(colorScheme = colorScheme) {
                Box(
                    modifier = Modifier
                        .testTag(STAGE_TAG)
                        .size(width = STAGE_WIDTH.dp, height = STAGE_HEIGHT.dp)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    HollerHost(
                        holler = holler,
                        closeContentDescription = CLOSE_DESCRIPTION,
                        expandContentDescription = EXPAND_DESCRIPTION,
                        collapseContentDescription = COLLAPSE_DESCRIPTION,
                        alignment = Alignment.Center,
                        stackingStyle = stackingStyle,
                    ) { value ->
                        if (value is Shot) ShotMessage(value)
                    }
                }
            }
        }

        holler.show()
        advanceTimeBy(ENTER_MILLIS)
    }

    /**
     * Takes the picture of the stage and compares it with the one kept under [name], or records it,
     * depending on the Gradle task that runs the test.
     *
     * @param name the file name of the picture, without its extension.
     */
    private fun ComposeUiTest.shoot(name: String) {
        onNodeWithTag(STAGE_TAG).captureRoboImage("$name.png")
    }
}
