package io.github.mradzinski.holler

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

/**
 * Where and how a message is being shown, given to a [HollerAnimation] so it can adapt, for
 * example by sliding in from the nearest screen edge.
 *
 * @param alignment where the host places its messages on screen.
 * @param layoutDirection the reading direction, so horizontal movement can follow it.
 */
@Immutable
public data class HollerAnimationContext(val alignment: Alignment, val layoutDirection: LayoutDirection)

/**
 * How a message appears and disappears.
 *
 * Use one of the ready-made animations ([Default], [Fade], [Slide], [Scale], [None]), combine
 * any Compose transitions with the [HollerAnimation] factory function, or subclass this to
 * build one that reacts to the [HollerAnimationContext].
 *
 * Each animation has two layers: the card itself ([messageEnter] / [messageExit]) and the content inside the
 * card ([messageContentEnter] / [messageContentExit]), so the content can, for instance, fade in while the
 * card grows.
 */
public abstract class HollerAnimation {

    /**
     * The transition that brings the card on screen.
     *
     * @param context where and how the message is shown.
     * @return the transition applied to the whole card.
     */
    public abstract fun messageEnter(context: HollerAnimationContext): EnterTransition

    /**
     * The transition that takes the card off-screen.
     *
     * @param context where and how the message is shown.
     * @return the transition applied to the whole card.
     */
    public abstract fun messageExit(context: HollerAnimationContext): ExitTransition

    /**
     * An extra transition for what is inside the card while it enters. None by default.
     *
     * @param context where and how the message is shown.
     * @return the transition applied to the card's content only.
     */
    public open fun messageContentEnter(context: HollerAnimationContext): EnterTransition = EnterTransition.None

    /**
     * An extra transition for what is inside the card while it exits. None by default.
     *
     * @param context where and how the message is shown.
     * @return the transition applied to the card's content only.
     */
    public open fun messageContentExit(context: HollerAnimationContext): ExitTransition = ExitTransition.None

    /** The ready-made animations. */
    public companion object {

        /**
         * The standard animation: the card quickly grows from its center while its content fades
         * in, and leaves by sliding out towards the end edge (right in left-to-right layouts).
         */
        public val Default: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition =
                scaleIn(tween(ENTER_MILLIS, easing = LinearOutSlowInEasing), initialScale = 0.2f) +
                    fadeIn(tween(ENTER_MILLIS))

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition {
                val direction = if (context.layoutDirection == LayoutDirection.Ltr) 1 else -1

                return slideOutHorizontally(tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) { width ->
                    direction * (width * 3) / 2
                } + fadeOut(tween(EXIT_MILLIS, easing = FastOutLinearInEasing))
            }

            /**
             * The content transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageContentEnter(context: HollerAnimationContext): EnterTransition =
                fadeIn(tween(ENTER_MILLIS, delayMillis = ENTER_MILLIS / 3))
        }

        /** The card fades in and out in place. */
        public val Fade: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition = fadeIn(tween(ENTER_MILLIS))

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition = fadeOut(tween(EXIT_MILLIS))
        }

        /**
         * The card slides in from, and back out to, the nearest horizontal screen edge: the top
         * when the host is top-aligned, the bottom otherwise.
         */
        public val Slide: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition {
                val direction = verticalDirection(context.alignment)

                val slide = slideInVertically(tween(EXIT_MILLIS, easing = LinearOutSlowInEasing)) { height ->
                    direction * height
                }

                return slide + fadeIn(tween(EXIT_MILLIS))
            }

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition {
                val direction = verticalDirection(context.alignment)

                val slide = slideOutVertically(tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) { height ->
                    direction * height
                }

                return slide + fadeOut(tween(EXIT_MILLIS))
            }
        }

        /** The card grows in from its center and shrinks back out, fading both ways. */
        public val Scale: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition =
                scaleIn(tween(ENTER_MILLIS), initialScale = 0.8f) + fadeIn(tween(ENTER_MILLIS))

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition =
                scaleOut(tween(ENTER_MILLIS), targetScale = 0.8f) + fadeOut(tween(ENTER_MILLIS))
        }

        /**
         * The card lifts a short way into place while fading in, and sinks back the same short way
         * while fading out. It moves away from the nearest horizontal screen edge: upwards when the
         * host is at the bottom, downwards when it is at the top. A quieter relative of [Slide], which
         * travels the whole height of the card.
         */
        public val Rise: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition {
                val direction = verticalDirection(context.alignment)

                val lift = slideInVertically(tween(EXIT_MILLIS, easing = LinearOutSlowInEasing)) { height ->
                    direction * height / RISE_DISTANCE_DIVISOR
                }

                return lift + fadeIn(tween(EXIT_MILLIS))
            }

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition {
                val direction = verticalDirection(context.alignment)

                val sink = slideOutVertically(tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) { height ->
                    direction * height / RISE_DISTANCE_DIVISOR
                }

                return sink + fadeOut(tween(EXIT_MILLIS))
            }
        }

        /**
         * The card grows in from its center a little past its full size and settles back, like
         * something landing. It leaves by shrinking slightly while fading out. A livelier relative of
         * [Scale], which stops at full size.
         */
        public val Pop: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition {
                val grow = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    initialScale = POP_START_SCALE,
                )

                return grow + fadeIn(tween(ENTER_MILLIS))
            }

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition =
                scaleOut(tween(ENTER_MILLIS), targetScale = POP_END_SCALE) + fadeOut(tween(ENTER_MILLIS))
        }

        /**
         * The card slides in from the nearest horizontal screen edge, goes a little past its place
         * and bounces back once. It leaves by sliding straight back out. A livelier relative of
         * [Slide], which arrives and stops.
         */
        public val Drop: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition {
                val direction = verticalDirection(context.alignment)

                val land = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                ) { height ->
                    direction * height
                }

                return land + fadeIn(tween(ENTER_MILLIS))
            }

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition {
                val direction = verticalDirection(context.alignment)

                val leave = slideOutVertically(tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) { height ->
                    direction * height
                }

                return leave + fadeOut(tween(EXIT_MILLIS))
            }
        }

        /** No animation: the card appears and disappears instantly. */
        public val None: HollerAnimation = object : HollerAnimation() {
            /**
             * The card transition for entering, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageEnter(context: HollerAnimationContext): EnterTransition = EnterTransition.None

            /**
             * The card transition for exiting, as described on this animation.
             *
             * @param context where and how the message is shown.
             * @return the transition to use.
             */
            override fun messageExit(context: HollerAnimationContext): ExitTransition = ExitTransition.None
        }

        /** Length of the quick enter animations, in milliseconds. */
        private const val ENTER_MILLIS: Int = 150

        /** Length of the exit and slide animations, in milliseconds. */
        private const val EXIT_MILLIS: Int = 250

        /** [Rise] moves the card by its own height divided by this: a third of it. */
        private const val RISE_DISTANCE_DIVISOR: Int = 3

        /** The share of its full size [Pop] starts from. */
        private const val POP_START_SCALE: Float = 0.8f

        /** The share of its full size [Pop] shrinks to while leaving. */
        private const val POP_END_SCALE: Float = 0.9f

        /**
         * Which way a vertical slide should travel for a host placed at [alignment].
         *
         * @param alignment where the host places its messages.
         * @return `-1` to move towards the top edge, `1` to move towards the bottom edge.
         */
        private fun verticalDirection(alignment: Alignment): Int {
            val towardTop = alignment is BiasAlignment && alignment.verticalBias < 0f

            return if (towardTop) -1 else 1
        }
    }
}

/**
 * Builds a [HollerAnimation] from plain Compose transitions, for when the animation does not
 * need to know where the message is shown.
 *
 * Example: `HollerAnimation(messageEnter = fadeIn() + expandVertically(), messageExit = fadeOut())`.
 *
 * @param messageEnter the transition that brings the card on screen.
 * @param messageExit the transition that takes the card off-screen.
 * @param messageContentEnter an extra transition for the card's content while entering. None by default.
 * @param messageContentExit an extra transition for the card's content while exiting. None by default.
 * @return an animation that always uses the given transitions.
 */
public fun HollerAnimation(
    messageEnter: EnterTransition,
    messageExit: ExitTransition,
    messageContentEnter: EnterTransition = EnterTransition.None,
    messageContentExit: ExitTransition = ExitTransition.None,
): HollerAnimation = object : HollerAnimation() {
    /**
     * The card transition for entering, as described on this animation.
     *
     * @param context where and how the message is shown.
     * @return the transition to use.
     */
    override fun messageEnter(context: HollerAnimationContext): EnterTransition = messageEnter

    /**
     * The card transition for exiting, as described on this animation.
     *
     * @param context where and how the message is shown.
     * @return the transition to use.
     */
    override fun messageExit(context: HollerAnimationContext): ExitTransition = messageExit

    /**
     * The content transition for entering, as described on this animation.
     *
     * @param context where and how the message is shown.
     * @return the transition to use.
     */
    override fun messageContentEnter(context: HollerAnimationContext): EnterTransition = messageContentEnter

    /**
     * The content transition for exiting, as described on this animation.
     *
     * @param context where and how the message is shown.
     * @return the transition to use.
     */
    override fun messageContentExit(context: HollerAnimationContext): ExitTransition = messageContentExit
}
