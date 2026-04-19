package crocalert.app.feature.alerts.data

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType
import kotlinx.datetime.Clock

/**
 * Centralized mock data for the Alerts List feature.
 *
 * Timestamps are relative to [Clock.System.now()] so date-based filters
 * always return predictable subsets regardless of when the app is opened.
 *
 * Distribution (9 alerts total):
 *
 *  Id       Priority   Age              TODAY  WEEK  MONTH  ALL
 *  001      CRITICAL   just now           ✓      ✓     ✓     ✓
 *  002      HIGH       12 min ago         ✓      ✓     ✓     ✓
 *  003      MEDIUM     45 min ago         ✓      ✓     ✓     ✓
 *  004      MEDIUM     2.5 hr ago         ✓      ✓     ✓     ✓
 *  007      CRITICAL   1 day ago          ✗      ✓     ✓     ✓
 *  008      HIGH       2 days ago         ✗      ✓     ✓     ✓
 *  009      MEDIUM     8 days ago         ✗      ✗     ✓     ✓
 *  005      LOW        3 days ago         ✗      ✓     ✓     ✓  (not shown in tabs)
 *  006      LOW        12 days ago        ✗      ✗     ✓     ✓  (not shown in tabs)
 *
 * LOW priority alerts exist in the raw data but are intentionally excluded
 * from both tabs (Alerts = CRITICAL+HIGH, Pre-Alerts = MEDIUM).
 */
object AlertSampleData {

    val alerts: List<Alert>
        get() {
            val now = Clock.System.now().toEpochMilliseconds()
            return listOf(
                Alert(
                    id = "alert-001",
                    title = "Posible Cocodrilo Detectado",
                    message = "Detección de alta confianza cerca de la ribera sur. Se recomienda inspección inmediata en sitio.",
                    type = AlertType.POSSIBLE_CROCODILE,
                    priority = AlertPriority.CRITICAL,
                    status = AlertStatus.OPEN,
                    createdAt = now,
                    sourceName = "CAM-12 Río Conchal",
                    isRead = false,
                    folder = "alertas",
                    thumbnailUrl = "https://drive.google.com/file/d/17458lCf5kwIodwI6hZ0j0E5VOrDu61_Y/view?usp=drivesdk",
                ),
                Alert(
                    id = "alert-002",
                    title = "Movimiento Detectado Cerca de la Ribera",
                    message = "Patrón de movimiento inusual en zona restringida. Marcado para revisión.",
                    type = AlertType.MOTION_DETECTED,
                    priority = AlertPriority.HIGH,
                    status = AlertStatus.IN_PROGRESS,
                    createdAt = now - 720_000L,                     // 12 min  → hoy
                    sourceName = "CAM-08 Laguna Norte",
                    isRead = false,
                    folder = "alertas",
                    thumbnailUrl = "https://drive.google.com/file/d/17458lCf5kwIodwI6hZ0j0E5VOrDu61_Y/view?usp=drivesdk",
                ),
                Alert(
                    id = "alert-003",
                    title = "Nueva Imagen Capturada",
                    message = "Nueva imagen subida desde cámara remota. Pendiente de análisis por IA.",
                    type = AlertType.IMAGE_UPLOADED,
                    priority = AlertPriority.MEDIUM,
                    status = AlertStatus.OPEN,
                    createdAt = now - 2_700_000L,                   // 45 min  → hoy
                    sourceName = "CAM-03 Sector Este",
                    isRead = true,
                    folder = "pre-alertas",
                    thumbnailUrl = "https://drive.google.com/file/d/17458lCf5kwIodwI6hZ0j0E5VOrDu61_Y/view?usp=drivesdk",
                ),
                Alert(
                    id = "alert-004",
                    title = "Advertencia de Comunicación del Dispositivo",
                    message = "La cámara no ha reportado estado en más de 2 horas. Verificar conectividad.",
                    type = AlertType.SYSTEM_WARNING,
                    priority = AlertPriority.MEDIUM,
                    status = AlertStatus.IN_PROGRESS,
                    createdAt = now - 9_000_000L,                   // 2.5 hr  → hoy
                    sourceName = "CAM-05 Margen Sur",
                    isRead = true,
                    folder = "pre-alertas",
                    thumbnailUrl = "https://drive.google.com/file/d/17458lCf5kwIodwI6hZ0j0E5VOrDu61_Y/view?usp=drivesdk",
                ),
                Alert(
                    id = "alert-007",
                    title = "Cocodrilo Cerca del Camino de Acceso",
                    message = "Animal detectado cruzando el camino de acceso norte. Área cerrada hasta inspección.",
                    type = AlertType.POSSIBLE_CROCODILE,
                    priority = AlertPriority.CRITICAL,
                    status = AlertStatus.OPEN,
                    createdAt = now - 1 * 24 * 3_600_000L,         // 1 día   → esta semana
                    sourceName = "CAM-02 Acceso Principal",
                    isRead = false,
                    folder = "alertas",
                    thumbnailUrl = "https://drive.google.com/file/d/17458lCf5kwIodwI6hZ0j0E5VOrDu61_Y/view?usp=drivesdk",
                ),
                Alert(
                    id = "alert-008",
                    title = "Gran Movimiento Detectado en Laguna",
                    message = "Movimiento significativo registrado en el perímetro de la laguna este.",
                    type = AlertType.MOTION_DETECTED,
                    priority = AlertPriority.HIGH,
                    status = AlertStatus.IN_PROGRESS,
                    createdAt = now - 2 * 24 * 3_600_000L,         // 2 días  → esta semana
                    sourceName = "CAM-09 Laguna Este",
                    isRead = true,
                    folder = "alertas",
                    thumbnailUrl = "https://drive.google.com/file/d/17458lCf5kwIodwI6hZ0j0E5VOrDu61_Y/view?usp=drivesdk",
                ),
                Alert(
                    id = "alert-009",
                    title = "Problema de Alineación de Cámara",
                    message = "El campo de visión de la cámara se ha desplazado. Se requiere recalibración manual.",
                    type = AlertType.SYSTEM_WARNING,
                    priority = AlertPriority.MEDIUM,
                    status = AlertStatus.OPEN,
                    createdAt = now - 8 * 24 * 3_600_000L,         // 8 días  → este mes
                    sourceName = "CAM-07 Zona Oeste",
                    isRead = true,
                    folder = "pre-alertas",
                    thumbnailUrl = "https://drive.google.com/file/d/17458lCf5kwIodwI6hZ0j0E5VOrDu61_Y/view?usp=drivesdk",
                ),
                Alert(
                    id = "alert-005",
                    title = "Batería Baja en Cámara Remota",
                    message = "Nivel de batería al 8%. El dispositivo puede desconectarse en 30 minutos.",
                    type = AlertType.BATTERY_LOW,
                    priority = AlertPriority.LOW,
                    status = AlertStatus.OPEN,
                    createdAt = now - 3 * 24 * 3_600_000L,         // 3 días  → esta semana
                    sourceName = "CAM-11 Acceso Norte",
                    isRead = true,
                ),
                Alert(
                    id = "alert-006",
                    title = "Sincronización Completada Exitosamente",
                    message = "Todos los registros de cámaras sincronizados. 248 capturas subidas.",
                    type = AlertType.SYNC_COMPLETED,
                    priority = AlertPriority.LOW,
                    status = AlertStatus.CLOSED,
                    createdAt = now - 12 * 24 * 3_600_000L,        // 12 días → este mes
                    sourceName = "Sistema",
                    isRead = true,
                ),
            )
        }
}
