package crocalert.app.ui.cameras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.cameras.components.CaptureGridSection
import kotlinx.datetime.LocalDate

private const val INTEGRITY_THRESHOLD = 0.9f

@Composable
fun CameraHistoryScreen(
    cameraId: String,
    cameraName: String,
    onBack: () -> Unit,
    viewModel: CameraHistoryViewModel = viewModel(key = cameraId) {
        CameraHistoryViewModel(cameraId, cameraName)
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // — Header ————————————————————————————————————————————————————————————
        HistoryHeader(cameraName = uiState.cameraName, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            // — Date + Stats card ————————————————————————————————————————————
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DateNavigationRow(
                        date = uiState.selectedDate,
                        canGoNext = uiState.canGoNext,
                        onPrev = viewModel::prevDay,
                        onNext = viewModel::nextDay,
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    CaptureStatsRow(
                        received = uiState.received,
                        missing = uiState.missing,
                        expected = uiState.expected,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // — Progress card ————————————————————————————————————————————————
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CrocBlue)
                        }
                    } else {
                        CaptureProgressSection(
                            received = uiState.received,
                            expected = uiState.expected,
                            missing = uiState.missing,
                            integrityFlags = uiState.integrityFlags,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // — Cadencia card ————————————————————————————————————————————————
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cadencia esperada vs actual",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (uiState.captureSlots.isNotEmpty()) {
                        CaptureGridSection(slots = uiState.captureSlots)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// — Private sub-composables ————————————————————————————————————————————————

@Composable
private fun HistoryHeader(cameraName: String, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            Text(
                text = "← Cámaras",
                style = MaterialTheme.typography.labelMedium,
                color = CrocBlue,
            )
        }
        Text(
            text = "HISTORIAL DE CAPTURAS",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = CrocBlue,
        )
        Text(
            text = cameraName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun DateNavigationRow(
    date: LocalDate,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Día anterior",
                tint = CrocBlue,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = date.displayFormat(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Día siguiente",
                tint = if (canGoNext) CrocBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun CaptureStatsRow(received: Int, missing: Int, expected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CaptureStatChip(label = "Recibidas", count = received, color = CrocBlue)
        CaptureStatChip(label = "Perdidas", count = missing, color = CrocAmber)
        CaptureStatChip(label = "Esperadas", count = expected, color = CrocNeutralDark)
    }
}

@Composable
private fun CaptureStatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color,
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CrocWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun CaptureProgressSection(
    received: Int,
    expected: Int,
    missing: Int,
    integrityFlags: Int,
) {
    val fillRatio = received.toFloat() / expected.coerceAtLeast(1)
    val progressColor = if (fillRatio >= INTEGRITY_THRESHOLD) CrocNeutralDark else CrocAmber

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Captura de imgs (24h)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$received / $expected expected",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(8.dp))

    LinearProgressIndicator(
        progress = { fillRatio },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = "$missing capturas faltantes · $integrityFlags alertas de integridad",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun LocalDate.displayFormat(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthNumber.toString().padStart(2, '0')}/$year"
