package crocalert.server.service

import crocalert.server.FirebaseInit
import crocalert.app.shared.data.dto.CaptureDto
import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import java.util.UUID

class CaptureService {

    private val db = FirebaseInit.firestore()
    private val col = db.collection("imagenes_drive")

    private fun DocumentSnapshot.toCaptureDto(): CaptureDto {
        val syncedTimestamp = getTimestamp("syncedAt")

        return CaptureDto(
            id = id,
            createdTime = getString("createdTime"),
            driveId = getString("driveId") ?: "",
            driveUrl = getString("driveUrl") ?: "",
            height = getLong("height")?.toInt(),
            width = getLong("width")?.toInt(),
            mimeType = getString("mimeType"),
            name = getString("name") ?: "",
            size = getString("size"),
            syncedAt = syncedTimestamp?.toDate()?.time
        )
    }

    suspend fun getAll(): List<CaptureDto> {
        val snap = col.get().get()
        return snap.documents.map { it.toCaptureDto() }
    }

    suspend fun getById(id: String): CaptureDto? {
        val doc = col.document(id).get().get()
        if (!doc.exists()) return null
        return doc.toCaptureDto()
    }

    suspend fun create(dto: CaptureDto): String {
        val id = dto.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        val data = mutableMapOf<String, Any?>(
            "createdTime" to dto.createdTime,
            "driveId" to dto.driveId,
            "driveUrl" to dto.driveUrl,
            "height" to dto.height,
            "width" to dto.width,
            "mimeType" to dto.mimeType,
            "name" to dto.name,
            "size" to dto.size
        )

        dto.syncedAt?.let {
            data["syncedAt"] = Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        col.document(id).set(data).get()
        return id
    }

    suspend fun update(id: String, dto: CaptureDto): Boolean {
        val ref = col.document(id)
        val current = ref.get().get()
        if (!current.exists()) return false

        val data = mutableMapOf<String, Any?>(
            "createdTime" to dto.createdTime,
            "driveId" to dto.driveId,
            "driveUrl" to dto.driveUrl,
            "height" to dto.height,
            "width" to dto.width,
            "mimeType" to dto.mimeType,
            "name" to dto.name,
            "size" to dto.size
        )

        dto.syncedAt?.let {
            data["syncedAt"] = Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        ref.set(data).get()
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