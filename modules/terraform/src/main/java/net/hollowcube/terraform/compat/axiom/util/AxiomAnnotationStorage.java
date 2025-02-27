package net.hollowcube.terraform.compat.axiom.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.compat.axiom.data.annotations.actions.*;
import net.hollowcube.compat.axiom.data.annotations.data.AnnotationData;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundAnnotationUpdatePacket;
import net.hollowcube.terraform.storage.TerraformInstanceStorage;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public class AxiomAnnotationStorage {

    private static final Tag<AxiomAnnotationStorage> TAG = DFU.Tag(
            Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Entry.CODEC)
                    .xmap(AxiomAnnotationStorage::new, it -> it.data),
            "axiom:annotation_storage"
    );

    private final Map<UUID, Entry> data = new ConcurrentHashMap<>();

    private AxiomAnnotationStorage(Map<UUID, Entry> data) {
        this.data.putAll(data);
    }

    public @Nullable AnnotationAction apply(@NotNull Player player, @NotNull AnnotationAction action) {
        var applied = switch (action) {
            case CreateAnnotationAction create -> {
                var it = data.putIfAbsent(create.id(), new Entry(player.getUuid(), create.data()));
                yield it == null;
            }
            case DeleteAnnotationAction delete -> {
                var it = data.remove(delete.id());
                yield it != null;
            }
            case MoveAnnotationAction move -> {
                var it = data.computeIfPresent(move.id(),
                        (id, entry) -> new Entry(entry.creator(), entry.data.withPosition(move.x(), move.y(), move.z()))
                );
                yield it != null;
            }
            case ClearAnnotationAction ignored -> {
                data.clear();
                yield true;
            }
            case RotateAnnotationAction rotate -> {
                var it = data.computeIfPresent(rotate.id(),
                        (id, entry) -> new Entry(entry.creator(), entry.data.withRotation(rotate.x(), rotate.y(), rotate.z(), rotate.w()))
                );
                yield it != null;
            }
            default -> false;
        };
        return applied ? action : null;
    }

    public void sendAllTo(@NotNull Player player) {
        List<AnnotationAction> actions = new ArrayList<>();
        actions.add(new ClearAnnotationAction());
        data.forEach((id, entry) -> actions.add(new CreateAnnotationAction(id, entry.data())));
        new AxiomClientboundAnnotationUpdatePacket(actions).send(player);
    }

    public static @Nullable AxiomAnnotationStorage get(@NotNull Player player) {
        var terraformStorage = TerraformInstanceStorage.get(player.getInstance());
        return terraformStorage == null ? null : terraformStorage.getAndUpdateTag(TAG, UnaryOperator.identity());
    }

    private record Entry(
            @NotNull UUID creator,
            @NotNull AnnotationData data
    ) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("creator").forGetter(Entry::creator),
                AnnotationData.CODEC.fieldOf("data").forGetter(Entry::data)
        ).apply(instance, Entry::new));
    }
}
