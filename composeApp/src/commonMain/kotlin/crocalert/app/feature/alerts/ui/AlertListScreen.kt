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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.shared.AppModule
import crocalert.app.feature.alerts.presentation.AlertFilter
import crocalert.app.feature.alerts.presentation.AlertsUiState
import crocalert.app.feature.alerts.presentation.AlertsViewModel
import crocalert.app.feature.alerts.presentation.DateRange
import crocalert.app.feature.alerts.presentation.SortDirection
import crocalert.app.feature.alerts.ui.components.AlertDateRangePickerDialog
import crocalert.app.feature.alerts.ui.components.AlertListItem
import crocalert.app.feature.alerts.ui.components.ErrorState
import crocalert.app.feature.alerts.ui.components.LoadingState
import crocalert.app.ui.components.SortButton
import crocalert.app.model.Alert
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlack
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralLight
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.components.EmptyStateView
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Tab definition ────────────────────────────────────────────────────────────

private enum class AlertTab(val label: String, val folder: String) {
    PRE_ALERTS("Pre-Alertas", "pre-alertas"),
    ALERTS("Alertas", "alertas"),
}

private fun List<Alert>.forTab(tab: AlertTab): List<Alert> =
    filter { it.folder == tab.folder }

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    onAlertClick: (String) -> Unit = {},
    viewModel: AlertsViewModel = viewModel { AlertsViewModel(AppModule.provideAlertRepository()) },
) {

    val uiState by viewModel.uiState.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val customRange by viewModel.customRange.collectAsState()

    var activeTab by remember { mutableStateOf(AlertTab.PRE_ALERTS) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to top whenever the sort direction changes
    LaunchedEffect(sortDirection) {
        scope.launch { listState.scrollToItem(0) }
    }

    val successAlerts = (uiState as? AlertsUiState.Success)?.alerts
    val unreadCount = successAlerts?.count { !it.isRead } ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ALERTAS",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = CrocBlue,
                    modifier = Modifier.weight(1f),
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
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
                    Spacer(Modifier.width(4.dp))
                }
            }

            // ── Preset date filter chips ───────────────────────────────────
            AlertPresetFilterChips(
                activeFilter = activeFilter,
                onFilterSelected = viewModel::setFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )

            // ── Custom date range bar + sort button ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CustomRangeBar(
                    isActive = activeFilter == AlertFilter.CUSTOM,
                    customRange = customRange,
                    onOpenPicker = { showDateRangePicker = true },
                    onClear = { viewModel.setFilter(AlertFilter.ALL) },
                    modifier = Modifier.weight(1f),
                )
                SortButton(
                    descending = sortDirection == SortDirection.DESC,
                    onToggle = viewModel::toggleSort,
                )
            }
        }

        HorizontalDivider()

        // ── Tabs ──────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = AlertTab.entries.indexOf(activeTab)) {
            AlertTab.entries.forEach { tab ->
                val count = successAlerts?.forTab(tab)?.size ?: 0
                Tab(
                    selected = tab == activeTab,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            text = if (count > 0) "${tab.label} ($count)" else tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        }

        // ── Content ───────────────────────────────────────────────────────
        when (val state = uiState) {
            is AlertsUiState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
            is AlertsUiState.Empty -> EmptyStateView(
                icon = Icons.Outlined.Notifications,
                title = "Sin ${activeTab.label.lowercase()}",
                subtitle = state.message,
            )
            is AlertsUiState.Error -> ErrorState(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize(),
            )
            is AlertsUiState.Success -> {
                val tabAlerts = state.alerts.forTab(activeTab)
                if (tabAlerts.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Outlined.Notifications,
                        title = "Sin ${activeTab.label.lowercase()}",
                        subtitle = "No hay ${activeTab.label.lowercase()} para este período.",
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(tabAlerts, key = { it.id }) { alert ->
                            AlertListItem(alert = alert, onClick = { onAlertClick(alert.id) })
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    // ── Date range picker dialog ───────────────────────────────────────────
    if (showDateRangePicker) {
        AlertDateRangePickerDialog(
            initialRange = customRange,
            onRangeSelected = { start, end ->
                viewModel.setCustomRange(start, end)
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false },
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun AlertPresetFilterChips(
    activeFilter: AlertFilter,
    onFilterSelected: (AlertFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = remember { AlertFilter.entries.filter { it != AlertFilter.CUSTOM } }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { filter ->
            if (filter == activeFilter) {
                Button(
                    onClick = { onFilterSelected(filter) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CrocBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(text = filter.label, style = MaterialTheme.typography.labelMedium, color = CrocWhite)
                }
            } else {
                OutlinedButton(
                    onClick = { onFilterSelected(filter) },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeBar(
    isActive: Boolean,
    customRange: DateRange?,
    onOpenPicker: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isActive && customRange != null) {
        Surface(
            onClick = onOpenPicker,
            shape = RoundedCornerShape(8.dp),
            color = CrocBlue,
            modifier = modifier,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = CrocWhite,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${formatDateMs(customRange.startMs)} – ${formatDateMs(customRange.endMs)}",
                    color = CrocWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onOpenPicker,
                    colors = ButtonDefaults.textButtonColors(contentColor = CrocWhite),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text("Editar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = CrocWhite),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text("× Limpiar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = onOpenPicker,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = null,
                tint = CrocBlue,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Personalizado",
                style = MaterialTheme.typography.bodySmall,
                color = CrocBlue,
            )
        }
    }
}

private fun formatDateMs(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${date.dayOfMonth}"
}
