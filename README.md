<h1 align="center">CrocAlert 🐊</h1>

<p align="center">
  <a href="https://kotlinlang.org/"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0.21-%237F52FF.svg?logo=kotlin"/></a>
  <a href="https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.6.11"><img alt="Compose Multiplatform" src="https://img.shields.io/badge/Compose%20Multiplatform-v1.6.11-%237F52FF"/></a>
  <a href="https://ktor.io/"><img alt="Ktor" src="https://img.shields.io/badge/Ktor-2.3.12-%23087CFA.svg?logo=ktor"/></a>
</p>

<p align="center">
  Kotlin Multiplatform wildlife monitoring system that detects crocodile sightings via camera traps,
  generates prioritized alerts, and keeps field teams in sync across Android, iOS, Desktop (JVM),
  and a Ktor backend — even without an internet connection.
</p>

---

## Project Structure

This is a Kotlin Multiplatform project targeting Android, iOS, and Desktop (JVM).

* [/composeApp](./composeApp/src) — Shared Compose Multiplatform UI library. Runs on Android, iOS, and Desktop. Contains:
  - [commonMain](./composeApp/src/commonMain/kotlin) — All screens, ViewModels, navigation, and theming.
  - [androidMain](./composeApp/src/androidMain/kotlin) — Android-specific Compose bootstrap.
  - [jvmMain](./composeApp/src/jvmMain/kotlin) — Desktop (JVM) entry point helpers.
  - [iosMain](./composeApp/src/iosMain/kotlin) — iOS-specific Compose configuration.

* [/shared](./shared/src) — Kotlin Multiplatform business logic: domain models, repository interfaces, Ktor HTTP client, SQLDelight local database, sync workers, and data mappers. The most important subfolder is [commonMain](./shared/src/commonMain/kotlin).

* [/androidApp](./androidApp) — Android application entry point. Initializes `AppModule` (SQLDelight, WorkManager, DataStore) and launches the Compose UI.

* [/desktopApp](./desktopApp) — Desktop (JVM) application entry point. Reads `CROCALERT_API_URL` from env and opens a Compose desktop window.

* [/server](./server/src) — Ktor backend server (port 8080). Exposes a REST API backed by Firebase Firestore. Secured by `X-API-Key` header (`CROC_API_KEY` env var; skipped when blank in dev mode).

* [/iosApp](./iosApp) — iOS Xcode project that wraps the shared Compose framework via Swift/Obj-C bridge.

### Module Summary

| Module | Plugin | Purpose |
|---|---|---|
| `:composeApp` | `com.android.library` + KMP | Shared Compose UI — consumed by `:androidApp`, iOS framework, Desktop JVM |
| `:shared` | `com.android.library` + KMP | Domain models, repositories, Ktor client, SQLDelight DB, sync logic |
| `:server` | Ktor + Netty | REST API server backed by Firebase Firestore |
| `:androidApp` | `com.android.application` | Android entry point — initializes DI and WorkManager, hosts `:composeApp` |
| `:desktopApp` | Compose Desktop | Desktop (JVM) entry point — Compose window with env-based API URL |

---

## Features

### Authentication
- Splash screen with automatic session restore ("remember device" flag).
- Login with email/password and client-side format validation.
- Multi-factor authentication (MFA) with OTP entry, resend, and backup code options.
- User registration form.
- Forgot password / recovery screen.
- Logout with confirmation dialog from the Profile screen.

### Alerts
- Real-time alert list loaded from the local SQLDelight cache (offline-first).
- Filter by status (`OPEN`, `IN_PROGRESS`, `CLOSED`) and date range.
- Sort ascending or descending.
- Alert detail screen with capture thumbnail, priority badge, assigned user, notes, and status history timeline.
- Alert types: `POSSIBLE_CROCODILE`, `MOTION_DETECTED`, `IMAGE_UPLOADED`, `BATTERY_LOW`, `SYNC_COMPLETED`, `SYSTEM_WARNING`.

### Cameras
- Camera trap list with per-camera daily stats (images captured, AI confidence).
- Search, filter by status/region, and sort.
- Create and edit cameras via an inline form dialog with numeric text fields.
- Deactivate cameras (soft delete).
- Camera history screen with full capture timeline per camera.

### Dashboard
- Tab-based home screen: **Home**, **Alerts**, **Cameras**, **Profile**.
- Home tab shows activity metrics and sync status.
- Background refresh of alert and camera stats on load.
- Displays last synced timestamp.

### Offline-First Sync
- Local SQLDelight database (Android) with in-memory fallback (iOS, Desktop).
- TTL-based cache: alerts refresh every 5 min, cameras every 15 min, sites every 60 min.
- Incremental sync for alerts (`GET /alerts?since=<timestamp>`).
- Android background sync via WorkManager (`AlertSyncWorker`, `CameraSyncWorker`, `SiteSyncWorker`).
- Manual force-refresh available from the UI.

---

## Architecture

CrocAlert follows a **clean architecture** pattern across the KMP stack with a strict one-way data flow.

```
Platform Entry Points
  androidApp/MainActivity      →  AppModule.setup(context) → CrocAlertTheme { App() }
  desktopApp/Main.kt           →  reads CROCALERT_API_URL → App()

composeApp / commonMain
  App.kt                       →  NavHost (splash → login → mfa → home → alert_detail)
  DashboardScreen              →  tab host: Home | Alerts | Cameras | Profile
  *ViewModel (StateFlow)       →  UI state via MutableStateFlow, coroutine scopes

shared / commonMain
  domain/repository/           →  interfaces (AlertRepository, CameraRepository, ...)
  data/remote/                 →  Ktor HTTP client → ApiResult<T> (Success / Error)
  data/local/                  →  SQLDelight queries (alert_entity, camera_entity, site_entity)
  data/repository/             →  offline-first: local cache + sync logic + TTL
  data/mapper/                 →  DTO ↔ domain model converters
  AppFactory.kt                →  wires remote + local → repository (common)
  AppModule.kt (androidMain)   →  wires SQLite driver + WorkManager + DataStore

server
  Ktor routes → *ServicePort → Firebase Firestore
```

**Layer rules:**
- UI (`composeApp`) only imports domain interfaces and models from `:shared` — never Ktor or HTTP types.
- `:shared/AppFactory` is the only place that wires `HttpClient` → `*RemoteDataSourceImpl` → `*RepositoryImpl`.
- Each platform entry point sets the base API URL before the UI starts.
- `:server` is completely independent of all client modules.

---

## Domain Models

### Core Entities

| Model | Key Fields |
|---|---|
| `Alert` | id, captureId, cameraId, aiConfidence, createdAt, status, priority, assignedToUserId, closedAt, notes, title, message, type, sourceName, thumbnailUrl, isRead, folder |
| `Camera` | id, name, isActive, siteId, createdAt, installedAt, expectedImages |
| `Site` | id, code, name, description, isActive, createdAt, updatedAt, centerLat, centerLng, region |
| `User` | id, email, fullName, isActive, mfaEnabled, createdAt, updatedAt, lastLoginAt |
| `Capture` | id, cameraId, capturedAt, imageUrl, thumbUrl, sha256, widthPx, heightPx, fileSizeBytes, source, metadataJson |
| `Notification` | id, alertId, contactPointId, channel, recipient, payloadJson, sentAt, deliveryStatus, failureReason |
| `UserContactPoint` | id, userId, type, value, label |
| `AlertStatusHistory` | id, alertId, changedByUserId, fromStatus, toStatus, changedAt, comment |

### Enumerations

| Enum | Values |
|---|---|
| `AlertStatus` | `OPEN`, `IN_PROGRESS`, `CLOSED` |
| `AlertPriority` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `AlertType` | `MOTION_DETECTED`, `IMAGE_UPLOADED`, `SYSTEM_WARNING`, `POSSIBLE_CROCODILE`, `BATTERY_LOW`, `SYNC_COMPLETED`, `UNKNOWN` |
| `ContactPointType` / `NotificationChannel` | `EMAIL`, `SMS`, `WHATSAPP`, `PUSH` |
| `DeliveryStatus` | `PENDING`, `SENT`, `FAILED` |

---

## Server API

Base URL: `http://<host>:8080`

Authentication: `X-API-Key: <CROC_API_KEY>` header (skipped when env var is blank in dev mode).

### Alerts — `/alerts`
| Method | Path | Description |
|---|---|---|
| `GET` | `/alerts` | List all alerts (optional `?since=<timestamp>` for incremental sync) |
| `GET` | `/alerts/{id}` | Get alert by ID |
| `POST` | `/alerts` | Create alert |
| `PUT` | `/alerts/{id}` | Update alert |
| `DELETE` | `/alerts/{id}` | Delete alert |

### Cameras — `/cameras`
| Method | Path | Description |
|---|---|---|
| `GET` | `/cameras` | List all cameras |
| `GET` | `/cameras/{id}` | Get camera by ID |
| `POST` | `/cameras` | Create camera |
| `PUT` | `/cameras/{id}` | Update camera |
| `DELETE` | `/cameras/{id}` | Delete camera |
| `GET` | `/cameras/{id}/daily-stats/{date}` | Daily stats for one camera (`date` format: `YYYY-MM-DD`) |
| `GET` | `/cameras/daily-stats/{date}` | Daily stats for all cameras |

### Captures — `/captures`
| Method | Path | Description |
|---|---|---|
| `GET` | `/captures` | List all captures |
| `GET` | `/captures/{id}` | Get capture by ID |
| `GET` | `/captures/by-camera/{cameraId}` | Captures for a specific camera |
| `GET` | `/captures/by-folder/{folder}` | Captures in a specific folder |
| `POST` | `/captures` | Create capture |
| `PUT` | `/captures/{id}` | Update capture |
| `DELETE` | `/captures/{id}` | Delete capture |

### Sites — `/sites`
| Method | Path | Description |
|---|---|---|
| `GET` | `/sites` | List all sites |
| `GET` | `/sites/{id}` | Get site by ID |

The server connects to **Firebase Firestore** using a service account key at `/firebase/serviceAccountKey.json` in the classpath.

---

## Build & Run

### Prerequisites

- Android Studio with KMP plugin
- JDK 11+
- Android emulator (Pixel 5 or equivalent) for mobile testing
- macOS + Xcode for iOS builds

### Server

Requires `server/src/main/resources/firebase/serviceAccountKey.json`.

```shell
# macOS / Linux
./gradlew :server:run

# Windows
.\gradlew.bat :server:run
```

> Set `CROC_API_KEY` env var to enable API key authentication. Leave it blank to skip auth in dev mode.

### Android

```shell
# Build debug APK — macOS / Linux
./gradlew :androidApp:assembleDebug

# Build debug APK — Windows
.\gradlew.bat :androidApp:assembleDebug

# Install on connected device / emulator — macOS / Linux
./gradlew :androidApp:installDebug

# Install on connected device / emulator — Windows
.\gradlew.bat :androidApp:installDebug
```

> Connects to `http://10.0.2.2:8080` by default (Android emulator loopback).
> Change `ApiRoutes.BASE` in `MainActivity.kt` to your machine's LAN IP for a real device.

### Desktop (JVM)

```shell
# macOS / Linux
./gradlew :desktopApp:run

# Windows
.\gradlew.bat :desktopApp:run
```

> Set `CROCALERT_API_URL` env var to override the default `http://localhost:8080`.

### iOS

Open the [/iosApp](./iosApp) directory in Xcode (macOS only) and run the scheme from the toolbar.

> iOS targets are disabled on Windows. Add `kotlin.native.ignoreDisabledTargets=true` to `gradle.properties` to suppress the warning (already configured).

---

## Configuration

| File | Purpose |
|---|---|
| `server/src/main/resources/firebase/serviceAccountKey.json` | Firebase Admin SDK credentials — **not committed** (see `.gitignore`) |
| `local.properties` | Android SDK path — **not committed** |
| `gradle/libs.versions.toml` | Centralized version catalog for all dependencies |
| `gradle.properties` | KMP/native flags (`kotlin.native.ignoreDisabledTargets=true`) |

---

## Key Dependencies

| Dependency | Version | Used In |
|---|---|---|
| Kotlin Multiplatform | 2.0.21 | All modules |
| Compose Multiplatform | 1.6.11 | `:composeApp` |
| Navigation Compose | 2.7.0-alpha07 | `:composeApp` |
| AndroidX Lifecycle | 2.8.0 | `:composeApp` |
| Ktor Client | 2.3.12 | `:shared` |
| Ktor Server (Netty) | 2.3.12 | `:server` |
| SQLDelight | 2.0.2 | `:shared` |
| WorkManager | 2.9.1 | `:androidApp` (background sync) |
| DataStore | 1.1.1 | `:androidApp` (sync preferences) |
| Firebase Admin SDK | 9.2.0 | `:server` |
| kotlinx-coroutines | 1.8.1 | `:shared`, `:composeApp` |
| kotlinx-datetime | 0.6.1 | `:shared`, `:composeApp` |
| kotlinx-serialization | 1.7.3 | `:shared`, `:server` |
| Turbine | 1.2.0 | Test (Flow assertions) |
| ktor-client-mock | 2.3.12 | Test (HTTP mocking) |

---

## Testing

Tests live in `shared/src/commonTest/` and `server/src/test/`.

| Test | What it covers |
|---|---|
| `AlertRepositoryImplTest` | Cache behavior, sync logic, `lastRefreshError` emission |
| `AlertRemoteDataSourceImplTest` | Ktor HTTP mock — request/response parsing |
| `AlertMapperTest` | DTO ↔ domain model conversions |
| `AlertStatusValidatorTest` | Valid status transitions (`OPEN → IN_PROGRESS → CLOSED`) |
| `CameraRepositoryImplTest` | Camera cache and sync |
| `CameraMapperTest` | Camera DTO ↔ model |
| `SyncPreferencesTest` | TTL preference read/write |
| `SafeCallTest` | `safeCall()` error handling wrapper |
| `AlertsRouteTest` | Server happy-path and error responses |
| `AuthTest` | `X-API-Key` validation |
| `ServerErrorHandlingTest` | HTTP error status codes |

Test infrastructure uses `FakeAlertRemoteDataSource`, `InMemoryAlertLocalDataSource`, `runTest {}` (coroutines), and Turbine for Flow testing.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)