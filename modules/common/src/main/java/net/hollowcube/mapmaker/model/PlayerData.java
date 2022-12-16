package net.hollowcube.mapmaker.model;

import net.hollowcube.mapmaker.util.TagUtil;
import net.minestom.server.tag.Tag;

/**
 * MapMaker data for a single player.
 */
public class PlayerData {
    public static final Tag<String> PLAYER_ID = Tag.String("mapmaker:player_id");

    public static final Tag<PlayerData> DATA = TagUtil.noop("mapmaker:player_data");

    private String id;
    private String uuid;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "id='" + id + '\'' +
                '}';
    }
}
