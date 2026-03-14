package net.hollowcube.mapmaker.hub.entity;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class NpcPlayer extends BaseNpcEntity {
    public static final Team NPC_TEAM = MinecraftServer.getTeamManager().createBuilder("hub_npcs")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build();

    private final String username;
    private final @Nullable PlayerSkin skin;

    private final NpcTextModel nameTag = new NpcTextModel();

    private final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();

    public NpcPlayer(String username, @Nullable PlayerSkin skin) {
        this(UUID.randomUUID(), username, skin);
    }

    public NpcPlayer(UUID uuid, String username, @Nullable PlayerSkin skin) {
        super(EntityType.PLAYER, uuid);
        this.username = username;
        this.skin = skin;

        this.nameTag.setAutoViewable(false);
        this.nameTag.getEntityMeta().setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        this.nameTag.getEntityMeta().setText(Component.text(username));
    }

    public void setNameTag(Component name) {
        this.nameTag.getEntityMeta().setText(name);
    }

    public void setEquipment(EquipmentSlot slot, ItemStack itemStack) {
        this.equipment.put(slot, itemStack);
    }

    @Override
    protected void movementTick() {
        // Intentionally do nothing
    }

    @Override
    public void update(long time) {
        super.update(time);

        // Look at nearby players (individually)
        for (var viewer : getViewers()) {
            if (this.position.distanceSquared(viewer.getPosition()) > 100) return;

            var newPosition = this.position.add(0, getEyeHeight(), 0).withLookAt(viewer.getPosition().add(0, viewer.getEyeHeight(), 0));
            var p1 = new EntityRotationPacket(getEntityId(), newPosition.yaw(), newPosition.pitch(), true);
            viewer.sendPacket(p1);
            var p2 = new EntityHeadLookPacket(getEntityId(), newPosition.yaw());
            viewer.sendPacket(p2);
        }

    }

    @Override
    public void updateNewViewer(Player player) {
        var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
        if (this.skin != null) {
            properties.add(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature()));
        }
        var entry = new PlayerInfoUpdatePacket.Entry(getUuid(), username, properties, false, 0, GameMode.SURVIVAL, null, null, 0, true);
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry));

        // Spawn the player entity
        super.updateNewViewer(player);

        // Enable skin layers
        player.sendPackets(new EntityMetaDataPacket(getEntityId(), Map.of(
                MetadataDef.Avatar.DISPLAYED_MODEL_PARTS_FLAGS.index(),
                Metadata.Byte((byte) 0b1111111))
        ));

        if (equipment.containsKey(EquipmentSlot.HELMET)) {
            player.sendPacket(new TeamsPacket(NPC_TEAM.getTeamName(), new TeamsPacket.AddEntitiesToTeamAction(List.of(username))));
            nameTag.addViewer(player);
        }
        if (!equipment.isEmpty()) {
            player.sendPacket(new EntityEquipmentPacket(getEntityId(), equipment));
        }
    }

    @Override
    public void updateOldViewer(Player player) {
        super.updateOldViewer(player);

        if (equipment.containsKey(EquipmentSlot.HELMET))
            player.sendPacket(new PlayerInfoRemovePacket(getUuid()));
        else nameTag.removeViewer(player);
    }

    @Override
    public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
        if (equipment.containsKey(EquipmentSlot.HELMET))
            this.nameTag.setInstance(instance, spawnPosition.add(0, 2.4, 0));
        return super.setInstance(instance, spawnPosition);
    }
}
