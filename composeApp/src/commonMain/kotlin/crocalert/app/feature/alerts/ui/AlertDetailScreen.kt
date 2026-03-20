package crocalert.app.feature.alerts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.feature.alerts.presentation.AlertDetailUiState
import crocalert.app.feature.alerts.presentation.AlertDetailViewModel
import crocalert.app.model.Alert
import crocalert.app.model.AlertStatus
import crocalert.app.model.Camera
import crocalert.app.model.Site
import crocalert.app.shared.createAlertRepository
import crocalert.app.shared.createCameraRepository
import crocalert.app.shared.createSiteRepository
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.components.BottomNavBar
import crocalert.app.ui.dashboard.DashboardTab
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    alertId: String,
    onBack: () -> Unit,
    selectedTab: DashboardTab,
    onTabSelect: (DashboardTab) -> Unit,
    viewModel: AlertDetailViewModel = viewModel(key = alertId) {
        AlertDetailViewModel(
            alertId = alertId,
            alertRepository = createAlertRepository(),
            cameraRepository = createCameraRepository(),
            siteRepository = createSiteRepository(),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DETALLE ALERTA",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CrocBlue,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomNavBar(selected = selectedTab, onSelect = onTabSelect)
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is AlertDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = CrocBlue)
                }
            }
            is AlertDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = viewModel::retry) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is AlertDetailUiState.Success -> {
                AlertDetailContent(
                    alert = state.alert,
                    camera = state.camera,
                    site = state.site,
                    capture = state.capture,
                    modifier = Modifier.padding(innerPadding),
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Detection card ──────────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Thumbnail placeholder (replace with AsyncImage/coil when available)
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {}

                Spacer(Modifier.height(12.dp))

                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CrocBlue,
                )

                Spacer(Modifier.height(8.dp))

                // Row 1: Estado + Confianza IA
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Estado: ${alert.status.toSpanish()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val confidenceText = alert.aiConfidence
                        ?.let { "Confianza IA: ${(it * 100).toInt()}%" }
                        ?: "Confianza IA: N/D"
                    Text(
                        text = confidenceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Row 2: Detectado
                Text(
                    text = "Detectado: ${alert.createdAt.toDisplayDateTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Capture origin card ─────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ORIGEN DE LA CAPTURA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                CaptureDetailRow("Cámara", camera?.name ?: "N/D")
                CaptureDetailRow("Sitio", site?.name ?: "N/D")
                CaptureDetailRow(
                    label = "Estado cámara",
                    value = when {
                        camera == null -> "N/D"
                        camera.isActive -> "Activa"
                        else -> "Inactiva"
                    },
                )

                Spacer(Modifier.height(8.dp))

                val resolution = if (capture?.width != null && capture.height != null) {
                    "${capture.width} × ${capture.height}"
                } else "N/D"
                CaptureDetailRow("Resolución", resolution)
                CaptureDetailRow("Tamaño archivo", capture?.size ?: "N/D")
            }
        }
    }
}

@Composable
private fun CaptureDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun AlertStatus.toSpanish() = when (this) {
    AlertStatus.OPEN -> "ACTIVA"
    AlertStatus.IN_PROGRESS -> "EN PROCESO"
    AlertStatus.CLOSED -> "CERRADA"
}

private fun Long.toDisplayDateTime(): String {
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
