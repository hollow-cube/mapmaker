package net.hollowcube.terraform.tool;

import com.google.auto.service.AutoService;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@AutoService(BuiltinTool.class)
public class DebugStickTool implements BuiltinTool {
    private static final NamespaceID TYPE = NamespaceID.from("terraform:debug_stick");

    private static final Tag<String> TAG_PROPERTY = Tag.String("property");

    private static final Int2ObjectMap<Map<String, String[]>> VALID_PROPERTIES;

    static {
        var blockmap = new Int2ObjectOpenHashMap<Map<String, String[]>>();
        for (var block : Block.values()) {
            var blockprops = new HashMap<String, String[]>();
            for (var propName : block.properties().keySet()) {
                var propValues = new HashSet<>();
                for (var state : block.possibleStates()) {
                    propValues.add(state.getProperty(propName));
                }
                blockprops.put(propName, propValues.toArray(new String[0]));
            }
            blockmap.put(block.id(), blockprops);
        }
        VALID_PROPERTIES = blockmap;
    }

    @Override
    public @NotNull NamespaceID namespace() {
        return TYPE;
    }

    @Override
    public int flags() {
        return RIGHT_CLICK_BLOCK | LEFT_CLICK_BLOCK;
    }

    @Override
    public @NotNull Material material() {
        return Material.DEBUG_STICK;
    }

    @Override
    public void leftClicked(@NotNull Click click) {
        // Determine the target selection
        var player = click.player();
        var itemStack = click.itemStack();

        var instance = click.instance();
        var blockPosition = click.blockPosition();
        var block = instance.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        var oldProperty = itemStack.getTag(TAG_PROPERTY);
        var newProperty = selectProperty(block, oldProperty, true);
        if (newProperty == null) {
            player.sendActionBar(Component.text(block.name() + " has no properties"));
            return;
        }

        click.updateItemStack(b -> b.setTag(TAG_PROPERTY, newProperty));
        player.sendActionBar(Component.text("selected \"" + newProperty + "\" (" + block.getProperty(newProperty) + ")"));
    }

    @Override
    public void rightClicked(@NotNull Click click) {
        // Determine the target selection
        var player = click.player();
        var itemStack = click.itemStack();

        var instance = click.instance();
        var blockPosition = click.blockPosition();
        var block = instance.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        // Get the old property or select the first one
        var oldProperty = itemStack.getTag(TAG_PROPERTY);
        var newProperty = selectProperty(block, oldProperty, false);
        if (newProperty == null) {
            player.sendActionBar(Component.text(block.name() + " has no properties"));
            return;
        }

        // Get the next value
        String name = null;
        var props = VALID_PROPERTIES.get(block.id()).get(newProperty);
        for (int i = 0; i < props.length; i++) {
            if (props[i].equals(block.getProperty(newProperty))) {
                name = props[(i + 1) % props.length];
                break;
            }
        }
        if (name == null) name = props[0];
        instance.setBlock(blockPosition, block.withProperty(newProperty, name), false);
        if (!newProperty.equals(oldProperty)) click.updateItemStack(b -> b.setTag(TAG_PROPERTY, newProperty));
        player.sendActionBar(Component.text("\"" + newProperty + "\" to " + name)); // "snowy" to false
    }

    /**
     * Selects the next property from the given block, or null if there are no properties on the block.
     *
     * @param block The block to select the next property from
     * @param old The last selected property, will use the next one or the first if not found
     * @return The next property (looping), or null if there are no properties on the block
     */
    private @Nullable String selectProperty(@NotNull Block block, @Nullable String old, boolean inc) {
        var properties = block.properties();
        if (properties.isEmpty()) return null;

        var keys = properties.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(old)) {
                return inc ? keys[(i + 1) % keys.length] : keys[i];
            }
        }

        return keys[0]; // if old is null or not present, return the first key
    }
}
