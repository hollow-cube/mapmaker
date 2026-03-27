package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.map.block.handler.sign.SignData;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class SignBlockHandler implements BlockHandler {
    public static final Tag<Boolean> IS_WAXED = Tag.Boolean("is_waxed").defaultValue(false);
    public static final Tag<SignData> FRONT_TEXT = Tag.Structure("front_text", SignData.SERIALIZER).defaultValue(SignData.empty());
    public static final Tag<SignData> FRONT_TEXT_FORMATTED = ExtraTags.MappedView(FRONT_TEXT, SignData::withFormatting);
    public static final Tag<SignData> BACK_TEXT = Tag.Structure("back_text", SignData.SERIALIZER).defaultValue(SignData.empty());
    public static final Tag<SignData> BACK_TEXT_FORMATTED = ExtraTags.MappedView(BACK_TEXT, SignData::withFormatting);

    private final Key key;

    SignBlockHandler(@NotNull String id) {
        this.key = Key.key(id);
    }

    @Override
    public @NotNull Key getKey() {
        return key;
    }

    @Override
    public byte getBlockEntityAction() {
        return 3;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(
                IS_WAXED,
                FRONT_TEXT_FORMATTED,
                BACK_TEXT_FORMATTED
        );
    }
}
