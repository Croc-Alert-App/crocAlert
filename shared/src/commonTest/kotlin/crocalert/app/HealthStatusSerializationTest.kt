package crocalert.app

import crocalert.app.shared.data.dto.HealthStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthStatusSerializationTest {

    private val json = Json

    @Test
    fun `HEALTHY round-trips as SALUDABLE`() {
        assertEquals("\"SALUDABLE\"", json.encodeToString(HealthStatus.HEALTHY))
        assertEquals(HealthStatus.HEALTHY, json.decodeFromString("\"SALUDABLE\""))
    }

    @Test
    fun `CAUTION round-trips as PRECAUCION`() {
        assertEquals("\"PRECAUCION\"", json.encodeToString(HealthStatus.CAUTION))
        assertEquals(HealthStatus.CAUTION, json.decodeFromString("\"PRECAUCION\""))
    }

    @Test
    fun `RISK round-trips as RIESGO`() {
        assertEquals("\"RIESGO\"", json.encodeToString(HealthStatus.RISK))
        assertEquals(HealthStatus.RISK, json.decodeFromString("\"RIESGO\""))
    }

    @Test
    fun `PENDING round-trips as PENDIENTE`() {
        assertEquals("\"PENDIENTE\"", json.encodeToString(HealthStatus.PENDING))
        assertEquals(HealthStatus.PENDING, json.decodeFromString("\"PENDIENTE\""))
    }
}
