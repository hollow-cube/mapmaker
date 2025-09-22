package net.hollowcube.mapmaker.runtime.freeform.lua.base;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMeta;
import net.hollowcube.luau.annotation.LuaStatic;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.luau.annotation.MetaType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

//todo dont really need to inherit from the generated type. we just get TYPE_NAME from it.
//     type name should also go away because we should be using tagged userdata.
@LuaType(implFor = Component.class, name = "Text")
public class LuaTextImpl implements LuaTextImpl$luau {
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
        state.newUserData(value);
        state.getMetaTable(TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static Component checkArg(LuaState state, int index) {
        return (Component) state.checkUserDataArg(index, TYPE_NAME);
    }

    public static Component checkAnyTextArg(LuaState state, int index) {
        state.checkAny(index); // Make sure they provided an arg
        return switch (state.type(index)) {
            case STRING -> Component.text(state.toString(index));
            case NUMBER, BOOLEAN, VECTOR -> Component.text(state.toStringRepr(index));
            case TABLE -> {
                state.argError(index, "Table to text is not yet supported");
                yield null;
            }
            case USERDATA -> checkArg(state, index);
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
    @LuaStatic
    public static int new_(LuaState state) {
        var raw = state.checkStringArg(1);
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
    @LuaStatic
    public static int sanitize(LuaState state) {
        var raw = state.checkStringArg(1);
        state.pushString(MINI_MESSAGE.escapeTags(raw));
        return 1;
    }

    //endregion

    //region Meta Methods

    @LuaMeta(MetaType.CONCAT)
    public static int luaConcat(LuaState state) {
        var lhs = checkArg(state, 1);
        var rhs = checkAnyTextArg(state, 2);
        push(state, Component.textOfChildren(lhs, rhs));
        return 1;
    }

    @LuaMeta(MetaType.LEN)
    public static int luaLen(LuaState state) {
        var component = checkArg(state, 1);
        state.pushInteger(PLAIN_TEXT_SERIALIZER.serialize(component).length());
        return 1;
    }

    @LuaMeta(MetaType.TOSTRING)
    public static int luaToString(LuaState state) {
        var component = checkArg(state, 1);
        state.pushString(PLAIN_TEXT_SERIALIZER.serialize(component));
        return 1;
    }

    //endregion
}

