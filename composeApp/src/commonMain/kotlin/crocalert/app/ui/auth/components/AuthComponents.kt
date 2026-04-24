package crocalert.app.ui.auth.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocBlueVibrant
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocNeutralLight
import crocalert.app.theme.CrocWhite

// ── Layout scaffold ──────────────────────────────────────────────────────────

@Composable
fun AuthScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CrocWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
        AuthFooterText(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

// ── Text inputs ───────────────────────────────────────────────────────────────

@Composable
fun CrocAlertTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = CrocNeutralDark) },
            singleLine = true,
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = authFieldColors(),
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

@Composable
fun CrocAlertPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "••••••••",
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = CrocNeutralDark) },
            singleLine = true,
            isError = isError,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = CrocNeutralDark,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = authFieldColors(),
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrocAlertDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String = "Select your role",
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                isError = isError,
                placeholder = { Text(placeholder, color = CrocNeutralDark) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                colors = authFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

// ── Buttons ───────────────────────────────────────────────────────────────────

@Composable
fun CrocAlertPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CrocBlue,
            contentColor = CrocWhite,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun CrocAlertSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrocBlue),
        border = BorderStroke(1.5.dp, CrocBlue),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// ── OTP input ─────────────────────────────────────────────────────────────────

@Composable
fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = { new ->
            if (new.length <= 6 && new.all { it.isDigit() }) onValueChange(new)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box {
                // Hidden — keeps keyboard/focus functional without showing cursor
                Box(Modifier.size(0.dp)) { innerTextField() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    repeat(6) { index ->
                        val char = value.getOrNull(index)?.toString() ?: ""
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = char,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrocBlue,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(
                                modifier = Modifier.width(36.dp),
                                thickness = 2.dp,
                                color = if (index < value.length) CrocBlue else CrocNeutralLight,
                            )
                        }
                    }
                }
            }
        },
    )
}

// ── Misc ──────────────────────────────────────────────────────────────────────

@Composable
fun AuthDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = CrocNeutralLight)
        Text(
            text = "O",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = CrocNeutralDark,
            fontSize = 14.sp,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = CrocNeutralLight)
    }
}

@Composable
fun AuthFooterText(modifier: Modifier = Modifier) {
    Text(
        text = "Protegido por la política de seguridad de Croc Alert",
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        color = CrocNeutralDark,
    )
}

// ── Password requirements hint ────────────────────────────────────────────────

private val PasswordGreen = Color(0xFF2E7D32)

/**
 * Live checklist shown below the password field.
 * Each rule is neutral (gray) until the user starts typing, then turns
 * green when satisfied or red when violated.
 */
@Composable
fun PasswordRequirementsHint(
    password: String,
    dirty: Boolean,
    modifier: Modifier = Modifier,
) {
    val rules = listOf(
        "Mínimo 8 caracteres" to (password.length >= 8),
        "Una letra mayúscula"  to password.any { it.isUpperCase() },
        "Una letra minúscula"  to password.any { it.isLowerCase() },
        "Un número"            to password.any { it.isDigit() },
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        rules.forEach { (label, met) ->
            val color = when {
                met          -> PasswordGreen
                dirty        -> MaterialTheme.colorScheme.error
                else         -> CrocNeutralDark
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (met) Icons.Default.CheckCircle
                                  else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = color,
                )
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = CrocBlue,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CrocBlueVibrant,
    unfocusedBorderColor = CrocNeutralLight,
    cursorColor = CrocBlueVibrant,
    focusedContainerColor = CrocWhite,
    unfocusedContainerColor = CrocWhite,
)
