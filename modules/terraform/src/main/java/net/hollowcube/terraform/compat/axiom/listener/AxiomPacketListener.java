package net.hollowcube.terraform.compat.axiom.listener;

import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomAckWorldPropertyPacket;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomChunkDataResponsePacket;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomEnablePacket;
import net.hollowcube.terraform.compat.axiom.world.property.WorldPropertiesRegistry;
import net.hollowcube.terraform.event.TerraformModifyEntityEvent;
import net.hollowcube.terraform.event.TerraformMoveEntityEvent;
import net.hollowcube.terraform.event.TerraformPreSpawnEntityEvent;
import net.hollowcube.terraform.event.TerraformSpawnEntityEvent;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class AxiomPacketListener {
    private static final Logger logger = LoggerFactory.getLogger(AxiomPacketListener.class);

    public void handleHelloMessage(@NotNull Player player, @NotNull AxiomClientHelloPacket packet) {
        var clientInfo = new Axiom.ClientInfo(packet.apiVersion(), packet.extraData());
        logger.info("Axiom is present for {} (API {})", player.getUsername(), clientInfo.apiVersion());
        if (clientInfo.apiVersion() < Axiom.MIN_API_VERSION) {
            player.sendMessage("Your version of Axiom is too old, please update to the latest version.");
        } else if (clientInfo.apiVersion() > Axiom.MAX_API_VERSION) {
            player.sendMessage("Your version of Axiom is not yet supported. Please be patient while we update.");
        } else {
            player.setTag(Axiom.CLIENT_INFO_TAG, clientInfo);

            // It may have been enabled before the hello message was received, so check that.
            if (Axiom.isEnabled(player)) {
                Axiom.enable(player); // This time the hello message will be sent.
            } else {
                var disablePacket = new AxiomEnablePacket(false);
                player.sendPacket(disablePacket.toPacket(player));
            }
        }
    }

    public void handleSetGamemode(@NotNull Player player, @NotNull AxiomClientSetGameModePacket packet) {
        if (!Axiom.isEnabled(player)) return;
        try {
            player.setGameMode(GameMode.fromId(packet.gameModeId()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid gamemode received from {} ({})", player.getUuid(), packet.gameModeId());
        }
    }

    public void handleSetFlySpeed(@NotNull Player player, @NotNull AxiomClientSetFlySpeedPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        player.setFlyingSpeed(packet.flySpeed());
    }

    public void handleSetHotbarSlot(@NotNull Player player, @NotNull AxiomClientSetHotbarSlotPacket packet) {

    }

    public void handleSwitchActiveHotbar(@NotNull Player player, @NotNull AxiomClientSwitchActiveHotbarPacket packet) {

    }

    public void handleTeleport(@NotNull Player player, @NotNull AxiomClientTeleportPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var playerDimensionName = player.getInstance().getDimensionName();
        if (!packet.dimensionName().equals(playerDimensionName)) {
            logger.warn("Received axiom teleport to different dimension ({} -> {}) from {}",
                    playerDimensionName, packet.dimensionName(), player.getUuid());
            return;
        }

        player.teleport(packet.position());
    }

    public void handleSetEditorViews(@NotNull Player player, @NotNull AxiomClientSetEditorViewsPacket packet) {

    }

    public void handleRequestChunkData(@NotNull Player player, @NotNull AxiomClientChunkDataRequestPacket packet) {
        //todo need to properly handle this in the future, for now just send back an empty response always
        var response = new AxiomChunkDataResponsePacket(packet.correlationId());
        player.sendPacket(response.toPacket(player));
    }

    public void handleSetBlock(@NotNull Player player, @NotNull AxiomClientSetBlockPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.info("Received axiom:set_block from {}", player.getUuid());

        try {
            var instance = player.getInstance();

            //todo use the other stuff here to do whatever fancy things.

            for (var entry : packet.blocks().entrySet()) {
                var blockPosition = entry.getKey();
                var block = entry.getValue();
                instance.setBlock(blockPosition, block, packet.updateNeighbors());
            }

        } finally {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
        }
    }

    public void handleSetBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:set_buffer from {}", player.getUuid());

        var playerDimensionName = player.getInstance().getDimensionName();
        if (!packet.dimensionName().equals(playerDimensionName)) {
            logger.warn("Received axiom set buffer for a different dimension ({} -> {}) from {}",
                    playerDimensionName, packet.dimensionName(), player.getUuid());
            return;
        }

        var session = LocalSession.forPlayer(player);
        session.buildTask("axiom-" + packet.correlationId())
                .metadata() //todo
                .buffer(packet.buffer())
                // Set the task as ephemeral, so it is not appended to the history stack
                // Axiom will manage undoing these changes itself.
                .ephemeral()
                .submit();
    }

    public void handleSetWorldProperty(@NotNull Player player, @NotNull AxiomClientSetWorldPropertyPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:set_world_property from {}", player.getUuid());

        var registry = WorldPropertiesRegistry.get(player.getInstance());
        var handled = registry.handlePropertyChange(player, packet);
        if (!handled) return;

        var response = new AxiomAckWorldPropertyPacket(packet.sequenceId());
        player.sendPacket(response.toPacket(player));
    }

    public void handleSetTime(@NotNull Player player, @NotNull AxiomClientSetTimePacket packet) {

    }

    public void handleSpawnEntities(@NotNull Player player, @NotNull AxiomClientSpawnEntitiesPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var instance = player.getInstance();
        for (var entry : packet.entries()) {
            try {
                if (entry.copyFrom() != null) throw new RuntimeException("cannot handle copyFrom yet.");

                var nbt = entry.nbt();
                var entityTypeName = entry.nbt().getString("id");
                if (entityTypeName == null) throw new NullPointerException("entity id is required");
                var entityType = EntityType.fromNamespaceId(entityTypeName);
                if (entityType == null) throw new NullPointerException("unknown entity type: " + entityTypeName);

                var preEvent = new TerraformPreSpawnEntityEvent(player, instance);
                EventDispatcher.call(preEvent);
                if (preEvent.isCancelled()) continue;

                var entity = preEvent.getConstructor().apply(entityType, UUID.randomUUID());
                applyEntityMetadataFromNbt(entity.getEntityMeta(), nbt);

                var event = new TerraformSpawnEntityEvent(player, instance, entity, entry.pos());
                EventDispatcher.callCancellable(event, () -> event.getEntity().setInstance(instance, event.getPosition()));

                //todo handle passengers
            } catch (Exception e) {
                logger.warn("Failed to spawn axiom entity: {}", e.getMessage());
                player.sendMessage(Component.translatable("axiom.entity_spawn_failed"));
            }
        }
    }

    public void handleModifyEntities(@NotNull Player player, @NotNull AxiomClientModifyEntitiesPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var instance = player.getInstance();
        for (var entry : packet.entries()) {
            var entity = Entity.getEntity(entry.uuid());
            if (entity == null) {
                logger.warn("Axiom tried to modify an unknown entity: {} player={}", entry.uuid(), player.getUuid());
                continue;
            }
            if (!instance.equals(entity.getInstance()) || entity instanceof Player) continue;

            var entryPos = entry.pos();
            if (entryPos != null) {
                var newX = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_X) != 0 ? entity.getPosition().x() + entryPos.x() : entryPos.x();
                var newY = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_Y) != 0 ? entity.getPosition().y() + entryPos.y() : entryPos.y();
                var newZ = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_Z) != 0 ? entity.getPosition().z() + entryPos.z() : entryPos.z();
                var newYaw = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_YAW) != 0 ? entity.getPosition().yaw() + entryPos.yaw() : entryPos.yaw();
                var newPitch = (entry.flags() & AxiomClientModifyEntitiesPacket.FLAG_PITCH) != 0 ? entity.getPosition().pitch() + entryPos.pitch() : entryPos.pitch();

                var event = new TerraformMoveEntityEvent(player, entity, new Pos(newX, newY, newZ, newYaw, newPitch));
                EventDispatcher.callCancellable(event, () -> entity.teleport(event.getNewPosition()));
            }

            applyEntityMetadataFromNbt(entity.getEntityMeta(), entry.nbt());
            EventDispatcher.call(new TerraformModifyEntityEvent(entity));

            //todo passengers
        }
    }

    public void handleDeleteEntities(@NotNull Player player, @NotNull AxiomClientDeleteEntitiesPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var instance = player.getInstance();
        for (var uuid : packet.uuids()) {
            var entity = Entity.getEntity(uuid);
            if (entity == null) {
                logger.warn("Axiom tried to delete an unknown entity: {} player={}", uuid, player.getUuid());
                continue;
            }

            if (!instance.equals(entity.getInstance()) || entity instanceof Player ||
                    entity.getPassengers().stream().anyMatch(e -> e instanceof Player)) continue;

            entity.remove();
        }
    }

    private void applyEntityMetadataFromNbt(@NotNull EntityMeta entityMeta, @NotNull NBTCompound nbt) {
        //todo i would prefer to generate this. need to look into including entity metadata in minestom data.
        entityMeta.setNotifyAboutChanges(false);
        for (var entry : nbt.getEntries()) {
            var key = entry.getKey();
            if ("id".equals(key)) continue;

            var value = entry.getValue();
            var applied = switch (entityMeta) {
                case BlockDisplayMeta blockDisplayMeta -> applyBlockDisplayMetaField(blockDisplayMeta, key, value);
                case ItemDisplayMeta itemDisplayMeta -> applyItemDisplayMetaField(itemDisplayMeta, key, value);
                case TextDisplayMeta textDisplayMeta -> applyTextDisplayMetaField(textDisplayMeta, key, value);
                default ->
                        throw new IllegalStateException("unexpected entity type: " + entityMeta.getClass().getName());
            };
            if (!applied) {
                logger.warn("Unhandled entity metadata field: {} -> {}", key, value);
            }
        }
        entityMeta.setNotifyAboutChanges(true);
    }

    private boolean applyEntityMetaField(@NotNull EntityMeta meta, @NotNull String key, @NotNull NBT value) {
        return false; //todo
    }

    private boolean applyDisplayMetaField(@NotNull AbstractDisplayMeta meta, @NotNull String key, @NotNull NBT value) {
        if (applyEntityMetaField(meta, key, value)) return true;

        switch (key) {
            case "start_interpolation" -> meta.setTransformationInterpolationStartDelta(assertInt(value));
            case "interpolation_duration" -> meta.setTransformationInterpolationDuration(assertInt(value));
            case "teleport_duration" -> meta.setPosRotInterpolationDuration(assertInt(value));
            case "transformation" -> {
                var transform = assertCompound(value);
                var translation = assertFloatArray(transform.get("translation"), 3);
                meta.setTranslation(new Vec(translation[0], translation[1], translation[2]));
                var scale = assertFloatArray(transform.get("scale"), 3);
                meta.setScale(new Vec(scale[0], scale[1], scale[2]));
                meta.setLeftRotation(assertFloatArray(transform.get("left_rotation"), 4));
                meta.setRightRotation(assertFloatArray(transform.get("right_rotation"), 4));
            }
            case "billboard" -> meta.setBillboardRenderConstraints(AbstractDisplayMeta.
                    BillboardConstraints.valueOf(assertString(value).toUpperCase(Locale.ROOT)));
            //todo brightness override
            case "view_range" -> meta.setViewRange(assertFloat(value));
            case "shadow_radius" -> meta.setShadowRadius(assertFloat(value));
            case "shadow_strength" -> meta.setShadowStrength(assertFloat(value));
            case "width" -> meta.setWidth(assertFloat(value));
            case "height" -> meta.setHeight(assertFloat(value));
            case "glow_color_override" -> meta.setGlowColorOverride(assertInt(value));
        }

        return false;
    }

    private boolean applyBlockDisplayMetaField(@NotNull BlockDisplayMeta meta, @NotNull String key, @NotNull NBT value) {
        if (applyDisplayMetaField(meta, key, value)) return true;

        if ("block_state".equals(key)) {
            var compound = assertCompound(value);

            var name = assertString(compound.get("Name"));
            var block = Block.fromNamespaceId(name);
            Check.notNull(block, "unknown block: " + name);

            var propertyMap = compound.getCompound("Properties");
            if (propertyMap != null) {
                var props = new HashMap<String, String>();
                propertyMap.forEach((k, v) -> props.put(k, assertString(v)));
                block = block.withProperties(props);
            }

            meta.setBlockState(block.stateId());
        }

        return true;
    }

    private boolean applyItemDisplayMetaField(@NotNull ItemDisplayMeta meta, @NotNull String key, @NotNull NBT value) {
        if (applyDisplayMetaField(meta, key, value)) return true;

        switch (key) {
            case "item" -> meta.setItemStack(ItemStack.fromItemNBT(assertCompound(value)));
            case "item_display" -> meta.setDisplayContext(ItemDisplayMeta.DisplayContext
                    .valueOf(assertString(value).toUpperCase(Locale.ROOT)));
        }

        return true;
    }

    private boolean applyTextDisplayMetaField(@NotNull TextDisplayMeta meta, @NotNull String key, @NotNull NBT value) {
        if (applyDisplayMetaField(meta, key, value)) return true;

        switch (key) {
            case "text" -> meta.setText(GsonComponentSerializer.gson().deserialize(assertString(value)));
            case "line_width" -> meta.setLineWidth(assertInt(value));
            case "background" -> meta.setBackgroundColor(assertInt(value));
            case "text_opacity" -> meta.setTextOpacity(assertByte(value));
            case "shadow" -> meta.setShadow(assertBool(value));
            case "see_through" -> meta.setSeeThrough(assertBool(value));
            case "default_background" -> meta.setUseDefaultBackground(assertBool(value));
            case "alignment" -> {
                var alignment = assertString(value);
                meta.setAlignLeft("left".equals(alignment) || "center".equals(alignment));
                meta.setAlignRight("right".equals(alignment) || "center".equals(alignment));
            }
        }

        return false;
    }

    private int assertInt(@Nullable NBT nbt) {
        if (nbt instanceof NBTInt i) return i.getValue();
        throw new IllegalArgumentException("expected int, got " + nbt.getClass().getName());
    }

    private float assertFloat(@Nullable NBT nbt) {
        if (nbt instanceof NBTFloat f) return f.getValue();
        throw new IllegalArgumentException("expected float, got " + nbt.getClass().getName());
    }

    private String assertString(@Nullable NBT nbt) {
        if (nbt instanceof NBTString s) return s.getValue();
        throw new IllegalArgumentException("expected string, got " + nbt.getClass().getName());
    }

    private byte assertByte(@Nullable NBT nbt) {
        if (nbt instanceof NBTByte b) return b.getValue();
        throw new IllegalArgumentException("expected byte, got " + nbt.getClass().getName());
    }

    private boolean assertBool(@Nullable NBT nbt) {
        if (nbt instanceof NBTByte b) return b.getValue() != 0;
        throw new IllegalArgumentException("expected bool, got " + nbt.getClass().getName());
    }

    private NBTCompound assertCompound(@Nullable NBT nbt) {
        if (nbt instanceof NBTCompound c) return c;
        throw new IllegalArgumentException("expected compound, got " + nbt.getClass().getName());
    }

    private float[] assertFloatArray(@Nullable NBT nbt, int length) {
        if (!(nbt instanceof NBTList<?> l))
            throw new IllegalArgumentException("expected float array, got " + nbt.getClass().getName());
        if (!l.getSubtagType().equals(NBTType.TAG_Float))
            throw new IllegalArgumentException("expected float array, got " + l.getSubtagType());
        if (l.getSize() != length)
            throw new IllegalArgumentException("expected float array of length " + length + ", got " + l.getSize());

        var array = new float[length];
        for (int i = 0; i < length; i++)
            array[i] = ((NBTFloat) l.get(i)).getValue();
        return array;

    }

}
