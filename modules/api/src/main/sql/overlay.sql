-- Schema overlay applied to the codegen but not actually executed on the database. Allows us to
-- cleanly stop referencing a column without dropping it and breaking existing deployments.
-- Generally something added here should only exist until a subsequent commit moves that change
-- into a real migration.
--
-- Only drops are allowed, and a dropped column must be nullable or have a default, since inserts
-- that leave it out still run against the real table until the migration lands.
alter table maps drop column total_likes;
