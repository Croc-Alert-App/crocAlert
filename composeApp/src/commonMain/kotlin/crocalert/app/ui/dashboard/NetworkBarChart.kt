package crocalert.app.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralLight

/**
 * Bar chart that draws both bars and day-of-week labels inside a single [Canvas],
 * guaranteeing pixel-perfect horizontal alignment between each bar and its label.
 */
@Composable
fun NetworkBarChart(
    data: List<NetworkTrendDay>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val todayLabelColor = CrocBlue

    Canvas(modifier = modifier) {
        val barCount = data.size
        if (barCount == 0) return@Canvas

        val labelHeightPx = 16.dp.toPx()
        val chartHeight = size.height - labelHeightPx - 4.dp.toPx()
        val barWidth = size.width / (barCount * 2f)
        val gap = barWidth

        data.forEachIndexed { index, day ->
            val barHeight = (chartHeight * day.value).coerceAtLeast(0f)
            val barLeft = index * (barWidth + gap) + gap / 2f
            val barCenter = barLeft + barWidth / 2f

            // Draw bar
            drawRoundRect(
                color = if (day.isToday) CrocBlue else CrocNeutralLight,
                topLeft = Offset(x = barLeft, y = chartHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Draw label centred under bar
            val labelStyle = TextStyle(
                fontSize = 10.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isToday) todayLabelColor else labelColor,
            )
            val measured = textMeasurer.measure(day.label, style = labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = barCenter - measured.size.width / 2f,
                    y = chartHeight + 4.dp.toPx()
                )
            )
        }
    }
}
