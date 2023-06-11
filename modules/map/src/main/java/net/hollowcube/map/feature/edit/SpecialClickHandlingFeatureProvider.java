package net.hollowcube.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@AutoService(FeatureProvider.class)
public class SpecialClickHandlingFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("special-click-handler", EventFilter.INSTANCE)
            .addListener(PlayerBlockInteractEvent.class, this::handeBlockInteract);

    private static final Set<String> TOGGLEABLE_STATES = Set.of("lit", "extendable");

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) == 0) return false;

        world.addScopedEventNode(eventNode);
        return true;
    }

    private void handeBlockInteract(PlayerBlockInteractEvent event) {
        if (event.getHand() != Player.Hand.MAIN || !event.getPlayer().isSneaking()) return;

        var block = event.getBlock();
        boolean changed = false;

        String state;
        if ((state = block.getProperty("lit")) != null) {
            block = block.withProperty("lit", state.equals("true") ? "false" : "true");
            changed = true;
        } else if ((state = block.getProperty("extendable")) != null) {
            block = block.withProperty("extendable", state.equals("true") ? "false" : "true");
            changed = true;
        } else if (block.id() == Block.FARMLAND.id()) {
            state = block.getProperty("moisture");
            block = block.withProperty("moisture", state.equals("7") ? "0" : "7");
            changed = true;
        } else if (block.name().contains("trapdoor") || block.id() == Block.BARREL.id()) {
            // todo should use trapdoor block tag i guess
            state = block.getProperty("open");
            block = block.withProperty("open", state.equals("true") ? "false" : "true");
            changed = true;
        } else if (block.id() == Block.CAKE.id()) {
            var bites = Integer.parseInt(block.getProperty("bites"));
            block = block.withProperty("bites", String.valueOf((bites + 1) % 7));
            changed = true;
        } else if (block.id() == Block.COMPOSTER.id()) {
            var level = Integer.parseInt(block.getProperty("level"));
            block = block.withProperty("level", String.valueOf((level + 1) % 9));
            changed = true;
        }

        if (changed) {
            // Update the block in the world to the new state
            event.getInstance().setBlock(event.getBlockPosition(), block);
        }
    }
}
