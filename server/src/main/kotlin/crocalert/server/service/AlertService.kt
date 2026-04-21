package crocalert.server.service

import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.domain.AlertStatusValidator
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.dto.AlertDto
import crocalert.server.FirebaseInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AlertService : AlertServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val capturesCol by lazy { db.collection("imagenes_drive") }

    override suspend fun getAll(since: Long?): List<AlertDto> {
        val sinceTs = since?.let {
            Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }
        fun query(folder: String) = if (sinceTs != null)
            capturesCol.whereEqualTo("folder", folder).whereGreaterThan("syncedAt", sinceTs)
        else
            capturesCol.whereEqualTo("folder", folder)

        val preAlerts = withContext(Dispatchers.IO) { query("pre-alertas").get().get() }
        val alerts    = withContext(Dispatchers.IO) { query("alertas").get().get() }
        return (preAlerts.documents + alerts.documents).mapNotNull { it.toCaptureAlertDto() }
    }

    override suspend fun getById(id: String): AlertDto? {
        val doc = withContext(Dispatchers.IO) { capturesCol.document(id).get().get() }
        return if (doc.exists()) doc.toCaptureAlertDto() else null
    }

    override suspend fun create(dto: AlertDto): String {
        val id = UUID.randomUUID().toString()   // always server-generated
        val normalized = dto.copy(
            id = id,
            captureId = dto.captureId.ifBlank { "" },
            createdAt = if (dto.createdAt == 0L) System.currentTimeMillis() else dto.createdAt,
            status = dto.status.ifBlank { AlertStatus.OPEN.name },
            priority = dto.priority.ifBlank { AlertPriority.MEDIUM.name }
        )
        withContext(Dispatchers.IO) { capturesCol.document(id).set(normalized).get() }
        return id
    }

    override suspend fun update(id: String, dto: AlertDto): Boolean {
        val newStatus = dto.status.takeUnless { it.isBlank() }?.let { s ->
            AlertStatus.entries.firstOrNull { it.name == s }
                ?: throw IllegalArgumentException("Unknown alert status: '$s'. Valid values: ${AlertStatus.entries.map { it.name }}")
        }
        val newPriority = dto.priority.takeUnless { it.isBlank() }?.let { p ->
            AlertPriority.entries.firstOrNull { it.name == p }
                ?: throw IllegalArgumentException("Unknown alert priority: '$p'. Valid values: ${AlertPriority.entries.map { it.name }}")
        }

        val ref = capturesCol.document(id)
        return withContext(Dispatchers.IO) {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(ref).get()
                if (!snapshot.exists()) return@runTransaction false

                val currentStatusStr = snapshot.getString("status") ?: AlertStatus.OPEN.name
                val currentStatus = AlertStatus.entries.firstOrNull { it.name == currentStatusStr }
                    ?: AlertStatus.OPEN
                if (newStatus != null && newStatus != currentStatus) {
                    AlertStatusValidator.requireValidTransition(currentStatus, newStatus)
                }

                val normalized = dto.copy(
                    id = id,
                    captureId = dto.captureId.ifBlank { "" },
                    createdAt = if (dto.createdAt == 0L) System.currentTimeMillis() else dto.createdAt,
                    // Preserve existing value when field is blank — prevents accidental reopening.
                    status = newStatus?.name ?: (snapshot.getString("status") ?: AlertStatus.OPEN.name),
                    priority = newPriority?.name ?: (snapshot.getString("priority") ?: AlertPriority.MEDIUM.name)
                )
                transaction.set(ref, normalized)
                true
            }.get()
        }
    }

    override suspend fun delete(id: String): Boolean {
        val ref = capturesCol.document(id)
        return withContext(Dispatchers.IO) {
            db.runTransaction { transaction ->
                if (!transaction.get(ref).get().exists()) return@runTransaction false
                transaction.delete(ref)
                true
            }.get()
        }
    }

    private fun DocumentSnapshot.toCaptureAlertDto(): AlertDto? {
        val folder = getString("folder") ?: return null
        return AlertDto(
            id = id,
            captureId = id,
            cameraId = getString("cameraId") ?: "",
            createdAt = getTimestamp("syncedAt")?.toDate()?.time
                ?: getLong("syncedAt")
                ?: 0L,
            status = AlertStatus.OPEN.name,
            priority = if (folder == "alertas") AlertPriority.HIGH.name else AlertPriority.MEDIUM.name,
            title = getString("name") ?: "",
            // P19: coerce to null so the client's thumbnailUrl?.let skips cleanly on missing images
            // P20: only allow https://drive.google.com URLs — prevents javascript:/file:// injection
            thumbnailUrl = getString("driveUrl")
                ?.takeIf { it.isNotBlank() && it.startsWith("https://drive.google.com/") },
            folder = folder,
        )
    }
}
