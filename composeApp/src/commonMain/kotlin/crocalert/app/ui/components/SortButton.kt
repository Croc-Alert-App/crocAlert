package crocalert.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocWhite

/**
 * Shared sort-direction toggle button used across Alerts and Cameras screens.
 *
 * Shows the CURRENT sort direction so the user knows what the list is sorted by.
 * Clicking switches to the opposite direction.
 *
 * [descending] = true → newest/highest first (↓ Reciente)
 * [descending] = false → oldest/lowest first (↑ Antiguo)
 */
@Composable
fun SortButton(
    descending: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onToggle,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = CrocBlue),
    ) {
        Icon(
            imageVector = if (descending) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
            contentDescription = if (descending) "Más reciente primero" else "Más antiguo primero",
            modifier = Modifier.size(14.dp),
            tint = CrocWhite,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (descending) "Reciente" else "Antiguo",
            style = MaterialTheme.typography.labelSmall,
            color = CrocWhite,
        )
    }
}
