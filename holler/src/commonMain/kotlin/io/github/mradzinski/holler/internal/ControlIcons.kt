package io.github.mradzinski.holler.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// The two symbols of the card's own buttons, as vector images for Material's `Icon`. They are part of
// how a message works, not decoration, so the library carries them; every other icon is the app's.
// Each outline is written in the path syntax of SVG and of Android vector drawables (`M` move, `L` line,
// `H` / `V` horizontal and vertical line), inside a 14 x 14 box: a 12dp symbol with a
// 1dp margin, so the round ends of its lines are not cut off. The color written here is replaced by the
// tint of the `Icon` that shows them.
//
// They are outlines in code and not vector files in the library's resources because resources are
// fetched on the web, so an icon would show up a moment after its button, and because the library would
// have to ship resource files to every app for two small symbols.

/** Width and height of the control icons, in dp and in drawing units. */
private const val ICON_BOX: Float = 14f

/** Line thickness of the chevron and the cross, in drawing units. */
private const val LINE_THICKNESS: Float = 1.5f

/** Outline of the chevron icon: two lines meeting at the bottom, like a "v". */
private const val CHEVRON_DOWN_OUTLINE: String = "M1,4.36 L7,10.36 L13,4.36"

/** Outline of the close icon: the two diagonals of the box, like an "x". */
private const val CLOSE_OUTLINE: String = "M1,1 L13,13 M13,1 L1,13"

/** A downward chevron ("v"), for the expand button. Rotate it to make it point up. */
internal val ChevronDownVector: ImageVector by lazy { strokedIcon(name = "ChevronDown", outline = CHEVRON_DOWN_OUTLINE) }

/** A cross ("x"), for the close button. */
internal val CloseVector: ImageVector by lazy { strokedIcon(name = "Close", outline = CLOSE_OUTLINE) }

/**
 * Builds a control icon drawn as lines with round ends and corners.
 *
 * @param name the name of the icon, for tooling.
 * @param outline the lines to draw, as a path string inside the 14 x 14 box.
 * @return the icon.
 */
private fun strokedIcon(name: String, outline: String): ImageVector = controlIcon(name)
    .addPath(
        pathData = addPathNodes(outline),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = LINE_THICKNESS,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
    .build()

/**
 * Starts a control icon with the shared size.
 *
 * @param name the name of the icon, for tooling.
 * @return a builder to add the icon's outline to.
 */
private fun controlIcon(name: String): ImageVector.Builder = ImageVector.Builder(
    name = name,
    defaultWidth = ICON_BOX.dp,
    defaultHeight = ICON_BOX.dp,
    viewportWidth = ICON_BOX,
    viewportHeight = ICON_BOX,
)
