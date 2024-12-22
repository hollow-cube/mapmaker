package net.hollowcube.mapmaker.map.obungus;

import com.google.inject.Inject;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.block.handler.StructureBlockHandler;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.obungus.ObungusCore;
import net.hollowcube.mapmaker.obungus.ObungusService;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.schem.reader.SpongeSchematicReader;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class ObungusBoxReviewMap extends PlayingMapWorld {
    public static final Constructor<ObungusBoxReviewMap> CTOR = AbstractMapWorld.ctor(ObungusBoxReviewMap::new, ObungusBoxReviewMap.class);

    private final ObungusService obungusService;

    @Inject
    public ObungusBoxReviewMap(@NotNull MapServer server, @NotNull MapData map) {
        super(server, map);
        this.obungusService = server.facet(ObungusService.class);
    }

    @Override
    protected void loadWorld() {
        // Do nothing.
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);

        var playerData = PlayerDataV2.fromPlayer(player);
        var box = obungusService.getBoxFromReviewQueue(playerData.id());
        var schematic = new SpongeSchematicReader().read(Base64.getDecoder().decode(box.schematicData()));
        schematic.forEachBlock((pos, block) -> {
            instance.setBlock(pos.add(-7, 40 - 4, 2), block);
        });
        instance.setBlock(0, 39, 0, StructureBlockHandler.forOffsetSize(
                new Vec(-7, -3, 2),
                new Vec(15, 19, 15),
                StructureBlockHandler.Mode.SAVE
        ));
        player.setGameMode(GameMode.CREATIVE);
        player.setPermissionLevel(4);

        {
            var text = new Entity(EntityType.TEXT_DISPLAY) {{
                setNoGravity(true);
                hasPhysics = false;
            }};
            var meta = (TextDisplayMeta) text.getEntityMeta();
            meta.setText(Component.text(Objects.requireNonNullElse(box.name(), "<unnamed>")));
            meta.setLeftRotation(new Quaternion(new Vec(0, 1, 0), Math.toRadians(180 + 30)).into());
            text.setInstance(instance, new Vec(1.5, 41.25, 1.5));
        }
        {
            var text = new Entity(EntityType.TEXT_DISPLAY) {{
                setNoGravity(true);
                hasPhysics = false;
            }};
            var meta = (TextDisplayMeta) text.getEntityMeta();
            Component authorText;
            if (box.playerId() != null) {
                try {
                    authorText = server().playerService().getPlayerDisplayName2(box.playerId())
                            .build(DisplayName.Context.DEFAULT);
                } catch (Exception e) {
                    authorText = Component.text("<error>");
                }
            } else if (box.legacyUsername() != null) {
                authorText = Component.text(box.legacyUsername());
            } else {
                authorText = Component.text("<unknown author>");
            }
            meta.setText(Component.text("by ").append(authorText));
            meta.setLeftRotation(new Quaternion(new Vec(0, 1, 0), Math.toRadians(180 + 30)).into());
            text.setInstance(instance, new Vec(1.5, 41 - 0.05, 1.5));
        }


        var endOffset = box.getEndOffset();
        if (endOffset != null) {
            instance.setBlock(endOffset.add(-7, 40 - 4, 2).add(0, 0, 1), Block.GOLD_BLOCK);
        } else {
            player.sendMessage("no end block");
        }
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
}
