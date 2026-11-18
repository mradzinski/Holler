package io.github.mradzinski.holler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeLessThan
import kotlin.test.Test

/** Width of the card under test, in dp. */
private const val SHADOW_TEST_CARD_WIDTH: Int = 300

/** How far the test drags the card: past the touch slop, well short of dismissing it. */
private const val SHADOW_TEST_DRAG: Float = 40f

/** How far under the close button the test reads a pixel: below the card, inside its shadow. */
private const val SHADOW_TEST_BELOW_CLOSE: Float = 24f

/** The brightest the pixel under the card may be for a shadow to count as drawn; white is 1. */
private const val SHADOW_TEST_MAX_BRIGHTNESS: Float = 0.98f

/**
 * Tests that read the pixels of a drawn message. They run where a test can get the picture of what
 * was drawn: on desktop, iOS and the web, and not on Android, where the screenshot tests check the look.
 */
@OptIn(ExperimentalTestApi::class)
class HollerShadowUiTest : UiTest() {

    @Test
    fun `the shadow stays while the card is being swiped`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
            ) {
                Box(Modifier.width(SHADOW_TEST_CARD_WIDTH.dp)) {
                    TestCard(
                        title = "Shadow",
                        onDismiss = {},
                        duration = HollerDuration.Infinite,
                    )
                }
            }
        }

        advanceTimeByFrame()
        onNodeWithText("Shadow").performTouchInput {
            down(center)
            moveBy(Offset(SHADOW_TEST_DRAG, 0f))
        }

        advanceTimeByFrame()

        // A point just under the card, where only the shadow can darken the white background.
        val close = onNodeWithContentDescription(CLOSE_DESCRIPTION).fetchSemanticsNode().boundsInRoot
        val pixels = onRoot()
            .captureToImage()
            .toPixelMap()
        val sampleX = (close.left - SHADOW_TEST_CARD_WIDTH / 2).toInt()
        val sampleY = (close.bottom + SHADOW_TEST_BELOW_CLOSE).toInt()
        val underTheCard = pixels[sampleX, sampleY]

        withClue("expected a shadow under the card, found $underTheCard") {
            underTheCard.red shouldBeLessThan SHADOW_TEST_MAX_BRIGHTNESS
        }

        onNodeWithText("Shadow").performTouchInput { up() }
        advanceTimeBy(1_000)
    }
}
