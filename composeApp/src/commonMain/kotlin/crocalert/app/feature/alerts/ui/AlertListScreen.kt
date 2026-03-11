package crocalert.app.feature.alerts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import crocalert.app.feature.alerts.data.MockAlertRepository
import crocalert.app.feature.alerts.presentation.AlertFilter
import crocalert.app.feature.alerts.presentation.AlertsUiState
import crocalert.app.feature.alerts.presentation.AlertsViewModel
import crocalert.app.feature.alerts.ui.components.AlertListItem
import crocalert.app.feature.alerts.ui.components.EmptyState
import crocalert.app.feature.alerts.ui.components.ErrorState
import crocalert.app.feature.alerts.ui.components.LoadingState
import crocalert.app.model.Alert
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlack
import crocalert.app.theme.CrocWhite

/**
 * Entry-point composable for the Alerts List feature.
 *
 * The [viewModel] parameter defaults to a [MockAlertRepository]-backed instance
 * so the screen works out-of-the-box in Phase 1. When Koin DI is wired up,
 * replace the default with: `viewModel: AlertsViewModel = koinInject()`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    viewModel: AlertsViewModel = remember { AlertsViewModel(MockAlertRepository()) },
) {
    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }

    val uiState by viewModel.uiState.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()

    val unreadCount = (uiState as? AlertsUiState.Success)
        ?.alerts?.count { !it.isRead } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Alert Panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CrocAmber),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrocBlack,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = CrocWhite,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AlertFilterChips(
                activeFilter = activeFilter,
                onFilterSelected = viewModel::setFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when (val state = uiState) {
                is AlertsUiState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
                is AlertsUiState.Empty -> EmptyState(modifier = Modifier.fillMaxSize())
                is AlertsUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize(),
                )
                is AlertsUiState.Success -> AlertList(
                    alerts = state.alerts,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Scrollable row of [FilterChip]s that represent the available [AlertFilter] options.
 * Selecting a chip calls [onFilterSelected] so the ViewModel can update the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertFilterChips(
    activeFilter: AlertFilter,
    onFilterSelected: (AlertFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlertFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == activeFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = CrocWhite,
                ),
            )
        }
    }
}

/**
 * Renders the sorted list of [Alert] items inside a [LazyColumn].
 * Each item is keyed by [Alert.id] for efficient recomposition.
 */
@Composable
private fun AlertList(
    alerts: List<Alert>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = alerts, key = { it.id }) { alert ->
            AlertListItem(alert = alert)
        }
    }
}
