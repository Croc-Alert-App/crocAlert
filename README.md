# CrocAlert

A Kotlin Multiplatform (KMP) wildlife monitoring system that detects crocodile sightings via camera traps, generates alerts, and notifies field teams across Android, iOS, Desktop (JVM), and a Ktor backend server.

---

## Project Structure

```
crocAlert/
├── composeApp/     # Compose Multiplatform UI — shared across Android, iOS, Desktop
├── shared/         # KMP business logic, domain models, network layer
├── server/         # Ktor backend server (Firebase Firestore + REST API)
├── androidapp/     # Standalone Android prototype (legacy — not the active app)
└── iosApp/         # iOS SwiftUI entry point
```

### Modules

| Module | Plugin | Purpose |
|---|---|---|
| `:composeApp` | `com.android.application` + KMP | Shared Compose UI (Android APK, iOS framework, Desktop JVM) |
| `:shared` | `com.android.library` + KMP | Domain models, repository interfaces, network client, data mappers |
| `:server` | Ktor | REST API server backed by Firebase Firestore |
| `:androidapp` | `com.android.application` | Legacy standalone prototype — not used by the active shared UI |

---

## Architecture

CrocAlert follows a **clean architecture** pattern across the KMP stack. The data flow is strictly one-way:

```
Platform entry points
  androidMain/MainActivity.kt   →  sets ApiRoutes.BASE for emulator
  jvmMain/main.kt               →  sets ApiRoutes.BASE for desktop

composeApp / commonMain
  App.kt                        →  shared Compose UI (AlertsScreen)
                                    uses AlertRepository (interface only)

shared / commonMain
  AppFactory.kt                 →  createAlertRepository() — wires Ktor internally
  AlertRepositoryImpl           →  business logic, Flow<List<Alert>>
  AlertRemoteDataSourceImpl     →  HTTP calls via Ktor (hidden from UI)
  HttpClientFactory (expect)    →  resolved per platform (Android/JVM/iOS)

server
  Ktor routes → AlertService → Firebase Firestore
```

**Layer rules:**
- UI (`composeApp`) only imports domain interfaces and models from `:shared` — never Ktor or HTTP types
- `:shared/AppFactory.kt` is the only place that wires `HttpClient` → `AlertRemoteDataSourceImpl` → `AlertRepositoryImpl`
- Each platform entry point sets `ApiRoutes.BASE` before the UI starts
- `:server` is completely independent of the client modules

---

## Domain Models

| Model | Key Fields |
|---|---|
| `Alert` | id, captureId, status (`OPEN/IN_PROGRESS/CLOSED`), priority (`LOW/MEDIUM/HIGH/CRITICAL`) |
| `Capture` | id, cameraId, imageUrl, capturedAt, sha256 |
| `Camera` | cameraId, siteId, code, name, isActive |
| `Site` | id, code, name, region, centerLat/Lng |
| `User` | id, name, email, contactPoints |
| `Notification` | channel (`EMAIL/SMS/WHATSAPP/PUSH`), deliveryStatus |
| `AlertStatusHistory` | alertId, status, changedAt |

---

## Server API

Base URL: `http://<host>:8080`

| Method | Path | Description |
|---|---|---|
| `GET` | `/alerts` | List all alerts |
| `POST` | `/alerts` | Create a new alert |
| `GET` | `/alerts/{id}` | Get alert by ID |
| `PUT` | `/alerts/{id}` | Update alert |
| `DELETE` | `/alerts/{id}` | Delete alert |

The server connects to **Firebase Firestore** using a service account key loaded from `/firebase/serviceAccountKey.json` in the classpath resources.

---

## Build & Run

### Prerequisites

- Android Studio with KMP plugin
- JDK 11+
- Android emulator (Pixel 5 or equivalent) for mobile testing

### Android (emulator or device)

```shell
# Build APK
.\gradlew.bat :composeApp:assembleDebug

# Or run directly from Android Studio:
# Select the "composeApp" run configuration (Android robot icon) → Run
```

> The app connects to `http://10.0.2.2:8080` by default (Android emulator localhost).
> Change `ApiRoutes.BASE` in `MainActivity.kt` to your machine's LAN IP when testing on a real device.

### Desktop (JVM)

```shell
.\gradlew.bat :composeApp:jvmRun -DmainClass=crocalert.app.MainKt --quiet
```

> The desktop app connects to `http://localhost:8080`.

### Server

```shell
.\gradlew.bat :server:run
```

> Requires `server/src/main/resources/firebase/serviceAccountKey.json` to be present.

### iOS

Open the `/iosApp` directory in Xcode (macOS only) and run the scheme, or use the Xcode run configuration.

> iOS targets are disabled on Windows. Add `kotlin.native.ignoreDisabledTargets=true` to `gradle.properties` to suppress the warning (already configured).

---

## Configuration

| File | Purpose |
|---|---|
| `server/src/main/resources/firebase/serviceAccountKey.json` | Firebase Admin SDK credentials — **not committed** (see `.gitignore`) |
| `composeApp/src/androidMain/.../MainActivity.kt` | Sets `ApiRoutes.BASE` for the Android emulator (`http://10.0.2.2:8080`) |
| `composeApp/src/jvmMain/.../main.kt` | Sets `ApiRoutes.BASE` for Desktop (`http://localhost:8080`) |
| `shared/src/commonMain/.../ApiRoutes.kt` | Declares the mutable `BASE` property and route constants |
| `local.properties` | Android SDK path — **not committed** |

---

## Key Dependencies

| Dependency | Version | Used In |
|---|---|---|
| Kotlin Multiplatform | 2.0.21 | All modules |
| Compose Multiplatform | 1.6.11 | `:composeApp` |
| Ktor Client | 2.3.12 | `:shared` |
| Ktor Server (Netty) | 2.3.12 | `:server` |
| Firebase Admin SDK | 9.2.0 | `:server` |
| Koin | 3.5.6 | `:composeApp` |
| kotlinx-coroutines | 1.8.1 | `:shared`, `:composeApp` |
| kotlinx-datetime | 0.6.1 | `:shared`, `:composeApp` |
| kotlinx-serialization | 1.7.3 | `:shared`, `:server` |

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
