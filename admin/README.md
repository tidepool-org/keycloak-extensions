# Keycloak Admin Endpoints

Additional admin endpoints for user administration

## Deployment

Copy the jar from the target directory to `$KEYCLOAK_HOME/standalone/deployments`

## User-activity outbox (CDC)

A global event listener (`tidepool-user-activity`) writes a compact stream of user-activity facts —
**last login**, **MFA enabled/disabled**, and **identity-provider links changed** — to the
`TIDEPOOL_USER_ACTIVITY_EVENT` table so an external system can consume them via a CDC connector (e.g.
Debezium). It uses the transactional-outbox pattern: each row commits in the same transaction as the
Keycloak action that produced it, and the table is created, migrated, and pruned automatically.

See **[docs/user-activity-outbox.md](docs/user-activity-outbox.md)** for the full event schema, event
types and payloads, triggers, retention, configuration, and CDC consumption guidance.
