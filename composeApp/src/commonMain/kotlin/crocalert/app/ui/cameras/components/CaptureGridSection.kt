package crocalert.app.ui.cameras.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.cameras.CaptureSlot
import crocalert.app.ui.cameras.CaptureSlotState

private const val CELLS_PER_ROW = 8

/**
 * Displays the expected-vs-actual capture cadence as a grid of hourly slots,
 * followed by a colour legend. Each slot corresponds to one hour (0–23).
 */
@Composable
fun CaptureGridSection(
    slots: List<CaptureSlot>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        slots.chunked(CELLS_PER_ROW).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { slot ->
                    CaptureSlotCell(slot = slot)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CaptureSlotLegendItem(color = CrocBlue, label = "Recibidas")
            CaptureSlotLegendItem(color = CrocAmber, label = "Perdidas")
            CaptureSlotLegendItem(color = CrocNeutralDark, label = "Esperadas")
        }
    }
}

@Composable
private fun CaptureSlotCell(slot: CaptureSlot, modifier: Modifier = Modifier) {
    val bgColor = when (slot.state) {
        CaptureSlotState.Received      -> CrocBlue
        CaptureSlotState.Missing       -> CrocAmber
        CaptureSlotState.IntegrityFlag -> CrocAmber
        CaptureSlotState.Expected      -> CrocNeutralDark
    }
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        when (slot.state) {
            CaptureSlotState.Received -> Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Recibida hora ${slot.hour}",
                tint = CrocWhite,
                modifier = Modifier.size(16.dp),
            )
            CaptureSlotState.IntegrityFlag -> Text(
                text = "!",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CrocWhite,
            )
            CaptureSlotState.Missing, CaptureSlotState.Expected -> Dash(
                color = CrocWhite.copy(alpha = if (slot.state == CaptureSlotState.Expected) 0.5f else 0.85f),
            )
        }
    }
}

@Composable
private fun Dash(color: Color) {
    Box(
        modifier = Modifier
            .width(10.dp)
            .height(2.dp)
            .background(color),
    )
}

@Composable
private fun CaptureSlotLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
