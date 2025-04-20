package net.hollowcube.mapmaker.map.block.handler.sign;

import net.hollowcube.common.util.CollectionUtil;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.misc.Emoji;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.tag.TagSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public record SignData(
        boolean hasGlowingText,
        @NotNull String color,
        @NotNull List<Component> lines
) {

    public static final int LINE_COUNT = 4;
    private static final List<Component> DEFAULT_MESSAGES = List.of(Component.empty(), Component.empty(), Component.empty(), Component.empty());
    private static final TextReplacementConfig EMOJI_REPLACER = TextReplacementConfig.builder()
            .match(Pattern.compile(":(?<name>[^:]+):"))
            .replacement((result, builder) -> {
                var id = result.group("name");
                var emoji = Emoji.findByName(id);
                return emoji != null ? emoji.supplier().apply(ThreadLocalRandom.current()) : Component.text(":" + id + ":");
            })
            .build();

    private static final Codec<SignData> CODEC = StructCodec.struct(
            "has_glowing_text", Codec.BOOLEAN.optional(false), SignData::hasGlowingText,
            "color", Codec.STRING.optional("black"), SignData::color,
            "messages", CollectionUtil.minSizeList(Codec.COMPONENT, LINE_COUNT, Component::empty).optional(DEFAULT_MESSAGES), SignData::lines,
            SignData::new);
    public static final TagSerializer<SignData> SERIALIZER = DFU.codecTagSerializer(CODEC);

    @Contract("-> new")
    public static SignData empty() {
        return new SignData(false, "black", List.of());
    }

    @Contract("_ -> new")
    public SignData withGlows(boolean hasGlowingText) {
        return new SignData(hasGlowingText, color, lines);
    }

    @Contract("_ -> new")
    public SignData withColor(@NotNull String color) {
        return new SignData(hasGlowingText, color, lines);
    }

    @Contract("_ -> new")
    public SignData withLines(@NotNull Component @NotNull [] lines) {
        return new SignData(hasGlowingText, color, List.of(lines));
    }

    @Contract("-> new")
    public SignData withFormatting() {
        Component[] newLines = new Component[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            newLines[i] = lines.get(i).replaceText(EMOJI_REPLACER);
        }
        return withLines(newLines);
    }

    @Contract("-> new")
    public CompoundBinaryTag toNbt() {
        return (CompoundBinaryTag) CODEC.encode(Transcoder.NBT, this).orElseThrow();
    }
}
