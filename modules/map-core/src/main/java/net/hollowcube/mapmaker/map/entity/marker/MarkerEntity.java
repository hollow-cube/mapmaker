package net.hollowcube.mapmaker.map.entity.marker;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomRequestMarkerDataEvent;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomUpdateMarkerDataEvent;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomMarkerDataPacket;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
    private static final NamespaceID UNKNOWN_TYPE = NamespaceID.from("mapmaker:unknown");

    // The persistent marker data is stored in `marker_data` where `type`, `name`, `min`, `max` are reserved.
    // They are kept in a separate tag to avoid sending the root to axiom players which may have weird content by accident.
    // When we eventually have an anvil converter, the `marker_data` tag will replace the root tag.
    private static final Tag<CompoundBinaryTag> DATA_TAG = Tag.NBT("marker_data")
            .map(n -> (CompoundBinaryTag) n, n -> n).defaultValue(CompoundBinaryTag.empty());
    private static final Tag<NamespaceID> TYPE_TAG = Tag.String("type").path("marker_data")
            .map(NamespaceID::from, NamespaceID::asString).defaultValue(UNKNOWN_TYPE);
    private static final Tag<@Nullable String> NAME_TAG = Tag.String("name").path("marker_data");
    // Region min and max are stored as relative values.
    private static final Tag<@Nullable Vec> REGION_MIN_TAG = ExtraTags.VecAsList("min").path("marker_data");
    private static final Tag<@Nullable Vec> REGION_MAX_TAG = ExtraTags.VecAsList("max").path("marker_data");

    private static final ListBinaryTag AXIOM_HIDE_LIST = ListBinaryTag.listBinaryTag(BinaryTagTypes.STRING, List.of(
            StringBinaryTag.stringBinaryTag("name"), StringBinaryTag.stringBinaryTag("min"), StringBinaryTag.stringBinaryTag("max"),
            StringBinaryTag.stringBinaryTag("line_argb"), StringBinaryTag.stringBinaryTag("line_thickness"), StringBinaryTag.stringBinaryTag("face_argb")
    ));
    private static final List<String> AXIOM_RESERVED_KEYS = List.of("line_argb", "line_thickness", "face_argb", "axiom:hide", "axiom:modify");

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(TerraformAxiomRequestMarkerDataEvent.class, MarkerEntity::handleAxiomRequestMarkerData)
                .addListener(TerraformAxiomUpdateMarkerDataEvent.class, MarkerEntity::handleAxiomUpdateMarkerData);
    }

    private boolean visible = false;

    public MarkerEntity() {
        this(UUID.randomUUID());
    }

    public MarkerEntity(@NotNull UUID uuid) {
        super(EntityType.MARKER, uuid);
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

    public void setType(@NotNull NamespaceID type) {
        setTag(TYPE_TAG, type);
        updateForViewers();
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        var world = MapWorld.unsafeFromInstance(instance);
        visible = world != null && !world.isReadOnly();
        return super.setInstance(instance, spawnPosition);
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        // Do not call super because we do not need to spawn the entity on the client.

        // see below for why this is commented
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return;

        Axiom.sendPacket(player, createAxiomAddPacket());
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        // Do not call super because we never actually spawned the entity on the client

        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return;

        // Destroy the axiom marker (if it is enabled/installed)
        Axiom.sendPacket(player, new AxiomMarkerDataPacket(getUuid()));
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


        var builder = CompoundBinaryTag.builder();
        builder.put(event.getData().getCompound("data"));
        AXIOM_RESERVED_KEYS.forEach(builder::remove);

        marker.setTag(DATA_TAG, builder.build());
        marker.updateForViewers(); // Send updated region to viewers
    }

    private void updateForViewers() {
        if (isRemoved() || !visible || viewers.isEmpty()) return;

        var axiomAddPacket = createAxiomAddPacket();
        Axiom.sendPacket(getInstance(), axiomAddPacket);
    }

    private @NotNull AxiomMarkerDataPacket createAxiomAddPacket() {
        var regionMin = getTag(REGION_MIN_TAG);
        if (regionMin != null) regionMin = regionMin.add(getPosition());
        var regionMax = getTag(REGION_MAX_TAG);
        if (regionMax != null) regionMax = regionMax.add(getPosition());
        return new AxiomMarkerDataPacket(new AxiomMarkerDataPacket.Entry(
                getUuid(), getPosition(), getDisplayName(),
                regionMin, regionMax,
                0, 0, 0
        ));
    }

    private static @Nullable MarkerEntity assertEditableMarker(@NotNull Player editor, @NotNull UUID entityUuid) {
        var entity = Entity.getEntity(entityUuid);
        if (entity == null) return null;

        // Ensure they are in the same instance, and that the world is editable
        var world = MapWorld.forPlayerOptional(editor);
        if (world == null || !world.instance().equals(entity.getInstance()) || !world.canEdit(editor))
            return null;

        return entity instanceof MarkerEntity marker ? marker : null;
    }

}
