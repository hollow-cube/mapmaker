package net.hollowcube.scripting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuaNamesTest {

    @Test
    void simpleCamelCaseToSnake() {
        assertEquals("on_join", LuaNames.toSnakeCase("onJoin"));
        assertEquals("spawn_entity", LuaNames.toSnakeCase("spawnEntity"));
        assertEquals("lua_to_string", LuaNames.toSnakeCase("luaToString"));
    }

    @Test
    void pascalCaseToSnake() {
        assertEquals("on_join", LuaNames.toSnakeCase("OnJoin"));
        assertEquals("event_source", LuaNames.toSnakeCase("EventSource"));
    }

    @Test
    void singleWordIsUnchangedExceptForCase() {
        assertEquals("name", LuaNames.toSnakeCase("name"));
        assertEquals("name", LuaNames.toSnakeCase("Name"));
        assertEquals("color", LuaNames.toSnakeCase("color"));
    }

    @Test
    void alreadySnakeCasePassesThrough() {
        assertEquals("on_join", LuaNames.toSnakeCase("on_join"));
        assertEquals("lua_to_string", LuaNames.toSnakeCase("lua_to_string"));
    }

    @Test
    void consecutiveUppercaseAcronymStaysOneToken() {
        // Acronym at end
        assertEquals("url", LuaNames.toSnakeCase("URL"));
        // Acronym in middle followed by another word
        assertEquals("parse_html_string", LuaNames.toSnakeCase("parseHTMLString"));
        assertEquals("xml_data_source", LuaNames.toSnakeCase("XMLDataSource"));
        // toLuaProperty strips the `get` prefix first, so an acronym property reads cleanly
        assertEquals("url", LuaNames.toLuaProperty("getURL"));
    }

    @Test
    void digitsTreatedAsLowerCaseForBoundary() {
        assertEquals("option_2", LuaNames.toSnakeCase("option2"));
        assertEquals("v_2_handler", LuaNames.toSnakeCase("v2Handler"));
    }

    @Test
    void emptyAndNullPassThrough() {
        assertEquals("", LuaNames.toSnakeCase(""));
        assertEquals(null, LuaNames.toSnakeCase(null));
    }

    @Test
    void toLuaPropertyStripsGetSetAndSnakeCases() {
        assertEquals("on_join", LuaNames.toLuaProperty("getOnJoin"));
        assertEquals("on_hit_player", LuaNames.toLuaProperty("getOnHitPlayer"));
        assertEquals("breed", LuaNames.toLuaProperty("setBreed"));
        assertEquals("x", LuaNames.toLuaProperty("getX"));
    }

    @Test
    void toLuaPropertyWithoutGetSetPrefixSnakeCases() {
        // Field-style access without get/set
        assertEquals("on_join", LuaNames.toLuaProperty("onJoin"));
    }

    @Test
    void toLuaMethodStripsTrailingUnderscoreAndSnakeCases() {
        // `wait_` is a Java escape for the keyword `wait` — drop the underscore.
        assertEquals("wait", LuaNames.toLuaMethod("wait_"));
        assertEquals("spawn_entity", LuaNames.toLuaMethod("spawnEntity"));
        assertEquals("once", LuaNames.toLuaMethod("once"));
    }
}
