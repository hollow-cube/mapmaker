package net.hollowcube.mapmaker.map.block.handler.sign;

import net.hollowcube.common.util.CollectionUtil;
import net.hollowcube.mapmaker.misc.Emoji;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public record SignData(
        boolean hasGlowingText,
        @NotNull String color,
        @NotNull Component[] lines
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

    private static final Tag<Boolean> HAS_GLOWING_TEXT = Tag.Boolean("has_glowing_text").defaultValue(false);
    private static final Tag<String> COLOR = Tag.String("color").defaultValue("black");
    private static final Tag<List<Component>> LINES = Tag.Component("messages").list().defaultValue(DEFAULT_MESSAGES);

    public static final TagSerializer<SignData> SERIALIZER = new TagSerializer<>() {

        @Override
        public @NotNull SignData read(@NotNull TagReadable reader) {
            return new SignData(
                    reader.getTag(HAS_GLOWING_TEXT),
                    reader.getTag(COLOR),
                    reader.getTag(LINES).toArray(Component[]::new)
            );
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull SignData value) {
            writer.setTag(HAS_GLOWING_TEXT, value.hasGlowingText);
            writer.setTag(COLOR, value.color);
            writer.setTag(LINES, CollectionUtil.copyWithMinSize(LINE_COUNT, Component::empty, value.lines));
        }
    };

    @Contract("-> new")
    public static SignData empty() {
        return new SignData(false, "black", new Component[0]);
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
    public SignData withLines(@NotNull Component @NotNull[] lines) {
        return new SignData(hasGlowingText, color, lines);
    }

    @Contract("-> new")
    public SignData withFormatting() {
        Component[] newLines = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) {
            newLines[i] = lines[i].replaceText(EMOJI_REPLACER);
        }
        return withLines(newLines);
    }

    @Contract("-> new")
    public CompoundBinaryTag toNbt() {
        var builder = CompoundBinaryTag.builder();
        HAS_GLOWING_TEXT.write(builder, this.hasGlowingText);
        COLOR.write(builder, this.color);
        LINES.write(builder, CollectionUtil.copyWithMinSize(LINE_COUNT, Component::empty, this.lines));
        return builder.build();
    }
}
