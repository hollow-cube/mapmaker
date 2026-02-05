package net.hollowcube.compat.noxesium.handshake;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.noxesium.components.NoxesiumComponentMap;
import net.hollowcube.compat.noxesium.components.NoxesiumComponentRegistry;
import net.hollowcube.compat.noxesium.components.NoxesiumComponentType;
import net.hollowcube.compat.noxesium.components.NoxesiumGameComponents;
import net.hollowcube.compat.noxesium.packets.v3.ClientboundHandshakeCompletePacket;
import net.hollowcube.compat.noxesium.packets.v3.ClientboundUpdateGameComponentsPacket;
import net.hollowcube.compat.noxesium.packets.v3.ServerboundHandshakeAcknowledgePacket;
import net.hollowcube.compat.noxesium.packets.v3.ServerboundRegistryUpdateResultPacket;
import net.hollowcube.posthog.PostHog;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class NoxesiumPlayer implements NoxesiumComponentType.Holder {

    private static final Tag<NoxesiumPlayer> TAG = Tag.Transient("noxesium:player");

    private final Player player;

    private final IntSet missingRegistries = new IntArraySet();
    private final IntSet pendingRegistries = new IntArraySet();
    private final NoxesiumComponentMap components = new NoxesiumComponentMap();

    private boolean syncingComponents = false;
    private boolean initialized = false;
    private boolean loggedMods = false;

    public NoxesiumPlayer(Player player) {
        this.player = player;
    }

    public void handle(ServerboundHandshakeAcknowledgePacket packet) {
        this.syncRegistries(NoxesiumGameComponents.REGISTRY);

        if (!this.loggedMods && Boolean.TRUE.equals(this.player.getTag(CompatProvider.FIRST_JOIN_TAG))) {
            this.loggedMods = true;

            PostHog.capture(this.player.getUuid().toString(), "session_mods", Map.ofEntries(
                Map.entry("mods", packet.mods().stream().map(ServerboundHandshakeAcknowledgePacket.Mod::id).toList())
            ));
        }
    }

    public void handle(ServerboundRegistryUpdateResultPacket packet) {
        this.missingRegistries.addAll(IntArrayList.wrap(packet.missing()));
        this.pendingRegistries.remove(packet.registry());

        if (this.pendingRegistries.isEmpty()) {
            this.initialized = true;
            new ClientboundHandshakeCompletePacket().send(this.player);
            new ClientboundUpdateGameComponentsPacket(true, this.components.copy()).send(this.player);
        }
    }

    @Override
    public <T> @Nullable T get(NoxesiumComponentType<T> type) {
        return this.components.get(type);
    }

    @Override
    public <T> void set(NoxesiumComponentType<T> type, @Nullable T value) {
        value = this.missingRegistries.contains(type.networkId()) ? null : value;
        this.components.set(type, value);

        this.syncComponents();
    }

    @Override
    public void clear() {
        this.components.clear();

        this.syncComponents();
    }

    private void syncComponents() {
        if (this.syncingComponents || !this.initialized) return;
        this.syncingComponents = true;

        this.player.scheduleNextTick(_ -> {
            this.syncingComponents = false;
            new ClientboundUpdateGameComponentsPacket(true, this.components.copy()).send(this.player);
        });
    }

    private void syncRegistries(NoxesiumComponentRegistry... registries) {
        for (var registry : registries) {
            var packet = registry.toPacket();
            this.pendingRegistries.add(packet.id());
            packet.send(this.player);
        }
    }

    public static NoxesiumPlayer get(Player player) {
        return player.updateAndGetTag(TAG, it -> it != null ? it : new NoxesiumPlayer(player));
    }
}
