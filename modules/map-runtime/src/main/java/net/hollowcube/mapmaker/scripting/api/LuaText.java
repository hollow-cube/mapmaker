package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.scripting.gen.LuaExport;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.gen.LuaLibrary.Scope;
import net.hollowcube.scripting.gen.LuaMethod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Objects;

/// Build and manipulate styled text. `Text` values can be passed anywhere the API accepts a
/// message, title, or label. Available as a global named `Text`, without `require`.
@LuaLibrary(name = "Text", scope = Scope.GLOBAL)
public final class LuaText {
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER =
        PlainTextComponentSerializer.plainText();
    // Extremely limited mini message for now, likely will expand in the future.
    // Not sure if we want open_url for example. Though probably do want some click events.
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
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

    public static void push(LuaState state, Component value) {
        LuaText$luau.pushText(state, new Text(value));
    }

    public static Component check(LuaState state, int index) {
        return LuaText$luau.checkTextArg(state, index).value;
    }

    public static Component checkAnyText(LuaState state, int index) {
        state.checkAny(index); // Make sure they provided an arg
        return switch (state.type(index)) {
            case STRING -> Component.text(Objects.requireNonNull(state.toString(index)));
            case NUMBER, BOOLEAN, VECTOR -> Component.text(state.toStringRepr(index));
            case TABLE -> {
                state.argError(index, "Table to text is not yet supported");
                yield null;
            }
            case USERDATA -> check(state, index);
            default -> {
                state.argError(index, "Expected a Text-able object");
                yield null;
            }
        };
    }

    //region Static Methods

    /// Parses a MiniMessage-formatted string into a `Text`. Always escape user input with
    /// `Text.sanitize` first to prevent unintended formatting.
    ///
    /// ```luau
    /// local greeting = Text.new("<red>welcome!</red>")
    /// player:send_message(greeting)
    /// ```
    ///
    /// @luaParam text string
    /// @luaReturn Text
    @LuaMethod
    public static int new_(LuaState state) {
        var raw = state.checkString(1);
        push(state, MINI_MESSAGE.deserialize(raw));
        return 1;
    }

    /// Escapes any MiniMessage tags in a string. Use this before passing user-supplied text
    /// to `Text.new` to prevent injected formatting.
    ///
    /// ```luau
    /// local safe = Text.sanitize(player_input)
    /// player:send_message(Text.new("you said: " .. safe))
    /// ```
    ///
    /// @luaParam text string
    /// @luaReturn string
    @LuaMethod
    public static int sanitize(LuaState state) {
        var raw = state.checkString(1);
        state.pushString(MINI_MESSAGE.escapeTags(raw));
        return 1;
    }

    //endregion

    /// A piece of styled text. Built with `Text.new`, accepted anywhere the API takes a
    /// message or label.
    // TODO: should implement some implFor thing on export where
    //       you can write a definition for an existing type (eg
    //       Component in this case)
    @LuaExport
    public record Text(Component value) {

        //region Meta Methods

        /// Concatenates two `Text` values, or a `Text` with any string-able value.
        @LuaMethod(meta = "__concat")
        public int luaConcat(LuaState state) {
            var rhs = checkAnyText(state, 1);
            push(state, Component.textOfChildren(value, rhs));
            return 1;
        }

        /// Returns the length of the rendered plain text, ignoring formatting.
        @LuaMethod(meta = "__len")
        public int luaLen(LuaState state) {
            state.pushInteger(PLAIN_TEXT_SERIALIZER.serialize(value).length());
            return 1;
        }

        /// Returns the plain-text form, without any formatting.
        @LuaMethod(meta = "__tostring")
        public int luaToString(LuaState state) {
            state.pushString(PLAIN_TEXT_SERIALIZER.serialize(value));
            return 1;
        }

        //endregion

    }


}
