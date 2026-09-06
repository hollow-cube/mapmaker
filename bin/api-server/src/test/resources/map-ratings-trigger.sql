create or replace function update_map_likes_count()
    returns trigger as
$$
begin
    if tg_op = 'INSERT' then
        if new.rating = 1 then
            update maps set total_likes = total_likes + 1 where id = new.map_id;
        elseif new.rating = 2 then
            update maps set total_likes = total_likes - 1 where id = new.map_id;
        end if;

    elseif tg_op = 'DELETE' then
        if old.rating = 1 then
            update maps set total_likes = total_likes - 1 where id = old.map_id;
        elseif old.rating = 2 then
            update maps set total_likes = total_likes + 1 where id = old.map_id;
        end if;

    elseif tg_op = 'UPDATE' then
        update maps
        set total_likes = total_likes +
                          case when new.rating = 1 then 1 when new.rating = 2 then -1 else 0 end -
                          case when old.rating = 1 then 1 when old.rating = 2 then -1 else 0 end
        where id = new.map_id; -- Could technically break if map_id is changed... but please for the love of god we're never doing that
    end if;

    return null;
end;
$$ language plpgsql;

create trigger map_ratings_update_likes_count
    after insert or delete or update of rating
    on map_ratings
    for each row
execute function update_map_likes_count();
