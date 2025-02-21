package net.hollowcube.mapmaker.map.entity.marker;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.event.entity.MarkerEntityEnteredEvent;
import net.hollowcube.mapmaker.map.event.entity.MarkerEntityExitedEvent;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomRequestMarkerDataEvent;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomUpdateMarkerDataEvent;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomMarkerDataPacket;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a viewable marker entity using axiom or particles to display (depending on the client).
 *
 * <p>Markers are used to add data to a map, such as spawn points, control points, entity spawn locations, etc.
 * Each marker has a namespace ID to describe its type, as well as some associated data dependent on the domain
 * specific use case. Markers can also represent a region with an aabb and have an alias to display in world.</p>
 *
 * <p>todo add a mechanism to handle this on map load (eg in the hub), and in the future with scripting</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class MarkerEntity extends MapEntity {
    private static final BoundingBox NO_BB = new BoundingBox(0, 0, 0);
    private static final NamespaceID UNKNOWN_TYPE = NamespaceID.from("mapmaker:unknown");

    // The persistent marker data is stored in `data` where `type`, `name`, `min`, `max` are reserved.
    // They are kept in a separate tag to avoid sending the root to axiom players which may have weird content by accident.
    // When we eventually have an anvil converter, the `data` tag will replace the root tag.
    private static final Tag<CompoundBinaryTag> DATA_TAG = Tag.NBT("data")
            .map(n -> (CompoundBinaryTag) n, n -> n).defaultValue(CompoundBinaryTag.empty());
    private static final Tag<NamespaceID> TYPE_TAG = Tag.String("type").path("data")
            .map(NamespaceID::from, NamespaceID::asString).defaultValue(UNKNOWN_TYPE);
    private static final Tag<@Nullable String> NAME_TAG = Tag.String("name").path("data");
    // Region min and max are stored as relative values.
    private static final Tag<@Nullable Vec> REGION_MIN_TAG = ExtraTags.VecAsList("min").path("data");
    private static final Tag<@Nullable Vec> REGION_MAX_TAG = ExtraTags.VecAsList("max").path("data");

    private static final ListBinaryTag AXIOM_HIDE_LIST = ListBinaryTag.listBinaryTag(BinaryTagTypes.STRING, List.of(
            StringBinaryTag.stringBinaryTag("name"), StringBinaryTag.stringBinaryTag("min"), StringBinaryTag.stringBinaryTag("max"),
            StringBinaryTag.stringBinaryTag("line_argb"), StringBinaryTag.stringBinaryTag("line_thickness"), StringBinaryTag.stringBinaryTag("face_argb")
    ));
    private static final List<String> AXIOM_RESERVED_KEYS = List.of("line_argb", "line_thickness", "face_argb", "axiom:hide", "axiom:modify");

    static {
        // Minestom event priorities are kinda dumb. They eval in this order (per node):
        // - direct listeners first, non deterministic order
        // - mapped nodes second, non deterministic order
        // - children third, ordered by priority
        // We need these to run first, so we directly register them on the root....
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(TerraformAxiomRequestMarkerDataEvent.class, MarkerEntity::handleAxiomRequestMarkerData);
        globalEventHandler.addListener(TerraformAxiomUpdateMarkerDataEvent.class, MarkerEntity::handleAxiomUpdateMarkerData);
    }

    private MarkerHandler handler;
    private boolean visible = false;

    private final Set<Player> insidePlayers = new HashSet<>();

    public MarkerEntity() {
        this(UUID.randomUUID());
    }

    public MarkerEntity(@NotNull UUID uuid) {
        super(EntityType.MARKER, uuid);

        hasPhysics = false;
        setNoGravity(true);
        collidesWithEntities = false;
    }

    public @NotNull String getDisplayName() {
        return Objects.requireNonNullElseGet(getTag(NAME_TAG), () -> getTag(TYPE_TAG).asString());
    }

    public @NotNull String getType() {
        return getTag(TYPE_TAG).asString();
    }

    public @NotNull CompoundBinaryTag getMarkerData() {
        return getTag(DATA_TAG);
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

    public void setType(@NotNull NamespaceID type) {
        setTag(TYPE_TAG, type);
        updateForViewers();
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        var world = MapWorld.unsafeFromInstance(instance);
        visible = world != null && !world.isReadOnly();
        if (handler != null) {
            handler.onRemove();
            handler = null;
        }
        return super.setInstance(instance, spawnPosition).thenRun(() -> {
            if (world == null) return;
            handler = world.markerRegistry().create(getType(), this);
        });
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        setTag(DATA_TAG, tag.getCompound("data"));
        updateBoundingBox();
        updateForViewers();
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        tag.put("data", getTag(DATA_TAG));
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        // Do not call super because we do not need to spawn the entity on the client.

        // see below for why this is commented
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;

        if (handler != null) handler.addViewer(player);
        if (world.canEdit(player)) {
            Axiom.sendPacket(player, createAxiomAddPacket());
        }
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        // Do not call super because we never actually spawned the entity on the client

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;

        if (handler != null) handler.removeViewer(player);
        if (world.canEdit(player)) {
            // Destroy the axiom marker (if it is enabled/installed)
            Axiom.sendPacket(player, new AxiomMarkerDataPacket(getUuid()));
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
    public @NotNull CompletableFuture<Void> teleport(@NotNull Pos position, long @Nullable [] chunks, int flags) {
        return super.teleport(position, chunks, flags).thenRun(() -> {

            // Update position for axiom viewers
            Axiom.sendPacket(getViewers(), createAxiomAddPacket());
        });
    }

    private static void handleAxiomRequestMarkerData(@NotNull TerraformAxiomRequestMarkerDataEvent event) {
        var marker = assertEditableMarker(event.getEditor(), event.getEntityUuid());
        if (marker == null) return;

        event.setData(marker.getTag(DATA_TAG)
                .put("axiom:hide", AXIOM_HIDE_LIST)
                .putString("type", marker.getType()));
    }

    private static void handleAxiomUpdateMarkerData(@NotNull TerraformAxiomUpdateMarkerDataEvent event) {
        var marker = assertEditableMarker(event.getEditor(), event.getEntityUuid());
        if (marker == null) return;

        var newData = event.getData().getCompound("data");
        var updating = !newData.getBoolean("axiom:modify", false);
        var minChanged = newData.get("min") != null;
        var maxChanged = newData.get("max") != null;

        var builder = CompoundBinaryTag.builder();
        if (updating) builder.put(marker.getTag(DATA_TAG));
        builder.put(newData);

        AXIOM_RESERVED_KEYS.forEach(builder::remove);

        var oldType = marker.getType();
        marker.setTag(DATA_TAG, builder.build());

        // TODO cant use update tag here because of a class cast exception, likely a minestom issue - Gravy
        if (updating) {
            if (minChanged) {
                var min = marker.getTag(REGION_MIN_TAG);
                marker.setTag(REGION_MIN_TAG, min.sub(marker.getPosition()));
            }
            if (maxChanged) {
                var max = marker.getTag(REGION_MAX_TAG);
                marker.setTag(REGION_MAX_TAG, max.sub(marker.getPosition()));
            }
        }

        marker.updateForViewers(); // Send updated region to viewers
        marker.updateBoundingBox();

        var newType = marker.getType();
        if (!Objects.equals(oldType, newType)) {
            if (marker.handler != null) marker.handler.onRemove();

            var world = MapWorld.unsafeFromInstance(marker.getInstance());
            if (world != null) {
                marker.handler = world.markerRegistry().create(newType, marker);
            } else marker.handler = null;
        }
        if (marker.handler != null) marker.handler.onDataChange(event.getEditor());
    }

    private void updateForViewers() {
        if (isRemoved() || !visible) return;

        var axiomAddPacket = createAxiomAddPacket();
        Axiom.sendPacket(getInstance(), axiomAddPacket);
    }

    private void updateBoundingBox() {
        Point min = getMin(), max = getMax();
        if (min == null || max == null) {
            setBoundingBox(NO_BB);
            return;
        }

        var size = max.sub(min);
        setBoundingBox(new BoundingBox(size.x(), size.y(), size.z(), min));
    }

    private @NotNull AxiomMarkerDataPacket createAxiomAddPacket() {
        var regionMin = getTag(REGION_MIN_TAG);
        if (regionMin != null) regionMin = regionMin.add(getPosition());
        var regionMax = getTag(REGION_MAX_TAG);
        if (regionMax != null) regionMax = regionMax.add(getPosition());
        return new AxiomMarkerDataPacket(new AxiomMarkerDataPacket.Entry(
                getUuid(), getPosition(), getDisplayName(),
                regionMin, regionMax, 0, 0, 0
        ));
    }

    private static @Nullable MarkerEntity assertEditableMarker(@NotNull Player editor, @NotNull UUID entityUuid) {
        var entity = editor.getInstance().getEntityByUuid(entityUuid);
        if (entity == null) return null;

        // Ensure they are in the same instance, and that the world is editable
        var world = MapWorld.forPlayerOptional(editor);
        if (world == null || !world.instance().equals(entity.getInstance()) || !world.canEdit(editor))
            return null;

        return entity instanceof MarkerEntity marker ? marker : null;
    }

    private void collisionTick() {
        var world = MapWorld.unsafeFromInstance(getInstance());
        if (world == null || world.playWorld() == null) return;
        world = world.playWorld();
        if (world == null || getInstance() != world.instance()) return;

        for (var player : world.players()) {
            if (insidePlayers.contains(player) || !CoordinateUtil.intersects(this, player)) continue;

            insidePlayers.add(player); // Just entered
            world.callEvent(new MarkerEntityEnteredEvent(world, player, this));
        }
        for (var player : insidePlayers) {
            if (world.isPlaying(player) && CoordinateUtil.intersects(this, player)) continue;

            insidePlayers.remove(player); // Just exited
            world.callEvent(new MarkerEntityExitedEvent(world, player, this));
        }
    }

    @Override
    public void update(long time) {
        super.update(time);

        if (getBoundingBox() != NO_BB)
            collisionTick();
    }

    @Override
    protected void movementTick() {
        // Intentionally do nothing
    }
}
