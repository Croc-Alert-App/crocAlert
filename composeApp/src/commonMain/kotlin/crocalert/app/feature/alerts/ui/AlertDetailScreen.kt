package crocalert.app.feature.alerts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BrokenImage
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.feature.alerts.presentation.AlertDetailUiState
import crocalert.app.feature.alerts.presentation.AlertDetailViewModel
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.createAlertRepository
import crocalert.app.shared.createCameraRepository
import crocalert.app.shared.createSiteRepository
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.components.BottomNavBar
import crocalert.app.ui.dashboard.DashboardTab
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
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
            alertId,
            createAlertRepository(),
            createCameraRepository(),
            createSiteRepository()
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFullImage by remember { mutableStateOf(false) }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DETALLE ALERTA", fontWeight = FontWeight.Bold, color = CrocBlue) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (showFullImage) showFullImage = false else onBack()
                        }) {
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
                        onShowFullImage = { showFullImage = true },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }

        // Fullscreen overlay — outside Scaffold so it covers top/bottom bars
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
                val alert = (uiState as? AlertDetailUiState.Success)?.alert
                alert?.thumbnailUrl?.let { url ->
                    KamelImage(
                        resource = asyncPainterResource(url.toDirectDriveImageUrl()),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onLoading = { progress ->
                            CircularProgressIndicator(
                                progress = { progress },
                                color = CrocWhite,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        },
                        onFailure = {
                            Icon(
                                Icons.Outlined.BrokenImage,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertDetailContent(
    alert: Alert,
    onShowFullImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    // P27: drive badge from priority enum, not fragile folder string
    val badgeLabel = when (alert.priority) {
        AlertPriority.CRITICAL -> "Crítico"
        AlertPriority.HIGH -> "Alerta"
        AlertPriority.MEDIUM -> "Pre-Alerta"
        AlertPriority.LOW -> "Info"
    }

    val badgeColor = when (alert.priority) {
        AlertPriority.CRITICAL -> MaterialTheme.colorScheme.error
        AlertPriority.HIGH -> CrocAmber
        AlertPriority.MEDIUM -> CrocBlue
        AlertPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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

                alert.thumbnailUrl?.let { url ->
                    KamelImage(
                        resource = asyncPainterResource(url.toDirectDriveImageUrl()),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onShowFullImage() },
                        onLoading = { progress ->
                            CircularProgressIndicator(
                                progress = { progress },
                                color = CrocBlue,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        },
                        onFailure = {
                            Icon(
                                Icons.Outlined.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Row 1: Estado (left) + priority badge (right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SmallLabel("Estado")
                        ValueText(alert.status.toSpanish())
                    }
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

                // Row 2: Detectado full width
                Column {
                    SmallLabel("Detectado")
                    ValueText(alert.createdAt.toDisplayDateTime())
                }

                if (!alert.notes.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    SmallLabel("Notas")
                    Text(alert.notes!!)
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

// P28: restored zero-padding and year
private fun Long.toDisplayDateTime(): String {
    val dt = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    val h = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "$d/$m/${dt.year} $h:$min"
}

private fun String.toDirectDriveImageUrl(): String {
    val match = Regex("/d/([a-zA-Z0-9_-]+)").find(this)
    val fileId = match?.groupValues?.get(1)
    return if (fileId != null) "https://drive.google.com/uc?export=view&id=$fileId" else this
}
