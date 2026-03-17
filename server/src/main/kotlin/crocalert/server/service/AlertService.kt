package crocalert.server.service

import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.AlertDto
import com.google.cloud.firestore.DocumentSnapshot
import crocalert.server.FirebaseInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AlertService {

    private val db = FirebaseInit.firestore()
    private val col = db.collection("alerts")

    private fun DocumentSnapshot.toAlertDto(): AlertDto {
        return AlertDto(
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
    // Lazy so constructing AlertService() in tests doesn't touch Firestore
    private val col by lazy { FirebaseInit.firestore().collection("alerts") }

    suspend fun getAll(): List<AlertDto> {
        val snap = col.get().get()
        return snap.documents.map { it.toAlertDto() }
        val snap = withContext(Dispatchers.IO) { col.get().get() }
        return snap.documents.map { it.toDto() }
    }

    suspend fun getById(id: String): AlertDto? {
        val doc = col.document(id).get().get()
        if (!doc.exists()) return null
        return doc.toAlertDto()
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
        return withContext(Dispatchers.IO) {
            FirebaseInit.firestore().runTransaction { transaction ->
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

    suspend fun delete(id: String): Boolean {
        val ref = col.document(id)
        val current = ref.get().get()
        if (!current.exists()) return false

        ref.delete().get()
        return true
    }
        return withContext(Dispatchers.IO) {
            FirebaseInit.firestore().runTransaction { transaction ->
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
        createdAt = getLong("createdAt") ?: 0L,
        status = getString("status") ?: "OPEN",
        priority = getString("priority") ?: "MEDIUM",
        assignedToUserId = getString("assignedToUserId"),
        closedAt = getLong("closedAt"),
        notes = getString("notes"),
        title = getString("title") ?: ""
    )
}