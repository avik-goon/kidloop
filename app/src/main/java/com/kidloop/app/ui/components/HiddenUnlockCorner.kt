package com.kidloop.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull

const val HIDDEN_UNLOCK_TEST_TAG = "HiddenUnlockCorner"

/**
 * An invisible 72dp x 72dp touch target located in the top-right corner.
 * Requires an uninterrupted 3-second long press to trigger [onUnlockTriggered].
 * Emits zero visual or audio cues to keep it completely hidden from toddlers.
 */
@Composable
fun HiddenUnlockCorner(
    onUnlockTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .testTag(HIDDEN_UNLOCK_TEST_TAG)
            .semantics {
                contentDescription = "Hidden parent unlock trigger"
            }
            .pointerInput(onUnlockTriggered) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    var releasedOrCancelled = false

                    // Wait 3000ms. If pointer is released before 3000ms,
                    // the inner loop breaks and withTimeoutOrNull returns Unit.
                    // If 3000ms elapses while still held, withTimeoutOrNull returns null.
                    val result = withTimeoutOrNull(3000L) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                releasedOrCancelled = true
                                break
                            }
                            change.consume()
                        }
                    }

                    if (result == null && !releasedOrCancelled) {
                        onUnlockTriggered()

                        // Consume remaining events until finger is finally lifted
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                break
                            }
                            change.consume()
                        }
                    }
                }
            }
    )
}
