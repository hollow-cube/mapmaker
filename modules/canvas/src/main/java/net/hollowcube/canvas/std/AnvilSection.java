package net.hollowcube.canvas.std;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.network.packet.client.play.ClientNameItemPacket;
import org.jetbrains.annotations.NotNull;

/**
 * A section which allows the player to input text using the anvil GUI.
 * <p>
 * This section should _only_ be used as the top level section within a {@link net.hollowcube.canvas.RootSection}.
 * It will not work correctly in other cases.
 * <p>
 * Anvil sections always have a width of 9, however only slot indices 0-2 are usable (the rest will be an error).
 * If the height is set higher than 1, the GUI will extend into the player inventory and slots 9+ will be usable like normal.
 */
public class AnvilSection extends ParentSection {
    private String input = "";

    protected AnvilSection() {
        this(1);
    }

    /**
     * Creates a new anvil section with the given height.
     */
    protected AnvilSection(int height) {
        super(9, height);
    }

    protected @NotNull String getInput() {
        return this.input;
    }

    protected void onInput(@NotNull String input) {
        this.input = input;
    }

    @Override
    protected void mount() {
        super.mount();

        var eventHandler = EventListener.builder(PlayerPacketEvent.class)
                .handler(this::handleNameItemPacket)
                .expireWhen(unused -> !isMounted())
                .build();
        //todo use eventnode specifically scoped to canvas
        MinecraftServer.getGlobalEventHandler().addListener(eventHandler);
    }

    private void handleNameItemPacket(@NotNull PlayerPacketEvent event) {
        if (!(event.getPacket() instanceof ClientNameItemPacket packet))
            return;
        // Ensure the player in question has this inventory open
        var root = find(RootSection.class);
        if (root == null || root.getInventory() != event.getPlayer().getOpenInventory())
            return;

        onInput(packet.itemName());
    }
}
