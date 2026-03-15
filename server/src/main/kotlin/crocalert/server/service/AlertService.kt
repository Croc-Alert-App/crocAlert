package crocalert.server.service

import crocalert.server.FirebaseInit
import crocalert.app.shared.data.dto.AlertDto
import java.util.UUID

class AlertService {

    private val db = FirebaseInit.firestore()
    private val col = db.collection("alerts")

    suspend fun getAll(): List<AlertDto> {
        val snap = col.get().get()
        return snap.documents.map { doc ->
            AlertDto(
                id = doc.id,
                captureId = doc.getString("captureId") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                status = doc.getString("status") ?: "OPEN",
                priority = doc.getString("priority") ?: "MEDIUM",
                assignedToUserId = doc.getString("assignedToUserId"),
                closedAt = doc.getLong("closedAt"),
                notes = doc.getString("notes"),
                title = doc.getString("title") ?: ""
            )
        }
    }

    suspend fun getById(id: String): AlertDto? {
        val doc = col.document(id).get().get()
        if (!doc.exists()) return null

        return AlertDto(
            id = doc.id,
            captureId = doc.getString("captureId") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L,
            status = doc.getString("status") ?: "OPEN",
            priority = doc.getString("priority") ?: "MEDIUM",
            assignedToUserId = doc.getString("assignedToUserId"),
            closedAt = doc.getLong("closedAt"),
            notes = doc.getString("notes"),
            title = doc.getString("title") ?: ""
        )
    }

    suspend fun create(dto: AlertDto): String {
        val id = dto.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        val normalized = dto.copy(
            id = id,
            captureId = dto.captureId.ifBlank { "" },
            createdAt = if (dto.createdAt == 0L) System.currentTimeMillis() else dto.createdAt,
            status = dto.status.ifBlank { "OPEN" },
            priority = dto.priority.ifBlank { "MEDIUM" }
        )

        col.document(id).set(normalized).get()
        return id
    }

    suspend fun update(id: String, dto: AlertDto): Boolean {
        val ref = col.document(id)
        val current = ref.get().get()
        if (!current.exists()) return false

        val normalized = dto.copy(
            id = id,
            captureId = dto.captureId.ifBlank { "" },
            createdAt = if (dto.createdAt == 0L) System.currentTimeMillis() else dto.createdAt,
            status = dto.status.ifBlank { "OPEN" },
            priority = dto.priority.ifBlank { "MEDIUM" }
        )

        ref.set(normalized).get()
        return true
    }

    suspend fun delete(id: String): Boolean {
        val ref = col.document(id)
        val current = ref.get().get()
        if (!current.exists()) return false
        ref.delete().get()
        return true
    }
}