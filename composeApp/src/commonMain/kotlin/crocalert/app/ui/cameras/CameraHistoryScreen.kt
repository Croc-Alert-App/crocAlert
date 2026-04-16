package crocalert.app.ui.cameras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.theme.*
import crocalert.app.ui.cameras.components.CaptureGridSection
import kotlinx.datetime.LocalDate

private const val INTEGRITY_THRESHOLD = 0.9f

@Composable
fun CameraHistoryScreen(
    cameraId: String,
    cameraName: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit = {},
    viewModel: CameraHistoryViewModel = viewModel(key = cameraId) {
        CameraHistoryViewModel(cameraId, cameraName)
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    val captureSlots = remember(
        uiState.received,
        uiState.missing,
        uiState.expected
    ) {
        buildSlots(uiState.received, uiState.missing, uiState.expected)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        HistoryHeader(cameraName = uiState.cameraName, onBack = onBack, onEdit = onEditClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {

            Spacer(Modifier.height(12.dp))

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
                        expectedPerDay = uiState.expectedPerDay,
                        onExpectedChange = viewModel::setExpectedPerDay,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

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

                    if (captureSlots.isNotEmpty()) {
                        CaptureGridSection(slots = captureSlots)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// =======================
// LOGICA SLOTS
// =======================

fun buildSlots(received: Int, missing: Int, expected: Int): List<CaptureSlot> {
    val slots = mutableListOf<CaptureSlot>()
    for (i in 0 until expected) {
        val state = when {
            i < received -> CaptureSlotState.Received
            i < received + missing -> CaptureSlotState.Missing
            else -> CaptureSlotState.Expected
        }
        slots.add(CaptureSlot(hour = i, state = state))
    }
    return slots
}

// =======================
// COMPONENTES FALTANTES
// =======================

@Composable
private fun HistoryHeader(cameraName: String, onBack: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = CrocBlue)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HISTORIAL DE CAPTURAS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = CrocBlue,
            )
            Text(
                text = cameraName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Editar cámara", tint = CrocBlue)
        }
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
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Día anterior", tint = CrocBlue)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = date.displayFormat(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Día siguiente",
                tint = if (canGoNext) CrocBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun CaptureStatsRow(
    received: Int,
    missing: Int,
    expectedPerDay: Int,
    onExpectedChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CaptureStatChip("Recibidas", received, CrocBlue)
        CaptureStatChip("Perdidas", missing, CrocAmber)
        ExpectedStepper(expectedPerDay, onExpectedChange)
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
    val isComplete = received >= expected

    val progressColor = when {
        isComplete -> CrocBlue
        fillRatio >= INTEGRITY_THRESHOLD -> CrocNeutralDark
        else -> CrocAmber
    }

    val textColor = if (isComplete) CrocBlue else MaterialTheme.colorScheme.onSurfaceVariant

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "Captura de imgs (24h)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            "$received / $expected esperadas",
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }

    Spacer(Modifier.height(8.dp))

    LinearProgressIndicator(
        progress = { fillRatio },
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Spacer(Modifier.height(6.dp))

    Text(
        "$missing capturas faltantes · $integrityFlags alertas de integridad",
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun CaptureStatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label)
        Spacer(Modifier.height(4.dp))
        Surface(color = color, shape = RoundedCornerShape(6.dp)) {
            Text(
                "$count",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = CrocWhite,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExpectedStepper(value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf("$value") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Esperadas")
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = text,
            onValueChange = {
                val filtered = it.filter { c -> c.isDigit() }.take(2)
                text = filtered
                filtered.toIntOrNull()?.coerceIn(1, 48)?.let(onValueChange)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .background(CrocBlueLight.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .width(52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    inner()
                }
            }
        )
    }
}

private fun LocalDate.displayFormat(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthNumber.toString().padStart(2, '0')}/$year"