package crocalert.server.service

import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.QuerySnapshot
import crocalert.app.shared.data.dto.CameraDto
import crocalert.server.FirebaseInit
import java.util.UUID
import java.time.LocalDate
import java.time.ZoneId
import com.google.cloud.Timestamp
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraHealthCheckDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto
import crocalert.app.shared.data.dto.HealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class CameraService : CameraServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("camera") }
    private val imagesPerDayCol by lazy { db.collection("images_per_day") }
    private val imagesDriveCol by lazy { db.collection("imagenes_drive") }

    // Timestamps for [00:00, 00:00+1d) on a given date in Costa Rica time (UTC-6, no DST).
    // Used as fallback when images_per_day has no entry for a camera.
    private fun dateRangeCR(date: String): Pair<Timestamp, Timestamp> {
        val zone = ZoneId.of("America/Costa_Rica")
        val local = LocalDate.parse(date)
        val start = local.atStartOfDay(zone).toInstant()
        val end   = local.plusDays(1).atStartOfDay(zone).toInstant()
        return Timestamp.ofTimeSecondsAndNanos(start.epochSecond, start.nano) to
               Timestamp.ofTimeSecondsAndNanos(end.epochSecond, end.nano)
    }

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
            // P11: retain typo-fallback — prod documents may still carry the misspelling
            expectedImages = getLong("expectedImages")?.toInt()
                ?: getLong("excpectedImages")?.toInt()
        )
    }

    override suspend fun getAll(): List<CameraDto> {
        val snap = withContext(Dispatchers.IO) { col.get().get() }  // P1
        return snap.documents.map { it.toCameraDto() }
    }

    override suspend fun getById(id: String): CameraDto? {
        val doc = withContext(Dispatchers.IO) { col.document(id).get().get() }  // P1
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
            data["createdAt"] = Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }
        dto.installedAt?.let {
            data["installedAt"] = Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        withContext(Dispatchers.IO) { col.document(id).set(data).get() }  // P1
        return id
    }

    override suspend fun update(id: String, dto: CameraDto): Boolean {
        val ref = col.document(id)
        val current = withContext(Dispatchers.IO) { ref.get().get() }  // P1

        if (!current.exists()) return false

        val data = mutableMapOf<String, Any>(
            "name"     to dto.name,
            "isActive" to dto.isActive,
        )
        dto.siteId?.let          { data["siteId"] = it }
        dto.expectedImages?.let  { data["expectedImages"] = it }
        dto.createdAt?.let {
            data["createdAt"] = Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }
        dto.installedAt?.let {
            data["installedAt"] = Timestamp.ofTimeSecondsAndNanos(it / 1000, ((it % 1000) * 1_000_000).toInt())
        }

        withContext(Dispatchers.IO) { ref.update(data).get() }  // P1
        return true
    }

    override suspend fun delete(id: String): Boolean {
        val ref = col.document(id)
        val current = withContext(Dispatchers.IO) { ref.get().get() }  // P1
        if (!current.exists()) return false
        withContext(Dispatchers.IO) { ref.delete().get() }  // P1
        return true
    }

    override suspend fun getDailyStats(cameraId: String, date: String): CameraDailyStatsDto? {
        // P1: parallel reads
        val (cameraDoc, dayDoc) = coroutineScope {
            val cam = async(Dispatchers.IO) { col.document(cameraId).get().get() }
            val day = async(Dispatchers.IO) { imagesPerDayCol.document(date).get().get() }
            cam.await() to day.await()
        }
        if (!cameraDoc.exists()) return null

        val isActive = cameraDoc.getBoolean("isActive") ?: true
        val installedAt = cameraDoc.getTimestamp("installedAt")?.toDate()?.time
        // P11: retain typo-fallback
        val expectedImages = cameraDoc.getLong("expectedImages")?.toInt()
            ?: cameraDoc.getLong("excpectedImages")?.toInt()
            ?: 0

        val precomputed = if (dayDoc.exists()) {
            val imagesMap = dayDoc.get("imagesPerDay") as? Map<*, *>
            (imagesMap?.get(cameraId) as? Number)?.toInt()
        } else null

        // Fall back to a live imagenes_drive count when the pre-computed value is absent.
        // This keeps single-camera and all-camera stats consistent even before the
        // scheduled aggregation has run for the day.
        val receivedImages = precomputed ?: run {
            val (startTs, endTs) = dateRangeCR(date)
            val snap = withContext(Dispatchers.IO) {
                imagesDriveCol
                    .whereEqualTo("cameraId", cameraId)
                    .whereGreaterThanOrEqualTo("captureTime", startTs)
                    .whereLessThan("captureTime", endTs)
                    .get().get()
            }
            snap.size()
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
        // P1 + P15: parallel reads
        val (camerasSnap, dayDoc) = coroutineScope {
            val cams = async(Dispatchers.IO) { col.get().get() }
            val day  = async(Dispatchers.IO) { imagesPerDayCol.document(date).get().get() }
            cams.await() to day.await()
        }

        val imagesMap = if (dayDoc.exists()) {
            dayDoc.get("imagesPerDay") as? Map<*, *> ?: emptyMap<Any, Any>()
        } else {
            emptyMap<Any, Any>()
        }

        return camerasSnap.documents.map { cameraDoc ->
            val cameraId = cameraDoc.id
            // P11: retain typo-fallback
            val expectedImages = cameraDoc.getLong("expectedImages")?.toInt()
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
        // P1 + P15: parallel reads
        val (camerasSnap, dayDoc) = coroutineScope {
            val cams = async(Dispatchers.IO) { col.get().get() }
            val day  = async(Dispatchers.IO) { imagesPerDayCol.document(date).get().get() }
            cams.await() to day.await()
        }

        val imagesMap = if (dayDoc.exists()) {
            dayDoc.get("imagesPerDay") as? Map<*, *> ?: emptyMap<Any, Any>()
        } else {
            emptyMap<Any, Any>()
        }

        val totalCameras = camerasSnap.documents.size
        val activeCameraDocs = camerasSnap.documents.filter { it.getBoolean("isActive") ?: false }
        val activeCameras = activeCameraDocs.size

        val expectedImagesTotal = activeCameraDocs.sumOf { cameraDoc ->
            // P11: retain typo-fallback
            cameraDoc.getLong("expectedImages")?.toInt()
                ?: cameraDoc.getLong("excpectedImages")?.toInt()
                ?: 0
        }

        val receivedImagesTotal = activeCameraDocs.sumOf { cameraDoc ->
            val cameraId = cameraDoc.id
            (imagesMap[cameraId] as? Number)?.toInt() ?: 0
        }

        val missingImagesTotal = (expectedImagesTotal - receivedImagesTotal).coerceAtLeast(0)
        // P12: use per-camera sumOf for consistency with getMonitoringDashboard
        val extraImagesTotal = activeCameraDocs.sumOf { cameraDoc ->
            val cameraId = cameraDoc.id
            val received = (imagesMap[cameraId] as? Number)?.toInt() ?: 0
            val expected = cameraDoc.getLong("expectedImages")?.toInt()
                ?: cameraDoc.getLong("excpectedImages")?.toInt()
                ?: 0
            (received - expected).coerceAtLeast(0)
        }

        // P9: cap at 100 — overshoot signals a data issue, not 140% health
        val captureRate = if (expectedImagesTotal > 0) {
            minOf((receivedImagesTotal.toDouble() / expectedImagesTotal.toDouble()) * 100.0, 100.0)
        } else 0.0

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

    // P10: expectedImages == 0 → PENDING (not monitored), not RISK
    // P9: cap rate at 100 — overshoot is a data anomaly
    private fun calculateHealthStatus(captureRate: Double, expectedImages: Int): HealthStatus {
        if (expectedImages == 0) return HealthStatus.PENDING
        return when {
            captureRate >= 97.5 -> HealthStatus.HEALTHY
            captureRate >= 90.0 -> HealthStatus.CAUTION
            else -> HealthStatus.RISK
        }
    }

    override suspend fun getAllCameraHealthChecks(date: String): List<CameraHealthCheckDto> {
        val dailyStats = getDailyStatsForAll(date)

        return dailyStats.map { stats ->
            // P9: cap at 100
            val captureRate = if (stats.expectedImages > 0) {
                minOf((stats.receivedImages.toDouble() / stats.expectedImages.toDouble()) * 100.0, 100.0)
            } else 0.0

            val extraImages = (stats.receivedImages - stats.expectedImages).coerceAtLeast(0)
            val healthStatus = calculateHealthStatus(captureRate, stats.expectedImages)

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

        // P9: cap at 100
        val captureRate = if (stats.expectedImages > 0) {
            minOf((stats.receivedImages.toDouble() / stats.expectedImages.toDouble()) * 100.0, 100.0)
        } else 0.0

        val extraImages = (stats.receivedImages - stats.expectedImages).coerceAtLeast(0)
        val healthStatus = calculateHealthStatus(captureRate, stats.expectedImages)

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
        val missingImagesTotal  = activeCameraChecks.sumOf { it.missingImages }
        val extraImagesTotal    = activeCameraChecks.sumOf { it.extraImages }

        // P9: cap at 100
        val globalCaptureRate = if (expectedImagesTotal > 0) {
            minOf((receivedImagesTotal.toDouble() / expectedImagesTotal.toDouble()) * 100.0, 100.0)
        } else 0.0

        val healthyCameras  = activeCameraChecks.count { it.healthStatus == HealthStatus.HEALTHY }
        val cautionCameras  = activeCameraChecks.count { it.healthStatus == HealthStatus.CAUTION }
        // P7: count RISK directly — avoids negative counts and handles future statuses (PENDING, etc.)
        val riskCameras     = activeCameraChecks.count { it.healthStatus == HealthStatus.RISK }

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
