package net.hollowcube.terraform.compat.worldedit;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.compat.worldedit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class WorldEditModule implements TerraformModule {

    @Override
    public @NotNull Set<Class<? extends CommandDsl>> commands() {
        return Set.of(
                // General
                GeneralCommands.Undo.class,
                GeneralCommands.Redo.class,
                GeneralCommands.ClearHistory.class,
//                GeneralCommands.GMask.class, //todo

                // Selection
                SelectionCommands.Pos1.class,
                SelectionCommands.Pos2.class,
                SelectionCommands.HPos1.class,
                SelectionCommands.HPos2.class,
                SelectionCommands.Chunk.class,
                SelectionCommands.Wand.class,
//                SelectionCommands.Contract.class, //todo
                SelectionCommands.Shift.class,
                SelectionCommands.Outset.class,
                SelectionCommands.Inset.class,
                SelectionCommands.Size.class,
                SelectionCommands.Count.class,
                SelectionCommands.Distr.class,
                SelectionCommands.Sel.class,
                SelectionCommands.Expand.class,

                // Region
                RegionCommands.Set.class,
//                RegionCommands.Line.class, //todo
//                RegionCommands.Curve.class, //todo
                RegionCommands.Replace.class,
                RegionCommands.Overlay.class,
                RegionCommands.Center.class,
//                RegionCommands.Naturalize.class, //todo
                RegionCommands.Walls.class,
                RegionCommands.Faces.class,
//                RegionCommands.Smooth.class, //todo
                RegionCommands.Move.class,
                RegionCommands.Stack.class,
                RegionCommands.Smear.class,
//                RegionCommands.Hollow.class, //todo

                // Generation
                GenerationCommands.HCyl.class,
                GenerationCommands.Cyl.class,
                GenerationCommands.HSphere.class,
                GenerationCommands.Sphere.class,
                GenerationCommands.HPyramid.class,
                GenerationCommands.Pyramid.class,

                // Schematic
                SchematicCommands.Schem.class,
                SchematicCommands.Copy.class,
                SchematicCommands.Cut.class,
                SchematicCommands.Paste.class,
                SchematicCommands.Rotate.class,
                SchematicCommands.Flip.class,
                SchematicCommands.ClearClipboard.class,

                // Tool
//                ToolCommands.Tool.class, //todo
//                ToolCommands.Mask.class, //todo
//                ToolCommands.Material.class, //todo
//                ToolCommands.Range.class, //todo
//                ToolCommands.TraceMask.class, //todo

                // Brush
                //todo

                // Biome
//                BiomeCommands.BiomeList.class, //todo
//                BiomeCommands.BiomeInfo.class, //todo
//                BiomeCommands.SetBiome.class, //todo

                // Chunk
//                ChunkCommands.ChunkInfo.class, //todo
//                ChunkCommands.ListChunks.class, //todo
//                ChunkCommands.DelChunks.class, //todo

                // Utility
//                UtilityCommands.Fill.class, //todo
//                UtilityCommands.Fillr.class, //todo
//                UtilityCommands.Drain.class, //todo
//                UtilityCommands.FixLava.class, //todo
//                UtilityCommands.FixWater.class, //todo
                UtilityCommands.RemoveAbove.class,
                UtilityCommands.RemoveBelow.class,
                UtilityCommands.RemoveNear.class,
                UtilityCommands.ReplaceNear.class
//                UtilityCommands.Help.class //todo
        );
    }
}
