package crocalert.server.service

import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.AlertDto
import crocalert.server.FirebaseInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AlertService {

    // Lazy so constructing AlertService() in tests doesn't touch Firestore
    private val col by lazy { FirebaseInit.firestore().collection("alerts") }

    suspend fun getAll(): List<AlertDto> {
        val snap = withContext(Dispatchers.IO) { col.get().get() }
        return snap.documents.map { it.toDto() }
    }

    suspend fun getById(id: String): AlertDto? {
        val doc = withContext(Dispatchers.IO) { col.document(id).get().get() }
        return if (doc.exists()) doc.toDto() else null
    }

    suspend fun create(dto: AlertDto): String {
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

    suspend fun update(id: String, dto: AlertDto): Boolean {
        val ref = col.document(id)
        val exists = withContext(Dispatchers.IO) { ref.get().get() }.exists()
        if (!exists) return false
        val normalized = dto.copy(
            id = id,
            captureId = dto.captureId.ifBlank { "" },
            createdAt = if (dto.createdAt == 0L) System.currentTimeMillis() else dto.createdAt,
            status = dto.status.ifBlank { "OPEN" },
            priority = dto.priority.ifBlank { "MEDIUM" }
        )
        withContext(Dispatchers.IO) { ref.set(normalized).get() }
        return true
    }

    suspend fun delete(id: String): Boolean {
        val ref = col.document(id)
        val exists = withContext(Dispatchers.IO) { ref.get().get() }.exists()
        if (!exists) return false
        withContext(Dispatchers.IO) { ref.delete().get() }
        return true
    }

    // MED-1: single mapping function — no duplication between getAll and getById
    private fun DocumentSnapshot.toDto() = AlertDto(
        id = id,
        captureId = getString("captureId") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
        status = getString("status") ?: "OPEN",
        priority = getString("priority") ?: "MEDIUM",
        assignedToUserId = getString("assignedToUserId"),
        closedAt = getLong("closedAt"),
        notes = getString("notes"),
        title = getString("title") ?: ""
    )
}