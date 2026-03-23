package crocalert.app.ui.cameras

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralLight
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.cameras.components.CameraCard
import crocalert.app.ui.cameras.components.CameraFormDialog
import crocalert.app.ui.cameras.components.DeletedCameraCard
import crocalert.app.ui.components.EmptyStateView

@Composable
fun CamerasScreen(viewModel: CamerasViewModel = viewModel { CamerasViewModel() }) {
    val historyCamera by viewModel.historyCamera.collectAsState()
    val showCameraForm by viewModel.showCameraForm.collectAsState()
    val cameraToEdit by viewModel.cameraToEdit.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    // Dialog is shown over whichever sub-screen is active (list or history)
    val camera = cameraToEdit
    if (showCameraForm && camera != null) {
        CameraFormDialog(
            cameraToEdit = camera,
            isSaving = isSaving,
            error = saveError,
            onSave = viewModel::saveCamera,
            onDismiss = viewModel::dismissCameraForm,
        )
    }

    historyCamera?.let { camera ->
        CameraHistoryScreen(
            cameraId = camera.id,
            cameraName = camera.name,
            onBack = viewModel::closeHistory,
            onEditClick = viewModel::openEditCamera,
        )
        return
    }

    val filteredCameras by viewModel.filteredCameras.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val visibilityFilter by viewModel.visibilityFilter.collectAsState()
    val statusCounts by viewModel.statusCounts.collectAsState()
    val expandedCameraId by viewModel.expandedCameraId.collectAsState()
    val sortDescending by viewModel.sortDescending.collectAsState()

    val hasActiveFilter = searchQuery.isNotBlank() ||
        selectedFilter != CameraFilter.All ||
        visibilityFilter != VisibilityFilter.Active

    Column(modifier = Modifier.fillMaxSize()) {
        // — Static header ——————————————————————————————————————————————————————
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CÁMARAS",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = CrocBlue,
                )
            }
            CameraSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchChange,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CameraVisibilityDropdown(
                    selected = visibilityFilter,
                    onSelect = viewModel::onVisibilityFilterSelect,
                    modifier = Modifier.weight(1f),
                )
                CameraSortButton(
                    descending = sortDescending,
                    onToggle = viewModel::toggleSort,
                    modifier = Modifier.weight(1f),
                )
            }
            if (visibilityFilter != VisibilityFilter.Deleted) {
                Spacer(Modifier.height(8.dp))
                CameraStatusFilterRow(
                    selected = selectedFilter,
                    statusCounts = statusCounts,
                    onSelect = viewModel::onFilterSelect,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // — Content area ———————————————————————————————————————————————————————
        if (filteredCameras.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Videocam,
                title = if (hasActiveFilter) "No se encontraron cámaras"
                        else "No hay cámaras registradas",
                subtitle = when {
                    searchQuery.isNotBlank() ->
                        "No hay resultados que coincidan con \"$searchQuery\". " +
                        "Prueba con otro ID o nombre de ubicación."
                    selectedFilter != CameraFilter.All ->
                        "No hay cámaras con estado \"${selectedFilter.label}\"."
                    else ->
                        "Aún no se han añadido cámaras al sistema."
                },
                actionLabel = if (hasActiveFilter) "Borrar búsqueda" else null,
                onAction = if (hasActiveFilter) viewModel::clearSearch else null,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredCameras, key = { it.id }) { camera ->
                    if (!camera.isActive) {
                        DeletedCameraCard(
                            camera = camera,
                            onActivateClick = { viewModel.activateCamera(camera.id) },
                        )
                    } else {
                        CameraCard(
                            camera = camera,
                            expanded = expandedCameraId == camera.id,
                            onToggle = { viewModel.toggleExpand(camera.id) },
                            onHistoryClick = { viewModel.openHistory(camera) },
                            onDeleteClick = { viewModel.deleteCamera(camera.id) },
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

}

// — Private sub-composables ————————————————————————————————————————————————————

@Composable
private fun CameraSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Buscar ID o ubicación...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = CrocBlue,
        ),
    )
}

@Composable
private fun CameraVisibilityDropdown(
    selected: VisibilityFilter,
    onSelect: (VisibilityFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, CrocNeutralLight),
        ) {
            Text(
                text = "${selected.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VisibilityFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = { onSelect(filter); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CameraSortButton(
    descending: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onToggle,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = CrocBlue),
    ) {
        Text(
            text = "Orden",
            style = MaterialTheme.typography.labelMedium,
            color = CrocWhite,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = if (descending) Icons.Outlined.ArrowDownward
                          else Icons.Outlined.ArrowUpward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = CrocWhite,
        )
    }
}

@Composable
private fun CameraStatusFilterRow(
    selected: CameraFilter,
    statusCounts: Map<CameraStatus, Int>,
    onSelect: (CameraFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "Todos" chip — no count label
        CameraFilterChip(
            label = CameraFilter.All.label,
            count = null,
            selected = selected == CameraFilter.All,
            onClick = { onSelect(CameraFilter.All) },
        )
        // Status chips with counts derived from full list
        CameraFilter.entries.filter { it != CameraFilter.All }.forEach { filter ->
            val count = filter.correspondingStatus?.let { statusCounts[it] } ?: 0
            CameraFilterChip(
                label = filter.label,
                count = count,
                selected = selected == filter,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun CameraFilterChip(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val text = if (count != null) "$label ($count)" else label
    if (selected) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = CrocBlue),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = CrocWhite)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = CircleShape,
            border = BorderStroke(1.dp, CrocNeutralLight),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
