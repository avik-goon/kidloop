package com.kidloop.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

const val TOUCH_LOCK_OVERLAY_TEST_TAG = "TouchLockOverlay"

/**
 * Transparent full-screen overlay that consumes all touches.
 * Intercepts vertical swipe gestures (Swipe Up = Next, Swipe Down = Previous)
 * and hosts the [HiddenUnlockCorner] in the top-right corner.
 * All other gestures (taps, pinches, horizontal swipes, long presses) are consumed and ignored.
 */
@Composable
fun TouchLockOverlay(
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onUnlockTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(TOUCH_LOCK_OVERLAY_TEST_TAG)
            .semantics {
                contentDescription = "Toddler touch lock overlay"
            }
            .pointerInput(onSwipeNext, onSwipePrevious) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Main)
                        val downChange = down.changes.firstOrNull { it.pressed } ?: continue
                        downChange.consume()

                        var totalDeltaY = 0f
                        var totalDeltaX = 0f
                        var swipeTriggered = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == downChange.id }

                            if (change == null || !change.pressed) {
                                // Finger lifted
                                break
                            }

                            val delta = change.position - change.previousPosition
                            totalDeltaY += delta.y
                            totalDeltaX += delta.x

                            // Consume touch so WebView never receives it
                            change.consume()

                            // Check swipe threshold
                            if (!swipeTriggered && abs(totalDeltaY) > swipeThresholdPx && abs(totalDeltaY) > abs(totalDeltaX) * 1.3f) {
                                swipeTriggered = true
                                if (totalDeltaY < 0f) {
                                    // Swiped up -> Next
                                    onSwipeNext()
                                } else {
                                    // Swiped down -> Previous
                                    onSwipePrevious()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Invisible 72x72dp top-right unlock corner
        HiddenUnlockCorner(
            onUnlockTriggered = onUnlockTriggered,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
