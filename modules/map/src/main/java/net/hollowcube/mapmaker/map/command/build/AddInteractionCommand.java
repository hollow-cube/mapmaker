package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerDataPacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerResponsePacket;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.network.packet.server.play.BundlePacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class AddInteractionCommand extends CommandDsl {

    public AddInteractionCommand() {
        super("addinteraction");

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::addInteractionEntity));
    }

    private void addInteractionEntity(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);

        var entity = new Interaction(1, 1);
        entity.setInstance(world.instance(), player.getPosition().withView(Pos.ZERO));
        player.sendMessage(Component.text("Interaction added.")
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                .clickEvent(ClickEvent.copyToClipboard(entity.getUuid().toString())));
    }

    private static class Interaction extends MapEntity {

        protected Interaction(float width, float height) {
            super(EntityType.INTERACTION);

            setNoGravity(true);
            hasPhysics = false;
            collidesWithEntities = false;

            final InteractionMeta meta = getEntityMeta();
            meta.setWidth(width);
            meta.setHeight(height);

            setBoundingBox(width, height, width);
        }

        @Override
        public @NotNull InteractionMeta getEntityMeta() {
            return (InteractionMeta) super.getEntityMeta();
        }

        @Override
        public void onBuildRightClick(@NotNull MapWorld world, @NotNull Player player, @NotNull PlayerHand hand, @NotNull Point interactPosition) {
            var min = new Vec(-this.getEntityMeta().getWidth() / 2, 0, -this.getEntityMeta().getWidth() / 2);
            var max = new Vec(this.getEntityMeta().getWidth() / 2, this.getEntityMeta().getHeight(), this.getEntityMeta().getWidth() / 2);

            player.sendPacket(new BundlePacket());
            new AxiomClientboundMarkerDataPacket(List.of(
                    new AxiomClientboundMarkerDataPacket.Entry(
                            this.getUuid(),
                            this.getPosition(),
                            "dasdas",
                            min, max,
                            AlphaColor.WHITE, AlphaColor.WHITE, 1
                    )
            ), List.of()).send(player);
            new AxiomClientboundMarkerResponsePacket(
                    this.getUuid(),
                    CompoundBinaryTag.empty()
            ).send(player);
//            new AxiomClientboundMarkerDataPacket(List.of(), List.of(this.getUuid())).send(player);
            player.sendPacket(new BundlePacket());
        }
    }
}
