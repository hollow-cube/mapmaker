package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record AxiomServerboundModifyEntitiesPacket(
        @NotNull List<Entry> entries
) implements ServerboundModPacket<AxiomServerboundModifyEntitiesPacket> {

    public static final Type<AxiomServerboundModifyEntitiesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "manipulate_entity",
            NetworkBufferTemplate.template(
                    Entry.CODEC.list(), AxiomServerboundModifyEntitiesPacket::entries,
                    AxiomServerboundModifyEntitiesPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundModifyEntitiesPacket> getType() {
        return TYPE;
    }


    public record Entry(
            @NotNull UUID id,
            @MagicConstant(flagsFromClass = RelativeFlags.class) byte flags,
            @Nullable Pos pos,
            @NotNull CompoundBinaryTag nbt,
            @NotNull PassengerChange passengerChange,
            @NotNull List<UUID> passengers
    ) {

        private static final NetworkBuffer.Type<PassengerChange> PASSENGER_CHANGE = NetworkBuffer.Enum(PassengerChange.class);
        private static final NetworkBuffer.Type<List<UUID>> PASSENGERS = NetworkBuffer.UUID.list(512);
        private static final NetworkBuffer.Type<Entry> CODEC = new NetworkBuffer.Type<>() {
            @Override
            public void write(@NotNull NetworkBuffer buffer, Entry value) {
                buffer.write(NetworkBuffer.UUID, value.id());
                if (value.pos != null && value.flags != -1) {
                    buffer.write(NetworkBuffer.BYTE, value.flags());
                    buffer.write(NetworkBuffer.POS, value.pos());
                } else {
                    buffer.write(NetworkBuffer.BYTE, (byte) -1);
                }

                buffer.write(NetworkBuffer.NBT_COMPOUND, value.nbt());
                buffer.write(PASSENGER_CHANGE, value.passengerChange());
                if (value.passengerChange().hasEntries()) {
                    buffer.write(PASSENGERS, value.passengers());
                }
            }

            @Override
            public Entry read(@NotNull NetworkBuffer buffer) {
                UUID id = buffer.read(NetworkBuffer.UUID);
                byte flags = buffer.read(NetworkBuffer.BYTE);
                Pos pos = flags != -1 ? buffer.read(NetworkBuffer.POS) : null;
                CompoundBinaryTag nbt = buffer.read(NetworkBuffer.NBT_COMPOUND);
                PassengerChange passengerChange = buffer.read(PASSENGER_CHANGE);
                List<UUID> passengers = passengerChange.hasEntries() ? buffer.read(PASSENGERS) : List.of();
                return new Entry(id, flags, pos, nbt, passengerChange, passengers);
            }
        };

        public boolean hasFlag(@MagicConstant(flagsFromClass = RelativeFlags.class) int flag) {
            return (this.flags & (byte) flag) == (byte) flag;
        }
    }

    public enum PassengerChange {
        NOTHING, CLEAR, ADD, REMOVE;

        public boolean hasEntries() {
            return this == ADD || this == REMOVE;
        }
    }
}
