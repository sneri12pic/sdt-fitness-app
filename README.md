# SDT Fitness App

SDT Fitness App is an Android workout tracker built with Kotlin and Jetpack Compose. It focuses on simple daily consistency: start a gym session, log sets, add exercises, track daily steps, review completed workouts, and view progress over time.

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

## Features

- Home dashboard with daily goal progress, today's plan, daily step quest, routine calendar, quick log, and rest day logging.
- Workout flow with an empty-state prompt, exercise search/filtering, custom exercise sets, and active set logging.
- Active workout tracking with set completion, exercise progress, exercise deletion, and add-exercise support during a session.
- Progress area with completed sessions, best lift, volume, session load, achievements, and completed-session review.
- Optional Health Connect integration for reading steps and weight.
- Local, account-scoped persistence using Room.

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Lifecycle and ViewModel
- Room database with schema exports
- Health Connect client
- JUnit, AndroidX test, Espresso, and Compose UI testing

## Project Structure

```text
SDTFitnessApp/
+-- app/
|   +-- src/main/java/com/stepandemianenko/sdtfitness/
|   |   +-- data/          # Room database, repositories, account/session data
|   |   +-- home/          # Home dashboard state and UI models
|   |   +-- progress/      # Progress, session history, and review screens
|   |   +-- quicklog/      # Quick activity logging
|   |   +-- startworkout/  # Workout setup, exercise picker, active workout flow
|   +-- schemas/           # Room schema exports
+-- docs/
|   +-- images/readme/     # README screenshots
+-- gradle/
    +-- libs.versions.toml # Version catalog
```

## Getting Started

1. Open the project root in Android Studio.
2. Let Gradle sync install the Android Gradle Plugin, Kotlin, Compose, Room, and Health Connect dependencies.
3. Select the `app` run configuration.
4. Run the app on an emulator or physical Android device.

The app targets SDK 36 and has a minimum SDK of 28.

## Useful Commands

Run these from the project root:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Health Connect

The app declares Health Connect permissions for reading steps and weight. On a device, Health Connect availability and permission grants determine whether imported health data can be shown or synced into the daily quest/progress surfaces.

## Notes

- Workout and settings data are stored locally with Room.
- The debug build includes account tools for creating test users, switching accounts, and wiping current-account data.
- Room schemas are exported under `app/schemas` to support migration testing.
