package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.gui.ActionEditorAnvil;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ChoiceFormat;
import java.util.OptionalDouble;
import java.util.Set;

public record ChatAction(
        @NotNull String message
) implements Action {
    private static final Sprite SPRITE = new Sprite("action/icon/chat", 3, 3);
    private static final MiniMessage CHAT_MESSAGE_PARSER = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.hoverEvent())
                    .resolver(StandardTags.pride())
                    .build()
            )
            .build();

    public static final Key KEY = Key.key("mapmaker:chat");
    public static final StructCodec<ChatAction> CODEC = StructCodec.struct(
            "message", Codec.STRING.optional(""), ChatAction::message,
            ChatAction::new
    );
    public static final Editor<ChatAction> EDITOR = new Editor<>(
            ChatAction::makeEditor, _ -> SPRITE,
            ChatAction::makeThumbnail, Set.of(KEY)
    );

    public @NotNull ChatAction withMessage(@NotNull String message) {
        return new ChatAction(message);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        try {
            player.sendMessage(CHAT_MESSAGE_PARSER.deserialize(this.message, gatherVariableResolvers(player, state)));
        } catch (Exception exception) {
            ExceptionReporter.reportException(exception, player);
        }
    }

    private static TagResolver[] gatherVariableResolvers(@NotNull Player player, @NotNull PlayState state) {
        return new TagResolver[]{
                Placeholder.unparsed("player", player.getUsername()),
                variable("progressindex", state.get(Attachments.PROGRESS_INDEX, 0)),
        };
    }

    private static @NotNull TagResolver variable(@Subst("") @NotNull String id, long value) {
        return TagResolver.resolver(id, (args, _) ->
                switch (args.hasNext() ? args.pop().value() : null) {
                    case "percent" -> {
                        var max = args.hasNext() ? args.pop().asDouble() : OptionalDouble.empty();
                        if (max.isEmpty()) yield createError("Expected a max value, none provided");
                        int percent = (int) ((value / max.getAsDouble()) * 100);
                        yield Tag.inserting(Component.text(percent + "%"));
                    }
                    case "choice" -> {
                        var pattern = args.hasNext() ? args.pop().value() : null;
                        if (pattern == null) yield createError("Expected a choice pattern, none provided");
                        var format = new ChoiceFormat(pattern);
                        yield Tag.inserting(Component.text(format.format(value)));
                    }
                    case null, default -> Tag.inserting(Component.text(value));
                }
        );
    }

    private static Tag createError(@NotNull String message) {
        return Tag.inserting(Component.text("ERROR")
                .color(NamedTextColor.RED)
                .hoverEvent(HoverEvent.showText(Component.text(message).color(NamedTextColor.GRAY)))
        );
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable ChatAction action) {
        if (action == null || action.message().isEmpty()) {
            return Component.translatable("gui.action.chat.thumbnail.empty");
        }
        return Component.translatable("gui.action.chat.thumbnail", Component.text(action.message()));
    }

    private static @NotNull Panel makeEditor(@NotNull ActionList.Ref ref) {
        return new ActionEditorAnvil<>(ref, ChatAction::message, ChatAction::withMessage);
    }

}
