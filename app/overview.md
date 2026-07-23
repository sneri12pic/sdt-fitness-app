# SDT Fitness Login Architecture and Screen Overview

Design-system source of truth: [[docs/design-system/Design System Hub|SDT Fitness Design System]]. This note remains authoritative for authentication behavior; shared visual decisions belong in the design-system branch.

This document describes the current login architecture and login screen design in the SDTFitnessApp Android project. It is written so a future developer or ChatGPT session can quickly understand what exists, why it was built this way, and what design choices should be preserved or improved next.

## Purpose of the Login Work

The app previously opened directly into `Home`. It now starts through an authentication gate so the app can decide whether to restore an authenticated session, show the login screen, or allow the user to continue as a local guest.

The main architectural decision is that login does not replace the existing local account model. The app already scopes workout, progress, and settings data by `accountId`, so authentication now links a remote identity to an `AccountEntity`. Existing guest accounts and their local workout data are preserved.

## Main Files

- `src/main/java/com/stepandemianenko/sdtfitness/auth/ui/AuthGateActivity.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/ui/LoginScreen.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/ui/CredentialAuthClient.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/viewmodel/AuthViewModel.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/domain/AuthRepository.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/domain/AuthResult.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/domain/SessionState.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/data/AuthRepositoryImpl.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/data/RemoteAuthDataSource.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/auth/data/SecureSessionStore.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/data/account/AccountSessionManager.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/data/local/WorkoutEntities.kt`
- `src/main/java/com/stepandemianenko/sdtfitness/data/local/WorkoutDatabase.kt`
- `src/main/AndroidManifest.xml`

## Current Launch Flow

`AuthGateActivity` is now the launcher activity. `Home` is no longer exported and is opened only after the auth gate decides the user can continue.

On start:

1. `AuthGateActivity` starts and applies `FLAG_SECURE`, which blocks screenshots and screen recording on the auth surface.
2. `AuthGateRoute` creates or receives an `AuthViewModel`.
3. `AuthViewModel` calls `authRepository.restoreSession()`.
4. If a stored refresh token exists and the remote backend refresh succeeds, the user becomes authenticated and the app navigates to `Home`.
5. If there is no stored session, or refresh fails, the state becomes `SignedOut` and the login screen is shown.
6. If the user chooses `Continue as guest`, the app switches to a guest account and navigates to `Home`.

## Current Login Screen Layout

The login screen is a full-screen Jetpack Compose screen built in `LoginScreen.kt`.

The screen is intentionally simple and focused. There is no decorative image, no card container, and no complex navigation. The content is centered vertically and horizontally within the full screen.

The structure is:

1. A full-screen `Surface`.
2. A centered outer `Column` that fills the screen.
3. An inner `Column` with a maximum width of `380.dp`.
4. A large title.
5. A short subtitle.
6. Email input.
7. Password input.
8. Confirm-password input when Register mode is active.
9. Optional error message.
10. Main sign-in/register button.
11. Register/sign-in mode switch button.
12. Saved credential button in Sign In mode.
13. Guest continuation button with helper text.

The screen uses these exact layout dimensions:

- Full-screen root via `Modifier.fillMaxSize()`.
- Outer padding: `24.dp` horizontal and `36.dp` vertical.
- Inner content max width: `380.dp`.
- Vertical spacing between main elements: `14.dp`.
- Small spacer after subtitle: `4.dp`.
- Primary button height: `54.dp`.
- Primary button corner radius: `10.dp`.

This gives the screen a compact mobile-first form layout. It should fit narrow Android screens without requiring horizontal scrolling.

## Current Screen Text

The current visible text on the login screen is:

- Title: `SDT Fitness`
- Subtitle: `Sign in to sync your workouts.`
- Email field label: `Email`
- Password field label: `Password`
- Primary button in Sign In mode: `Sign in`
- Primary button in Register mode: `Register`
- Register mode switch: `Need an account? Register`
- Sign-in mode switch: `Already have an account? Sign in`
- Secondary button: `Use saved credential`
- Guest button: `Continue as guest`
- Guest helper text: `Use the app locally without sync.`

Current error messages:

- Empty email or password: `Enter your email and password.`
- Empty registration fields: `Enter your email, password, and confirmation.`
- Registration password mismatch: `Passwords do not match.`
- Registration password too short: `Password must be at least 8 characters.`
- Missing or invalid backend configuration: `Sign in is not available right now.`
- Missing or invalid registration backend configuration: `Registration is not available right now.`
- Invalid credentials, network error, or unknown auth failure: `We could not sign you in. Check your details and try again.`
- Registration network or unknown failure: `We could not create your account. Try again later.`

The UI intentionally keeps auth errors generic so sensitive backend/auth details are not exposed to users.

## Current Colour Scheme

The login screen uses the same warm fitness-app palette as the rest of the app.

Current login-specific colours:

- Background: `Color(0xFFEBC0B0)`
- Primary text: `Color(0xFF4F2912)`
- Secondary text: `Color(0xFF6B4637)`
- Primary action button: `Color(0xFFF27F3E)`
- Primary action text and loading spinner: `Color(0xFFFFF2E9)`
- Error text: `Color(0xFF9A2E1F)`

Design interpretation:

- The background is a warm peach tone and matches the Home/Profile visual language.
- The title uses a deep brown colour to keep strong contrast without using harsh black.
- The subtitle uses a softer brown for supporting text.
- The sign-in button uses a warm orange accent that matches the app's action colour family.
- The button text uses a pale cream tone for contrast and consistency with existing buttons.
- Error text uses a dark red-brown tone that reads as warning/error while still fitting the warm palette.

The email, password, and confirm-password fields use custom `OutlinedTextField` colours so the active/focused outline uses the app's orange action colour instead of the default Material purple.

## Typography

The login screen uses direct Compose text sizes rather than a centralized typography scale.

Current typography:

- Title: `36.sp`, line height `38.sp`, `FontWeight.Bold`
- Subtitle: `16.sp`, line height `20.sp`
- Button text: `18.sp`, line height `18.sp`, `FontWeight.SemiBold`
- Error text: `13.sp`, line height `16.sp`

The title is large enough to establish brand identity but not so large that it creates a marketing-style landing page. The screen remains a functional login tool.

## Input Fields

There are two inputs:

1. Email input
   - `OutlinedTextField`
   - Full width
   - Single line
   - Keyboard type: `KeyboardType.Email`
   - Disabled while loading

2. Password input
   - `OutlinedTextField`
   - Full width
   - Single line
   - Keyboard type: `KeyboardType.Password`
   - Uses `PasswordVisualTransformation` unless the visibility toggle is active
   - Disabled while loading
   - Has a password visibility toggle

3. Confirm-password input
   - Shown only in Register mode
   - `OutlinedTextField`
   - Full width
   - Single line
   - Keyboard type: `KeyboardType.Password`
   - Uses `PasswordVisualTransformation` unless the visibility toggle is active
   - Disabled while loading
   - Has a password visibility toggle

Current behavior:

- Email is trimmed in the ViewModel when changed.
- Password is kept as typed while editing.
- Password and confirm password are cleared after a success or failure.
- Fields are disabled while an auth request is in progress.
- Email IME moves to password.
- Login password IME submits sign-in.
- Registration password IME moves to confirm password.
- Registration confirm-password IME submits registration.

Security note:

Passwords are never stored in Room. Email/password credentials are sent only to a configured HTTPS backend through `RemoteAuthDataSource`.

## Buttons and Actions

### Sign In

The `Sign in` button is the primary action. It is a filled Material 3 `Button` with:

- Full width
- Height `54.dp`
- Corner radius `10.dp`
- Orange background
- Cream text

When loading, the button content changes from text to a cream `CircularProgressIndicator`.

### Use Saved Credential

The `Use saved credential` button uses AndroidX Credential Manager.

Current behavior:

- It asks the system for a saved password credential.
- If a credential is returned, the app sends the saved email/password pair to the same backend login flow.
- If no credential is returned, a generic login failure is shown.

This is an initial Credential Manager integration. It currently supports saved password credentials. It is structured so passkeys or federated sign-in can be added later without pushing credential logic into the UI.

### Register

Register mode is available from the login screen through `Need an account? Register`.

Current behavior:

- Shows email, password, and confirm-password fields.
- Validates blank fields, password mismatch, and minimum password length locally.
- Sends registration only to the remote backend through `POST /auth/register`.
- Uses the same secure response handling as login.
- Saves refresh tokens only through `SecureSessionStore`.
- Links or creates the local authenticated account through `AccountSessionManager.linkAuthenticatedAccountAndSwitch()`.
- Navigates to `Home` through the same auth-gate session-state mechanism as sign-in.

### Continue As Guest

The `Continue as guest` action preserves the existing local-first app behavior.

Current behavior:

- It calls `accountSessionManager.switchToGuestAccount()`.
- It switches to the most recent guest account or creates one if needed.
- It changes session state to `Guest`.
- The auth gate then opens `Home`.

This lets users keep using the app without creating an account.

## MVVM Architecture

The auth feature follows MVVM with clear package boundaries.

### UI Layer

Files:

- `AuthGateActivity.kt`
- `LoginScreen.kt`
- `CredentialAuthClient.kt`

Responsibilities:

- Render auth state.
- Collect `AuthUiState` with lifecycle awareness.
- Send user actions to `AuthViewModel`.
- Navigate to `Home` only after session state becomes `Authenticated` or `Guest`.
- Keep Credential Manager interaction out of the ViewModel because it needs Android UI context.

### ViewModel Layer

File:

- `AuthViewModel.kt`

Responsibilities:

- Own `AuthUiState`.
- Validate blank email/password input.
- Convert UI events into repository calls.
- Expose loading and error state.
- Clear password after auth attempts.
- Keep user-facing error messages generic.

Current `AuthUiState` fields:

- `mode`
- `email`
- `password`
- `confirmPassword`
- `isPasswordVisible`
- `isConfirmPasswordVisible`
- `isLoading`
- `errorMessage`
- `sessionState`

Current `AuthUiEvent` events:

- `EmailChanged`
- `PasswordChanged`
- `ConfirmPasswordChanged`
- `SwitchToSignIn`
- `SwitchToRegister`
- `TogglePasswordVisibility`
- `ToggleConfirmPasswordVisibility`
- `SubmitEmailLogin`
- `SubmitRegistration`
- `ContinueAsGuest`
- `SavedPasswordReceived`
- `CredentialTokenReceived`
- `CredentialLoginFailed`

### Domain Layer

Files:

- `AuthRepository.kt`
- `AuthResult.kt`
- `SessionState.kt`

Responsibilities:

- Define auth behavior independent of UI.
- Represent session states: `Checking`, `SignedOut`, `Guest`, `Authenticated`.
- Represent auth outcomes and failure reasons.

### Data Layer

Files:

- `AuthRepositoryImpl.kt`
- `RemoteAuthDataSource.kt`
- `SecureSessionStore.kt`

Responsibilities:

- Talk to the remote auth backend.
- Store refresh tokens securely.
- Keep access tokens in memory.
- Link successful auth responses to local `AccountEntity` rows.
- Restore sessions on cold start.
- Clear sessions on sign out.

## Remote Auth Contract

The current remote auth implementation is HTTPS-only and uses `HttpURLConnection`.

The matching local backend lives in the root `auth-api` Gradle module. It is a Kotlin/Ktor service that exposes the Android auth contract and can be run locally with:

```powershell
.\gradlew.bat :auth-api:run
```

The backend base URL is read from the Gradle property:

```text
SDT_AUTH_BASE_URL
```

It becomes:

```kotlin
BuildConfig.AUTH_BASE_URL
```

The remote data source refuses blank or non-HTTPS base URLs and returns a service-unavailable auth failure.

Expected backend endpoints:

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/credential`
- `POST /auth/refresh`

Expected successful response fields:

- `remoteUserId`
- `email`
- `displayName`
- `authProvider`
- `accessToken`
- `refreshToken`
- `accessTokenExpiresAtMillis`

Current design rule:

The app must not authenticate email/password locally. A real backend must validate credentials.

## Token and Session Security

The current implementation follows these rules:

- Access token is kept in memory in `AuthRepositoryImpl`.
- Refresh token is stored through `SecureSessionStore`.
- Refresh token is encrypted with an Android Keystore-backed AES-GCM key.
- The encrypted session file is written to `context.noBackupFilesDir`.
- The encrypted session file name is `sdt_auth_session.bin`.
- Session files are excluded from Android backup/data extraction rules.
- Tokens are not stored in Room.
- Tokens are not logged.
- Raw auth responses are not logged.

`AuthGateActivity` uses `FLAG_SECURE` to prevent screenshots and screen recording while the user is on the auth surface.

The app manifest has:

- `android:usesCleartextTraffic="false"`
- `INTERNET` permission for backend auth

## Room and Account Model Changes

The Room database is now version 4.

The `accounts` table gained nullable auth metadata fields:

- `remoteUserId`
- `email`
- `displayName`
- `authProvider`
- `lastLoginAt`

Indexes were added for remote identity lookup:

- `remoteUserId`
- unique `authProvider + remoteUserId`

This lets the app link remote users to local accounts without changing existing workout tables. Existing workout, exercise, set log, and settings rows still reference `accountId`.

Migration behavior:

- Existing guest rows survive.
- Existing workout data survives.
- New auth columns are nullable, so old accounts migrate cleanly.
- Authenticated users get `type = auth`.
- Guest users keep `type = guest`.

## Account Linking Behavior

On successful login:

1. The backend returns a remote user identity and tokens.
2. The refresh token is securely saved.
3. `AccountSessionManager.linkAuthenticatedAccountAndSwitch()` runs.
4. The manager searches for an existing account with the same `authProvider` and `remoteUserId`.
5. If found, it updates the auth profile and activates that account.
6. If not found, it creates a new auth account, creates default user settings, and activates it.
7. Existing guest data is not wiped or automatically merged.

This is deliberate. Guest-to-auth data merge can be added later as an explicit user-controlled flow.

## Sign Out From Profile

The Profile screen now includes a top-right vertical three-dot menu. Opening it shows a settings dialog with a destructive red `Sign Out` button.

Sign-out behavior:

- Shows a confirmation dialog before signing out.
- Confirmation title: `Sign out?`
- Confirmation body: `You can sign back in anytime. Local guest data will stay on this device.`
- Confirm action: `Sign Out`
- Cancel action: `Cancel`
- Calls `AuthRepository.signOut()`.
- Clears the secure refresh token.
- Clears the in-memory access token.
- Navigates back to `AuthGateActivity`.
- Clears the back stack so Back cannot return to authenticated screens.
- Does not delete Room accounts.
- Does not delete guest data.
- Does not delete workout, progress, or settings data.

## Current Limitations

- There is no forgot-password flow yet.
- Credential Manager support currently handles saved password credentials; passkey/federated functionality is not fully built out.
- The local `auth-api` module implements email/password auth endpoints; production deployment, production database configuration, and password-reset support are still future work.
- Without `SDT_AUTH_BASE_URL`, email/password login correctly fails as unavailable.
- Release minification is still disabled.

## Testing and Verification

Current verification performed after implementation:

- `.\gradlew.bat :auth-api:test`
- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:compileDebugAndroidTestKotlin`

Added or updated tests:

- Secure session store unit test for save/load and clear behavior.
- Room migration test source updated to include migration from version 3 to 4.

Connected Android tests were compiled but not run against an emulator/device.

## Improvement Ideas Requested by Product Owner

This section captures the requested improvements for the next auth iteration.

### Add Registration

Requested idea:

> I would like an addition of a register button with proper functionalities.

Status: Implemented.

Recommended implementation details:

- Add a `Register` button or text button below the `Sign in` button.
- Add a `RegisterScreen` or a mode toggle inside the auth flow.
- Add a new `AuthUiEvent.SubmitRegistration`.
- Add `AuthRepository.registerWithEmail(...)`.
- Add `RemoteAuthDataSource.registerWithEmail(...)`.
- Expected backend endpoint could be `POST /auth/register`.
- Registration should require at minimum email and password.
- Consider adding confirm password if this remains email/password based.
- Registration should use the same security rules as login:
  - Backend validates credentials.
  - No password stored locally.
  - Refresh token stored only through `SecureSessionStore`.
  - Account linked through `AccountSessionManager.linkAuthenticatedAccountAndSwitch()`.
- After successful registration, navigate to `Home` the same way successful login does.
- Keep registration errors generic, but allow safe field validation such as blank fields or mismatched passwords.

### Fix Text Field Focus Colour

Requested idea:

> I would like the Email input Box and password input box not to have this weird purple outline when on active make it fit the colour scheme better.

Status: Implemented.

Recommended implementation details:

- Customize `OutlinedTextField` colours in `LoginScreen.kt`.
- Use warm palette colours instead of default Material purple.
- Suggested focused border colour: `AuthAction` / `Color(0xFFF27F3E)`.
- Suggested unfocused border colour: `AuthSecondaryText.copy(alpha = 0.45f)`.
- Suggested focused label colour: `AuthPrimaryText` or `AuthAction`.
- Suggested cursor colour: `AuthAction`.
- Suggested text colour: `AuthPrimaryText`.
- Suggested container colour: a pale warm cream such as `Color(0xFFFFEFE5)` or transparent if the current minimal look is preferred.

The goal is for input focus to feel native to the existing peach, brown, and orange design system.

### Add Sign Out From Profile

Requested idea:

> After we log in or register, I want us to be able to log off if needed, i want it to be in the profile screen maybe create an icon of settings or 3 vertical lines icon where there will BE a red button saying Sign Out.

Status: Implemented.

Recommended implementation details:

- Add a settings/menu affordance to the Profile screen.
- The icon could be:
  - a settings gear icon, or
  - a vertical three-dot menu icon.
- Prefer Material/lucide-style iconography if an icon library is available.
- Place the icon near the top-right of the Profile overview header.
- Opening the icon should reveal a small settings/menu panel or a dedicated settings screen.
- Add a destructive red `Sign Out` button.
- Suggested red button colour: a clear destructive red, for example `Color(0xFFC62828)`.
- Suggested text colour: white or warm cream.
- Add a confirmation dialog before signing out:
  - Title: `Sign out?`
  - Body: `You can sign back in anytime. Local guest data will stay on this device.`
  - Confirm: `Sign Out`
  - Cancel: `Cancel`
- On confirmation:
  - Call `AuthRepository.signOut()`.
  - Clear the secure refresh token.
  - Clear in-memory access token.
  - Navigate back to `AuthGateActivity`.
  - Clear the back stack so Back does not return to authenticated screens.
- Do not wipe Room data during sign out.
- Do not delete guest or authenticated account rows during sign out.

## Suggested Next Implementation Order

1. Add passkey/federated Credential Manager support after backend capabilities are finalized.
2. Add a real forgot-password flow only when a backend endpoint exists.
3. Add guest-to-auth data merge as a separate explicit flow, not as automatic registration behavior.
4. Review release minification and ProGuard/R8 rules before production release.
