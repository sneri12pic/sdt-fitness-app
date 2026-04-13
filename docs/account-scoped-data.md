# Account-Scoped Data

## Schema Overview
- `accounts`: local user identities (`id`, `type`, `createdAt`, `isActive`, `updatedAt`).
- `user_settings`: one row per account for home/dashboard state and quick-log/recovery fields.
- `workout_sessions`, `session_exercises`, `session_set_logs`: now include `accountId` and are scoped to one account.
- Account-scoped tables include audit/sync-prep columns:
  - `createdAt`
  - `updatedAt`
  - `deletedAt` (nullable)
  - `syncState` (`local_only`, `pending_upload`, `synced`, `pending_delete`)

## Active Account Lifecycle
1. `AccountSessionManager` bootstraps local state.
2. If no account exists, a guest account is created and marked active.
3. A minimal `user_settings` row is created for the active account.
4. Reads/writes in repositories use active `accountId`.
5. Debug tooling can create and switch test accounts.

## Per-Account Wipe
- `wipeAccountData(accountId)` deletes only data tied to that account:
  - `session_set_logs`
  - `session_exercises`
  - `workout_sessions`
  - `user_settings`
- Then it recreates a fresh default `user_settings` row for that account.
- Other accounts are unaffected.

## Migration Notes
- DB version bumped from 1 to 2.
- Migration `MIGRATION_1_2`:
  - Creates `accounts` and `user_settings`.
  - Recreates workout tables with `accountId`, FK constraints, and new indexes.
  - Backfills existing workout rows into a default migrated account id.
  - Seeds a default settings row for the migrated account.

## Future Server Auth Integration
- Map server user identity to local `accounts.id`.
- Keep one local active account at a time.
- Use `syncState` + timestamps for upload/reconciliation.
- Promote guest account to auth account by linking/migrating `accountId` after login.
