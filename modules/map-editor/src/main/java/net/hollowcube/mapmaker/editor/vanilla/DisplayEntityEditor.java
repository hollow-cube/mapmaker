package net.hollowcube.mapmaker.editor.vanilla;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.SidebarDisplay;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDespawnEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import static net.hollowcube.mapmaker.map.entity.impl.DisplayEntity.SELECTED_DISPLAY_ENTITY;

public class DisplayEntityEditor {
    public static final Tag<SidebarDisplay> SELECTED_ENTITY_SIDEBAR = Tag.Transient("mapmaker:selected_entity_sidebar");

    public static final EventNode<InstanceEvent> EVENT_NODE = EventNode.type("display-entity-event-node", EventFilter.INSTANCE)
            .addListener(PlayerTickEndEvent.class, DisplayEntityEditor::onPlayerTick)
            .addListener(EntityDespawnEvent.class, DisplayEntityEditor::onEntityDespawn);

    public static void clear(Player player) {
        setSelectedDisplayEntity(player, null);
    }

    private static void onEntityDespawn(EntityDespawnEvent event) {
        var entity = event.getEntity();
        if (!(entity instanceof DisplayEntity displayEntity)) return;
        var sidebar = displayEntity.getTag(SELECTED_ENTITY_SIDEBAR);
        if (sidebar == null) return;
        sidebar.destory();
    }

    private static void onPlayerTick(PlayerTickEndEvent event) {
        var player = event.getPlayer();
        var selectedEntity = OpUtils.map(player.getTag(SELECTED_DISPLAY_ENTITY), player.getInstance()::getEntityByUuid);

        if (!(selectedEntity instanceof DisplayEntity displayEntity)) {
            setSelectedDisplayEntity(player, null);
        } else {
            var selectedSidebar = getDetailsSidebar(displayEntity, false);
            if (selectedSidebar == null) return;

            selectedSidebar.update("x", selectedEntity.getPosition().x());
            selectedSidebar.update("y", selectedEntity.getPosition().y());
            selectedSidebar.update("z", selectedEntity.getPosition().z());

            var leftRot = displayEntity.getEntityMeta().getLeftRotation();
            var rightRot = displayEntity.getEntityMeta().getRightRotation();

            var euler = new Quaternion(leftRot[0], leftRot[1], leftRot[2], leftRot[3])
                    .mulThis(new Quaternion(rightRot[0], rightRot[1], rightRot[2], rightRot[3]))
                    .toEulerAngles();

            selectedSidebar.update("rotation", euler.x(), euler.y(), euler.z());

            var scale = displayEntity.getEntityMeta().getScale();
            selectedSidebar.update("scale", scale.x(), scale.y(), scale.z());
        }
    }

    public static void setSelectedDisplayEntity(Player player, @Nullable DisplayEntity entity) {
        var instance = player.getInstance();
        if (instance == null) return;

        var previousEntity = OpUtils.map(
                player.getAndSetTag(SELECTED_DISPLAY_ENTITY, OpUtils.map(entity, DisplayEntity::getUuid)),
                instance::getEntityByUuid
        );

        if (previousEntity instanceof DisplayEntity displayEntity) {
            var sidebar = getDetailsSidebar(displayEntity, false);
            if (sidebar != null) sidebar.removeViewer(player);

            displayEntity.forceSendMetaPacket();
        }

        if (entity != null) {
            getDetailsSidebar(entity, true).addViewer(player);
            entity.forceSendMetaPacket();
        }
    }

    @Nullable
    @Contract("_, true -> !null")
    public static SidebarDisplay getDetailsSidebar(DisplayEntity entity, boolean create) {
        var sidebar = entity.getTag(SELECTED_ENTITY_SIDEBAR);
        if (sidebar == null && create) {
            sidebar = SidebarDisplay.create(Component.translatable("sidebar.display_entity.title"))
                    .withSpacer()
                    .withLine(Component.translatable("sidebar.display_entity.type.title"))
                    .withLine(Component.translatable("sidebar.display_entity.type", Component.text(entity.getEntityType().name())))
                    .withSpacer()
                    .withLine(Component.translatable("sidebar.display_entity.position.title"))
                    .withLine("x", "sidebar.display_entity.x", 0.0)
                    .withLine("y", "sidebar.display_entity.y", 0.0)
                    .withLine("z", "sidebar.display_entity.z", 0.0)
                    .withSpacer()
                    .withLine(Component.translatable("sidebar.display_entity.rotation.title"))
                    .withLine("rotation", "sidebar.display_entity.rotation", 0.0, 0.0, 0.0)
                    .withSpacer()
                    .withLine(Component.translatable("sidebar.display_entity.scale.title"))
                    .withLine("scale", "sidebar.display_entity.scale", 1.0, 1.0, 1.0)
                    .withSpacer();
            entity.setTag(SELECTED_ENTITY_SIDEBAR, sidebar);
        }
        return sidebar;
    }
}
