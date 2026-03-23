package crocalert.server.service
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.CameraDto
import crocalert.server.FirebaseInit
import java.util.UUID
import com.google.cloud.Timestamp
import crocalert.app.shared.data.dto.CameraDailyStatsDto

class CameraService : CameraServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("camera") }
    private val imagesPerDayCol by lazy { db.collection("images_per_day") }
    private fun DocumentSnapshot.toCameraDto(): CameraDto {

        val created = getTimestamp("createdAt")?.toDate()?.time
        val installed = getTimestamp("installedAt")?.toDate()?.time

        return CameraDto(
            id = id,
            name = getString("name") ?: "",
            isActive = getBoolean("isActive") ?: true,
            siteId = (get("siteId") as? DocumentReference)?.path ?: getString("siteId"),
            createdAt = created,
            installedAt = installed,
            expectedImages = getLong("expectedImages")?.toInt()
        )
    }

    override suspend fun getAll(): List<CameraDto> {
        val snap = col.get().get()
        return snap.documents.map { it.toCameraDto() }
    }

    override suspend fun getById(id: String): CameraDto? {
        val doc = col.document(id).get().get()
        if (!doc.exists()) return null
        return doc.toCameraDto()
    }

    override suspend fun create(dto: CameraDto): String {

        val id = dto.id.ifBlank { UUID.randomUUID().toString() }

        val data = mutableMapOf<String, Any?>(
            "name" to dto.name,
            "isActive" to dto.isActive,
            "siteId" to dto.siteId,
            "expectedImages" to dto.expectedImages
        )

        dto.createdAt?.let {
            data["createdAt"] =
                Timestamp.ofTimeSecondsAndNanos(
                    it / 1000,
                    ((it % 1000) * 1_000_000).toInt()
                )
        }

        dto.installedAt?.let {
            data["installedAt"] =
                Timestamp.ofTimeSecondsAndNanos(
                    it / 1000,
                    ((it % 1000) * 1_000_000).toInt()
                )
        }

        col.document(id).set(data).get()

        return id
    }

    override suspend fun update(id: String, dto: CameraDto): Boolean {

        val ref = col.document(id)
        val current = ref.get().get()

        if (!current.exists()) return false

        val data = mutableMapOf<String, Any?>(
            "name" to dto.name,
            "isActive" to dto.isActive,
            "siteId" to dto.siteId,
            "expectedImages" to dto.expectedImages
        )

        dto.createdAt?.let {
            data["createdAt"] =
                Timestamp.ofTimeSecondsAndNanos(
                    it / 1000,
                    ((it % 1000) * 1_000_000).toInt()
                )
        }

        dto.installedAt?.let {
            data["installedAt"] =
                Timestamp.ofTimeSecondsAndNanos(
                    it / 1000,
                    ((it % 1000) * 1_000_000).toInt()
                )
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
     override suspend fun getDailyStats(cameraId: String, date: String): CameraDailyStatsDto? {
        val cameraDoc = col.document(cameraId).get().get()
        if (!cameraDoc.exists()) return null

        val isActive = cameraDoc.getBoolean("isActive") ?: true
        val installedAt = cameraDoc.getTimestamp("installedAt")?.toDate()?.time
        val expectedImages =
            cameraDoc.getLong("expectedImages")?.toInt()
                ?: cameraDoc.getLong("excpectedImages")?.toInt()
                ?: 0

        val dayDoc = imagesPerDayCol.document(date).get().get()

        val receivedImages = if (dayDoc.exists()) {
            val imagesMap = dayDoc.get("imagesPerDay") as? Map<*, *>
            (imagesMap?.get(cameraId) as? Number)?.toInt() ?: 0
        } else {
            0
        }

        val missingImages = (expectedImages - receivedImages).coerceAtLeast(0)

        return CameraDailyStatsDto(
            cameraId = cameraId,
            date = date,
            expectedImages = expectedImages,
            receivedImages = receivedImages,
            missingImages = missingImages,
            isActive = isActive,
            installedAt = installedAt
        )
    }

     override suspend fun getDailyStatsForAll(date: String): List<CameraDailyStatsDto> {
        val camerasSnap = col.get().get()
        val dayDoc = imagesPerDayCol.document(date).get().get()

        val imagesMap = if (dayDoc.exists()) {
            dayDoc.get("imagesPerDay") as? Map<*, *> ?: emptyMap<Any, Any>()
        } else {
            emptyMap<Any, Any>()
        }

        return camerasSnap.documents.map { cameraDoc ->
            val cameraId = cameraDoc.id
            val expectedImages =
                cameraDoc.getLong("expectedImages")?.toInt()
                    ?: cameraDoc.getLong("excpectedImages")?.toInt()
                    ?: 0

            val receivedImages = (imagesMap[cameraId] as? Number)?.toInt() ?: 0
            val missingImages = (expectedImages - receivedImages).coerceAtLeast(0)

            CameraDailyStatsDto(
                cameraId = cameraId,
                date = date,
                expectedImages = expectedImages,
                receivedImages = receivedImages,
                missingImages = missingImages,
                isActive = cameraDoc.getBoolean("isActive") ?: true,
                installedAt = cameraDoc.getTimestamp("installedAt")?.toDate()?.time
            )
        }
    }
}