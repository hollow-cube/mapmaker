package net.hollowcube.map.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class PlayerHeadBlockHandler implements BlockHandler {
    private static final Tag<@Nullable String> NOTE_BLOCK_SOUND = Tag.String("note_block_sound");


    public static final NamespaceID ID = NamespaceID.from("minecraft:player_head");
    public static final PlayerHeadBlockHandler INSTANCE = new PlayerHeadBlockHandler();

    private PlayerHeadBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(NOTE_BLOCK_SOUND);
    }

}
