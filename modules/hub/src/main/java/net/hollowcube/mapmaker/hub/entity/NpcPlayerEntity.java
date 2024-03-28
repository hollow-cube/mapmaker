package net.hollowcube.mapmaker.hub.entity;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Common logic for player-like NPCs in the hub.
 */
@SuppressWarnings("UnstableApiUsage")
public class NpcPlayerEntity extends BaseNpcEntity {
    private static final Team NPC_TEAM = MinecraftServer.getTeamManager().createBuilder("npcs")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build();

    private static final BadSprite RMB_SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("icon/mouse_right_small"), "icon/mouse_right_small");
    private static final Component PROMPT_BASE = Component.text("", TextColor.color(0xCCCCCC))
            .append(Component.text(RMB_SPRITE.fontChar() + FontUtil.computeOffset(3), NamedTextColor.WHITE))
            .append(Component.text("ᴛᴏ "));

    private static final Tag<PlayerSkin> SKIN_TAG = Tag.Structure("skin", PlayerSkin.class);

    private static int nameCounter = 1;

    private final String fakeUsername = String.valueOf(nameCounter++);

    private final NpcTextModel titleEntity = new NpcTextModel();
    private final NpcTextModel subtitleEntity = new NpcTextModel();

    public NpcPlayerEntity(@NotNull NBTCompound nbt) {
        this(UUID.randomUUID(), nbt);
    }

    public NpcPlayerEntity(@NotNull UUID uuid, @NotNull NBTCompound nbt) {
        super(EntityType.PLAYER, uuid);
        // TODO: this is set because minestom doesnt set correct entity attachment heights. In 1.20.5 this can be data generated so should be done.
        setSynchronizationTicks(Integer.MAX_VALUE);

        // TODO: this is set because minestom doesnt set correct entity attachment heights. In 1.20.5 this can be data generated so should be done.
        titleEntity.setSynchronizationTicks(Integer.MAX_VALUE);
        var titleMeta = titleEntity.getEntityMeta();
        titleMeta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        titleMeta.setTranslation(new Vec(0, 0.3, 0));

        // TODO: this is set because minestom doesnt set correct entity attachment heights. In 1.20.5 this can be data generated so should be done.
        subtitleEntity.setSynchronizationTicks(Integer.MAX_VALUE);
        var subtitleMeta = subtitleEntity.getEntityMeta();
        subtitleMeta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        subtitleMeta.setTranslation(new Vec(0, 0.1, 0));
        subtitleMeta.setScale(new Vec(0.7));

        var title = Objects.requireNonNullElseGet(nbt.getString("name"), () -> Objects.requireNonNullElse(nbt.getString("type"), "mapmaker:unknown"));
        titleEntity.getEntityMeta().setText(Component.text(title));
        var prompt = nbt.getString("prompt");
        if (prompt != null) subtitleEntity.getEntityMeta().setText(PROMPT_BASE.append(Component.text(prompt)));
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        return super.setInstance(instance, spawnPosition).thenRun(() -> {
            titleEntity.setInstance(instance, spawnPosition).thenRun(() -> addPassenger(titleEntity));
            subtitleEntity.setInstance(instance, spawnPosition).thenRun(() -> addPassenger(subtitleEntity));
        });
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
    public void updateNewViewer(@NotNull Player player) {
        var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
        var skin = getTag(SKIN_TAG);
        if (skin != null) {
            properties.add(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature()));
        }
        var entry = new PlayerInfoUpdatePacket.Entry(getUuid(), fakeUsername, properties, false, 0, GameMode.SURVIVAL, null, null);
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry));

        // Spawn the player entity
        super.updateNewViewer(player);

        // Enable skin layers
        player.sendPackets(new EntityMetaDataPacket(getEntityId(), Map.of(17, Metadata.Byte((byte) 127))));

        // Put them on the NPC team to hide their name tag
        final TeamsPacket addPlayerPacket = new TeamsPacket(NPC_TEAM.getTeamName(),
                new TeamsPacket.AddEntitiesToTeamAction(List.of(fakeUsername)));
        sendPacketToViewers(addPlayerPacket);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);

        player.sendPacket(new PlayerInfoRemovePacket(getUuid()));
    }

}
