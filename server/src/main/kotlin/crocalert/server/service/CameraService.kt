package crocalert.server.service
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.CameraDto
import crocalert.server.FirebaseInit
import java.util.UUID

class CameraService : CameraServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("camera") }

    private fun DocumentSnapshot.toCameraDto(): CameraDto {

        val created = getTimestamp("createdAt")?.toDate()?.time
        val installed = getTimestamp("installedAt")?.toDate()?.time

        return CameraDto(
            id = id,
            name = getString("name") ?: "",
            isActive = getBoolean("isActive") ?: true,
            siteId = (get("siteId") as? DocumentReference)?.path ?: getString("siteId"),
            createdAt = created,
            installedAt = installed
        )
    }

    suspend fun getAll(): List<CameraDto> {
        val snap = col.get().get()
        return snap.documents.map { it.toCameraDto() }
    }

    suspend fun getById(id: String): CameraDto? {
        val doc = col.document(id).get().get()
        if (!doc.exists()) return null
        return doc.toCameraDto()
    }

    suspend fun create(dto: CameraDto): String {

        val id = dto.id.ifBlank { UUID.randomUUID().toString() }

        val data = mapOf(
            "name" to dto.name,
            "isActive" to dto.isActive,
            "siteId" to dto.siteId,
            "createdAt" to dto.createdAt,
            "installedAt" to dto.installedAt
        )

        col.document(id).set(data).get()

        return id
    }

    suspend fun update(id: String, dto: CameraDto): Boolean {

        val ref = col.document(id)
        val current = ref.get().get()

        if (!current.exists()) return false

        val data = mapOf(
            "name" to dto.name,
            "isActive" to dto.isActive,
            "siteId" to dto.siteId,
            "createdAt" to dto.createdAt,
            "installedAt" to dto.installedAt
        )

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