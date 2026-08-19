# Auth API deployment

This stack runs the Ktor auth API behind automatic HTTPS with Caddy and stores auth data in PostgreSQL. Only Caddy publishes host ports; the API and database stay on the private Compose network.

## LAN-only deployment (no domain)

Use `compose.lan.yaml` when the Android debug build and server are on the same trusted network. This publishes plain HTTP on port 8080, so do not forward that port on the router or expose it to the internet.

```bash
cd SDTFitnessApp/deploy
cp .env.lan.example .env.lan
chmod 600 .env.lan
docker compose --env-file .env.lan -f compose.lan.yaml config --quiet
docker compose --env-file .env.lan -f compose.lan.yaml up -d --build --wait
curl --fail --silent --show-error http://127.0.0.1:8080/health
```

Build and install the debug app with the server's LAN address:

```powershell
.\gradlew.bat :app:installDebug -PSDT_AUTH_BASE_URL=http://YOUR_SERVER_LAN_IP:8082
```

The LAN stack defaults to host port `8082`. Change `AUTH_LAN_PORT` in `.env.lan` if needed.

The LAN deployment is a bridge to the public setup below. A public/release deployment needs Android-trusted HTTPS, normally provided by a domain and Caddy.

## Server prerequisites

- A Linux server with Docker Engine and the Docker Compose plugin.
- A DNS `A`/`AAAA` record for the auth domain pointing at the server.
- Inbound TCP ports 80 and 443, plus UDP 443, allowed by the firewall.

## First deployment

Copy or clone the repository onto the server, then run:

```bash
cd SDTFitnessApp/deploy
cp .env.example .env
chmod 600 .env
```

Edit `.env` with the real domain, TLS email, database password, and two independent secrets. Generate secrets with a password manager or, on Linux:

```bash
openssl rand -base64 48
openssl rand -base64 48
openssl rand -base64 48
```

Use one generated value for each password/secret. Then validate and start the stack:

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 auth-api caddy
```

Verify the public endpoint after DNS and TLS are ready:

```bash
curl --fail --silent --show-error "https://auth.example.com/health"
```

Expected response:

```json
{"status":"ok"}
```

## Android build

Compile the public HTTPS URL into the Android app:

```powershell
.\gradlew.bat :app:assembleRelease -PSDT_AUTH_BASE_URL=https://auth.example.com
```

Release builds reject cleartext auth URLs even if a developer has a LAN URL in `gradle.properties`.

## Updates and operations

From the checked-out `deploy` directory:

```bash
git pull --ff-only
docker compose up -d --build
docker compose ps
```

Back up PostgreSQL before risky upgrades:

```bash
docker compose exec -T postgres pg_dump -U sdt_auth sdt_fitness_auth > auth-backup.sql
```

The `.env` file, database volume, and Caddy certificate data are server state. They are excluded from Git and must be backed up separately.
