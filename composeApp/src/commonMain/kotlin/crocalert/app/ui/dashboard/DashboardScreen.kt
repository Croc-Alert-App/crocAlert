package crocalert.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.components.BottomNavBar
import crocalert.app.ui.components.DashboardTab
import crocalert.app.ui.components.StatCard
import crocalert.app.ui.components.SyncBanner
import crocalert.app.ui.components.SyncStatus

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel { DashboardViewModel() }) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lastSynced by viewModel.lastSynced.collectAsState()

    Scaffold(
        bottomBar = { BottomNavBar(selected = selectedTab, onSelect = viewModel::selectTab) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SyncBanner(
                status = syncStatus,
                lastUpdated = lastSynced,
                onRetry = viewModel::retry
            )
            when (val state = uiState) {
                is DashboardUiState.Loading -> LoadingContent()
                is DashboardUiState.Error -> ErrorContent(state.message)
                is DashboardUiState.Success -> when (selectedTab) {
                    DashboardTab.Home -> DashboardContent(state.data)
                    DashboardTab.Cameras -> PlaceholderScreen("Cámaras")
                    DashboardTab.Alerts -> PlaceholderScreen("Alertas")
                    DashboardTab.Profile -> PlaceholderScreen("Perfil")
                }
            }
        }
    }
}

// — State renderers ———————————————————————————————————————

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun DashboardContent(data: DashboardData) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HeaderSection(data) }
        item { StatsGridSection(data) }
        item { NetworkTrendSection(data) }
        item { MetadataQualitySection(data) }
        item { RecentActivitySection(data) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// — Sections ——————————————————————————————————————————————

@Composable
private fun HeaderSection(data: DashboardData) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = "PANEL",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = CrocBlue
        )
        Text(
            text = "Trabajador del SINAC · Región de Tárcoles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsGridSection(data: DashboardData) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                badge = "${data.activeCameras}",
                badgeColor = CrocBlue,
                label = "Cámaras activas",
                value = "${(data.networkHealthPct * 100).toInt()}%",
                subtitle = "Salud",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                badge = "${data.activeAlerts}",
                badgeColor = CrocAmber,
                label = "Alertas activas",
                value = "${data.criticalAlerts}",
                subtitle = "Crítico",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                badge = "${(data.captureRatePct * 100).toInt()}%",
                badgeColor = CrocBlue,
                label = "Tasa de captura",
                value = data.captureRate,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                badge = "OK",
                badgeColor = CrocBlue,
                label = "Integridad",
                value = "${(data.integrityPct * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun NetworkTrendSection(data: DashboardData) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tendencia de la red (7d)",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrocBlue
                )
                TextButton(onClick = {}, enabled = false) {
                    Text(
                        text = "Vista de experto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            NetworkBarChart(
                data = data.networkTrend,
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.networkTrend.forEach { day ->
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.isToday) CrocBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun MetadataQualitySection(data: DashboardData) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Índice de calidad de metadatos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            data.metadataMetrics.forEachIndexed { index, metric ->
                MetricProgressRow(metric = metric)
                if (index < data.metadataMetrics.lastIndex) Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun RecentActivitySection(data: DashboardData) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Actividad reciente",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        data.recentActivity.forEachIndexed { index, event ->
            ActivityEventCard(event = event)
            if (index < data.recentActivity.lastIndex) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
