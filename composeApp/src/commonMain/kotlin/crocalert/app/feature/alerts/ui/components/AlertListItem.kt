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

/**
 * A single alert card.
 *
 * Color mapping (amber = alert, blue = pre-alert — no red):
 *  CRITICAL / HIGH  →  CrocAmber  →  "Alerta" badge
 *  MEDIUM           →  CrocBlue   →  "Pre-Alerta" badge
 *  LOW              →  neutral    →  "Info" badge
 *
 * Unread alerts receive a brighter card surface + bold title + small accent dot.
 * Read alerts are shown on the muted surfaceVariant background.
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
                        text = alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
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

                Spacer(Modifier.height(6.dp))

                // ── Meta info ─────────────────────────────────────────────
                Text(
                    text = "Cámara: ${alert.sourceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatRelativeTime(alert.createdAt),
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

private fun formatRelativeTime(epochMillis: Long): String {
    val diffMs = Clock.System.now().toEpochMilliseconds() - epochMillis
    return when {
        diffMs < 60_000L -> "Ahora mismo"
        diffMs < 3_600_000L -> "Hace ${diffMs / 60_000L} min"
        diffMs < 86_400_000L -> "Hace ${diffMs / 3_600_000L} h"
        else -> "Hace ${diffMs / 86_400_000L} días"
    }
}
