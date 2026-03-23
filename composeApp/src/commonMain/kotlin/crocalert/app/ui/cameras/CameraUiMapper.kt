package crocalert.app.ui.cameras

import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CaptureDto
import kotlinx.datetime.Clock

internal const val DEFAULT_EXPECTED_PER_DAY = 24
private const val INTEGRITY_THRESHOLD = 0.90f
private const val DAY_MILLIS = 86_400_000L

/**
 * Builds a [CameraUiItem] from server-authoritative daily stats.
 * Used by the cameras list screen — avoids per-camera capture fetches.
 */
fun Camera.toUiItem(stats: CameraDailyStatsDto): CameraUiItem {
    val received = stats.receivedImages
    val expected = expectedImages?.takeIf { it > 0 }
        ?: stats.expectedImages.takeIf { it > 0 }
        ?: DEFAULT_EXPECTED_PER_DAY
    val missing = stats.missingImages
    val rate = if (expected > 0) received.toFloat() / expected else 1f

    val status = when {
        !isActive || received == 0 -> CameraStatus.Alert
        rate < INTEGRITY_THRESHOLD -> CameraStatus.Attention
        else                       -> CameraStatus.Ok
    }

    return CameraUiItem(
        id = id,
        name = name,
        isActive = isActive,
        status = status,
        lastCapture = "—",          // not available from stats endpoint
        imagesSent = received,
        imagesExpected = expected,
        captureCount = received,
        captureExpected = expected,
        missingCaptures = missing,
        integrityFlags = 0,         // detail-level; shown in history screen only
    )
}

/**
 * Builds a [CameraUiItem] from individual captures (used when stats are unavailable).
 * Also used by the history screen where capture-level data is needed.
 */
fun Camera.toUiItem(
    captures: List<CaptureDto>,
    expectedPerDay: Int = expectedImages?.takeIf { it > 0 } ?: DEFAULT_EXPECTED_PER_DAY,
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

    val integrityFlags = todayCaptures.count { it.driveUrl.isBlank() }

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
        integrityFlags = integrityFlags,
    )
}

internal fun Long.toRelativeTime(): String {
    val diffMs = Clock.System.now().toEpochMilliseconds() - this
    val diffMin = diffMs / 60_000
    return when {
        diffMin < 1    -> "Ahora mismo"
        diffMin < 60   -> "Hace ${diffMin} min"
        diffMin < 1440 -> "Hace ${diffMin / 60} h"
        else           -> "Hace ${diffMin / 1440} días"
    }
}
