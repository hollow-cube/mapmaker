package net.hollowcube.mapmaker.map.entity.object;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.AxiomPlayer;
import net.hollowcube.compat.axiom.events.AxiomMarkerDataRequestEvent;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerDataPacket;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomUpdateCustomEntityDataEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.instance.Instance;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.tag.Tag;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * An object entity is an entity of which axiom can modify, or another feature can modify in a similar way.
 * These act as markers in the axiom client but can be any entity in the server.
 */
public abstract class ObjectEntity extends MapEntity implements TerraformAxiomUpdateCustomEntityDataEvent.Receiver {

    private static final BoundingBox NO_BB = new BoundingBox(0, 0, 0);
    private static final Key UNKNOWN_TYPE = Key.key("mapmaker:unknown");

    protected static final Tag<CompoundBinaryTag> DATA_TAG = Tag.NBT("data")
            .map(n -> (CompoundBinaryTag) n, n -> n)
            .defaultValue(CompoundBinaryTag.empty());
    protected static final Tag<Key> TYPE_TAG = Tag.String("type").path("data")
            .map(Key::key, Key::asString)
            .defaultValue(UNKNOWN_TYPE);
    protected static final Tag<@Nullable String> NAME_TAG = Tag.String("name").path("data");
    protected static final Tag<@Nullable Vec> REGION_MIN_TAG = VecAsList("min").path("data");
    protected static final Tag<@Nullable Vec> REGION_MAX_TAG = VecAsList("max").path("data");

    static {
        var events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AxiomMarkerDataRequestEvent.RightClick.class, ObjectEntity::handleAxiomRequestMarkerData);
        events.addListener(TerraformAxiomUpdateCustomEntityDataEvent.class, ObjectEntity::handleAxiomUpdateMarkerData);
    }

    protected @Nullable ObjectEntityHandler handler;
    protected boolean visible = false;
    protected boolean sendToClient = false;

    protected ObjectEntity(@NotNull EntityType entityType) {
        this(entityType, UUID.randomUUID());
    }

    protected ObjectEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);

        hasPhysics = false;
        setNoGravity(true);
        collidesWithEntities = false;
        preventBlockPlacement = false;
    }

    public @NotNull String getDisplayName() {
        return Objects.requireNonNullElseGet(getTag(NAME_TAG), () -> getTag(TYPE_TAG).asString());
    }

    public @NotNull String getType() {
        return getTag(TYPE_TAG).asString();
    }

    public @NotNull CompoundBinaryTag getData() {
        return getTag(DATA_TAG);
    }

    public <T> @NotNull Result<T> getData(@NotNull Codec<T> codec) {
        var coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
        return codec.decode(coder, getData());
    }

    public @Nullable Point getMin() {
        return getTag(REGION_MIN_TAG);
    }

    public @Nullable Point getMax() {
        return getTag(REGION_MAX_TAG);
    }

    public void setRegion(@Nullable Vec min, @Nullable Vec max) {
        setTag(REGION_MIN_TAG, min);
        setTag(REGION_MAX_TAG, max);
        updateBoundingBox();
    }

    public void setType(@NotNull Key type) {
        setTag(TYPE_TAG, type);
        updateForViewers();
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        var world = MapWorld.forInstance(instance); // todo this is more complex, also update visible
//        visible = world != null && !world.isReadOnly(); TODO: visible shouldnt exist, should just use manual viewers.
        if (handler != null) {
            handler.onRemove();
            handler = null;
        }
        return super.setInstance(instance, spawnPosition).thenRun(() -> {
            if (world != null) {
                handler = world.objectEntityHandlers().create(getType(), this);
            }
        });
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        // Do not read default entity tags

        setTag(DATA_TAG, tag.getCompound("data"));
        updateBoundingBox();
        updateForViewers();
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        // Do not write default entity tags

        tag.put("data", getTag(DATA_TAG));
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        if (sendToClient) super.updateNewViewer(player);

        var world = MapWorld.forPlayer(player);
        if (world == null) return;

        if (handler != null) handler.addViewer(world, player);
        if (world.canEdit(player) && AxiomPlayer.isEnabled(player)) {
            createAxiomMarkerUpdatePacket().send(player);
        }
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        if (sendToClient) super.updateOldViewer(player);

        var world = MapWorld.forPlayer(player);
        if (world == null) return;

        if (handler != null) handler.removeViewer(world, player);
        if (world.canEdit(player)) {
            // Destroy the axiom marker (if it is enabled/installed)
            createAxiomMarkerRemovePacket().send(player);
        }
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (handler != null) handler.onTick();
    }

    @Override
    protected void remove(boolean permanent) {
        if (handler != null) handler.onRemove();
        super.remove(permanent);
    }

    @Override
    public @NotNull CompletableFuture<Void> teleport(
            @NotNull Pos position,
            long @Nullable [] chunks,
            @MagicConstant(flagsFromClass = RelativeFlags.class) int flags
    ) {
        return super.teleport(position, chunks, flags)
                .thenRun(() -> createAxiomMarkerUpdatePacket().sendToViewers(this));
    }

    @Override
    protected void movementTick() {
        // Intentionally do nothing
    }

    private static void handleAxiomRequestMarkerData(@NotNull AxiomMarkerDataRequestEvent.RightClick event) {
        var world = MapWorld.forPlayer(event.player());
        if (!(event.marker() instanceof ObjectEntity entity)) return;
        if (world == null || !world.canEdit(event.player())) return;
        if (!world.instance().equals(entity.getInstance())) return;

        event.setData(entity.getTag(DATA_TAG)
                .put("axiom:hide", AxiomAPI.HIDDEN_MARKER_DATA)
                .putString("type", entity.getType()));
    }

    private static void handleAxiomUpdateMarkerData(@NotNull TerraformAxiomUpdateCustomEntityDataEvent event) {
        var marker = assertEditableObject(event.editor(), event.entityUuid());
        if (marker == null) return;

        var newData = event.data().getCompound("data");
        var updating = !newData.getBoolean("axiom:modify", false);
        var minChanged = newData.get("min") != null;
        var maxChanged = newData.get("max") != null;

        var builder = CompoundBinaryTag.builder();
        if (updating) builder.put(marker.getTag(DATA_TAG));
        builder.put(newData);

        AxiomAPI.RESERVED_MARKER_DATA.forEach(builder::remove);

        var oldType = marker.getType();
        marker.setTag(DATA_TAG, builder.build());

        // TODO cant use update tag here because of a class cast exception, likely a minestom issue - Gravy
        if (updating) {
            if (minChanged) marker.setTag(REGION_MIN_TAG, marker.getTag(REGION_MIN_TAG).sub(marker.getPosition()));
            if (maxChanged) marker.setTag(REGION_MAX_TAG, marker.getTag(REGION_MAX_TAG).sub(marker.getPosition()));
        }

        marker.updateForViewers(); // Send updated region to viewers
        marker.updateBoundingBox();

        var newType = marker.getType();
        if (!Objects.equals(oldType, newType)) {
            if (marker.handler != null) marker.handler.onRemove();

            var world = MapWorld.forInstance(marker.getInstance());
            if (world != null) {
                marker.handler = world.objectEntityHandlers().create(newType, marker);
            } else marker.handler = null;
        }
        if (marker.handler != null) marker.handler.onDataChange(event.editor());
    }

    private void updateForViewers() {
        if (isRemoved() || !visible) return;
        createAxiomMarkerUpdatePacket().sendToViewers(this);
    }

    protected void updateBoundingBox() {
        Point min = getMin(), max = getMax();
        if (min == null || max == null) {
            setBoundingBox(NO_BB);
            return;
        }

        var minX = Math.min(min.x(), max.x());
        var minY = Math.min(min.y(), max.y());
        var minZ = Math.min(min.z(), max.z());
        var maxX = Math.max(min.x(), max.x());
        var maxY = Math.max(min.y(), max.y());
        var maxZ = Math.max(min.z(), max.z());

        setBoundingBox(new BoundingBox(
                maxX - minX, maxY - minY, maxZ - minZ,
                new Vec(minX, minY, minZ)
        ));
    }

    private @NotNull AxiomClientboundMarkerDataPacket createAxiomMarkerUpdatePacket() {
        var pos = this.getPosition();
        return AxiomClientboundMarkerDataPacket.updateMarker(
                this.getUuid(), pos, this.getDisplayName(),
                OpUtils.map(this.getMin(), pos::add), OpUtils.map(this.getMax(), pos::add),
                new AlphaColor(0), new AlphaColor(0), 0
        );
    }

    private @NotNull AxiomClientboundMarkerDataPacket createAxiomMarkerRemovePacket() {
        return AxiomClientboundMarkerDataPacket.removeMarker(this.getUuid());
    }

    private static @Nullable ObjectEntity assertEditableObject(@NotNull Player editor, @NotNull UUID entityUuid) {
        var entity = editor.getInstance().getEntityByUuid(entityUuid);
        if (entity == null) return null;

        // Ensure they are in the same instance, and that the world is editable
        var world = MapWorld.forPlayer(editor);
        if (world == null || !world.instance().equals(entity.getInstance()) || !world.canEdit(editor))
            return null;

        return entity instanceof ObjectEntity object ? object : null;
    }

    // Axiom allows yo uto use '~' to represent a position relative to the position, but all our vecs are relative.
    private static double asAxiomDouble(@NotNull BinaryTag tag) {
        if (tag instanceof NumberBinaryTag number) {
            return number.doubleValue();
        } else if (tag instanceof StringBinaryTag string) {
            try {
                return string.value().startsWith("~") ? Double.parseDouble(string.value().substring(1))
                        : Double.parseDouble(string.value());
            } catch (Exception _) {
            }
        }
        return 0.0;
    }

    private static @NotNull Tag<Vec> VecAsList(@NotNull String key) {
        return Tag.NBT(key).list().map(
                entries -> {
                    double x = 0.0, y = 0.0, z = 0.0;
                    if (entries.size() >= 1) x = asAxiomDouble(entries.get(0));
                    if (entries.size() >= 2) y = asAxiomDouble(entries.get(1));
                    if (entries.size() >= 3) z = asAxiomDouble(entries.get(2));
                    return new Vec(x, y, z);
                },
                vec -> List.of(
                        DoubleBinaryTag.doubleBinaryTag(vec.x()),
                        DoubleBinaryTag.doubleBinaryTag(vec.y()),
                        DoubleBinaryTag.doubleBinaryTag(vec.z())
                )
        );
    }


}
