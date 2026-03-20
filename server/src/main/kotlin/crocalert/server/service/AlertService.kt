package crocalert.server.service

import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.AlertDto
import crocalert.server.FirebaseInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AlertService : AlertServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("alerts") }

    override suspend fun getAll(): List<AlertDto> {
        val snap = withContext(Dispatchers.IO) { col.get().get() }
        return snap.documents.map { it.toDto() }
    }

    override suspend fun getById(id: String): AlertDto? {
        val doc = withContext(Dispatchers.IO) { col.document(id).get().get() }
        return if (doc.exists()) doc.toDto() else null
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
        val ref = col.document(id)
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
        val ref = col.document(id)
        return withContext(Dispatchers.IO) {
            db.runTransaction { transaction ->
                if (!transaction.get(ref).get().exists()) return@runTransaction false
                transaction.delete(ref)
                true
            }.get()
        }
    }

    // MED-1: single mapping function — no duplication between getAll and getById
    private fun DocumentSnapshot.toDto() = AlertDto(
        id = id,
        captureId = getString("captureId") ?: "",
        cameraId = getString("cameraId") ?: "",
        aiConfidence = getDouble("aiConfidence")?.toFloat(),
        createdAt = getLong("createdAt") ?: 0L,
        status = getString("status") ?: "OPEN",
        priority = getString("priority") ?: "MEDIUM",
        assignedToUserId = getString("assignedToUserId"),
        closedAt = getLong("closedAt"),
        notes = getString("notes"),
        title = getString("title") ?: ""
    )
}
