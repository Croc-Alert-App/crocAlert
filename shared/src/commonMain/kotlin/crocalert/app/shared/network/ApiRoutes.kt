package crocalert.app.shared.network

object ApiRoutes {
    // En emulador Android: 10.0.2.2 apunta a tu PC
    var BASE: String = "http://10.0.2.2:8080"
    val ALERTS: String get() = "$BASE/alerts"
}