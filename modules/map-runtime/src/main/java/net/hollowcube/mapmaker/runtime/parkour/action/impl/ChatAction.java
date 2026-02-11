package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorAnvil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.util.OptionalDouble;
import java.util.Set;

public record ChatAction(String message) implements Action {

    private static final Sprite SPRITE = new Sprite("action/icon/chat", 2, 2);
    private static final MiniMessage CHAT_MESSAGE_PARSER = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.rainbow())
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

    public ChatAction withMessage(String message) {
        return new ChatAction(message);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        try {
            player.sendMessage(Component.translatable(
                    "chat.action.message",
                    CHAT_MESSAGE_PARSER.deserialize(this.message, gatherVariableResolvers(player, state))
            ));
        } catch (Exception exception) {
            ExceptionReporter.reportException(exception, player);
        }
    }

    private static TagResolver[] gatherVariableResolvers(Player player, PlayState state) {
        return new TagResolver[]{
                Placeholder.unparsed("player", player.getUsername()),
                variable("progressindex", state.get(Attachments.PROGRESS_INDEX, 0)),
                variable("resetheight", state.get(Attachments.RESET_HEIGHT, -69)),
                TagResolver.resolver("variable", (args, _) -> {
                    var variable = args.hasNext() ? args.pop().value() : null;
                    if (variable == null) return createError("Expected a variable name, none provided");
                    var variables = state.get(Attachments.VARIABLES);
                    return parseTag(args, variables != null ? variables.getOrDefault(variable, 0) : 0.0);
                }),
        };
    }

    private static TagResolver variable(@Subst("") String id, double value) {
        return TagResolver.resolver(id, (args, _) -> parseTag(args, value));
    }

    private static Tag parseTag(ArgumentQueue args, double value) {
        return switch (args.hasNext() ? args.pop().value() : null) {
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
            case "format" -> {
                var pattern = args.hasNext() ? args.pop().value() : null;
                if (pattern == null) yield createError("Expected a format pattern, none provided");
                var format = new DecimalFormat(pattern);
                yield Tag.inserting(Component.text(format.format(value)));
            }
            case null, default -> Tag.inserting(Component.text(value));
        };
    }

    private static Tag createError(String message) {
        return Tag.inserting(Component.text("ERROR")
                .color(NamedTextColor.RED)
                .hoverEvent(HoverEvent.showText(Component.text(message).color(NamedTextColor.GRAY)))
        );
    }

    private static TranslatableComponent makeThumbnail(@Nullable ChatAction action) {
        if (action == null || action.message().isEmpty()) {
            return Component.translatable("gui.action.chat.thumbnail.empty");
        }
        return Component.translatable("gui.action.chat.thumbnail", Component.text(action.message()));
    }

    private static Panel makeEditor(ActionList.Ref ref) {
        return new ActionEditorAnvil<>(ref, ChatAction::message, ChatAction::withMessage);
    }

}
