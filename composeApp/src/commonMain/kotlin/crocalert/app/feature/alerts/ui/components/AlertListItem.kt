package crocalert.app.feature.alerts.ui.components

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

/**
 * A single alert card that mirrors the design in the Alerts Panel mockup.
 *
 * Visual anatomy:
 * ┌─ [4dp border] ─────────────────────────────────────┐
 * │  [48dp thumb]  Title                          [dot] │
 * │                Camera: <sourceName>                 │
 * │                <relative time>                      │
 * │                <message (2 lines max)>              │
 * │                                     [severity badge]│
 * └─────────────────────────────────────────────────────┘
 *
 * Colour mapping (matches mockup):
 *  CRITICAL → amber border/thumb/badge
 *  HIGH     → error colour border/thumb/badge
 *  MEDIUM   → navy (CrocBlue) border/thumb/badge
 *  LOW      → neutral grey border/thumb/badge
 *
 * Unread alerts receive full-surface background + bold title + accent dot.
 * Read alerts receive muted surfaceVariant background.
 */
@Composable
fun AlertListItem(
    alert: Alert,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (alert.priority) {
        AlertPriority.CRITICAL -> CrocAmber
        AlertPriority.HIGH -> MaterialTheme.colorScheme.error
        AlertPriority.MEDIUM -> CrocBlue
        AlertPriority.LOW -> CrocNeutralDark
    }

    val badgeLabel = when (alert.priority) {
        AlertPriority.CRITICAL, AlertPriority.HIGH -> "Alert"
        AlertPriority.MEDIUM -> "Pre-Alert"
        AlertPriority.LOW -> "Info"
    }

    val badgeTextColor = when (alert.priority) {
        AlertPriority.CRITICAL -> CrocBlack
        else -> CrocWhite
    }

    val cardBackground =
        if (alert.isRead) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (alert.isRead) 0.dp else 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Left severity border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Thumbnail placeholder — coloured square tinted by severity
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                )

                Column(modifier = Modifier.weight(1f)) {
                    // Title row with unread dot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (!alert.isRead) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Camera: ${alert.sourceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatRelativeTime(alert.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (alert.message.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Severity badge — bottom right
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
}

/** Returns a human-readable relative time string for the given epoch milliseconds. */
private fun formatRelativeTime(epochMillis: Long): String {
    val diffMs = Clock.System.now().toEpochMilliseconds() - epochMillis
    return when {
        diffMs < 60_000L -> "Just now"
        diffMs < 3_600_000L -> "${diffMs / 60_000L} min ago"
        diffMs < 86_400_000L -> "${diffMs / 3_600_000L} hr ago"
        else -> "${diffMs / 86_400_000L} days ago"
    }
}
