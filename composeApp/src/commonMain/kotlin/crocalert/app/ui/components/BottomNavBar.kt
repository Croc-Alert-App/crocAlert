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

enum class DashboardTab { Home, Cameras, Alerts, Profile }

@Composable
fun BottomNavBar(
    selected: DashboardTab,
    onSelect: (DashboardTab) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = selected == DashboardTab.Home,
            onClick = { onSelect(DashboardTab.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CrocBlue,
                selectedTextColor = CrocBlue
            )
        )
        NavigationBarItem(
            selected = selected == DashboardTab.Cameras,
            onClick = { onSelect(DashboardTab.Cameras) },
            icon = { Icon(Icons.Default.Videocam, contentDescription = "Cámaras") },
            label = { Text("Cámaras") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CrocBlue,
                selectedTextColor = CrocBlue
            )
        )
        NavigationBarItem(
            selected = selected == DashboardTab.Alerts,
            onClick = { onSelect(DashboardTab.Alerts) },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Alertas") },
            label = { Text("Alertas") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CrocBlue,
                selectedTextColor = CrocBlue
            )
        )
        NavigationBarItem(
            selected = selected == DashboardTab.Profile,
            onClick = { onSelect(DashboardTab.Profile) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CrocBlue,
                selectedTextColor = CrocBlue
            )
        )
    }
}
