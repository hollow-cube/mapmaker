package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.util.GenericTempActionBarProvider;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DebugStickState;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DebugStickItem extends ItemHandler {
    public static final DebugStickItem INSTANCE = new DebugStickItem();

    private static final Tag<DebugStickState> TAG_PROPERTY = ExtraTags.DataComponent("state", ItemComponent.DEBUG_STICK_STATE)
            .defaultValue(DebugStickState.EMPTY);

    private DebugStickItem() {
        super("minecraft:debug_stick", RIGHT_CLICK_BLOCK | LEFT_CLICK_BLOCK);
    }

    @Override
    public @NotNull Material material() {
        return Material.DEBUG_STICK;
    }

    @Override
    public int customModelData() {
        return -1; // Match based on material not custom model data.
    }

    @Override
    protected void leftClicked(@NotNull Click click) {
        var player = click.player();
        var block = click.instance().getBlock(click.blockPosition(), Block.Getter.Condition.TYPE);
        var blockId = block.namespace().asString();

        final var state = click.itemStack().getTag(TAG_PROPERTY);
        final var property = state.state().get(blockId);
        final var newProperty = cycleProperty(block, property, player.isSneaking() ? -1 : 1);

        if (newProperty == null) {
            sendActionBar(player, block.name() + " has no properties");
        } else {
            sendActionBar(player, String.format("Selected \"%s\" (%s)", newProperty, block.getProperty(newProperty)));
            if (!newProperty.equals(property)) {
                click.updateItemStack(b -> b.set(TAG_PROPERTY, state.set(blockId, newProperty)));
            }
        }
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var block = click.instance().getBlock(click.blockPosition());
        var blockId = block.namespace().asString();

        final var state = click.itemStack().getTag(TAG_PROPERTY);
        final var property = state.state().get(blockId);
        final var newProperty = cycleProperty(block, property, 0); // Don't cycle just ensure it's a valid property.

        if (newProperty == null) {
            sendActionBar(player, block.name() + " has no properties");
        } else {
            final var value = block.getProperty(newProperty);
            final var newValue = cycle(BlockUtil.getBlockProperties(block).get(newProperty), value, player.isSneaking() ? -1 : 1);
            click.instance().setBlock(click.blockPosition(), block.withProperty(newProperty, newValue), false);

            sendActionBar(player, String.format("Set \"%s\" to %s", newProperty, newValue));

            if (!newProperty.equals(property)) {
                click.updateItemStack(b -> b.set(TAG_PROPERTY, state.set(blockId, newProperty)));
            }
        }
    }

    private @Nullable String cycleProperty(@NotNull Block block, @Nullable String current, int amount) {
        var properties = block.properties();
        if (properties.isEmpty()) return null;
        return cycle(properties.keySet().toArray(new String[0]), current, amount);
    }

    private void sendActionBar(@NotNull Player player, @NotNull String message) {
        var ab = ActionBar.forPlayer(player);
        ab.addProvider(new GenericTempActionBarProvider(message, 1000L));
    }

    private static String cycle(@NotNull String[] array, @Nullable String current, int amount) {
        if (current == null) return array[0];
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(current)) {
                return array[(i + amount + array.length) % array.length];
            }
        }
        return array[0];
    }

}
