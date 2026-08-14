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
import kotlin.math.hypot
import kotlinx.coroutines.delay

/**
 * Short press → [onTap].
 * Move before long-press → cancel (pager can scroll).
 * Long-press while holding → [onLongPress] (menu).
 * Then drag past touch slop → [onDragStart] / [onDrag] until up.
 * Release without dragging → [onLongPressRelease] (never [onTap]).
 */
suspend fun PointerInputScope.detectLongPressMenuOrDrag(
    onTap: () -> Unit = {},
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
        if (longPress == null) {
            val change = currentEvent.changes.fastFirstOrNull { it.id == down.id }
            val lifted = change == null ||
                change.changedToUpIgnoreConsumed() ||
                !change.pressed
            val moved = hypot(
                ((change?.position?.x ?: down.position.x) - down.position.x).toDouble(),
                ((change?.position?.y ?: down.position.y) - down.position.y).toDouble()
            ).toFloat() > viewConfiguration.touchSlop
            if (lifted && !moved) {
                change?.consume()
                onTap()
            }
            return@awaitEachGesture
        }

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
            if (dragging) onDragCancel() else onLongPressRelease()
            throw t
        }
    }
}

/**
 * Long-press on empty space (wallpaper / gaps). Waits a tick so icon handlers can
 * consume the pointer first — otherwise holding an app would also enter edit mode.
 */
suspend fun PointerInputScope.detectUnconsumedLongPress(onLongPress: () -> Unit) {
    while (true) {
        val (down, longPress) = awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = true)
            down to awaitLongPressOrCancellation(down.id)
        }
        if (longPress == null) continue
        delay(1)
        if (down.isConsumed || longPress.isConsumed) continue
        down.consume()
        longPress.consume()
        onLongPress()
    }
}
