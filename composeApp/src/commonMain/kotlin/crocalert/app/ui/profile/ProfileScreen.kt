package crocalert.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocWhite

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel { ProfileViewModel() }) {
    val prefs by viewModel.syncPreferences.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "PERFIL",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = CrocBlue,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Trabajador del SINAC · Región de Tárcoles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        // ── Sync settings section ──────────────────────────────────────────────
        Text(
            text = "Configuración de sincronización",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tiempo mínimo entre sincronizaciones automáticas. Valores menores actualizan más seguido pero consumen más datos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TtlRow(
                    label = "Alertas y pre-alertas",
                    description = "Frecuencia de sincronización de imágenes detectadas",
                    value = prefs.alertsTtlMinutes,
                    onDecrement = { viewModel.setAlertsTtl(prefs.alertsTtlMinutes - 1) },
                    onIncrement = { viewModel.setAlertsTtl(prefs.alertsTtlMinutes + 1) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                TtlRow(
                    label = "Cámaras",
                    description = "Frecuencia de sincronización del estado de cámaras",
                    value = prefs.camerasTtlMinutes,
                    onDecrement = { viewModel.setCamerasTtl(prefs.camerasTtlMinutes - 1) },
                    onIncrement = { viewModel.setCamerasTtl(prefs.camerasTtlMinutes + 1) },
                )
            }
        }
    }
}

@Composable
private fun TtlRow(
    label: String,
    description: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = onDecrement,
                modifier = Modifier.size(28.dp),
                enabled = value > 1,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CrocNeutralDark),
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrocWhite)
            }
            Text(
                text = "$value min",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CrocBlue,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            FilledIconButton(
                onClick = onIncrement,
                modifier = Modifier.size(28.dp),
                enabled = value < 120,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CrocNeutralDark),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrocWhite)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (value == 1) "Cada minuto" else "Cada $value minutos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
