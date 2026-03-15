package crocalert.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralLight

@Composable
fun NetworkBarChart(
    data: List<NetworkTrendDay>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = data.size
        if (barCount == 0) return@Canvas
        val barWidth = size.width / (barCount * 2f)
        val gap = barWidth

        data.forEachIndexed { index, day ->
            val barHeight = size.height * day.value
            drawRoundRect(
                color = if (day.isToday) CrocBlue else CrocNeutralLight,
                topLeft = Offset(x = index * (barWidth + gap) + gap / 2f, y = size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}
