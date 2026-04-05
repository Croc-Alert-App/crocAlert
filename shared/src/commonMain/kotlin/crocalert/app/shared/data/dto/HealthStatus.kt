package crocalert.app.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class HealthStatus {
    @SerialName("SALUDABLE") HEALTHY,
    @SerialName("PRECAUCION") CAUTION,
    @SerialName("RIESGO") RISK
}
