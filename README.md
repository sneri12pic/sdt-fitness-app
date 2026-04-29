# SDT Fitness App

SDT Fitness App is an Android workout tracker built with Kotlin and Jetpack Compose. It focuses on simple daily consistency: start a gym session, log sets, add exercises, track daily steps, review completed workouts, and view progress over time.

## Screenshots

<table>
  <tr>
    <td align="center"><strong>Home</strong></td>
    <td align="center"><strong>Home dashboard</strong></td>
    <td align="center"><strong>Workout setup</strong></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/home.jpg" width="220" alt="Home screen"></td>
    <td><img src="docs/images/readme/home2.jpg" width="220" alt="Home dashboard screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Ongoing workout</strong></td>
    <td align="center"><strong>Workout logging</strong></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/OngoingWorkout.jpg" width="220" alt="Ongoing workout screen"></td>
    <td><img src="docs/images/readme/OngoingWorkout2.jpg" width="220" alt="Workout set logging screen"></td>
    <td><img src="docs/images/readme/AddExercise.jpg" width="220" alt="Add exercise screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Exercise selected</strong></td>
    <td align="center"><strong>Progress</strong></td>
    <td align="center"><strong>Progress details</strong></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/AddExerciseSelected.jpg" width="220" alt="Selected exercise screen"></td>
    <td><img src="docs/images/readme/Progress.jpg" width="220" alt="Progress overview screen"></td>
    <td><img src="docs/images/readme/Progress2.jpg" width="220" alt="Progress details screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Completed sessions</strong></td>
    <td align="center"><strong>Session review</strong></td>
    <td></td>
  </tr>
  <tr>
    <td><img src="docs/images/readme/progress-sessionsCompleted.jpg" width="220" alt="Completed sessions screen"></td>
    <td><img src="docs/images/readme/progress-sessionsCompleted-session.jpg" width="220" alt="Completed session review screen"></td>
    <td></td>
  </tr>
</table>

## Features

- Home dashboard with daily goal progress, today's plan, daily step quest, routine calendar, quick log, and rest day logging.
- Workout setup flow with exercise selection, adjustable weight and reps, session shortening, and custom exercise sets.
- Ongoing workout tracking with set completion, exercise progress, exercise deletion, and add-exercise support during an active session.
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
