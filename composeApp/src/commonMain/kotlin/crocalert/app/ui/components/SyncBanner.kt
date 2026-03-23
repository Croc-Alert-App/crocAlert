package crocalert.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.dashboard.SyncStatus

@Composable
fun SyncBanner(
    status: SyncStatus,
    lastUpdated: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        SyncStatus.Syncing, SyncStatus.Synced -> CrocBlue
        SyncStatus.Error -> MaterialTheme.colorScheme.error
    }
    val message = when (status) {
        SyncStatus.Syncing -> if (lastUpdated.isBlank()) "Sincronizando..."
                              else "Sincronizando... Última actualización $lastUpdated"
        SyncStatus.Synced  -> "Última actualización $lastUpdated"
        SyncStatus.Error   -> if (lastUpdated.isBlank()) "Error de sincronización"
                              else "Error de sincronización · Última actualización $lastUpdated"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = CrocWhite,
            modifier = Modifier.weight(1f)
        )
        if (status == SyncStatus.Error) {
            TextButton(onClick = onRefresh) {
                Text(
                    text = "Reintentar",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrocWhite
                )
            }
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Sincronizar ahora",
                tint = CrocWhite,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
