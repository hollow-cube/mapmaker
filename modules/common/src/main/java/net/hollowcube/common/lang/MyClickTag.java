package net.hollowcube.common.lang;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

public class MyClickTag {
    private static final String CLICK = "click";

    static final TagResolver RESOLVER = TagResolver.resolver(
            CLICK,
            MyClickTag::create
    );

    private MyClickTag() {
    }

    static Tag create(final ArgumentQueue args, final Context ctx) throws ParsingException {
        final String actionName = args.popOr(() -> "A click tag requires an action of one of " + ClickEvent.Action.NAMES.keys()).lowerValue();
        final ClickEvent.@Nullable Action action = ClickEvent.Action.NAMES.value(actionName);
        if (action == null) {
            throw ctx.newException("Unknown click event action '" + actionName + "'", args);
        }

        final String value = args.popOr("Click event actions require a value").value();
        return new MyClickTagImpl(action, value);
    }
}
