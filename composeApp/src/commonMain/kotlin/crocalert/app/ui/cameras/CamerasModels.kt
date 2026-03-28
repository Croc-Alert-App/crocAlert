package crocalert.app.ui.cameras

import androidx.compose.ui.graphics.Color
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocNeutralDark

data class CameraUiItem(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val status: CameraStatus,
    val lastCapture: String,
    val imagesSent: Int,
    val imagesExpected: Int,
    val captureCount: Int,
    val captureExpected: Int,
    val missingCaptures: Int,
    val integrityFlags: Int,
)

enum class CameraStatus {
    Alert, Attention, Ok;

    val severity: Int get() = when (this) {
        Alert     -> 0
        Attention -> 1
        Ok        -> 2
    }

    val badgeLabel: String get() = when (this) {
        Alert     -> "Alerta"
        Attention -> "Precaución"
        Ok        -> "Bien"
    }

    /** Left-border / dot / badge accent color for this status. */
    val accentColor: Color get() = when (this) {
        Alert     -> CrocAmber
        Attention -> CrocBlue
        Ok        -> CrocNeutralDark
    }
}

enum class VisibilityFilter {
    Active, Deleted, All;

    val label: String get() = when (this) {
        Active  -> "Activas"
        Deleted -> "Desactivadas"
        All     -> "Todas"
    }
}

enum class CameraFilter {
    All, Ok, Attention, Alert;

    val label: String get() = when (this) {
        All       -> "Todos"
        Ok        -> "Bien"
        Attention -> "Precaución"
        Alert     -> "Alerta"
    }

    /** Corresponding [CameraStatus] for count lookup; null for [All]. */
    val correspondingStatus: CameraStatus? get() = when (this) {
        All       -> null
        Ok        -> CameraStatus.Ok
        Attention -> CameraStatus.Attention
        Alert     -> CameraStatus.Alert
    }

    fun matches(status: CameraStatus): Boolean = when (this) {
        All       -> true
        Ok        -> status == CameraStatus.Ok
        Attention -> status == CameraStatus.Attention
        Alert     -> status == CameraStatus.Alert
    }
}
