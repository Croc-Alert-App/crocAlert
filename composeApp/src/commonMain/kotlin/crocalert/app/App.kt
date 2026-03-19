package crocalert.app

import androidx.compose.runtime.Composable
import crocalert.app.ui.dashboard.DashboardScreen

@Composable
fun App() {
    //AlertsScreen()
    AlertHistoryScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsScreen() {
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(emptyList<Alert>()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            isLoading = true
            statusMessage = "Loading…"
            try {
                val result: List<Alert> = fetchAlerts()
                alerts = result
                statusMessage = "${result.size} alert(s) loaded"
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrocAlert") },
                actions = {
                    Button(
                        onClick = { load() },
                        enabled = !isLoading,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (alerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No alerts found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = alerts, key = { alert -> alert.id }) { alert ->
                        AlertCard(alert = alert)
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
