package net.hollowcube.terraform.compat.worldedit;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.compat.worldedit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class WorldEditModule implements TerraformModule {

    @Override
    public @NotNull Set<CommandDsl> commands() {
        return Set.of(
                // General
                new GeneralCommands.Undo(),
                new GeneralCommands.Redo(),
                new GeneralCommands.ClearHistory(),
//                new GeneralCommands.GMask(), //todo

                // Selection
                new SelectionCommands.Pos1(),
                new SelectionCommands.Pos2(),
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
//                new RegionCommands.Line(), //todo
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
//                new BiomeCommands.BiomeList(), //todo
//                new BiomeCommands.BiomeInfo(), //todo
//                new BiomeCommands.SetBiome(), //todo

                // Chunk
//                new ChunkCommands.ChunkInfo(), //todo
//                new ChunkCommands.ListChunks(), //todo
//                new ChunkCommands.DelChunks(), //todo

                // Utility
//                new UtilityCommands.Fill(), //todo
//                new UtilityCommands.Fillr(), //todo
//                new UtilityCommands.Drain(), //todo
//                new UtilityCommands.FixLava(), //todo
//                new UtilityCommands.FixWater(), //todo
                new UtilityCommands.RemoveAbove(),
                new UtilityCommands.RemoveBelow(),
                new UtilityCommands.RemoveNear(),
                new UtilityCommands.ReplaceNear()
//                new UtilityCommands.Help() //todo
        );
    }
}
