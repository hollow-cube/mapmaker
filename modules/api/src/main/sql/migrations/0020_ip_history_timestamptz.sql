-- Apply to the existing database before deploying the player port. Interpret legacy values as
-- UTC explicitly so the session timezone cannot shift them during conversion.
--
-- Safe with the Go api-server still running: it only ever writes now() into these columns and never
-- reads them back (AddPlayerIP / GetPlayerIPHistory). The type change rewrites the table under an
-- exclusive lock, so logins block for the few seconds the rewrite takes.
alter table ip_history
    alter column first_seen type timestamptz using first_seen at time zone 'UTC',
    alter column last_seen type timestamptz using last_seen at time zone 'UTC';
