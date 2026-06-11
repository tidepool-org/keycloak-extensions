# User-activity outbox

A Keycloak extension that publishes a compact, stable stream of user-activity facts — **last login**,
**MFA enabled/disabled**, and **identity-provider links changed** — to a database table that an
external system consumes via Change Data Capture (CDC), e.g. [Debezium](https://debezium.io/).

- [Why a custom table](#why-a-custom-table)
- [How it works](#how-it-works)
- [Event schema](#event-schema)
- [Event types](#event-types)
  - [`LOGIN`](#login)
  - [`MFA_ENABLED` / `MFA_DISABLED`](#mfa_enabled--mfa_disabled)
  - [`IDP_LINKS_CHANGED`](#idp_links_changed)
- [What triggers an event](#what-triggers-an-event)
- [Retention & cleanup](#retention--cleanup)
- [Configuration](#configuration)
- [Consuming the table (CDC)](#consuming-the-table-cdc)
- [Indexes](#indexes)
- [Out of scope / caveats](#out-of-scope--caveats)
- [Source](#source)

## Why a custom table

Keycloak already has a built-in event store (`EVENT_ENTITY`) with event-type filtering and time-based
expiration, and you *could* point CDC at it. We don't, for three reasons:

1. **"MFA enabled/disabled" is not an event Keycloak emits.** MFA is not a flag — it is "does the user
   hold a second-factor credential". Deciding whether 2FA is now on or off requires inspecting the
   user's remaining credentials at event time, which only in-process code can do.
2. **`EVENT_ENTITY` is a generic, Keycloak-owned schema** (details live in a `DETAILS_JSON` blob, and
   the schema can change across upgrades). Coupling an external contract to it is brittle.
3. **Cleanup must be size-bounded too**, which the built-in time-only expiration does not offer.

So this extension writes a purpose-built table with a flat schema it owns, holding only the handful of
facts we publish.

## How it works

This is the [transactional-outbox](https://microservices.io/patterns/data/transactional-outbox.html)
pattern.

- A **global event listener** (provider id `tidepool-user-activity`) observes Keycloak user and admin
  events and writes one row per fact into `TIDEPOOL_USER_ACTIVITY_EVENT`.
- Rows are written through the request's **shared JPA session**, so each row commits in the **same
  transaction** as the Keycloak action that produced it. There is no separate publish step that could
  drift from the source change.
- The table is **created and migrated automatically** via a Keycloak JPA entity provider (Liquibase
  changelog); no manual SQL or DDL is required.
- A **cluster-aware timer** prunes the table by age and size (see [Retention & cleanup](#retention--cleanup)).
- An external **CDC connector** streams inserts out of the table to the downstream system.

```
Keycloak action ──fires──> event listener ──persist (same txn)──> TIDEPOOL_USER_ACTIVITY_EVENT
                                                                          │
                                                   CDC connector (Debezium) reads inserts
                                                                          │
                                                                  external system
```

## Event schema

Table: **`TIDEPOOL_USER_ACTIVITY_EVENT`**

| Column               | Type           | Null? | Description                                                                                 |
|----------------------|----------------|-------|---------------------------------------------------------------------------------------------|
| `ID`                 | `VARCHAR(36)`  | no    | Primary key (UUID).                                                                          |
| `REALM_ID`           | `VARCHAR(255)` | no    | Realm the user belongs to.                                                                  |
| `USER_ID`            | `VARCHAR(255)` | no    | Keycloak user id.                                                                            |
| `EVENT_TYPE`         | `VARCHAR(32)`  | no    | One of `LOGIN`, `MFA_ENABLED`, `MFA_DISABLED`, `IDP_LINKS_CHANGED`. This **is** the fact — MFA on/off is read straight from `MFA_ENABLED` vs `MFA_DISABLED`. |
| `IDENTITY_PROVIDERS` | `VARCHAR(4000)`| yes   | JSON array of linked IdPs for `IDP_LINKS_CHANGED` rows; `null` otherwise.                    |
| `EVENT_TIME`         | `BIGINT`       | no    | Epoch **milliseconds** the source event occurred. Drives ordering and pruning.              |

Every row carries `REALM_ID`, `USER_ID`, `EVENT_TYPE`, and `EVENT_TIME`. `IDENTITY_PROVIDERS` is the only
payload column and is populated for `IDP_LINKS_CHANGED` rows only.

## Event types

### `LOGIN`

One row per **user session** — i.e. per fresh interactive login. Keycloak fires a `LOGIN` event for
cookie/SSO re-authentications too (every additional client the user reaches while already logged in),
which would otherwise flood the table. The dedup is deterministic: the first `LOGIN` of a session
records a row and sets a marker note on the `UserSession`; every later `LOGIN` reusing that session
sees the note and is skipped. No time-window heuristics — slow required actions, SSO bursts, and clock
skew cannot cause drops or duplicates. When the session cannot be resolved at all, the listener fails
toward recording rather than losing a login.

| Column               | Value                          |
|----------------------|--------------------------------|
| `EVENT_TYPE`         | `LOGIN`                        |
| `EVENT_TIME`         | Login time (epoch ms)          |
| `IDENTITY_PROVIDERS` | `null`                         |

"Last login" for a user is the most recent `LOGIN` row for that `USER_ID`.

### `MFA_ENABLED` / `MFA_DISABLED`

Emitted when a credential change **toggles** whether the user holds any second-factor credential
(OTP, WebAuthn, or WebAuthn passwordless). The **direction comes from the event itself, not from stored
history** — the outbox is pruned, so it can't be its own source of truth:

- An **add/update** of a second-factor credential records `MFA_ENABLED`, as long as the user has a second
  factor afterwards.
- A **removal** of a second-factor credential records `MFA_DISABLED`, but **only once no second factor
  remains**.

On either kind of event the listener recomputes the user's current credentials, so the row always states
the user's **true current state** — it is therefore safe for the consumer to upsert and ignore repeats.
When the changed credential's type is known and is not a second factor (e.g. a password), the listener
short-circuits without even loading the user.

| Column               | Value                                            |
|----------------------|--------------------------------------------------|
| `EVENT_TYPE`         | `MFA_ENABLED` or `MFA_DISABLED`                  |
| `EVENT_TIME`         | Time of the credential change (epoch ms)         |
| `IDENTITY_PROVIDERS` | `null`                                           |

This means:

- Enrolling a **first** authenticator → `MFA_ENABLED`.
- Adding a **second** authenticator → `MFA_ENABLED` again (a redundant "still on"; the consumer upserts).
- Removing **one of two** authenticators → **no row** (a second factor still remains).
- Removing the **last** authenticator → `MFA_DISABLED`.
- Changing or removing a password → **no row** (not a second factor).

### `IDP_LINKS_CHANGED`

Emitted whenever an identity-provider link is added or removed. The payload is the user's **full**
current set of linked IdPs (not just the one that changed), so the external system can replace its view
wholesale.

`IDENTITY_PROVIDERS` is a JSON array sorted by `alias`:

```json
[
  {"alias": "google", "name": "Google"},
  {"alias": "azure-ad", "name": "Corporate Azure AD"}
]
```

- `alias` — the IdP's unique alias in the realm.
- `name` — the IdP's configured **display name**; when it has none, a human-readable name derived from
  the alias (non-alphanumeric separators become spaces and each word is capitalized, e.g.
  `google-tidepool` → `Google Tidepool`). Always present.

When the user's **last** link is removed, the array is empty: `[]`.

| Column               | Value                              |
|----------------------|------------------------------------|
| `EVENT_TYPE`         | `IDP_LINKS_CHANGED`                |
| `IDENTITY_PROVIDERS` | JSON array as above (`[]` if none) |
| `EVENT_TIME`         | Time of the link change (epoch ms) |

## What triggers an event

Both self-service (end-user) and admin-initiated changes are captured.

| Outbox row          | Self-service (user events)                              | Admin (admin events, by resource path)                              |
|---------------------|--------------------------------------------------------|---------------------------------------------------------------------|
| `LOGIN`             | `LOGIN` — only when it creates a new session (SSO/cookie re-logins are skipped) | — (login has no admin equivalent)        |
| `MFA_ENABLED`       | `UPDATE_CREDENTIAL`, `UPDATE_TOTP` (second-factor added) | — (admins cannot enrol a second factor for a user)                |
| `MFA_DISABLED`      | `REMOVE_CREDENTIAL`, `REMOVE_TOTP` (last second factor removed) | `DELETE users/{id}/credentials/...`, `users/{id}/disable-credential-types` |
| `IDP_LINKS_CHANGED` | `FEDERATED_IDENTITY_LINK`, `REMOVE_FEDERATED_IDENTITY`, `FEDERATED_IDENTITY_OVERRIDE_LINK` | `users/{id}/federated-identity/...`        |

Failed operations (admin events carrying an error) are ignored. For MFA and IdP changes the listener
recomputes state from the user model regardless of trigger, so admin and self-service paths produce
identical, consistent rows. A non-`DELETE` operation on a credential (e.g. setting a label) is not
treated as a removal.

## Backfilling existing IdP links

The listener only emits `IDP_LINKS_CHANGED` when a link changes *after* it's deployed, so users linked
beforehand aren't represented. A one-off, admin-authenticated endpoint seeds them:

```
POST /realms/{realm}/tidepool-admin/backfill-idp-links
Authorization: Bearer <admin token with manage-realm on {realm}>
→ 200 {"realm":"<realm>","backfilled":<n>,"skipped":<n>,"failed":<n>}
```

It finds the users in the realm that currently have a federated link (Keycloak's `FEDERATED_IDENTITY`
table), and writes one `IDP_LINKS_CHANGED` row per user with their current link set — using the **same
code path** as live events, so the rows are identical. Permission (`manage-realm`) is checked against
the **target** realm in the path.

The work runs in **batches of 100, each in its own transaction**, so memory stays bounded on large
realms and a failure only loses its own batch (`failed`, with the error logged) instead of rolling back
the whole run. Users whose user record or links disappeared between enumeration and processing are
counted in `skipped` (no empty `[]` rows are written). Run it once per realm after deploying. It is
**idempotent**: re-running just re-records current state (consumers upsert). There is no login/MFA
backfill (Keycloak does not retain that history); only IdP links can be reconstructed from current
state.

## Retention & cleanup

The table is insert-only from the extension's side and is pruned by a **cluster-aware scheduled task**
(only one node prunes per interval). Two bounds are applied each sweep:

1. **Age** — rows older than the retention window are deleted (default: one week); disabled when
   `retention-hours <= 0`.
2. **Size** — when enabled, the oldest rows are trimmed so at most `max-rows` remain. This is a *soft*
   cap: ties at the boundary millisecond may leave the table slightly under the cap, never over. The
   boundary timestamp is found by scanning only the overflow rows (oldest-first, offset by the overflow
   count), not the whole retained set, so the trim stays cheap even at a large `max-rows`.

Because pruning issues `DELETE`s, the CDC consumer should **ignore deletes** — the outbox is
insert-driven and deletes are cleanup only.

## Configuration

Set as Keycloak SPI options (CLI flags, `keycloak.conf`, or `KC_SPI_*` env vars). Defaults shown:

```
spi-events-listener-tidepool-user-activity-retention-hours=168          # age cutoff (1 week); <= 0 disables age pruning
spi-events-listener-tidepool-user-activity-max-rows=5000000             # size cap; <= 0 disables it
spi-events-listener-tidepool-user-activity-cleanup-interval-minutes=60  # prune frequency
```

Setting `retention-hours` to `0` or a negative value **disables** age-based pruning (it does not collapse
the cutoff to "now"); disable both bounds only if you prune the table by some other means.

The listener is **global** — it runs for every realm with no per-realm "events listeners"
configuration required.

## Consuming the table (CDC)

- **Log-based CDC (recommended)** — point Debezium at `TIDEPOOL_USER_ACTIVITY_EVENT`. No extra schema
  is needed; the `ID` primary key covers Postgres `REPLICA IDENTITY` and connector keying. Filter to
  insert (`c`) events and ignore deletes.
- **Derived state in the downstream system:**
  - *Last login* = latest `EVENT_TIME` among `LOGIN` rows per `USER_ID`.
  - *MFA enabled?* = whether the latest `MFA_*` row per `USER_ID` is `MFA_ENABLED` (vs `MFA_DISABLED`).
  - *Linked IdPs* = `IDENTITY_PROVIDERS` of the latest `IDP_LINKS_CHANGED` row per `USER_ID`.
- Rows are independent and idempotent to re-process; ordering within a user is by `EVENT_TIME`.

## Indexes

Defined in the changelog and sized to the access patterns:

| Index                       | Columns                            | Serves                                                |
|-----------------------------|------------------------------------|-------------------------------------------------------|
| `PK_TIDEPOOL_USER_ACTIVITY_EVENT` | `ID`                         | Primary key / CDC keying.                             |
| `IDX_TP_ACTIVITY_USER_TIME` | `(REALM_ID, USER_ID, EVENT_TIME)`  | Operational "activity for a user" queries. The listener no longer reads it. |
| `IDX_TP_ACTIVITY_TIME`      | `(EVENT_TIME)`                     | Age- and size-based pruning, ordered scans.           |

## Out of scope / caveats

- **No "MFA setup" admin event.** Admins cannot enrol a second factor *for* a user (that goes through a
  required action the user completes, which arrives as a self-service event). Admin-side MFA changes are
  therefore disablements (credential removal / `disable-credential-types`).
- **`EVENT_TIME` is milliseconds**, taken from the source Keycloak event.
- **Deletes are cleanup.** Anything the consumer must retain should be projected into downstream storage;
  the outbox is not a durable archive.

## Source

`admin/src/main/java/org/tidepool/keycloak/extensions/activity/`

| File | Role |
|------|------|
| `UserActivityEventEntity.java` | JPA entity + named queries for the outbox table. |
| `UserActivityEventListenerProvider.java` (+ `Factory`) | Observes events, writes outbox rows, schedules cleanup. |
| `UserActivityRecorder.java` | Shared row writer (IdP-links JSON + persist); used by the listener and the backfill. |
| `UserActivityJpaEntityProvider.java` (+ `Factory`) | Registers the entity and its changelog with Keycloak's datasource. |
| `UserActivityCleanupTask.java` | Age- and size-based pruning. |
| `admin/src/main/resources/META-INF/tidepool-user-activity-changelog.xml` | Liquibase schema. |
| `resource/TidepoolAdminResource.java` → `backfillIdpLinks()` | Admin endpoint that backfills `IDP_LINKS_CHANGED` for existing links. |
