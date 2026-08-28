-- Head search matches the name and the tags as one document.
--
-- `array_to_string` is only STABLE, so the expression cannot be indexed directly and needs an
-- immutable wrapper. `simple` is the only text search configuration available (pglite ships no
-- dictionary files), which is also the one we want: head names are labels, not english prose to be
-- stemmed. The index is on the expression rather than a generated column so that `head_db.*` keeps
-- returning the five columns the table is actually about.
create or replace function head_db_search(name varchar, tags varchar[]) returns tsvector
    language sql immutable strict parallel safe
as
$$
select to_tsvector('simple', name || ' ' || array_to_string(tags, ' '))
$$;

create index if not exists head_db_search_idx on head_db using gin (head_db_search(name, tags));
