alter table ip_history
    alter column first_seen type timestamptz using first_seen at time zone 'UTC',
    alter column last_seen type timestamptz using last_seen at time zone 'UTC';
