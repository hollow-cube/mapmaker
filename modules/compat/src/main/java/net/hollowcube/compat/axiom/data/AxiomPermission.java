package net.hollowcube.compat.axiom.data;

import net.minestom.server.network.NetworkBuffer;

public enum AxiomPermission {
    ALL("axiom.all"),
    PAPER_DEBUG("axiom.debug"),
    ENTITY("axiom.entity.*"),
    ENTITY_SPAWN("axiom.entity.spawn"),
    ENTITY_MANIPULATE("axiom.entity.manipulate"),
    ENTITY_DELETE("axiom.entity.delete"),
    ENTITY_REQUESTDATA("axiom.entity.request_data"),
    BLUEPRINT("axiom.blueprint.*"),
    BLUEPRINT_UPLOAD("axiom.blueprint.upload"),
    BLUEPRINT_REQUEST("axiom.blueprint.request"),
    BLUEPRINT_MANIFEST("axiom.blueprint.manifest"),
    ANNOTATION("axiom.annotation.*"),
    ANNOTATION_CREATE("axiom.annotation.create"),
    ANNOTATION_CLEARALL("axiom.annotation.clear_all"),
    DEFAULT("axiom.*"),
    USE("axiom.use"),
    ALLOW_COPYING_OTHER_PLOTS("axiom.allow_copying_other_plots"),
    CAN_IMPORT_BLOCKS("axiom.can_import_blocks"),
    CAN_EXPORT_BLOCKS("axiom.can_export_blocks"),
    CHUNK("axiom.chunk.*"),
    CHUNK_REQUEST("axiom.chunk.request"),
    CHUNK_REQUESTBLOCKENTITY("axiom.chunk.request_block_entity"),
    BUILD("axiom.build.*"),
    BUILD_PLACE("axiom.build.place"),
    BUILD_SECTION("axiom.build.section"),
    BUILD_NBT("axiom.build.nbt"),
    EDITOR("axiom.editor.*"),
    EDITOR_USE("axiom.editor.use"),
    EDITOR_VIEWS("axiom.editor.views"),

    PLAYER("axiom.player.*"),
    PLAYER_BYPASS_MOVEMENT_RESTRICTIONS("axiom.player.bypass_movement_restrictions"),
    PLAYER_SPEED("axiom.player.speed"),
    PLAYER_TELEPORT("axiom.player.teleport"),
    PLAYER_GAMEMODE("axiom.player.gamemode.*"),
    PLAYER_GAMEMODE_SURVIVAL("axiom.player.gamemode.survival"),
    PLAYER_GAMEMODE_CREATIVE("axiom.player.gamemode.creative"),
    PLAYER_GAMEMODE_SPECTATOR("axiom.player.gamemode.spectator"),
    PLAYER_GAMEMODE_ADVENTURE("axiom.player.gamemode.adventure"),
    PLAYER_HOTBAR("axiom.player.hotbar"),
    PLAYER_SETNOPHYSICALTRIGGER("axiom.player.set_no_physical_trigger"),

    WORLD("axiom.world.*"),
    WORLD_TIME("axiom.world.time"),
    WORLD_PROPERTY("axiom.world.property"),
    CAPABILITY("axiom.capability.*"),
    BULLDOZER("axiom.capability.bulldozer"),
    REPLACE_MODE("axiom.capability.replace_mode"),
    FORCE_PLACE("axiom.capability.force_place"),
    NO_UPDATES("axiom.capability.no_updates"),
    TINKER("axiom.capability.tinker"),
    INFINITE_REACH("axiom.capability.infinite_reach"),
    FAST_PLACE("axiom.capability.fast_place"),
    ANGEL_PLACEMENT("axiom.capability.angel_placement"),
    NO_CLIP("axiom.capability.no_clip"),
    PHANTOM("axiom.capability.phantom"),

    TOOL("axiom.tool.*"),
    TOOL_MAGICSELECT("axiom.tool.magic_select"),
    TOOL_BOXSELECT("axiom.tool.box_select"),
    TOOL_FREEHANDSELECT("axiom.tool.freehand_select"),
    TOOL_LASSOSELECT("axiom.tool.lasso_select"),
    TOOL_RULER("axiom.tool.ruler"),
    TOOL_ANNOTATION("axiom.tool.annotation"),
    TOOL_PAINTER("axiom.tool.painter"),
    TOOL_NOISEPAINTER("axiom.tool.noise_painter"),
    TOOL_BIOMEPAINTER("axiom.tool.biome_painter"),
    TOOL_GRADIENTPAINTER("axiom.tool.gradient_painter"),
    TOOL_SCRIPTBRUSH("axiom.tool.script_brush"),
    TOOL_FREEHANDDRAW("axiom.tool.freehand_draw"),
    TOOL_SCULPTDRAW("axiom.tool.sculpt_draw"),
    TOOL_ROCK("axiom.tool.rock"),
    TOOL_WELD("axiom.tool.weld"),
    TOOL_MELT("axiom.tool.melt"),
    TOOL_STAMP("axiom.tool.stamp"),
    TOOL_TEXT("axiom.tool.text"),
    TOOL_SHAPE("axiom.tool.shape"),
    TOOL_PATH("axiom.tool.path"),
    TOOL_MODELLING("axiom.tool.modelling"),
    TOOL_FLOODFILL("axiom.tool.floodfill"),
    TOOL_FLUIDBALL("axiom.tool.fluidball"),
    TOOL_ELEVATION("axiom.tool.elevation"),
    TOOL_SLOPE("axiom.tool.slope"),
    TOOL_SMOOTH("axiom.tool.smooth"),
    TOOL_DISTORT("axiom.tool.distort"),
    TOOL_ROUGHEN("axiom.tool.roughen"),
    TOOL_SHATTER("axiom.tool.shatter"),
    TOOL_EXTRUDE("axiom.tool.extrude"),
    TOOL_MODIFY("axiom.tool.modify"),

    BUILDERTOOL("axiom.builder_tool.*"),
    BUILDERTOOL_MOVE("axiom.builder_tool.move"),
    BUILDERTOOL_CLONE("axiom.builder_tool.clone"),
    BUILDERTOOL_STACK("axiom.builder_tool.stack"),
    BUILDERTOOL_SMEAR("axiom.builder_tool.smear"),
    BUILDERTOOL_EXTRUDE("axiom.builder_tool.extrude"),
    BUILDERTOOL_ERASE("axiom.builder_tool.erase"),
    BUILDERTOOL_SETUPSYMMETRY("axiom.builder_tool.setup_symmetry")
    ;

    public static final NetworkBuffer.Type<AxiomPermission> TYPE = NetworkBuffer.STRING
        .transform(AxiomPermission::fromString, AxiomPermission::id);

    private final String id;

    AxiomPermission(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static AxiomPermission fromString(String name) {
        for (AxiomPermission permission : AxiomPermission.values()) {
            if (permission.id.equals(name)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown AxiomPermission: " + name);
    }
}
