package crocalert.server.service

import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.AlertDto
import crocalert.server.FirebaseInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AlertService : AlertServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("alerts") }
    private val capturesCol by lazy { db.collection("imagenes_drive") }

    override suspend fun getAll(since: Long?): List<AlertDto> {
        val sinceTs = since?.let {
            Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }
        fun query(folder: String) = if (sinceTs != null)
            capturesCol.whereEqualTo("folder", folder).whereGreaterThan("captureTime", sinceTs)
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
            status = dto.status.ifBlank { "OPEN" },
            priority = dto.priority.ifBlank { "MEDIUM" }
        )
        withContext(Dispatchers.IO) { col.document(id).set(normalized).get() }
        return id
    }

    override suspend fun update(id: String, dto: AlertDto): Boolean {
        val ref = capturesCol.document(id)
        return withContext(Dispatchers.IO) {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(ref).get()
                if (!snapshot.exists()) return@runTransaction false
                val normalized = dto.copy(
                    id = id,
                    captureId = dto.captureId.ifBlank { "" },
                    createdAt = if (dto.createdAt == 0L) System.currentTimeMillis() else dto.createdAt,
                    // Preserve the document's existing value instead of defaulting to "OPEN"/"MEDIUM",
                    // preventing a blank-field PUT from accidentally reopening a closed alert.
                    status = dto.status.ifBlank { snapshot.getString("status") ?: "OPEN" },
                    priority = dto.priority.ifBlank { snapshot.getString("priority") ?: "MEDIUM" }
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

    /** Maps an imagenes_drive capture document to an AlertDto for the mobile client. */
    private fun DocumentSnapshot.toCaptureAlertDto(): AlertDto? {
        val folder = getString("folder") ?: return null
        return AlertDto(
            id = id,
            captureId = id,
            cameraId = getString("cameraId") ?: "",
            createdAt = getTimestamp("captureTime")?.toDate()?.time
                ?: getLong("captureTime")
                ?: getTimestamp("syncedAt")?.toDate()?.time
                ?: getLong("syncedAt")
                ?: 0L,
            status = "OPEN",
            priority = if (folder == "alertas") "HIGH" else "MEDIUM",
            title = getString("name") ?: "",
            folder = folder,
        )
    }
}
