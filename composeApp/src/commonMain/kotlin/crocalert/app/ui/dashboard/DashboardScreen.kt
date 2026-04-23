package crocalert.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.feature.alerts.ui.AlertListScreen
import crocalert.app.feature.alerts.ui.components.AlertListItem
import crocalert.app.shared.UserSession
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.cameras.CamerasScreen
import crocalert.app.ui.components.BottomNavBar
import crocalert.app.ui.components.StatCard
import crocalert.app.ui.components.SyncBanner
import crocalert.app.ui.profile.ProfileScreen
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DashboardScreen(
    onAlertClick: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel { DashboardViewModel() },
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lastSynced by viewModel.lastSynced.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val visibleTabs = if (UserSession.isAdmin) {
        DashboardTab.entries
    } else {
        listOf(DashboardTab.Home, DashboardTab.Alerts, DashboardTab.Profile)
    }

    val effectiveTab = if (selectedTab in visibleTabs) selectedTab else DashboardTab.Home

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selected = effectiveTab,
                onSelect = viewModel::selectTab,
                visibleTabs = visibleTabs,
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SyncBanner(
                status = syncStatus,
                lastUpdated = lastSynced,
                onRefresh = viewModel::retry
            )
            when (val state = uiState) {
                is DashboardUiState.Loading -> LoadingContent()
                is DashboardUiState.Error -> ErrorContent(state.message, onRetry = viewModel::retry)
                is DashboardUiState.Success -> when (effectiveTab) {
                    DashboardTab.Home -> Box {
                        DashboardContent(
                            data = state.data,
                            activeFilter = activeFilter,
                            onAlertClick = onAlertClick,
                            onFilterSelected = viewModel::setFilter,
                        )
                        if (isRefreshing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = CrocBlue)
                            }
                        }
                    }
                    DashboardTab.Cameras -> CamerasScreen()
                    DashboardTab.Alerts -> AlertListScreen(onAlertClick = onAlertClick)
                    DashboardTab.Profile -> ProfileScreen(onLogout = onLogout)
                }
            }
        }
    }

}

// ── State renderers ───────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Sin datos disponibles",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CrocBlue),
        ) {
            Text("Reintentar", fontWeight = FontWeight.SemiBold)
        }
    }
}

private val PRESET_FILTERS = listOf(
    DashboardFilter.LastDays(1) to "Hoy",
    DashboardFilter.LastDays(7) to "7 días",
    DashboardFilter.LastDays(30) to "30 días",
)

@Composable
private fun DashboardContent(
    data: DashboardData,
    activeFilter: DashboardFilter,
    onAlertClick: (String) -> Unit,
    onFilterSelected: (DashboardFilter) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HeaderSection(activeFilter, onFilterSelected) }
        item { StatsGridSection(data, activeFilter) }
        item { RecentActivitySection(data, onAlertClick = onAlertClick) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Sections ─────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(
    activeFilter: DashboardFilter,
    onFilterSelected: (DashboardFilter) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = "PANEL",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = CrocBlue
        )
        val nameLabel = UserSession.fullName.ifBlank { UserSession.email.substringBefore("@") }
        Text(
            text = "$nameLabel · Reserva Conchal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (UserSession.isAdmin) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESET_FILTERS.forEach { (filter, label) ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrocBlue,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGridSection(data: DashboardData, activeFilter: DashboardFilter) {
    val windowLabel = when (val f = activeFilter) {
        is DashboardFilter.LastDays -> if (f.days == 1) "Hoy" else "Últimos ${f.days}d"
        is DashboardFilter.Custom -> "${formatDateMs(f.startMs)} – ${formatDateMs(f.endMs)}"
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Row 1 — Alertas + Pre-alertas
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                badge = "${data.activeAlertas}",
                badgeColor = CrocAmber,
                label = "Alertas",
                value = "${data.activeAlertas}",
                subtitle = windowLabel,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                badge = "${data.activePreAlertas}",
                badgeColor = CrocBlue,
                label = "Pre-alertas",
                value = "${data.activePreAlertas}",
                subtitle = windowLabel,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Row 2 — Cámaras activas + Tasa de captura
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                badge = "${data.activeCameras}",
                badgeColor = CrocBlue,
                label = "Cámaras activas",
                value = "${data.networkHealthPct.coerceIn(0f, 100f).toInt()}%",
                subtitle = windowLabel,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                badge = "${data.captureRatePct.coerceIn(0f, 100f).toInt()}%",
                badgeColor = CrocBlue,
                label = "Tasa de captura",
                value = data.captureRate,
                subtitle = windowLabel,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun RecentActivitySection(data: DashboardData, onAlertClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Actividad reciente",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (data.recentActivity.isEmpty()) {
            Text(
                text = "Sin actividad reciente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            data.recentActivity.forEachIndexed { index, alert ->
                AlertListItem(
                    alert = alert,
                    onClick = { onAlertClick(alert.id) },
                )
                if (index < data.recentActivity.lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDateMs(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.dayOfMonth}"
}
