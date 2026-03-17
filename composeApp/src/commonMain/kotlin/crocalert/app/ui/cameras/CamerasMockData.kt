package crocalert.app.ui.cameras

// Preview-only. Not used in production — CamerasViewModel loads from CameraRepository.
internal object CamerasMockData {
    val cameras: List<CameraUiItem> = listOf(
        CameraUiItem(
            id = "cam-12",
            name = "CAM-12 Delta",
            status = CameraStatus.Alert,
            lastCapture = "2d ago",
            imagesSent = 0,
            imagesExpected = 24,
            captureCount = 18,
            captureExpected = 24,
            missingCaptures = 6,
            integrityFlags = 1,
        ),
        CameraUiItem(
            id = "cam-04",
            name = "CAM-04 Curva del río",
            status = CameraStatus.Attention,
            lastCapture = "1h ago",
            imagesSent = 20,
            imagesExpected = 24,
            captureCount = 20,
            captureExpected = 24,
            missingCaptures = 4,
            integrityFlags = 1,
        ),
        CameraUiItem(
            id = "cam-01",
            name = "CAM-01 Bridge",
            status = CameraStatus.Ok,
            lastCapture = "5m ago",
            imagesSent = 24,
            imagesExpected = 24,
            captureCount = 24,
            captureExpected = 24,
            missingCaptures = 0,
            integrityFlags = 0,
        ),
    )
}
