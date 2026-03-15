package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.shared.network.ApiRoutes

object AppModule {

    fun provideAlertRepository(): AlertRepository =
        createAlertRepository(baseUrl = ApiRoutes.DEFAULT_BASE)
}