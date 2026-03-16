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
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralLight
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.cameras.components.CameraCard
import crocalert.app.ui.components.EmptyStateView

@Composable
fun CamerasScreen(viewModel: CamerasViewModel = viewModel { CamerasViewModel() }) {
    val filteredCameras by viewModel.filteredCameras.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val statusCounts by viewModel.statusCounts.collectAsState()

    val hasActiveFilter = searchQuery.isNotBlank() || selectedFilter != CameraFilter.All

    Column(modifier = Modifier.fillMaxSize()) {
        // — Static header (scrolls with content via LazyColumn below) ——————————
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "CÁMARAS",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = CrocBlue,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )
            CameraSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchChange,
            )
            Spacer(Modifier.height(8.dp))
            CameraSortFilterRow()
            Spacer(Modifier.height(8.dp))
            CameraStatusFilterRow(
                selected = selectedFilter,
                statusCounts = statusCounts,
                onSelect = viewModel::onFilterSelect,
            )
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
                    CameraCard(camera = camera)
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
private fun CameraSortFilterRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = {},
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Outlined.Sort,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(text = "Orden: Severidad", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
            onClick = {},
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(text = "Filtrado", style = MaterialTheme.typography.labelMedium)
        }
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
