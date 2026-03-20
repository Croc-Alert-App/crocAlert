package crocalert.server.service

import com.google.cloud.firestore.DocumentSnapshot
import crocalert.app.shared.data.dto.SiteDto
import crocalert.server.FirebaseInit

class SiteService : SiteServicePort {

    private val db by lazy { FirebaseInit.firestore() }
    private val col by lazy { db.collection("sites") }

    private fun DocumentSnapshot.toSiteDto() = SiteDto(
        id = id,
        code = getString("code") ?: "",
        name = getString("name") ?: "",
        description = getString("description"),
        isActive = getBoolean("isActive") ?: true,
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt"),
        centerLat = getDouble("centerLat"),
        centerLng = getDouble("centerLng"),
        region = getString("region")
    )

    override suspend fun getAll(): List<SiteDto> =
        col.get().get().documents.map { it.toSiteDto() }

    override suspend fun getById(id: String): SiteDto? {
        val doc = col.document(id).get().get()
        return if (doc.exists()) doc.toSiteDto() else null
    }
}
