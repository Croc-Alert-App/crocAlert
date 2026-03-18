package crocalert.app

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.shared.createAlertRepository
import crocalert.app.shared.network.ApiRoutes

@Composable
fun App(baseUrl: String = ApiRoutes.DEFAULT_BASE) {
    AlertsScreen(baseUrl = baseUrl)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsScreen(baseUrl: String) {
    val repository = remember { createAlertRepository(baseUrl = baseUrl) }
    val alerts by repository.observeAlerts().collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("CrocAlert") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when {
                alerts == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                alerts!!.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No alerts found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = alerts!!, key = { it.id }) { alert ->
                            AlertCard(alert = alert)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: Alert) {
    val containerColor = when (alert.priority) {
        AlertPriority.HIGH, AlertPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        AlertPriority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
        AlertPriority.LOW -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = alert.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Priority: ${alert.priority.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            alert.assignedToUserId?.let { userId ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Assigned: $userId", style = MaterialTheme.typography.bodySmall)
            }

            alert.notes?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(text = notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
