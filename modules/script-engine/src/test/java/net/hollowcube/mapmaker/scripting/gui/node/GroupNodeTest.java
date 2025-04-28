package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static net.hollowcube.mapmaker.scripting.util.Proxies.proxyObject;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupNodeTest {
    private static final Context context = Context.create("js");

    @AfterAll
    static void afterAll() {
        context.close();
    }

    @Test
    void testGroupEmpty() {
        var group = new GroupNode();
        var items = build(group, 3, 3);
        var empty = new ItemStack[3 * 3];
        Arrays.fill(empty, MenuBuilder.EMPTY_ITEM);
        assertArrayEquals(empty, items);
    }

    @Test
    void testGroupRow() {
        var group = new GroupNode();
        group.updateFromProps(context.eval("js", "({layout: 'row'})"));
        group.appendChild(item("1"));
        group.appendChild(item("2"));
        group.appendChild(item("3"));

        var items = build(group, 3, 3);
        assertEquals("1", items[0].get(DataComponents.ITEM_MODEL));
        assertEquals("2", items[1].get(DataComponents.ITEM_MODEL));
        assertEquals("3", items[2].get(DataComponents.ITEM_MODEL));
    }

    @Test
    void testGroupRowWrapped() {
        var group = new GroupNode();
        group.updateFromProps(context.eval("js", "({layout: 'row', wrap: true})"));
        for (int i = 0; i < 9; i++) group.appendChild(item(String.valueOf(i + 1)));

        var items = build(group, 3, 3);
        for (int i = 0; i < 9; i++) {
            assertEquals(String.valueOf(i + 1), items[i].get(DataComponents.ITEM_MODEL));
        }
    }

    @Test
    void testGroupRowWrappedWideFirstRow() {
        var group = new GroupNode();
        group.updateFromProps(context.eval("js", "({layout: 'row', wrap: true})"));
        group.appendChild(item("wide", 3, 1));
        for (int i = 0; i < 6; i++) group.appendChild(item(String.valueOf(i + 1)));

        var items = build(group, 3, 3);
        for (int i = 0; i < 9; i++) {
            assertEquals(i < 3 ? "wide" : String.valueOf(i - 2), items[i].get(DataComponents.ITEM_MODEL));
        }
    }

    private static ItemStack[] build(@NotNull Node node, int slotWidth, int slotHeight) {
        var builder = new MenuBuilder(slotWidth, slotHeight, slotHeight);
        node.build(builder);
        return builder.getItems();
    }

    private static Value wrap(@NotNull Map<String, Object> map) {
        return Value.asValue(proxyObject(map));
    }

    private static Node item(String model) {
        return item(model, 1, 1);
    }

    private static Node item(String model, int width, int height) {
        return new Node("test-item") {
            {
                slotWidth = width;
                slotHeight = height;
            }

            @Override
            public void build(@NotNull MenuBuilder builder) {
                builder.editSlots(0, 0, width, height, DataComponents.ITEM_MODEL, model);
            }
        };
    }


}
