package dev.hollowcube.replay.playback;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Metadata;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.SpawnPlayerPacket;

import java.util.ArrayList;
import java.util.Map;

public class PlaybackPlayer extends Entity {
    public PlaybackPlayer() {
        super(EntityType.PLAYER);
    }

    @Override
    public void spawn() {
        var texture = "ewogICJ0aW1lc3RhbXAiIDogMTYxNDA3NzE5MzQ4MiwKICAicHJvZmlsZUlkIiA6ICJkMGI4MjE1OThmMTE0NzI1ODBmNmNiZTliOGUxYmU3MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJqYmFydHl5IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2E0NjQxMmMzOTgwODZlYzMxMzIxMzlhMTE5NWU0ODNlMGE0OWRmYThiZWMzZWIxNDc0ZjViMTliMTY0NTE4NmUiCiAgICB9CiAgfQp9";
        var sig = "krIl4GecgEcDkkEG2DpUvZWnIqpxhJRcFhkaWYYG9A35u49mGkM3eHJX1ys60lF042EXiPpujonZt4Kp8NF0Hn1WblxfnJUfu1o97Tv7iI+vfB2M/cIfEAKnqVvagPkWvhf6N9TITF7szQc6j0ksMNrFv+gV0NKDZFoQPgw+q+ADGPicQdDpub0rr6hzPmv1H1kNQxDgLGxwIAV4fzVToGKRqLKLCHSMC64d3/GJkrtxQzsvnWji9nD3WJzilLmKN7yjB8Cs1xAWTfxIdayeT/k6pDADyqlRs9PCfGISEwMQKHz0kOqNBDA5J6UB08q7zhO1uZyW4v3QaIXXQeyp7dWkffFdRedcNovmWpiOLnC4u8XF5kFUySkvZg4nMHvO+Vr20uFhxiZ7EWaNt1syfNMDEwiZ8oqljTtbEPUhpj68n1hViThhbatj1XeEa6zSqB1pmDw36q+QQUKX4Q894uDXMr6Gij/1LNJ00rfNoDN43UXt29XPN/qQ1Pj5TXctJdV8zBVhrK60qDpII8miHlslGTTwwpMKgEh5K0JJ5xWtmk4tY4TON0RsQ4RjD+UxmfrPDgOyAXoRxWnPVeUldODbkFHn8wMnK6M/k5g8AFOGOM5h8CfnjDqUeiyZPYC6EihC0dxK9yzPZxe2av27LiHUYDWDs3P5zKVKXRbFaR8=";

        var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
        properties.add(new PlayerInfoUpdatePacket.Property("textures", texture, sig));
        sendPacketToViewers(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, new PlayerInfoUpdatePacket.Entry(
                getUuid(),
                "sethsux",
                properties,
                false,
                0,
                GameMode.SURVIVAL,
                null, null
        )));

        sendPacketToViewers(new SpawnPlayerPacket(
                getEntityId(),
                getUuid(),
                getPosition()
        ));

        sendPacketToViewers(new EntityMetaDataPacket(getEntityId(), Map.of(17, Metadata.Byte((byte) 127))));
    }

    @Override
    public void remove() {
        super.remove();

        sendPacketToViewers(new PlayerInfoRemovePacket(getUuid()));
    }
}
