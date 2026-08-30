package net.hollowcube.anticheat.protocol;

/// `play respawn`. A dimension change rebuilds the client's level, so the world model clears with
/// it; the keep flags decide whether entity data and attributes survive.
public sealed interface S2CRespawn extends Packet permits S2CRespawn.V776 {

    byte KEEP_ATTRIBUTE_MODIFIERS = 1;
    byte KEEP_ENTITY_DATA = 2;

    CommonPlayerSpawnInfo spawnInfo();

    byte dataToKeep();

    record V776(CommonPlayerSpawnInfo spawnInfo, byte dataToKeep) implements S2CRespawn {

        public static V776 decode(ByteReader reader) {
            return new V776(CommonPlayerSpawnInfo.decode(reader), reader.i8());
        }

        @Override
        public void encode(ByteWriter writer) {
            spawnInfo.encode(writer);
            writer.u8(dataToKeep);
        }
    }
}
