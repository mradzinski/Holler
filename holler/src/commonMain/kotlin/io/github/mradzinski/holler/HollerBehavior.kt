package io.github.mradzinski.holler

/**
 * How a message tells the user that it has actions.
 *
 * This only matters for a message that has **both** a long text and actions, and whose actions are
 * placed [HollerActionsPosition.Middle]. A message with actions but no long text has nothing to
 * expand, so its actions are always visible whatever value is chosen here; so are actions placed
 * at the top or at the bottom of the card.
 */
public enum class HollerActionsVisibility {

    /**
     * The actions are always on screen. Only the long text collapses: the action row sits
     * under the title while collapsed and under the long text while expanded.
     * This is the default.
     */
    AlwaysVisible,

    /**
     * The actions live inside the expandable part, and the message starts expanded so they are
     * seen immediately. The user can still collapse it. Such a message has no timer: its duration
     * defaults to [HollerDuration.Infinite] and must be that, unless its `stoppableBy` is
     * [HollerStopTrigger.None]. Expanding is what stops a timer, and a message that is already
     * expanded could only stop it by being collapsed and expanded again.
     */
    StartExpanded,

    /**
     * The actions live inside the expandable part and the message starts collapsed. The expand
     * button carries a small accent dot and moves once shortly after the message appears, hinting
     * that there is more inside.
     */
    ChevronHint,
}

/**
 * How a host arranges the messages it shows at the same time.
 */
public enum class HollerStackingStyle {

    /**
     * One under the other, in the order they were shown, each at its full size. Every one of them is
     * visible, so every one of them counts its time down. This is the default.
     */
    Column,

    /**
     * As a pile: the newest message in front, and the earlier ones behind it, each a little smaller and
     * showing past the one in front. Only the front message is visible as a message: it is the only one
     * that counts its time down and that can be pressed, swiped or acted on. A message that gets covered
     * keeps the time it had left and goes on from there when it is in front again.
     */
    Pile,
}

/**
 * Where the actions of a message are placed.
 */
public enum class HollerActionsPosition {

    /**
     * In the top row of the card, after the title and before the card's own buttons. The actions are
     * always visible. It suits one short action.
     */
    Top,

    /**
     * Between the top row and the strip at the bottom: under the long text while it is shown, and
     * under the title otherwise. Whether the actions are always visible or revealed by expanding
     * follows [HollerActionsVisibility]. This is the default.
     */
    Middle,

    /**
     * In the strip at the bottom of the card, after the countdown. The actions are always visible, and
     * the strip stays for as long as the message does: it does not leave with the countdown and the
     * progress bar when the timer is stopped, and a message with [HollerDuration.Infinite] has it too.
     */
    Bottom,
}

/**
 * Where the close button of a message is placed, if it has one. Whatever the value, the message can
 * always be closed by swiping it away sideways.
 */
public enum class HollerClosePosition {

    /** At the end of the top row of the card, after the title. This is the default. */
    Top,

    /**
     * At the end of the strip at the bottom of the card. The strip then stays for as long as the message
     * does: it does not leave with the countdown and the progress bar when the timer is stopped, and a
     * message with [HollerDuration.Infinite] has it too.
     */
    Bottom,

    /** The message has no close button. It is closed by swiping, by one of its actions, by code or by its timer. */
    None,
}

/**
 * What the user can do to stop the timer of a message for good. A stopped timer never runs again: the
 * countdown and the progress bar disappear, and the message stays until it is closed.
 *
 * This is about stopping only. Whatever the value, the message can always be closed, and dragging it
 * (or pressing or hovering over it, unless `pauseOnPressOrHover` is off) still pauses the timer for a moment.
 * It has no effect on a message with [HollerDuration.Infinite], which has no timer.
 */
public enum class HollerStopTrigger {

    /**
     * Expanding the message stops the timer, on the basis that the user has shown interest. A message
     * that starts expanded, with [HollerActionsVisibility.StartExpanded], has no timer at all. A message that
     * cannot be expanded, because it has no long text, has nothing that stops its timer. This is the
     * default.
     */
    Expand,

    /**
     * Nothing stops the timer. Expanding the message leaves it running, and the message closes when its
     * time is up unless the user closes it first.
     */
    None,
}

/** Why a message was closed. Returned by [HollerHandle.awaitDismiss]. */
public enum class HollerDismissReason {

    /** The time on screen ran out. */
    Timeout,

    /** The user pressed the close button. */
    CloseButton,

    /** The user swiped the message away. */
    Swipe,

    /** One of the message's actions called [HollerActionsScope.dismiss]. */
    Action,

    /**
     * The app closed it from code, through [HollerHandle.dismiss], [Holler.dismiss] or
     * [Holler.dismissAll].
     */
    Programmatic,

    /**
     * The app showed another message with the same key, which took this one's place. The card stays
     * on screen with the new content, so nothing was closed for the user.
     */
    Replaced,
}

/**
 * Receiver of the `actions` slot of a message. It lets an action close the message it belongs to.
 */
public interface HollerActionsScope {

    /**
     * Closes the message this action belongs to. A caller waiting in [HollerHandle.awaitDismiss]
     * receives [HollerDismissReason.Action].
     */
    public fun dismiss()
}
