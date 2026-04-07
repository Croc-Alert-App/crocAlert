package crocalert.app.feature.alerts.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.feature.alerts.presentation.*
import crocalert.app.model.*
import crocalert.app.shared.*
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.theme.*
import crocalert.app.ui.components.BottomNavBar
import crocalert.app.ui.dashboard.DashboardTab
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    alertId: String,
    onBack: () -> Unit,
    selectedTab: DashboardTab,
    onTabSelect: (DashboardTab) -> Unit,
    viewModel: AlertDetailViewModel = viewModel(key = alertId) {
        AlertDetailViewModel(
            alertId,
            createAlertRepository(),
            createCameraRepository(),
            createSiteRepository()
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DETALLE ALERTA", fontWeight = FontWeight.Bold, color = CrocBlue) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(selected = selectedTab, onSelect = onTabSelect)
        }
    ) { padding ->

        when (val state = uiState) {

            is AlertDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator(color = CrocBlue)
                }
            }

            is AlertDetailUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = viewModel::retry) { Text("Reintentar") }
                    }
                }
            }

            is AlertDetailUiState.Success -> {
                AlertDetailContent(
                    alert = state.alert,
                    camera = state.camera,
                    site = state.site,
                    capture = state.capture,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun AlertDetailContent(
    alert: Alert,
    camera: Camera?,
    site: Site?,
    capture: CaptureDto?,
    modifier: Modifier = Modifier
) {

    var showFullImage by remember { mutableStateOf(false) }

    val displayTitle = when (alert.type) {
        AlertType.POSSIBLE_CROCODILE -> "Posible Cocodrilo Detectado"
        AlertType.IMAGE_UPLOADED -> "Nueva Captura"
        else -> "Alerta"
    }

    // 🔥 AQUÍ ESTÁ LA MAGIA (folder-based)
    val badgeLabel = when (alert.folder) {
        "alertas" -> "Alerta"
        "pre-alertas" -> "Pre-Alerta"
        else -> "Info"
    }

    val badgeColor = when (alert.folder) {
        "alertas" -> CrocAmber
        "pre-alertas" -> CrocBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box {

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(Modifier.padding(16.dp)) {

                    // Imagen
                    alert.thumbnailUrl?.let { url ->
                        KamelImage(
                            resource = asyncPainterResource(url.toDirectDriveImageUrl()),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showFullImage = true }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            displayTitle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = badgeColor
                        ) {
                            Text(
                                text = badgeLabel,
                                color = CrocWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (alert.title.isNotBlank()) {
                        Text(alert.title)
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            SmallLabel("Estado")
                            ValueText(alert.status.toSpanish())
                        }
                        Column(Modifier.weight(1f)) {
                            SmallLabel("Confianza IA")
                            ValueText(alert.aiConfidence?.let { "${(it * 100).toInt()}%" } ?: "N/D")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Column {
                        SmallLabel("Detectado")
                        ValueText(alert.createdAt.toDisplayDateTime())
                    }

                    Spacer(Modifier.height(16.dp))

                    SmallLabel("Origen")
                    CaptureDetailRow("Cámara", camera?.name ?: "N/D")
                    CaptureDetailRow("Sitio", site?.name ?: "N/D")

                    if (!alert.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        SmallLabel("Notas")
                        Text(alert.notes!!)
                    }
                }
            }
        }

        // FULLSCREEN
        AnimatedVisibility(
            visible = showFullImage,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullImage = false },
                contentAlignment = Alignment.Center
            ) {
                alert.thumbnailUrl?.let { url ->
                    KamelImage(
                        resource = asyncPainterResource(url.toDirectDriveImageUrl()),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

// ── COMPONENTES ─────────────────────────

@Composable
private fun SmallLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ValueText(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun CaptureDetailRow(label: String, value: String) {
    Row {
        Text("$label: ")
        Text(value)
    }
}

// ── HELPERS ─────────────────────────────

private fun AlertStatus.toSpanish() = when (this) {
    AlertStatus.OPEN -> "ACTIVA"
    AlertStatus.IN_PROGRESS -> "EN PROCESO"
    AlertStatus.CLOSED -> "CERRADA"
}

private fun Long.toDisplayDateTime(): String {
    val dt = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth}/${dt.monthNumber} ${dt.hour}:${dt.minute}"
}

private fun String.toDirectDriveImageUrl(): String {
    val regex = Regex("/d/([a-zA-Z0-9_-]+)")
    val match = regex.find(this)
    val fileId = match?.groupValues?.get(1)
    return if (fileId != null) {
        "https://drive.google.com/uc?export=view&id=$fileId"
    } else this
}