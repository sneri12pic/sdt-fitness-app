# SDT Fitness Auth API

Kotlin/Ktor authentication API for the SDT Fitness Android app.

## Contract

The API matches the Android client in `app/src/main/java/com/stepandemianenko/sdtfitness/auth/data/RemoteAuthDataSource.kt`.

Endpoints:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/credential`

Successful auth responses use the exact Android field names:

```json
{
  "remoteUserId": "stable-public-user-id",
  "email": "user@example.com",
  "displayName": null,
  "authProvider": "password",
  "accessToken": "short-lived-access-token",
  "refreshToken": "long-lived-refresh-token",
  "accessTokenExpiresAtMillis": 1760000000000
}
```

## Environment

Local defaults use an H2 database under `build/auth-api`. Production should set:

```text
AUTH_ENVIRONMENT=production
AUTH_HOST=0.0.0.0
AUTH_DATABASE_URL=jdbc:postgresql://host:5432/sdt_fitness_auth
AUTH_DATABASE_USER=sdt_auth
AUTH_DATABASE_PASSWORD=change-me
AUTH_JWT_SECRET=replace-with-at-least-32-random-characters
AUTH_REFRESH_TOKEN_PEPPER=replace-with-a-different-32-char-secret
AUTH_ALLOWED_ORIGINS=https://your-domain.example
AUTH_TRUST_PROXY_HEADERS=true
PORT=8080
```

Production configuration fails fast unless the database is PostgreSQL and all database/token secrets are present. Keep `AUTH_TRUST_PROXY_HEADERS=true` only when the API is reachable exclusively through a trusted reverse proxy, as it is in the Compose deployment under `deploy/`.

## Deploy

The production Docker Compose stack, automatic HTTPS proxy, environment template, and server runbook are in [`deploy/`](../deploy/README.md).

Optional:

```text
AUTH_ACCESS_TOKEN_TTL_MILLIS=900000
AUTH_REFRESH_TOKEN_TTL_MILLIS=2592000000
AUTH_BCRYPT_COST=12
AUTH_RATE_LIMIT_MAX_REQUESTS=20
AUTH_RATE_LIMIT_WINDOW_MILLIS=60000
```

## Run Locally

From the project root:

```powershell
.\gradlew.bat :auth-api:run
```

The API starts on `http://localhost:8080` by default. You can check that it is running with:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

Expected response:

```json
{"status":"ok"}
```

For Android emulator or device testing against your machine, HTTPS is preferred. Debug builds may use an explicitly allow-listed LAN HTTP host from `network_security_config.xml`; release builds always reject non-HTTPS auth URLs. You can use a tunnel such as Cloudflare Tunnel, ngrok, or another HTTPS reverse proxy.

### Android local development with Cloudflare Tunnel

Use three terminals.

Terminal 1: start the auth API from the project root:

```powershell
cd C:\Users\Stepan\Documents\SDTFitnessApp
.\gradlew.bat :auth-api:run
```

Leave this terminal running.

Terminal 2: start a Cloudflare quick tunnel:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Cloudflare prints a public HTTPS URL similar to:

```text
https://deserve-sport-robots-unsubscribe.trycloudflare.com
```

Leave this terminal running too. You can check the tunnel with:

```powershell
Invoke-RestMethod https://deserve-sport-robots-unsubscribe.trycloudflare.com/health
```

Terminal 3: build and install the Android app with the tunnel URL:

```powershell
cd C:\Users\Stepan\Documents\SDTFitnessApp
.\gradlew.bat :app:installDebug -PSDT_AUTH_BASE_URL=https://deserve-sport-robots-unsubscribe.trycloudflare.com
```

Then open the app on the emulator or device and register or log in with email and password.

If the Cloudflare tunnel is restarted, it may generate a new URL. Rebuild and reinstall the Android app with the new `SDT_AUTH_BASE_URL`, because the auth base URL is compiled into `BuildConfig.AUTH_BASE_URL`.

If `cloudflared` is not installed, install it with:

```powershell
winget install --id Cloudflare.cloudflared
```

Then close and reopen PowerShell before running `cloudflared`.

### Auth base URL

Then set the Android Gradle property:

```properties
SDT_AUTH_BASE_URL=https://your-https-dev-url.example
```

You can place that in `gradle.properties` or pass it at build time:

```powershell
.\gradlew.bat :app:assembleDebug -PSDT_AUTH_BASE_URL=https://your-https-dev-url.example
```

## Curl

Register:

```bash
curl -X POST "$AUTH_BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

Login:

```bash
curl -X POST "$AUTH_BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

Refresh:

```bash
curl -X POST "$AUTH_BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"the-refresh-token"}'
```

Credential sign-in currently fails safely with `401 invalid_credentials`; the route is present so passkeys or federated credential verification can be added without changing the Android contract.

## Tests

```powershell
.\gradlew.bat :auth-api:test
```

The tests cover registration, duplicate registration, login, invalid login, refresh-token rotation, and expired refresh tokens.
