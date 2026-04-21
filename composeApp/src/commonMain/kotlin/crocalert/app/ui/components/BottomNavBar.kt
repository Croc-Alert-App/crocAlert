package crocalert.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.dashboard.DashboardTab

@Composable
fun BottomNavBar(
    selected: DashboardTab,
    onSelect: (DashboardTab) -> Unit,
    visibleTabs: List<DashboardTab> = DashboardTab.entries,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = CrocBlue,
        selectedTextColor = CrocBlue,
        indicatorColor = CrocBlue.copy(alpha = 0.12f),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        if (DashboardTab.Home in visibleTabs) {
            NavigationBarItem(
                selected = selected == DashboardTab.Home,
                onClick = { onSelect(DashboardTab.Home) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                label = { Text("Inicio") },
                colors = itemColors
            )
        }
        if (DashboardTab.Cameras in visibleTabs) {
            NavigationBarItem(
                selected = selected == DashboardTab.Cameras,
                onClick = { onSelect(DashboardTab.Cameras) },
                icon = { Icon(Icons.Default.Videocam, contentDescription = "Cámaras") },
                label = { Text("Cámaras") },
                colors = itemColors
            )
        }
        if (DashboardTab.Alerts in visibleTabs) {
            NavigationBarItem(
                selected = selected == DashboardTab.Alerts,
                onClick = { onSelect(DashboardTab.Alerts) },
                icon = { Icon(Icons.Default.Notifications, contentDescription = "Alertas") },
                label = { Text("Alertas") },
                colors = itemColors
            )
        }
        if (DashboardTab.Profile in visibleTabs) {
            NavigationBarItem(
                selected = selected == DashboardTab.Profile,
                onClick = { onSelect(DashboardTab.Profile) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                label = { Text("Perfil") },
                colors = itemColors
            )
        }
    }
}
