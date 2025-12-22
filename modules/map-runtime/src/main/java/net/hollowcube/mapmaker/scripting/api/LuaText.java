package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaLibrary.Scope;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.luau.gen.Meta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Objects;

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

    /// Construct a new Text object from a minimessage string.
    ///
    /// ```luau
    /// local redText = Text.new("<red>This is red text</red>")
    /// player:SendMessage(redText) -- Player will see "This is red text" in red.
    ///```
    ///
    /// @return My return vaue
    /// @luaParam text: string - The string text in minimessage format. User input text be escaped using [#sanitize].
    /// @luaReturn Text - The parsed text component.
    @LuaMethod
    public static int new_(LuaState state) {
        var raw = state.checkString(1);
        push(state, MINI_MESSAGE.deserialize(raw));
        return 1;
    }

    /// Sanitizes input text for any minimessage tags.
    ///
    /// ```luau
    /// local raw = "<red>This is not red text</red>"
    /// local safe = Text.sanitize(raw)
    /// -- Safe contains the text "<red>This is not red text</red>", with no formatting.
    ///```
    ///
    /// @luaParam text: string - The raw text, possibly containing minimessage tags
    /// @luaReturn string - The same string, but with any minimessage tags escaped.
    @LuaMethod
    public static int sanitize(LuaState state) {
        var raw = state.checkString(1);
        state.pushString(MINI_MESSAGE.escapeTags(raw));
        return 1;
    }

    //endregion

    // TODO: should implement some implFor thing on export where
    //       you can write a definition for an existing type (eg
    //       Component in this case)
    @LuaExport
    public record Text(Component value) {

        //region Meta Methods

        @LuaMethod(meta = Meta.CONCAT)
        public int luaConcat(LuaState state) {
            var rhs = checkAnyText(state, 1);
            push(state, Component.textOfChildren(value, rhs));
            return 1;
        }

        @LuaMethod(meta = Meta.LEN)
        public int luaLen(LuaState state) {
            state.pushInteger(PLAIN_TEXT_SERIALIZER.serialize(value).length());
            return 1;
        }

        @LuaMethod(meta = Meta.TOSTRING)
        public int luaToString(LuaState state) {
            state.pushString(PLAIN_TEXT_SERIALIZER.serialize(value));
            return 1;
        }

        //endregion

    }


}
