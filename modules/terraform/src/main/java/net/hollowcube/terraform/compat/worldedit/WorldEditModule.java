package net.hollowcube.terraform.compat.worldedit;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.events.PlayerGiveCreativeItemEvent;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.compat.worldedit.command.*;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.Material;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class WorldEditModule implements TerraformModule {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("map-worldedit", EventFilter.INSTANCE)
            .addListener(PlayerGiveCreativeItemEvent.class, this::onGiveCreativeItem);

    @Override
    public @NotNull Set<CommandDsl> commands() {
        return Set.of(
                // General
                new GeneralCommands.Undo(),
                new GeneralCommands.Redo(),
                new GeneralCommands.ClearHistory(),
//                new GeneralCommands.GMask(), //todo

                // Selection
                SelectionCommands.Pos.Pos1(),
                SelectionCommands.Pos.Pos2(),
                new SelectionCommands.HPos1(),
                new SelectionCommands.HPos2(),
                new SelectionCommands.Chunk(),
                new SelectionCommands.Wand(),
//                new SelectionCommands.Contract(), //todo
                new SelectionCommands.Shift(),
                new SelectionCommands.Outset(),
                new SelectionCommands.Inset(),
                new SelectionCommands.Size(),
                new SelectionCommands.Count(),
                new SelectionCommands.Distr(),
                new SelectionCommands.Sel(),
                new SelectionCommands.Expand(),

                // Region
                new RegionCommands.Set(),
                new RegionCommands.Line(),
//                new RegionCommands.Curve(), //todo
                new RegionCommands.Replace(),
                new RegionCommands.Overlay(),
                new RegionCommands.Center(),
//                new RegionCommands.Naturalize(), //todo
                new RegionCommands.Walls(),
                new RegionCommands.Faces(),
//                new RegionCommands.Smooth(), //todo
                new RegionCommands.Move(),
                new RegionCommands.Stack(),
                new RegionCommands.Smear(),
//                new RegionCommands.Hollow(), //todo

                // Generation
                new GenerationCommands.HCyl(),
                new GenerationCommands.Cyl(),
                new GenerationCommands.HSphere(),
                new GenerationCommands.Sphere(),
                new GenerationCommands.HPyramid(),
                new GenerationCommands.Pyramid(),

                // Schematic
                new SchematicCommands.Schem(),
                new SchematicCommands.Copy(),
                new SchematicCommands.Cut(),
                new SchematicCommands.Paste(),
                new SchematicCommands.Rotate(),
                new SchematicCommands.Flip(),
                new SchematicCommands.ClearClipboard(),

                // Tool
//                new ToolCommands.Tool(), //todo
//                new ToolCommands.Mask(), //todo
//                new ToolCommands.Material(), //todo
//                new ToolCommands.Range(), //todo
//                new ToolCommands.TraceMask(), //todo

                // Brush
                //todo

                // Biome
                new BiomeCommands.BiomeList(),
                new BiomeCommands.BiomeInfo(),
                new BiomeCommands.SetBiome(),

                // Chunk
//                new ChunkCommands.ChunkInfo(), //todo
//                new ChunkCommands.ListChunks(), //todo
//                new ChunkCommands.DelChunks(), //todo

                // Utility
                new UtilityCommands.Fill(),
                new UtilityCommands.Drain(),
//                new UtilityCommands.FixLava(), //todo
//                new UtilityCommands.FixWater(), //todo
                new UtilityCommands.RemoveAbove(),
                new UtilityCommands.RemoveBelow(),
                new UtilityCommands.RemoveNear(),
                new UtilityCommands.ReplaceNear()
//                new UtilityCommands.Help() //todo
        );
    }

    @Override
    public @NotNull Set<EventNode<InstanceEvent>> eventNodes() {
        return Set.of(eventNode);
    }

    private void onGiveCreativeItem(@NotNull PlayerGiveCreativeItemEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) return;
        var inventory = event.getPlayer().getInventory();
        if (event.slot() < 0 || event.slot() >= inventory.getSize()) return;

        if (event.item().material() == Material.WOODEN_AXE) {
            var tf = LocalSession.forPlayer(event.getPlayer()).terraform();
            var itemStack = tf.toolHandler().createBuiltinTool("terraform:wand");
            int slot = PlayerInventoryUtils.convertWindow0SlotToMinestomSlot(event.slot());
            inventory.setItemStack(slot, itemStack);
            inventory.sendSlotRefresh(slot, itemStack);

            event.setCancelled(true);
        }
    }
}
