package crocalert.app

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.shared.data.repository.AlertRepositoryImpl
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.createHttpClient

fun provideAlertRepository(): AlertRepository {
    ApiRoutes.BASE = "http://127.0.0.1:8080" // backend local

    val client = createHttpClient() // jvmMain
    val remote = AlertRemoteDataSourceImpl(client) // commonMain
    return AlertRepositoryImpl(remote) // commonMain
}