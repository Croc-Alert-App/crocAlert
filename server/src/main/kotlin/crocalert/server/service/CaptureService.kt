package crocalert.server.service

import crocalert.app.shared.data.dto.CaptureDto
import crocalert.server.FirebaseInit
import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import java.util.UUID

class CaptureService : CaptureServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("imagenes_drive") }

    private fun DocumentSnapshot.toCaptureDto(): CaptureDto {
        return CaptureDto(
            id = id,
            cameraId = getString("cameraId") ?: "",
            cameraRef = get("cameraRef", com.google.cloud.firestore.DocumentReference::class.java)?.path,
            captureTime = getTimestamp("captureTime")?.toDate()?.time,
            createdTime = getString("createdTime"),
            driveId = getString("driveId") ?: "",
            driveUrl = getString("driveUrl") ?: "",
            folder = getString("folder"),
            height = getLong("height")?.toInt(),
            width = getLong("width")?.toInt(),
            mimeType = getString("mimeType"),
            name = getString("name") ?: "",
            size = getString("size"),
            syncedAt = getTimestamp("syncedAt")?.toDate()?.time
        )
    }

    override suspend fun getAll(): List<CaptureDto> {
        val snap = col.get().get()
        return snap.documents.map { it.toCaptureDto() }
    }

    override suspend fun getById(id: String): CaptureDto? {
        val doc = col.document(id).get().get()
        if (!doc.exists()) return null
        return doc.toCaptureDto()
    }

    override suspend fun getByCameraId(cameraId: String): List<CaptureDto> {
        val snap = col
            .whereEqualTo("cameraId", cameraId)
            .get()
            .get()

        return snap.documents
            .map { it.toCaptureDto() }
            .sortedByDescending { it.captureTime ?: 0L }
    }
    override suspend fun getByFolder(folder: String): List<CaptureDto> {
        val snap = col
            .whereEqualTo("folder", folder)
            .get()
            .get()

        return snap.documents
            .map { it.toCaptureDto() }
            .sortedByDescending { it.captureTime ?: 0L }
    }

    override suspend fun create(dto: CaptureDto): String {
        val id = dto.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        val data = mutableMapOf<String, Any?>(
            "cameraId" to dto.cameraId,
            "createdTime" to dto.createdTime,
            "driveId" to dto.driveId,
            "driveUrl" to dto.driveUrl,
            "folder" to dto.folder,
            "height" to dto.height,
            "width" to dto.width,
            "mimeType" to dto.mimeType,
            "name" to dto.name,
            "size" to dto.size
        )

        dto.cameraRef?.let {
            data["cameraRef"] = db.document(it.removePrefix("/"))
        }

        dto.captureTime?.let {
            data["captureTime"] =
                Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        dto.syncedAt?.let {
            data["syncedAt"] =
                Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        col.document(id).set(data).get()
        return id
    }

    override suspend fun update(id: String, dto: CaptureDto): Boolean {
        val ref = col.document(id)
        val current = ref.get().get()
        if (!current.exists()) return false

        val data = mutableMapOf<String, Any?>(
            "cameraId" to dto.cameraId,
            "createdTime" to dto.createdTime,
            "driveId" to dto.driveId,
            "driveUrl" to dto.driveUrl,
            "folder" to dto.folder,
            "height" to dto.height,
            "width" to dto.width,
            "mimeType" to dto.mimeType,
            "name" to dto.name,
            "size" to dto.size
        )

        dto.cameraRef?.let {
            data["cameraRef"] = db.document(it.removePrefix("/"))
        }

        dto.captureTime?.let {
            data["captureTime"] =
                Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        dto.syncedAt?.let {
            data["syncedAt"] =
                Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        ref.set(data).get()
        return true
    }

    override suspend fun delete(id: String): Boolean {
        val ref = col.document(id)
        val current = ref.get().get()
        if (!current.exists()) return false

        ref.delete().get()
        return true
    }
}