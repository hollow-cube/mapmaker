package net.hollowcube.common.lang;


import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.internal.parser.node.ElementNode;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

final class MyHoverTag {
    private static final String HOVER = "hover";

    static final TagResolver RESOLVER = TagResolver.resolver(HOVER, MyHoverTag::create);

    private MyHoverTag() {
    }

    @SuppressWarnings("unchecked")
    static Tag create(final ArgumentQueue args, final Context ctx) throws ParsingException {
        final String actionName = args.popOr("Hover event requires an action as its first argument").value();
        final HoverEvent.Action<Object> action = (HoverEvent.Action<Object>) HoverEvent.Action.NAMES.value(actionName);
        final MyHoverTag.ActionHandler<Object> value = actionHandler(action);
        if (value == null) {
            throw ctx.newException("Don't know how to turn '" + args + "' into a hover event", args);
        }

        if (actionName.equals("show_text")) {
            return new MyStylingTagImpl((ElementNode) value.parse(args, ctx));
        } else {
            return Tag.styling(HoverEvent.hoverEvent(action, value.parse(args, ctx)));
        }
    }

    @SuppressWarnings("unchecked")
    static <V> MyHoverTag.@Nullable ActionHandler<V> actionHandler(final HoverEvent.Action<V> action) {
        MyHoverTag.ActionHandler<?> ret = null;
        if (action == HoverEvent.Action.SHOW_TEXT) {
            ret = MyHoverTag.ShowText.INSTANCE;
        } else if (action == HoverEvent.Action.SHOW_ITEM) {
            ret = MyHoverTag.ShowItem.INSTANCE;
        } else if (action == HoverEvent.Action.SHOW_ENTITY) {
            ret = MyHoverTag.ShowEntity.INSTANCE;
        }

        return (MyHoverTag.ActionHandler<V>) ret;
    }

    interface ActionHandler<V> {
        @NotNull V parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException;
    }

    static final class ShowText implements MyHoverTag.ActionHandler<ElementNode> {
        private static final MyHoverTag.ShowText INSTANCE = new MyHoverTag.ShowText();

        private ShowText() {
        }

        @Override
        public @NotNull ElementNode parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException {
            return LanguageProviderV2.deserializeToTree(args.popOr("show_text action requires a message").value());
        }
    }

    static final class ShowItem implements MyHoverTag.ActionHandler<HoverEvent.ShowItem> {
        private static final MyHoverTag.ShowItem INSTANCE = new MyHoverTag.ShowItem();

        private ShowItem() {
        }

        @Override
        public HoverEvent.@NotNull ShowItem parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException {
            try {
                final Key key = Key.key(args.popOr("Show item hover needs at least an item ID").value());
                final int count = args.hasNext() ? args.pop().asInt().orElseThrow(() -> ctx.newException("The count argument was not a valid integer")) : 1;
                if (args.hasNext()) {
                    return HoverEvent.ShowItem.showItem(key, count, BinaryTagHolder.binaryTagHolder(args.pop().value()));
                } else {
                    return HoverEvent.ShowItem.showItem(key, count);
                }
            } catch (final InvalidKeyException | NumberFormatException ex) {
                throw ctx.newException("Exception parsing show_item hover", ex, args);
            }
        }
    }

    static final class ShowEntity implements MyHoverTag.ActionHandler<HoverEvent.ShowEntity> {
        static final MyHoverTag.ShowEntity INSTANCE = new MyHoverTag.ShowEntity();

        private ShowEntity() {
        }

        @Override
        public HoverEvent.@NotNull ShowEntity parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException {
            try {
                final Key key = Key.key(args.popOr("Show entity needs a type argument").value());
                final UUID id = UUID.fromString(args.popOr("Show entity needs an entity UUID").value());
                if (args.hasNext()) {
                    final Component name = ctx.deserialize(args.pop().value());
                    return HoverEvent.ShowEntity.showEntity(key, id, name);
                }
                return HoverEvent.ShowEntity.showEntity(key, id);
            } catch (final IllegalArgumentException | InvalidKeyException ex) {
                throw ctx.newException("Exception parsing show_entity hover", ex, args);
            }
        }
    }
}

