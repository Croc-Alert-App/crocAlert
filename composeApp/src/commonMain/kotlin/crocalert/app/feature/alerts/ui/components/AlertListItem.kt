package crocalert.app.feature.alerts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlack
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocWhite
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A single alert card.
 *
 * Color mapping (amber = alert, blue = pre-alert — no red):
 *  CRITICAL / HIGH  →  CrocAmber  →  "Alerta" badge
 *  MEDIUM           →  CrocBlue   →  "Pre-Alerta" badge
 *  LOW              →  neutral    →  "Info" badge
 *
 * Title is derived from the alert type (human-readable).
 * The raw filename is shown as a subtitle.
 * Date is shown as "23 Mar · 03:36" alongside the relative time.
 */
@Composable
fun AlertListItem(
    alert: Alert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (alert.priority) {
        AlertPriority.CRITICAL, AlertPriority.HIGH -> CrocAmber
        AlertPriority.MEDIUM -> CrocBlue
        AlertPriority.LOW -> CrocNeutralDark
    }

    val badgeLabel = when (alert.priority) {
        AlertPriority.CRITICAL, AlertPriority.HIGH -> "Alerta"
        AlertPriority.MEDIUM -> "Pre-Alerta"
        AlertPriority.LOW -> "Info"
    }

    val badgeTextColor = when (alert.priority) {
        AlertPriority.CRITICAL, AlertPriority.HIGH -> CrocBlack
        else -> CrocWhite
    }

    val cardBackground =
        if (alert.isRead) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface

    val displayTitle = alert.alertDisplayTitle()

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (alert.isRead) 0.dp else 2.dp,
        ),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // ── Left accent bar ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // ── Title row ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!alert.isRead) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                        )
                    }
                }

                // ── Filename subtitle ──────────────────────────────────────
                if (alert.title.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ── Meta info ─────────────────────────────────────────────
                Text(
                    text = buildDateLine(alert.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (alert.message.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ── Severity badge ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = accentColor,
                    ) {
                        Text(
                            text = badgeLabel,
                            color = badgeTextColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Best-effort camera name for the card title.
 *
 * Priority:
 * 1. sourceName (resolved display name)
 * 2. cameraId (raw id from Firestore)
 * 3. Name extracted from filename pattern NAME_YYYY_MM_DDTHH…
 * 4. Folder-based fallback
 */
private fun Alert.alertDisplayTitle(): String {
    if (sourceName.isNotBlank()) return sourceName
    if (cameraId.isNotBlank()) return cameraId
    // Extract camera name from filename: everything before _YYYY_MM_DDTHH
    val extracted = Regex("""^(.+?)_\d{4}_\d{2}_\d{2}T""").find(title)?.groupValues?.getOrNull(1)
    if (!extracted.isNullOrBlank()) return extracted
    return if (folder == "alertas") "Alerta Detectada" else "Pre-Alerta"
}

/** "23 Mar · 03:36  ·  Hace 2 h" */
private fun buildDateLine(epochMillis: Long): String {
    val formatted = epochMillis.toDisplayDate()
    val relative = formatRelativeTime(epochMillis)
    return "$formatted  ·  $relative"
}

private fun Long.toDisplayDate(): String {
    if (this == 0L) return "N/D"
    val dt = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val month = when (dt.monthNumber) {
        1 -> "Ene"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Abr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Ago"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dic"
    }
    val h = dt.hour.toString().padStart(2, '0')
    val m = dt.minute.toString().padStart(2, '0')
    return "${dt.dayOfMonth} $month · $h:$m"
}

private fun formatRelativeTime(epochMillis: Long): String {
    val diffMs = Clock.System.now().toEpochMilliseconds() - epochMillis
    return when {
        diffMs < 60_000L -> "Ahora mismo"
        diffMs < 3_600_000L -> "Hace ${diffMs / 60_000L} min"
        diffMs < 86_400_000L -> "Hace ${diffMs / 3_600_000L} h"
        else -> "Hace ${diffMs / 86_400_000L} días"
    }
}
