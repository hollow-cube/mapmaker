package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.UUID;

public record AxiomClientModifyEntitiesPacket(
        @NotNull List<Entry> entries
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientModifyEntitiesPacket> SERIALIZER = NetworkBufferTemplate.template(
            Entry.SERIALIZER.list(512), AxiomClientModifyEntitiesPacket::entries,
            AxiomClientModifyEntitiesPacket::new);

    public static final byte FLAG_X = 1;
    public static final byte FLAG_Y = 1 << 1;
    public static final byte FLAG_Z = 1 << 2;
    public static final byte FLAG_YAW = 1 << 3;
    public static final byte FLAG_PITCH = 1 << 4;

    public AxiomClientModifyEntitiesPacket {
        entries = List.copyOf(entries);
    }

    public record Entry(
            @NotNull UUID uuid,
            byte flags,
            @Nullable Pos pos,
            @NotNull CompoundBinaryTag nbt,
            @NotNull PassengerChange passengerChange,
            @UnknownNullability List<@NotNull UUID> passengers
    ) {
        public static final NetworkBuffer.Type<Entry> SERIALIZER = new NetworkBuffer.Type<>() {
            private static final NetworkBuffer.Type<List<UUID>> UUID_LIST = NetworkBuffer.UUID.list(512);

            @Override
            public void write(@NotNull NetworkBuffer buffer, Entry value) {
                buffer.write(NetworkBuffer.UUID, value.uuid);
                buffer.write(NetworkBuffer.BYTE, value.flags);
                if (value.pos != null)
                    buffer.write(NetworkBuffer.POS, value.pos);
                buffer.write(NetworkBuffer.NBT, value.nbt);
                buffer.write(PassengerChange.NETWORK_TYPE, value.passengerChange);
                if (value.passengerChange.hasEntries())
                    buffer.write(UUID_LIST, value.passengers);
            }

            @Override
            public Entry read(@NotNull NetworkBuffer buffer) {
                var uuid = buffer.read(NetworkBuffer.UUID);
                byte flags = buffer.read(NetworkBuffer.BYTE);
                var pos = flags >= 0 ? buffer.read(NetworkBuffer.POS) : null;
                var nbt = buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag nbtCompound ? nbtCompound : CompoundBinaryTag.empty();
                var passengerChange = buffer.read(PassengerChange.NETWORK_TYPE);
                var passengers = passengerChange.hasEntries() ? buffer.read(UUID_LIST) : null;
                return new Entry(uuid, flags, pos, nbt, passengerChange, passengers);
            }
        };

        public Entry(@NotNull Entry other) {
            this(other.uuid(), other.flags(), other.pos(), other.nbt(), other.passengerChange(), other.passengers());
        }
    }

    public enum PassengerChange {
        NONE,
        REMOVE_ALL,
        ADD_LIST,
        REMOVE_LIST;

        public static final NetworkBuffer.Type<PassengerChange> NETWORK_TYPE = NetworkBuffer.Enum(PassengerChange.class);

        public boolean hasEntries() {
            return this == ADD_LIST || this == REMOVE_LIST;
        }
    }
}
