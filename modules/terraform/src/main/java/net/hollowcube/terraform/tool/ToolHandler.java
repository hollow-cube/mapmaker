package net.hollowcube.terraform.tool;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Responsible for managing tool items and triggering their actions.
 * <p/>
 * todo basically a duplication of ItemRegistry in map module, it seems like the map module stuff could
 *      just be builtin tool implementations. Just need to somehow register them per map, could probably
 *      have a tool handler per map.
 */
public class ToolHandler {
    private static final System.Logger logger = System.getLogger(ToolHandler.class.getName());

    private final EventNode<InstanceEvent> eventNode = EventNode.type("terraform:tool/handler", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, this::handleBreakBlock)
            .addListener(PlayerUseItemOnBlockEvent.class, this::handleUseItemOnBlock)
            .addListener(PlayerBlockInteractEvent.class, this::handleBlockInteract)
            .addListener(PlayerUseItemEvent.class, this::handleUseItem)
            .addListener(PlayerBlockPlaceEvent.class, this::handlePlaceBlock);

    private final Map<String, BuiltinTool> tools = new HashMap<>();
    private final List<String> toolNames;

    public ToolHandler() {
        this(true);
    }

    public ToolHandler(boolean discover) {
        var names = new ArrayList<String>();
        if (discover) {
            for (var tool : ServiceLoader.load(BuiltinTool.class)) {
                tools.put(tool.name(), tool);
                names.add(tool.namespace().path());
            }
        }
        this.toolNames = List.copyOf(names);
    }

    public @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    /**
     * Returns the short names of all builtin tools
     */
    public @NotNull List<String> getToolNames() {
        return toolNames;
    }

    public @NotNull ItemStack createBuiltinTool(@NotNull String name) {
        if (name.contains(":")) return createBuiltinTool(NamespaceID.from(name));
        return createBuiltinTool(NamespaceID.from("terraform", name));
    }

    public @NotNull ItemStack createBuiltinTool(@NotNull NamespaceID namespace) {
        var tool = tools.get(namespace.asString());
        Check.notNull(tool, "missing tool: " + namespace.asString());
        return ItemStack.of(tool.material())
                .withTag(BuiltinTool.TYPE, namespace.asString());
    }

    private void handleBreakBlock(@NotNull PlayerBlockBreakEvent event) {
        var itemStack = event.getPlayer().getItemInMainHand();
        var tool = getTool(itemStack);

        if (tool == null) return;
        event.setCancelled(true);
        if ((tool.flags() & BuiltinTool.LEFT_CLICK_BLOCK) == 0) return;

        tool.leftClicked(new BuiltinTool.Click(
                tool,
                event.getPlayer(),
                itemStack,
                Player.Hand.MAIN,
                event.getBlockPosition(),
                null,
                event.getBlockFace(),
                null
        ));
    }

    private void handleUseItem(@NotNull PlayerUseItemEvent event) {
        var tool = getTool(event.getItemStack());
        if (tool == null || (tool.flags() & BuiltinTool.RIGHT_CLICK_AIR) == 0) return;

        var player = event.getPlayer();
        if (player.getTargetBlockPosition(5) != null) {
            // For some dumb reason this is triggered when right clicking on a block, so is playerUseItemOnBlockEvent
            // so we filter this one out.
            return;
        }

        tool.rightClicked(new BuiltinTool.Click(
                tool,
                player,
                event.getItemStack(),
                event.getHand(),
                null, null,
                null, null
        ));
    }

    private void handleUseItemOnBlock(@NotNull PlayerUseItemOnBlockEvent event) {
        var itemStack = event.getItemStack();
        var tool = getTool(itemStack);
        if (tool == null || (tool.flags() & BuiltinTool.RIGHT_CLICK_BLOCK) == 0) return;

        var placeOffset = event.getBlockFace().toDirection();
        tool.rightClicked(new BuiltinTool.Click(
                tool,
                event.getPlayer(),
                event.getItemStack(),
                event.getHand(),
                event.getPosition(),
                event.getPosition().add(placeOffset.normalX(), placeOffset.normalY(), placeOffset.normalZ()),
                event.getBlockFace(),
                null
        ));
    }

    private void handleBlockInteract(@NotNull PlayerBlockInteractEvent event) {
        var itemStack = event.getPlayer().getItemInHand(event.getHand());
        var tool = getTool(itemStack);
        if (tool == null || (tool.flags() & BuiltinTool.RIGHT_CLICK_BLOCK) == 0) return;

        event.setCancelled(true);
    }

    private void handlePlaceBlock(@NotNull PlayerBlockPlaceEvent event) {
        var itemStack = event.getPlayer().getItemInHand(event.getHand());
        var tool = getTool(itemStack);

        if (tool == null) return;
        event.setCancelled(true);
        if ((tool.flags() & BuiltinTool.RIGHT_CLICK_BLOCK) == 0) return;

        var placeOffset = event.getBlockFace().toDirection();
        tool.rightClicked(new BuiltinTool.Click(
                tool,
                event.getPlayer(),
                itemStack,
                event.getHand(),
                event.getBlockPosition().sub(placeOffset.normalX(), placeOffset.normalY(), placeOffset.normalZ()),
                event.getBlockPosition(),
                event.getBlockFace(),
                null
        ));
    }

    private @Nullable BuiltinTool getTool(@NotNull ItemStack itemStack) {
        var toolType = itemStack.getTag(BuiltinTool.TYPE);
        if (toolType == null) return null;

        var tool = tools.get(toolType);
        if (tool == null) {
            logger.log(System.Logger.Level.WARNING, "Unknown tool type: " + toolType);
            return null;
        }

        return tool;
    }
}
