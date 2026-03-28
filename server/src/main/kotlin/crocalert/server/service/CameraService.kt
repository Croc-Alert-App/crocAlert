package crocalert.server.service
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.CameraDto
import crocalert.server.FirebaseInit
import java.util.UUID
import com.google.cloud.Timestamp
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraHealthCheckDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto

class CameraService : CameraServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("camera") }
    private val imagesPerDayCol by lazy { db.collection("images_per_day") }
    private fun DocumentSnapshot.toCameraDto(): CameraDto {

        val created   = getTimestamp("createdAt")?.toDate()?.time   ?: getLong("createdAt")
        val installed = getTimestamp("installedAt")?.toDate()?.time ?: getLong("installedAt")

        return CameraDto(
            id = id,
            name = getString("name") ?: "",
            isActive = getBoolean("isActive") ?: true,
            siteId = (get("siteId") as? DocumentReference)?.path ?: getString("siteId"),
            createdAt = created,
            installedAt = installed,
            expectedImages = (getLong("expectedImages") ?: getLong("excpectedImages"))?.toInt()
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

        // Use a non-null map so ref.update() doesn't reject null entries.
        // Nullable fields are only included when they have a value — this preserves
        // any existing Firestore fields we didn't read (e.g. legacy typo fields).
        val data = mutableMapOf<String, Any>(
            "name"     to dto.name,
            "isActive" to dto.isActive,
        )

        dto.siteId?.let          { data["siteId"] = it }
        dto.expectedImages?.let  { data["expectedImages"] = it }

        dto.createdAt?.let {
            data["createdAt"] = Timestamp.ofTimeSecondsAndNanos(
                it / 1000, ((it % 1000) * 1_000_000).toInt()
            )
        }

        dto.installedAt?.let {
            data["installedAt"] = Timestamp.ofTimeSecondsAndNanos(
                it / 1000, ((it % 1000) * 1_000_000).toInt()
            )
        }

        ref.update(data).get()

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
    override suspend fun getGlobalDailyCaptureRate(date: String): GlobalDailyCaptureRateDto {
        val camerasSnap = col.get().get()
        val dayDoc = imagesPerDayCol.document(date).get().get()

        val imagesMap = if (dayDoc.exists()) {
            dayDoc.get("imagesPerDay") as? Map<*, *> ?: emptyMap<Any, Any>()
        } else {
            emptyMap<Any, Any>()
        }

        val totalCameras = camerasSnap.documents.size
        val activeCameraDocs = camerasSnap.documents.filter { it.getBoolean("isActive") ?: true }
        val activeCameras = activeCameraDocs.size

        val expectedImagesTotal = activeCameraDocs.sumOf { cameraDoc ->
            cameraDoc.getLong("expectedImages")?.toInt()
                ?: cameraDoc.getLong("excpectedImages")?.toInt()
                ?: 0
        }

        val receivedImagesTotal = activeCameraDocs.sumOf { cameraDoc ->
            val cameraId = cameraDoc.id
            (imagesMap[cameraId] as? Number)?.toInt() ?: 0
        }

        val missingImagesTotal = (expectedImagesTotal - receivedImagesTotal).coerceAtLeast(0)
        val extraImagesTotal = (receivedImagesTotal - expectedImagesTotal).coerceAtLeast(0)

        val captureRate = if (expectedImagesTotal > 0) {
            (receivedImagesTotal.toDouble() / expectedImagesTotal.toDouble()) * 100.0
        } else {
            0.0
        }

        return GlobalDailyCaptureRateDto(
            date = date,
            totalCameras = totalCameras,
            activeCameras = activeCameras,
            expectedImagesTotal = expectedImagesTotal,
            receivedImagesTotal = receivedImagesTotal,
            missingImagesTotal = missingImagesTotal,
            extraImagesTotal = extraImagesTotal,
            captureRate = captureRate
        )
    }
    private fun calculateHealthStatus(captureRate: Double): String {
        return when {
            captureRate >= 97.5 -> "HEALTHY"
            captureRate >= 90.0 -> "CAUTION"
            else -> "RISK"
        }
    }
    override suspend fun getAllCameraHealthChecks(date: String): List<CameraHealthCheckDto> {
        val dailyStats = getDailyStatsForAll(date)

        return dailyStats.map { stats ->
            val captureRate = if (stats.expectedImages > 0) {
                (stats.receivedImages.toDouble() / stats.expectedImages.toDouble()) * 100.0
            } else {
                0.0
            }

            val extraImages = (stats.receivedImages - stats.expectedImages).coerceAtLeast(0)
            val healthStatus = calculateHealthStatus(captureRate)

            CameraHealthCheckDto(
                cameraId = stats.cameraId,
                date = stats.date,
                expectedImages = stats.expectedImages,
                receivedImages = stats.receivedImages,
                missingImages = stats.missingImages,
                extraImages = extraImages,
                captureRate = captureRate,
                healthStatus = healthStatus,
                isActive = stats.isActive,
                installedAt = stats.installedAt
            )
        }
    }
    override suspend fun getCameraHealthCheck(cameraId: String, date: String): CameraHealthCheckDto? {
        val stats = getDailyStats(cameraId, date) ?: return null

        val captureRate = if (stats.expectedImages > 0) {
            (stats.receivedImages.toDouble() / stats.expectedImages.toDouble()) * 100.0
        } else {
            0.0
        }

        val extraImages = (stats.receivedImages - stats.expectedImages).coerceAtLeast(0)
        val healthStatus = calculateHealthStatus(captureRate)

        return CameraHealthCheckDto(
            cameraId = stats.cameraId,
            date = stats.date,
            expectedImages = stats.expectedImages,
            receivedImages = stats.receivedImages,
            missingImages = stats.missingImages,
            extraImages = extraImages,
            captureRate = captureRate,
            healthStatus = healthStatus,
            isActive = stats.isActive,
            installedAt = stats.installedAt
        )
    }
    override suspend fun getMonitoringDashboard(date: String): CameraMonitoringDashboardDto {
        val cameraHealthChecks = getAllCameraHealthChecks(date)

        val totalCameras = cameraHealthChecks.size
        val activeCameraChecks = cameraHealthChecks.filter { it.isActive }
        val activeCameras = activeCameraChecks.size

        val expectedImagesTotal = activeCameraChecks.sumOf { it.expectedImages }
        val receivedImagesTotal = activeCameraChecks.sumOf { it.receivedImages }
        val missingImagesTotal = activeCameraChecks.sumOf { it.missingImages }
        val extraImagesTotal = activeCameraChecks.sumOf { it.extraImages }

        val globalCaptureRate = if (expectedImagesTotal > 0) {
            (receivedImagesTotal.toDouble() / expectedImagesTotal.toDouble()) * 100.0
        } else 0.0

        val healthyCameras = activeCameraChecks.count { it.healthStatus == "HEALTHY" }
        val cautionCameras = activeCameraChecks.count { it.healthStatus == "CAUTION" }
        val riskCameras = activeCameraChecks.count { it.healthStatus == "RISK" }

        val healthyRate = if (activeCameras > 0) {
            (healthyCameras.toDouble() / activeCameras.toDouble()) * 100.0
        } else 0.0

        val operationalRate = if (activeCameras > 0) {
            ((healthyCameras + cautionCameras).toDouble() / activeCameras.toDouble()) * 100.0
        } else 0.0

        return CameraMonitoringDashboardDto(
            date = date,
            totalCameras = totalCameras,
            activeCameras = activeCameras,
            expectedImagesTotal = expectedImagesTotal,
            receivedImagesTotal = receivedImagesTotal,
            missingImagesTotal = missingImagesTotal,
            extraImagesTotal = extraImagesTotal,
            globalCaptureRate = globalCaptureRate,
            healthyCameras = healthyCameras,
            cautionCameras = cautionCameras,
            riskCameras = riskCameras,
            healthyRate = healthyRate,
            operationalRate = operationalRate,
            cameras = cameraHealthChecks
        )
    }

}