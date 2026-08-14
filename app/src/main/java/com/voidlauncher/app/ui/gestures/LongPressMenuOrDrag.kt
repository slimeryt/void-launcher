package com.voidlauncher.app.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.util.fastFirstOrNull

/**
 * Short press → leave alone (child tap handler fires).
 * Move before long-press → cancel (pager can scroll).
 * Long-press while holding → [onLongPress] (menu).
 * Then drag past touch slop → [onDragStart] / [onDrag] until up.
 * Release without dragging → [onLongPressRelease].
 */
suspend fun PointerInputScope.detectLongPressMenuOrDrag(
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit = onDragEnd,
    onLongPressRelease: () -> Unit = {}
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val longPress = awaitLongPressOrCancellation(down.id)
        if (longPress == null) return@awaitEachGesture

        // Consume so sibling tap handlers don't fire on release after a hold.
        down.consume()
        longPress.consume()
        onLongPress()

        val touchSlop = viewConfiguration.touchSlop
        var dragging = false
        var total = Offset.Zero
        val pointerId = down.id

        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.fastFirstOrNull { it.id == pointerId }
                if (change == null) {
                    if (dragging) onDragCancel() else onLongPressRelease()
                    return@awaitEachGesture
                }
                if (change.changedToUpIgnoreConsumed()) {
                    change.consume()
                    if (dragging) onDragEnd() else onLongPressRelease()
                    return@awaitEachGesture
                }
                // IgnoreConsumed: overlays/clickables may have consumed the change already.
                val delta = change.positionChangeIgnoreConsumed()
                if (delta == Offset.Zero) continue
                change.consume()
                total += delta
                if (!dragging && total.getDistance() > touchSlop) {
                    dragging = true
                    onDragStart()
                }
                if (dragging) {
                    onDrag(change, delta)
                }
            }
        } catch (t: Throwable) {
            if (dragging) onDragCancel()
            throw t
        }
    }
}
