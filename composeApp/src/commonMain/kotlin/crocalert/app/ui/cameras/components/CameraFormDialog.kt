package crocalert.app.ui.cameras.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import crocalert.app.model.Camera
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.cameras.DEFAULT_EXPECTED_PER_DAY
import crocalert.app.theme.CrocBlueLight
import crocalert.app.theme.CrocWhite
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private const val MIN_EXPECTED = 1
private const val MAX_EXPECTED = 48
private val DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

// ── Date helpers ──────────────────────────────────────────────────────────────

private fun Long.toDateString(): String {
    val dt = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "%04d-%02d-%02d".format(dt.year, dt.monthNumber, dt.dayOfMonth)
}

private fun String.toEpochMs(): Long? = runCatching {
    val parts = trim().split("-")
    if (parts.size != 3) return null
    val date = LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}.getOrNull()

// ── Dialog ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraFormDialog(
    cameraToEdit: Camera,
    isSaving: Boolean,
    error: String?,
    onSave: (
        name: String,
        isActive: Boolean,
        siteId: String?,
        expectedImages: Int?,
        createdAtMs: Long?,
        installedAtMs: Long?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var name           by remember(cameraToEdit) { mutableStateOf(cameraToEdit.name) }
    var isActive       by remember(cameraToEdit) { mutableStateOf(cameraToEdit.isActive) }
    var siteId         by remember(cameraToEdit) { mutableStateOf(cameraToEdit.siteId ?: "") }
    var expectedImages by remember(cameraToEdit) {
        mutableIntStateOf(
            (cameraToEdit.expectedImages?.takeIf { it > 0 } ?: DEFAULT_EXPECTED_PER_DAY)
                .coerceIn(MIN_EXPECTED, MAX_EXPECTED)
        )
    }
    var expectedImagesText by remember(cameraToEdit) {
        mutableStateOf(
            (cameraToEdit.expectedImages?.takeIf { it > 0 } ?: DEFAULT_EXPECTED_PER_DAY)
                .coerceIn(MIN_EXPECTED, MAX_EXPECTED).toString()
        )
    }
    var installedAt    by remember(cameraToEdit) { mutableStateOf(cameraToEdit.installedAt?.toDateString() ?: "") }

    // ── Validation errors ─────────────────────────────────────────────────────
    var nameError        by remember { mutableStateOf<String?>(null) }
    var installedAtError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var ok = true
        nameError = if (name.trim().isBlank()) {
            ok = false; "El nombre es obligatorio"
        } else null
        installedAtError = if (installedAt.isNotBlank() && !DATE_REGEX.matches(installedAt.trim())) {
            ok = false; "Formato inválido. Use YYYY-MM-DD"
        } else null
        return ok
    }

    BasicAlertDialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Title ──────────────────────────────────────────────────
                Text(
                    text = "Editar cámara",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CrocBlue,
                )

                Spacer(Modifier.height(20.dp))

                // ── ID (read-only) ─────────────────────────────────────────
                OutlinedTextField(
                    value = cameraToEdit.id,
                    onValueChange = {},
                    label = { Text("ID (no editable)") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(Modifier.height(12.dp))

                // ── Name ───────────────────────────────────────────────────
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = nameError != null,
                    supportingText = nameError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(Modifier.height(12.dp))

                // ── Site ID ────────────────────────────────────────────────
                OutlinedTextField(
                    value = siteId,
                    onValueChange = { siteId = it },
                    label = { Text("ID de sitio (ej: /site/CONCHAL-001)") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(Modifier.height(12.dp))

                // ── Installed At ───────────────────────────────────────────
                OutlinedTextField(
                    value = installedAt,
                    onValueChange = { installedAt = it; installedAtError = null },
                    label = { Text("Fecha de instalación") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = installedAtError != null,
                    supportingText = installedAtError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(Modifier.height(16.dp))

                // ── Expected Images input ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Imágenes esperadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    BasicTextField(
                        value = expectedImagesText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() }.take(2)
                            expectedImagesText = filtered
                            filtered.toIntOrNull()?.coerceIn(MIN_EXPECTED, MAX_EXPECTED)?.let {
                                expectedImages = it
                            }
                        },
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .background(CrocBlueLight.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .width(56.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                innerTextField()
                            }
                        },
                    )
                }

                // ── Active toggle ──────────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Cámara activa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = isActive,
                            onCheckedChange = { if (!isSaving) isActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CrocWhite, checkedTrackColor = CrocBlue),
                        )
                    }

                // ── Server / remote error ──────────────────────────────────
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Buttons ────────────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (!validate()) return@Button
                            onSave(
                                name,
                                isActive,
                                siteId.trim().ifBlank { null },
                                expectedImages,
                                cameraToEdit.createdAt,
                                installedAt.trim().toEpochMs(),
                            )
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = CrocBlue),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CrocWhite, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(text = "Guardar", color = CrocWhite)
                    }
                }
            }
        }
    }
}
