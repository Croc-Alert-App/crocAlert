package crocalert.app.ui.cameras

import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CaptureDto
import kotlinx.datetime.Clock

private const val EXPECTED_CAPTURES_PER_DAY = 24
private const val INTEGRITY_THRESHOLD = 0.90f
private const val DAY_MILLIS = 86_400_000L

fun Camera.toUiItem(
    captures: List<CaptureDto>,
    expectedPerDay: Int = expectedImages ?: EXPECTED_CAPTURES_PER_DAY,
): CameraUiItem {
    val now = Clock.System.now().toEpochMilliseconds()
    val dayAgo = now - DAY_MILLIS
    val todayCaptures = captures.filter { (it.captureTime ?: 0L) >= dayAgo }
    val lastCapture = captures.maxByOrNull { it.captureTime ?: 0L }

    val sent = todayCaptures.count { it.driveUrl.isNotBlank() }
    val captureCount = todayCaptures.size
    val missing = maxOf(0, expectedPerDay - captureCount)
    val rate = if (expectedPerDay > 0) captureCount.toFloat() / expectedPerDay else 1f

    val status = when {
        !isActive || captureCount == 0 -> CameraStatus.Alert
        rate < INTEGRITY_THRESHOLD     -> CameraStatus.Attention
        else                           -> CameraStatus.Ok
    }

    val integrityFlags = if (rate < INTEGRITY_THRESHOLD) 1 else 0

    return CameraUiItem(
        id = id,
        name = name,
        isActive = isActive,
        status = status,
        lastCapture = lastCapture?.captureTime?.toRelativeTime() ?: "—",
        imagesSent = sent,
        imagesExpected = expectedPerDay,
        captureCount = captureCount,
        captureExpected = expectedPerDay,
        missingCaptures = missing,
        integrityFlags = integrityFlags
    )
}

private fun Long.toRelativeTime(): String {
    val diffMs = Clock.System.now().toEpochMilliseconds() - this
    val diffMin = diffMs / 60_000
    return when {
        diffMin < 1    -> "just now"
        diffMin < 60   -> "${diffMin}m ago"
        diffMin < 1440 -> "${diffMin / 60}h ago"
        else           -> "${diffMin / 1440}d ago"
    }
}
