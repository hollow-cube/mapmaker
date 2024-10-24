package net.hollowcube.terraform.compat.axiom.packet.client;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientModifyEntitiesPacket(
        @NotNull List<Entry> entries
) implements AxiomClientPacket {
    private static final int MAX_ENTRIES = 512;

    public static final byte FLAG_X = 1 << 0;
    public static final byte FLAG_Y = 1 << 1;
    public static final byte FLAG_Z = 1 << 2;
    public static final byte FLAG_YAW = 1 << 3;
    public static final byte FLAG_PITCH = 1 << 4;

    public AxiomClientModifyEntitiesPacket {
        entries = List.copyOf(entries);
    }

    // TODO: 1.21.2
//    public AxiomClientModifyEntitiesPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
//        this(buffer.readCollection(b1 -> new Entry(b1, apiVersion), MAX_ENTRIES));
//    }

    public record Entry(
            @NotNull UUID uuid,
            byte flags,
            @Nullable Pos pos,
            @NotNull CompoundBinaryTag nbt,
            @NotNull PassengerChange passengerChange,
            @UnknownNullability List<@NotNull UUID> passengers
    ) {

//        public Entry(@NotNull NetworkBuffer buffer, int apiVersion) {
//            this(read(buffer, apiVersion));
//        }

        public Entry(@NotNull Entry other) {
            this(other.uuid(), other.flags(), other.pos(), other.nbt(), other.passengerChange(), other.passengers());
        }

//        private static @NotNull Entry read(@NotNull NetworkBuffer buffer, int apiVersion) {
//            var uuid = buffer.read(NetworkBuffer.UUID);
//            byte flags = buffer.read(NetworkBuffer.BYTE);
//            var pos = flags > 0 ? ProtocolUtil.readPos(buffer) : null;
//            var nbt = buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag nbtCompound ? nbtCompound : CompoundBinaryTag.empty();
//            var passengerChange = buffer.readEnum(PassengerChange.class);
//            var passengers = passengerChange.hasEntries() ? buffer.readCollection(NetworkBuffer.UUID, MAX_ENTRIES) : null;
//            return new Entry(uuid, flags, pos, nbt, passengerChange, passengers);
//        }
    }

    public enum PassengerChange {
        NONE,
        REMOVE_ALL,
        ADD_LIST,
        REMOVE_LIST;

        public boolean hasEntries() {
            return this == ADD_LIST || this == REMOVE_LIST;
        }
    }
}
