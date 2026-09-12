# SDT Fitness App

SDT Fitness App is a Kotlin Android workout tracker built with Jetpack Compose. It supports a local guest mode for offline use and an optional Kotlin/Ktor authentication API for email/password accounts, refresh-token restore, and account-scoped workout data.

The app is organized around daily consistency: plan a routine, start a session, add exercises, log sets, track steps and weight through Health Connect, review completed workouts, and follow progress over time.

## Current App Surface

- Home dashboard with today's plan, routine calendar, daily quest progress, quick activity logging, rest day logging, and Health Connect step imports.
- Workout builder with exercise search/filtering, editable targets, custom exercises, and empty-state guidance.
- Active workout logging with set completion, previous-set feedback, exercise deletion, rest timer flow, and add-exercise support during a session.
- Progress screens for completed sessions, best lift, total volume, session load, achievements, Health Connect metrics, and per-session review.
- Profile area for routine setup, reminder scheduling, account controls, and user preferences.
- Authentication gate with email/password login and registration, refresh-token session restore, saved credential support, guest mode, and sign out.
- Companion `auth-api` module that implements the Android auth contract for local development or deployment.

## Demo

A short walkthrough of the main app flow: planning a routine, starting a session, logging sets, and reviewing progress.

https://github.com/user-attachments/assets/efff661e-f4e8-4b23-8123-3ca83de7efae

## Screenshots

Current prototype screens from the main app flow.

<table>
  <tr>
    <td align="center"><strong>Home</strong></td>
    <td align="center"><strong>Routine</strong></td>
    <td align="center"><strong>Empty workout</strong></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/home.jpg" width="220" alt="Home screen"></td>
    <td><img src="docs/images/readme/home-routine.jpg" width="220" alt="Home routine screen"></td>
    <td><img src="docs/images/readme/empty-workout.jpg" width="220" alt="Empty workout screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Add exercise</strong></td>
    <td align="center"><strong>Filtered exercises</strong></td>
    <td align="center"><strong>Log workout</strong></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/add-exercise.jpg" width="220" alt="Add exercise screen"></td>
    <td><img src="docs/images/readme/add-exercise-filtered.jpg" width="220" alt="Filtered exercises screen"></td>
    <td><img src="docs/images/readme/log-workout.jpg" width="220" alt="Log workout screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Progress</strong></td>
    <td align="center"><strong>Health metrics</strong></td>
    <td align="center"><strong>Completed sessions</strong></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/progress-overview.jpg" width="220" alt="Progress overview screen"></td>
    <td><img src="docs/images/readme/progress-health.jpg" width="220" alt="Health metrics screen"></td>
    <td><img src="docs/images/readme/completed-sessions.jpg" width="220" alt="Completed sessions screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Session review</strong></td>
    <td></td>
    <td></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/session-review.jpg" width="220" alt="Completed session review screen"></td>
    <td></td>
    <td></td>
  </tr>
</table>

## Tech Stack

- Kotlin 2.0 and Android Gradle Plugin 8.11.
- Jetpack Compose, Material 3, AndroidX Lifecycle, ViewModel, and Compose UI tests.
- Room database with exported schemas under `app/schemas`.
- Health Connect client for steps and weight.
- AndroidX Credential Manager for saved credential flows.
- Kotlin/Ktor auth API with H2 local storage, PostgreSQL support, Flyway migrations, JWT access tokens, refresh-token rotation, bcrypt password hashing, and JUnit 5 tests.

## Project Structure

```text
SDTFitnessApp/
+-- app/
|   +-- src/main/java/com/stepandemianenko/sdtfitness/
|   |   +-- auth/          # Auth gate, login/register UI, session restore, secure token storage
|   |   +-- data/          # App graph, Room database, repositories, account/session data, Health Connect
|   |   +-- domain/        # Domain models, repository contracts, use cases
|   |   +-- home/          # Home dashboard state and repositories
|   |   +-- profile/       # Profile state, routine reminders, user settings
|   |   +-- progress/      # Progress, completed sessions, charts, and session review screens
|   |   +-- quicklog/      # Quick activity logging
|   |   +-- startworkout/  # Workout setup, exercise picker, active workout flow, rest timer
|   |   +-- ui/            # Theme and shared UI components
|   +-- schemas/           # Room schema exports
+-- auth-api/
|   +-- src/main/kotlin/   # Ktor authentication service
|   +-- src/main/resources/db/migration/
|   +-- README.md          # API contract, environment variables, curl examples, tunnel setup
+-- docs/images/readme/    # README screenshots
+-- gradle/libs.versions.toml
```

## Requirements

- Android Studio with JDK 17 available for Gradle.
- Android SDK 36 installed.
- Emulator or physical device running Android 9.0 or newer, because `minSdk` is 28.
- Optional: Health Connect installed and permissions granted for steps and weight.
- Optional for remote auth testing: `cloudflared`, ngrok, or another HTTPS tunnel.

## Run The Android App

Open the project root in Android Studio, let Gradle sync, select the `app` run configuration, and run it on an emulator or device.

The app can be used immediately as a local guest. Guest and authenticated account data are stored locally with Room and are scoped per account.

From PowerShell, useful local commands are:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Run With The Auth API

The Android client only uses the auth API when `SDT_AUTH_BASE_URL` is supplied at build time. The URL must be HTTPS for emulator/device auth testing.

Start the local API:

```powershell
.\gradlew.bat :auth-api:run
```

In another terminal, expose it through HTTPS:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Install the debug app with the generated tunnel URL:

```powershell
.\gradlew.bat :app:installDebug -PSDT_AUTH_BASE_URL=https://your-tunnel-url.trycloudflare.com
```

If the tunnel URL changes, rebuild and reinstall the app with the new `SDT_AUTH_BASE_URL`, because the value is compiled into `BuildConfig.AUTH_BASE_URL`.

See [auth-api/README.md](auth-api/README.md) for the full endpoint contract, environment variables, curl examples, and production database settings.

## Auth API Commands

```powershell
.\gradlew.bat :auth-api:test
.\gradlew.bat :auth-api:run
Invoke-RestMethod http://localhost:8080/health
```

Local API defaults use H2 under `auth-api/build`. Production-style runs should provide PostgreSQL settings, JWT secret, refresh-token pepper, allowed origins, and port through environment variables documented in [auth-api/README.md](auth-api/README.md).

## Data And Health Connect

- Workout sessions, exercise plans, accounts, user settings, daily quest records, and creatine intake logs are persisted with Room.
- Room schema exports live under `app/schemas/com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase`.
- Health Connect imports today's steps and recent weight history when the provider is available and the user grants the requested permissions.
- Remote auth links server users to local Room accounts without deleting guest workout history.
- Refresh tokens are stored through secure local session storage; passwords are never stored in Room.

## Development Notes

- Main Android package: `com.stepandemianenko.sdtfitness`.
- Main modules: `:app` and `:auth-api`.
- Android target SDK: 36.
- Android minimum SDK: 28.
- Keep auth API field names aligned with `RemoteAuthDataSource`; the Android client expects the current response contract exactly.
- Keep Room schemas committed when database entities or migrations change.


## Research
[Balancing Hedonic and Utilitarian Design in Fitness Applications A Self-Determination Theory-Informed Prototype Study.pdf](https://github.com/user-attachments/files/32147100/Balancing.Hedonic.and.Utilitarian.Design.in.Fitness.Applications.A.Self-Determination.Theory-Informed.Prototype.Study.pdf)
