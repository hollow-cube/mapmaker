package net.hollowcube.mapmaker.map.obungus;

import com.google.inject.Inject;
import net.hollowcube.mapmaker.gui.world.InWorldGui;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.block.handler.StructureBlockHandler;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.obungus.ui.MapReviewGui;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.obungus.ObungusBoxData;
import net.hollowcube.mapmaker.obungus.ObungusCore;
import net.hollowcube.mapmaker.obungus.ObungusService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.schem.reader.SpongeSchematicReader;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.UUID;

public class ObungusBoxReviewMap extends PlayingMapWorld {
    private final ObungusService obungusService;

    @Inject
    public ObungusBoxReviewMap(
            @NotNull MapServer server,
            @NotNull FeatureList features,
            @NotNull MapData map,
            @NotNull ObungusService obungusService
    ) {
        super(server, features, map);
        this.obungusService = obungusService;
    }

    @Override
    protected void loadWorld() {
        // Do nothing.
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);

        player.setGameMode(GameMode.CREATIVE);
        player.setPermissionLevel(4);

        var firstBoxPos = new Vec(0, 39, 1);

        var playerData = PlayerDataV2.fromPlayer(player);

//        for (int i = 0; i < 10; i++) {
//            var box = obungusService.getBoxFromReviewQueue(playerData.id());
//            spawnBoxAtPosWithRotation(box, firstBoxPos.add(0, 0, i * 20));
//
//        }
        var box = obungusService.getBoxFromReviewQueue(playerData.id());
        spawnBoxAtPosWithRotation(box, firstBoxPos.add(0, 0, 0));
        var ratingGui = new MapReviewGui(obungusService, box);
        ratingGui.setInstance(instance, firstBoxPos);
        ratingGui.addViewer(player);

        instance.setBlock(firstBoxPos.sub(0, 0, 1), StructureBlockHandler.forOffsetSize(
                new Vec(-7, -3, 2),
                new Vec(15, 19, 15),
                StructureBlockHandler.Mode.SAVE
        ));

//        {
//            var text = new Entity(EntityType.TEXT_DISPLAY) {{
//                setNoGravity(true);
//                hasPhysics = false;
//            }};
//            var meta = (TextDisplayMeta) text.getEntityMeta();
//            meta.setText(Component.text(Objects.requireNonNullElse(box.name(), "<unnamed>")));
//            meta.setLeftRotation(new Quaternion(new Vec(0, 1, 0), Math.toRadians(180 + 30)).into());
//            text.setInstance(instance, new Vec(1.5, 41.25, 1.5));
//        }
//        {
//            var text = new Entity(EntityType.TEXT_DISPLAY) {{
//                setNoGravity(true);
//                hasPhysics = false;
//            }};
//            var meta = (TextDisplayMeta) text.getEntityMeta();
//            Component authorText;
//            if (box.playerId() != null) {
//                try {
//                    authorText = server().playerService().getPlayerDisplayName2(box.playerId())
//                            .build(DisplayName.Context.DEFAULT);
//                } catch (Exception e) {
//                    authorText = Component.text("<error>");
//                }
//            } else if (box.legacyUsername() != null) {
//                authorText = Component.text(box.legacyUsername());
//            } else {
//                authorText = Component.text("<unknown author>");
//            }
//            meta.setText(Component.text("by ").append(authorText));
//            meta.setLeftRotation(new Quaternion(new Vec(0, 1, 0), Math.toRadians(180 + 30)).into());
//            text.setInstance(instance, new Vec(1.5, 41 - 0.05, 1.5));
//        }
//
//        var endOffset = box.getEndOffset();
//        if (endOffset != null) {
//            instance.setBlock(endOffset.add(-7, 40 - 4, 2).add(0, 0, 1), Block.GOLD_BLOCK);
//        } else {
//            player.sendMessage("no end block");
//        }

    }

    @Override
    protected @NotNull SaveState getOrCreateSaveState(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);
        return new SaveState(
                UUID.randomUUID().toString(),
                playerData.id(),
                ObungusCore.REVIEW_MAP_ID,
                SaveStateType.PLAYING,
                PlayState.SERIALIZER,
                new PlayState()
        );
    }

    // pos is the spawn block of the box 1 outside the buildable area
    private void spawnBoxAtPosWithRotation(@NotNull ObungusBoxData box, @NotNull Point pos) {
        var schematic = new SpongeSchematicReader().read(Base64.getDecoder().decode(box.schematicData()));

        var schematicOrigin = pos.add(-7, -3, 1);
        schematic.forEachBlock((blockPosition, block) -> {
            instance.setBlock(schematicOrigin.add(blockPosition), block);
        });

        // Create the end platform
        for (int i = 0; i < 10; i++) {

        }
    }
}
