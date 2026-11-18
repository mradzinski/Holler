package io.github.mradzinski.holler.sample.ui.playground

/**
 * The messages the playground can show, one per button, as plain data. A button shows one with
 * `holler.show(DemoMessage.…)`, and [DemoMessageContent] gives each its look.
 */
internal sealed interface DemoMessage {

    /** A message that uses every part at once. */
    data object Everything : DemoMessage

    /** The smallest message with a look of its own: an icon and a title. */
    data object TitleOnly : DemoMessage

    /** A message whose two actions stay visible while its long text is collapsed. */
    data object ActionsAlwaysVisible : DemoMessage

    /** A message that opens expanded, so its action is seen at once. */
    data object StartsExpanded : DemoMessage

    /** A message that never closes by itself. */
    data object Infinite : DemoMessage

    /** A message with a countdown but no progress bar, fading in and out. */
    data object WithoutProgressBar : DemoMessage

    /** The messages that show where the actions and the close button of a message can be placed. */
    sealed interface Position : DemoMessage {

        /** A message whose one action sits in the top row, next to the title. */
        data object ActionAtTop : Position

        /** A message whose actions sit in the strip at the bottom, and stay there when its timer stops. */
        data object ActionsAtBottom : Position

        /** A message whose close button sits in the strip at the bottom. */
        data object CloseAtBottom : Position

        /** A message without a close button, which is closed by swiping it away or by its timer. */
        data object WithoutCloseButton : Position
    }

    /** A message with colors and a style of its own, whatever look the host gives to the others. */
    data object CustomLook : DemoMessage

    /** The first message of the replace demo: a pretend upload is running. */
    data object Uploading : DemoMessage

    /** The second message of the replace demo: the upload ended. It takes the place of [Uploading]. */
    data object Uploaded : DemoMessage
}
