package net.hollowcube.mapmaker.runtime.parkour.action.impl.entity;

import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.entity.PlayerEntityTracker;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public record SpawnEntityAction(
    @Nullable EntityType type
) implements Action {
    private static final Sprite SPRITE = new Sprite("icon2/1_1/volume_max", 1, 1);

    public static final Key KEY = Key.key("mapmaker:spawn_entity");
    public static final StructCodec<SpawnEntityAction> CODEC = StructCodec.struct(
        "type", EntityType.CODEC.optional(), SpawnEntityAction::type,
        SpawnEntityAction::new);
    public static final Action.Editor<SpawnEntityAction> EDITOR = new Action.Editor<>(
        Editor::new, _ -> SPRITE,
        SpawnEntityAction::makeThumbnail, Set.of()
    );

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        var entity = MapEntityType.create(EntityType.CHICKEN, UUID.randomUUID());
        PlayerEntityTracker.forPlayer(player).add(entity);

        entity.setInstance(player.getInstance(), player.getPosition().withView(0, 0));
    }

    private static TranslatableComponent makeThumbnail(@Nullable SpawnEntityAction action) {
        return Component.translatable("gui.action.spawn_entity.thumbnail");
    }

    private static class Editor extends AbstractActionEditorPanel<SpawnEntityAction> {

        public Editor(ActionList.Ref ref) {
            super(ref, true);

            background("action/editor/container_sm", -10, -31);
        }

        @Override
        protected void update(SpawnEntityAction data) {

        }
    }

}
