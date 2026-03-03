package crocalert.server.service

import crocalert.server.FirebaseInit
import crocalert.server.routes.AlertDto
import kotlinx.coroutines.future.await
import java.util.UUID

class AlertService {

    private val db = FirebaseInit.firestore()
    private val col = db.collection("alerts")

    suspend fun getAll(): List<AlertDto> {
        val snap = col.get().get()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(AlertDto::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun getById(id: String): AlertDto? {
        val doc = col.document(id).get().get()
        return if (doc.exists()) doc.toObject(AlertDto::class.java)?.copy(id = doc.id) else null
    }

    suspend fun create(dto: AlertDto): String {
        val id = dto.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        col.document(id).set(dto.copy(id = id)).get()
        return id
    }

    suspend fun update(id: String, dto: AlertDto): Boolean {
        val ref = col.document(id)
        val current = ref.get().get()
        if (!current.exists()) return false
        ref.set(dto.copy(id = id)).get()
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