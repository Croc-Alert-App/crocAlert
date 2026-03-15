package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.shared.data.remote.AlertRemoteDataSourceImpl
import crocalert.app.shared.data.repository.AlertRepositoryImpl
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.HttpClientFactory

object AppModule {

    fun provideAlertRepository(): AlertRepository {
        ApiRoutes.BASE = "http://10.0.2.2:8080"

        val client = HttpClientFactory.create()
        val remote = AlertRemoteDataSourceImpl(client)

        return AlertRepositoryImpl(remote)
    }
}