package crocalert.app.feature.alerts.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.feature.alerts.presentation.AlertDetailUiState
import crocalert.app.feature.alerts.presentation.AlertDetailViewModel
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType
import crocalert.app.model.Camera
import crocalert.app.model.Site
import crocalert.app.shared.createAlertRepository
import crocalert.app.shared.createCameraRepository
import crocalert.app.shared.createSiteRepository
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlack
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocWhite
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
    val accentColor = when (alert.priority) {
        AlertPriority.CRITICAL, AlertPriority.HIGH -> CrocAmber
        AlertPriority.MEDIUM -> CrocBlue
        AlertPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val badgeLabel = when (alert.priority) {
        AlertPriority.CRITICAL, AlertPriority.HIGH -> "Alerta"
        AlertPriority.MEDIUM -> "Pre-Alerta"
        AlertPriority.LOW -> "Info"
    }
    val displayTitle = when (alert.type) {
        AlertType.POSSIBLE_CROCODILE -> "Posible Cocodrilo Detectado"
        AlertType.IMAGE_UPLOADED     -> "Nueva Captura"
        AlertType.MOTION_DETECTED    -> "Movimiento Detectado"
        AlertType.SYSTEM_WARNING     -> "Advertencia del Sistema"
        AlertType.BATTERY_LOW        -> "Batería Baja"
        AlertType.SYNC_COMPLETED     -> "Sincronización Completa"
        AlertType.UNKNOWN            -> if (alert.folder == "alertas") "Alerta Detectada" else "Nueva Captura"
    }

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
            Column(modifier = Modifier.padding(16.dp)) {
                // Image placeholder row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Thumbnail placeholder — replace with AsyncImage when Storage URL is available
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Title + type badge
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrocBlue,
                        )
                        Spacer(Modifier.height(4.dp))
                        // Priority badge inline
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = accentColor,
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (alert.priority == AlertPriority.MEDIUM) CrocWhite else CrocBlack,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Filename
                if (alert.title.isNotBlank()) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Detection metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DetailChip(label = "Estado", value = alert.status.toSpanish())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val confidenceText = alert.aiConfidence
                            ?.let { "${(it * 100).toInt()}%" }
                            ?: "N/D"
                        DetailChip(label = "Confianza IA", value = confidenceText)
                    }
                }

                Spacer(Modifier.height(8.dp))
                DetailChip(label = "Detectado", value = alert.createdAt.toDisplayDateTime())
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "ORIGEN DE LA CAPTURA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))

                CaptureDetailRow("Cámara", camera?.name ?: alert.cameraId.ifBlank { "N/D" })
                CaptureDetailRow("Sitio", site?.name ?: "N/D")
                CaptureDetailRow(
                    label = "Estado cámara",
                    value = when {
                        camera == null -> "N/D"
                        camera.isActive -> "Activa"
                        else -> "Inactiva"
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val resolution = if (capture?.width != null && capture.height != null) {
                    "${capture.width} × ${capture.height}"
                } else "N/D"
                CaptureDetailRow("Resolución", resolution)
                CaptureDetailRow("Tamaño archivo", capture?.size ?: "N/D")
                if (!alert.folder.isNullOrBlank()) {
                    CaptureDetailRow("Carpeta", alert.folder.orEmpty())
                }
            }
        }

        // ── Notes card ──────────────────────────────────────────────────
        if (!alert.notes.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "NOTAS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = alert.notes.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
