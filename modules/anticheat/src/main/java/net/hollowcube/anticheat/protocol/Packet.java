package net.hollowcube.anticheat.protocol;

/// A decoded packet. Every wire packet is a `{C2S|S2C}{Name}` interface carrying the fields the
/// model reads, and every layout that packet has ever had is a `V{pvn}` record nested in it whose
/// `pvn` is the protocol version the layout was **introduced in** — so a record is reused by every
/// later version that did not change it.
///
/// The hierarchy is sealed at this level and at each packet below it, so a packet the model has
/// decoded is always one of the shapes listed here and a `switch` over them can say so. The two
/// keying markers are `non-sealed`, because a packet joins them for how it is *cached*, not for
/// what it is, and either may pick up members without the family list meaning anything different.
///
/// [#encode] must reproduce the exact bytes the record was decoded from: the capture format stores
/// raw frames, and the round trip is what proves a decoder read the whole packet and nothing else.
public sealed interface Packet permits
    C2SConfigurationAcknowledged, C2SFinishConfiguration, C2SPong, ContainerKeyed, CustomPayload,
    EntityKeyed, MovePlayer, S2CBlockUpdate, S2CBundleDelimiter, S2CFinishConfiguration,
    S2CForgetLevelChunk, S2CLevelChunkWithLight, S2CLogin, S2CPing, S2CPlayerPosition,
    S2CPlayerRotation, S2CRegistryData, S2CRemoveEntities, S2CRespawn, S2CSectionBlocksUpdate,
    S2CSetChunkCacheCenter, S2CSetChunkCacheRadius, S2CSetPlayerInventory, S2CStartConfiguration,
    S2CUpdateTags {

    void encode(ByteWriter writer);

    default byte[] toByteArray() {
        var writer = new ByteWriter();
        encode(writer);
        return writer.toByteArray();
    }
}
